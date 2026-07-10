package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.entity.TrendingQuery;
import com.roomwallah.search.domain.model.SearchFilter;
import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.model.SortOption;
import com.roomwallah.search.domain.port.GeoSearchPort;
import com.roomwallah.search.domain.port.SearchEnginePort;
import com.roomwallah.search.domain.repository.TrendingQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component("postgreSqlSearchAdapter")
@RequiredArgsConstructor
@Slf4j
public class PostgreSqlSearchAdapter implements SearchEnginePort {

    private final EntityManager entityManager;
    private final GeoSearchPort geoSearchPort;
    private final com.roomwallah.search.infrastructure.config.SearchFeatureFlags featureFlags;
    private final TrendingQueryRepository trendingQueryRepository;

    @org.springframework.beans.factory.annotation.Value("${roomwallah.search.query-timeout-ms:1000}")
    private int queryTimeoutMs;

    @Override
    @Transactional(readOnly = true)
    public SearchResult search(SearchQuery query) {
        SearchFilter filter = query.getFilter() != null ? query.getFilter() : SearchFilter.builder().build();
        SortOption sort = query.getSort();
        int limit = query.getPage() != null ? query.getPage().getSize() : 20;
        String cursor = query.getPage() != null ? query.getPage().getCursor() : null;

        Map<String, Object> params = new HashMap<>();
        List<String> trendingTexts = new ArrayList<>();
        if (featureFlags.isTrendingBoostsEnabled()) {
            try {
                trendingTexts = trendingQueryRepository.findTop20ByOrderBySearchCountDesc().stream()
                        .map(TrendingQuery::getQueryText)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .toList();
            } catch (Exception e) {
                log.warn("Failed to fetch trending queries for boosts: {}", e.getMessage());
            }
        }

        boolean hasText = query.getText() != null && !query.getText().isBlank();
        boolean isTreatment = "TREATMENT".equalsIgnoreCase(query.getExperimentalBucket());
        boolean useRelevanceScore = hasText || !trendingTexts.isEmpty() || isTreatment || featureFlags.isHybridSearchEnabled();

        StringBuilder sql = new StringBuilder("SELECT sd.*");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM search_documents sd");

        if (useRelevanceScore) {
            sql.append(", (");
            if (hasText) {
                sql.append("ts_rank(to_tsvector('english', sd.title || ' ' || coalesce(sd.description, '')), plainto_tsquery('english', :textQuery))");
            } else {
                sql.append("0.0");
            }
            if (!trendingTexts.isEmpty()) {
                sql.append(" + (CASE ");
                for (int i = 0; i < Math.min(trendingTexts.size(), 5); i++) {
                    String paramName = "trending_" + i;
                    sql.append(" WHEN LOWER(sd.title) LIKE :").append(paramName).append(" THEN 0.2 ");
                    params.put(paramName, "%" + trendingTexts.get(i) + "%");
                }
                sql.append(" ELSE 0.0 END)");
            }
            if (featureFlags.isHybridSearchEnabled() && hasText) {
                sql.append(" + 0.15");
            }
            if (isTreatment) {
                sql.append(" + (CASE WHEN sd.owner_verified = true THEN 0.3 ELSE 0.0 END) + (sd.trust_score * 0.002)");
            }
            sql.append(") as relevance_score");
        }
        sql.append(" FROM search_documents sd");

        StringBuilder whereClause = new StringBuilder(" WHERE sd.property_status = 'ACTIVE'");

        if (hasText) {
            whereClause.append(" AND to_tsvector('english', sd.title || ' ' || coalesce(sd.description, '')) @@ plainto_tsquery('english', :textQuery)");
            params.put("textQuery", query.getText());
        }

        buildFilterConditions(whereClause, filter, params);

        CursorInfo cursorInfo = decodeCursor(cursor);
        if (cursorInfo != null) {
            applyCursorCondition(whereClause, cursorInfo, sort, hasText, params);
        }

        sql.append(whereClause);
        countSql.append(whereClause);

        // Apply ordering
        String sortField = sort != null ? sort.getField() : "createdAt";
        boolean asc = sort != null && sort.isAscending();
        if ("price".equalsIgnoreCase(sortField)) {
            sql.append(" ORDER BY sd.price ").append(asc ? "ASC" : "DESC").append(", sd.property_id ASC");
        } else if ("viewCount".equalsIgnoreCase(sortField)) {
            sql.append(" ORDER BY sd.view_count ").append(asc ? "ASC" : "DESC").append(", sd.property_id ASC");
        } else if ("trustScore".equalsIgnoreCase(sortField)) {
            sql.append(" ORDER BY sd.trust_score ").append(asc ? "ASC" : "DESC").append(", sd.property_id ASC");
        } else if ("publishedAt".equalsIgnoreCase(sortField)) {
            sql.append(" ORDER BY sd.published_at ").append(asc ? "ASC" : "DESC").append(", sd.property_id ASC");
        } else if ("relevance".equalsIgnoreCase(sortField) && useRelevanceScore) {
            sql.append(" ORDER BY relevance_score DESC, sd.property_id ASC");
        } else if (useRelevanceScore && sort == null) {
            sql.append(" ORDER BY relevance_score DESC, sd.property_id ASC");
        } else {
            sql.append(" ORDER BY sd.created_at ").append(asc ? "ASC" : "DESC").append(", sd.property_id ASC");
        }

        log.info("EXEC SQL Search Query: {}, Params: {}", sql.toString(), params);
        log.info("EXEC SQL Count Query: {}, Params: {}", countSql.toString(), params);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), SearchDocument.class);
        Query countQuery = entityManager.createNativeQuery(countSql.toString());

        // Apply query timeouts
        try {
            nativeQuery.setHint("jakarta.persistence.query.timeout", queryTimeoutMs);
            countQuery.setHint("jakarta.persistence.query.timeout", queryTimeoutMs);
        } catch (Exception e) {
            log.warn("Failed to set query timeout hints: {}", e.getMessage());
        }

        // Set parameters
        params.forEach((k, v) -> {
            nativeQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        nativeQuery.setMaxResults(limit + 1);

        @SuppressWarnings("unchecked")
        List<SearchDocument> results = nativeQuery.getResultList();
        long totalCount = ((Number) countQuery.getSingleResult()).longValue();

        boolean hasNext = results.size() > limit;
        List<SearchDocument> paginatedResults = hasNext ? results.subList(0, limit) : results;
        String nextCursor = null;
        if (!paginatedResults.isEmpty() && hasNext) {
            nextCursor = encodeCursor(paginatedResults.get(paginatedResults.size() - 1), sort, hasText);
        }

        // Populate explainable ranking if active
        if (featureFlags.isExplainRankingEnabled() && Boolean.TRUE.equals(query.getExplain())) {
            for (SearchDocument doc : paginatedResults) {
                Map<String, Object> explanation = new LinkedHashMap<>();
                boolean locationMatch = filter.getCity() != null && filter.getCity().equalsIgnoreCase(doc.getCity());
                explanation.put("locationMatch", locationMatch ? "Exact city match (+1.0)" : "No matching city filter");

                boolean budgetMatch = true;
                if (filter.getPriceRange() != null) {
                    BigDecimal price = doc.getPrice();
                    BigDecimal min = filter.getPriceRange().getMinPrice();
                    BigDecimal max = filter.getPriceRange().getMaxPrice();
                    if (min != null && price.compareTo(min) < 0) budgetMatch = false;
                    if (max != null && price.compareTo(max) > 0) budgetMatch = false;
                }
                explanation.put("budgetMatch", budgetMatch ? "Within user budget (+1.0)" : "Outside budget bounds");
                explanation.put("verifiedOwner", doc.isOwnerVerified() ? "Owner is identity-verified (+0.3)" : "Owner not verified (+0.0)");
                explanation.put("trustScoreBoost", "Trust score: " + doc.getTrustScore() + " (Weight: 0.002 per score in treatment)");

                boolean trendingMatch = false;
                if (featureFlags.isTrendingBoostsEnabled()) {
                    for (String trend : trendingTexts) {
                        if (doc.getTitle().toLowerCase().contains(trend)) {
                            trendingMatch = true;
                            break;
                        }
                    }
                }
                explanation.put("trendingBoost", trendingMatch ? "Title matches trending query terms (+0.2)" : "No trending terms matched (+0.0)");

                if (featureFlags.isHybridSearchEnabled() && hasText) {
                    explanation.put("hybridSearchBoost", "Hybrid vector similarity score: +0.15 (Placeholder)");
                }
                if (hasText) {
                    explanation.put("textRelevance", "Postgres FTS ts_rank score included");
                }
                explanation.put("experimentBucket", query.getExperimentalBucket() != null ? query.getExperimentalBucket() : "CONTROL");
                doc.setRankingExplanation(explanation);
            }
        }

        return new SearchResult(paginatedResults, nextCursor, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(SearchQuery query) {
        SearchFilter filter = query.getFilter() != null ? query.getFilter() : SearchFilter.builder().build();
        boolean hasText = query.getText() != null && !query.getText().isBlank();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM search_documents sd WHERE sd.property_status = 'ACTIVE'");
        Map<String, Object> params = new HashMap<>();

        if (hasText) {
            countSql.append(" AND to_tsvector('english', sd.title || ' ' || coalesce(sd.description, '')) @@ plainto_tsquery('english', :textQuery)");
            params.put("textQuery", query.getText());
        }

        buildFilterConditions(countSql, filter, params);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        try {
            countQuery.setHint("jakarta.persistence.query.timeout", queryTimeoutMs);
        } catch (Exception e) {
            log.warn("Failed to set count query timeout hint: {}", e.getMessage());
        }
        params.forEach(countQuery::setParameter);

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String providerName() {
        return "postgresql";
    }

    private void buildFilterConditions(StringBuilder where, SearchFilter filter, Map<String, Object> params) {
        if (filter.getCity() != null && !filter.getCity().isBlank()) {
            where.append(" AND sd.city = :city");
            params.put("city", filter.getCity());
        }
        if (filter.getLocality() != null && !filter.getLocality().isBlank()) {
            where.append(" AND sd.locality = :locality");
            params.put("locality", filter.getLocality());
        }
        if (filter.getPropertyType() != null && !filter.getPropertyType().isBlank()) {
            where.append(" AND sd.property_type = :propertyType");
            params.put("propertyType", filter.getPropertyType());
        }
        if (filter.getListingPurpose() != null && !filter.getListingPurpose().isBlank()) {
            where.append(" AND sd.listing_purpose = :listingPurpose");
            params.put("listingPurpose", filter.getListingPurpose());
        }
        if (filter.getPriceRange() != null) {
            if (filter.getPriceRange().getMinPrice() != null) {
                where.append(" AND sd.price >= :minPrice");
                params.put("minPrice", filter.getPriceRange().getMinPrice());
            }
            if (filter.getPriceRange().getMaxPrice() != null) {
                where.append(" AND sd.price <= :maxPrice");
                params.put("maxPrice", filter.getPriceRange().getMaxPrice());
            }
        }
        if (filter.getBedrooms() != null) {
            where.append(" AND sd.bedrooms = :bedrooms");
            params.put("bedrooms", filter.getBedrooms());
        }
        if (filter.getBathrooms() != null) {
            where.append(" AND sd.bathrooms = :bathrooms");
            params.put("bathrooms", filter.getBathrooms());
        }
        if (filter.getPetFriendly() != null) {
            where.append(" AND sd.pet_friendly = :petFriendly");
            params.put("petFriendly", filter.getPetFriendly());
        }
        if (filter.getOwnerVerified() != null) {
            where.append(" AND sd.owner_verified = :ownerVerified");
            params.put("ownerVerified", filter.getOwnerVerified());
        }
        if (filter.getGeoRadius() != null) {
            var radius = filter.getGeoRadius();
            if (radius.getLatitude() != null && radius.getLongitude() != null && radius.getRadiusKm() != null) {
                String distanceCondition = geoSearchPort.buildDistanceCondition(
                        "sd.latitude", "sd.longitude",
                        radius.getLatitude(), radius.getLongitude(),
                        radius.getRadiusKm()
                );
                where.append(" AND ").append(distanceCondition);
            }
        }
        if (filter.getFurnishingStatus() != null && !filter.getFurnishingStatus().isBlank()) {
            where.append(" AND sd.furnishing_status = :furnishingStatus");
            params.put("furnishingStatus", filter.getFurnishingStatus());
        }
        if (filter.getParkingCount() != null) {
            where.append(" AND sd.parking_count >= :parkingCount");
            params.put("parkingCount", filter.getParkingCount());
        }
        if (filter.getFacingDirection() != null && !filter.getFacingDirection().isBlank()) {
            where.append(" AND sd.facing_direction = :facingDirection");
            params.put("facingDirection", filter.getFacingDirection());
        }
        if (filter.getAvailabilityDate() != null) {
            where.append(" AND sd.availability_date <= :availabilityDate");
            params.put("availabilityDate", filter.getAvailabilityDate());
        }
        if (filter.getMinTrustScore() != null) {
            where.append(" AND sd.trust_score >= :minTrustScore");
            params.put("minTrustScore", filter.getMinTrustScore());
        }
        if (filter.getBboxSouthWestLat() != null && filter.getBboxSouthWestLon() != null &&
                filter.getBboxNorthEastLat() != null && filter.getBboxNorthEastLon() != null) {
            where.append(" AND sd.latitude BETWEEN :swLat AND :neLat AND sd.longitude BETWEEN :swLon AND :neLon");
            params.put("swLat", filter.getBboxSouthWestLat());
            params.put("neLat", filter.getBboxNorthEastLat());
            params.put("swLon", filter.getBboxSouthWestLon());
            params.put("neLon", filter.getBboxNorthEastLon());
        }
    }

    private void applyCursorCondition(StringBuilder where, CursorInfo cursor, SortOption sort, boolean hasText, Map<String, Object> params) {
        String sortField = sort != null ? sort.getField() : "createdAt";
        boolean asc = sort != null && sort.isAscending();

        if ("price".equalsIgnoreCase(sortField)) {
            BigDecimal price = new BigDecimal(cursor.value);
            where.append(String.format(" AND (sd.price %s :cursorPrice OR (sd.price = :cursorPrice AND sd.property_id > :cursorId))", asc ? ">" : "<"));
            params.put("cursorPrice", price);
        } else if ("viewCount".equalsIgnoreCase(sortField)) {
            int views = Integer.parseInt(cursor.value);
            where.append(String.format(" AND (sd.view_count %s :cursorView OR (sd.view_count = :cursorView AND sd.property_id > :cursorId))", asc ? ">" : "<"));
            params.put("cursorView", views);
        } else if ("trustScore".equalsIgnoreCase(sortField)) {
            int trust = Integer.parseInt(cursor.value);
            where.append(String.format(" AND (sd.trust_score %s :cursorTrust OR (sd.trust_score = :cursorTrust AND sd.property_id > :cursorId))", asc ? ">" : "<"));
            params.put("cursorTrust", trust);
        } else if ("publishedAt".equalsIgnoreCase(sortField)) {
            Instant published = Instant.parse(cursor.value);
            where.append(String.format(" AND (sd.published_at %s :cursorPub OR (sd.published_at = :cursorPub AND sd.property_id > :cursorId))", asc ? ">" : "<"));
            params.put("cursorPub", published);
        } else if ("relevance".equalsIgnoreCase(sortField) && hasText) {
            double rank = Double.parseDouble(cursor.value);
            where.append(" AND (ts_rank(to_tsvector('english', sd.title || ' ' || coalesce(sd.description, '')), plainto_tsquery('english', :textQuery)) < :cursorRank OR (ts_rank(to_tsvector('english', sd.title || ' ' || coalesce(sd.description, '')), plainto_tsquery('english', :textQuery)) = :cursorRank AND sd.property_id > :cursorId))");
            params.put("cursorRank", rank);
        } else {
            Instant created = Instant.parse(cursor.value);
            where.append(String.format(" AND (sd.created_at %s :cursorCreated OR (sd.created_at = :cursorCreated AND sd.property_id > :cursorId))", asc ? ">" : "<"));
            params.put("cursorCreated", created);
        }
        params.put("cursorId", cursor.id);
    }

    private CursorInfo decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = decoded.split("_", 2);
            if (parts.length == 2) {
                return new CursorInfo(parts[0], UUID.fromString(parts[1]));
            }
        } catch (Exception e) {
            log.warn("Failed to decode cursor: {}, ignoring", cursor, e);
        }
        return null;
    }

    private String encodeCursor(SearchDocument doc, SortOption sort, boolean hasText) {
        String sortField = sort != null ? sort.getField() : "createdAt";
        String val = "";

        if ("price".equalsIgnoreCase(sortField)) {
            val = doc.getPrice().toString();
        } else if ("viewCount".equalsIgnoreCase(sortField)) {
            val = String.valueOf(doc.getViewCount());
        } else if ("trustScore".equalsIgnoreCase(sortField)) {
            val = String.valueOf(doc.getTrustScore());
        } else if ("publishedAt".equalsIgnoreCase(sortField)) {
            val = doc.getPublishedAt() != null ? doc.getPublishedAt().toString() : "";
        } else {
            val = doc.getCreatedAt().toString();
        }

        String combined = val + "_" + doc.getPropertyId().toString();
        return Base64.getEncoder().encodeToString(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private record CursorInfo(String value, UUID id) {}
}
