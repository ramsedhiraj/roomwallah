package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.TrendingQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendingQueryRepository extends JpaRepository<TrendingQuery, UUID> {

    List<TrendingQuery> findByCityOrderBySearchCountDesc(String city);

    List<TrendingQuery> findTop20ByOrderBySearchCountDesc();

    Optional<TrendingQuery> findByQueryTextAndCity(String queryText, String city);
}
