package com.roomwallah.wishlist;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyType;
import com.roomwallah.property.domain.entity.ListingPurpose;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import com.roomwallah.property.domain.valueobject.Money;
import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.repository.SearchDocumentRepository;
import com.roomwallah.search.presentation.dto.PropertyCardDto;
import com.roomwallah.wishlist.application.service.WishlistService;
import com.roomwallah.wishlist.domain.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WishlistIntegrationTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistItemRepository wishlistRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private SearchDocumentRepository searchDocumentRepository;

    private UUID userId;
    private Property activeProperty;

    @BeforeEach
    public void setUp() {
        wishlistRepository.deleteAll();
        searchDocumentRepository.deleteAll();

        userId = UUID.randomUUID();

        activeProperty = new Property();
        activeProperty.setOwnerId(UUID.randomUUID());
        activeProperty.setTitle("2 BHK Bandra");
        activeProperty.setPropertyType(PropertyType.APARTMENT);
        activeProperty.setListingPurpose(ListingPurpose.RENT);
        activeProperty.setStatus(PropertyStatus.ACTIVE);
        activeProperty.setPrice(new Money(BigDecimal.valueOf(50000), "INR"));
        activeProperty.setAddress(new Address("Line 1", "Bandra West", "Mumbai", "Maharashtra", "India", "400050"));
        activeProperty.setSlug("bhk-bandra-" + System.currentTimeMillis());
        activeProperty.setCreatedAt(Instant.now());
        activeProperty.setUpdatedAt(Instant.now());
        activeProperty.setPublishedAt(Instant.now());
        activeProperty.generateListingRef("Mumbai");
        activeProperty = propertyRepository.save(activeProperty);

        // Also save to search document so we can read card info
        SearchDocument doc = new SearchDocument();
        doc.setPropertyId(activeProperty.getId());
        doc.setOwnerId(activeProperty.getOwnerId());
        doc.setListingRef(activeProperty.getListingRef());
        doc.setSlug(activeProperty.getSlug());
        doc.setTitle(activeProperty.getTitle());
        doc.setCity(activeProperty.getAddress().getCity());
        doc.setLocality(activeProperty.getAddress().getLine2());
        doc.setPropertyType(activeProperty.getPropertyType().name());
        doc.setListingPurpose(activeProperty.getListingPurpose().name());
        doc.setPrice(activeProperty.getPrice().getAmount());
        doc.setPropertyStatus(activeProperty.getStatus().name());
        doc.setCreatedAt(activeProperty.getCreatedAt());
        doc.setUpdatedAt(activeProperty.getUpdatedAt());
        doc.setLastEventTimestamp(Instant.now());
        searchDocumentRepository.save(doc);
    }

    @Test
    public void testAddToWishlist_Success() {
        wishlistService.addToWishlist(userId, activeProperty.getId());
        assertThat(wishlistService.isInWishlist(userId, activeProperty.getId())).isTrue();

        List<PropertyCardDto> wishlist = wishlistService.getWishlist(userId);
        assertThat(wishlist).hasSize(1);
        assertThat(wishlist.get(0).getPropertyId()).isEqualTo(activeProperty.getId());
    }

    @Test
    public void testAddToWishlist_DeletedProperty_ThrowsException() {
        activeProperty.setDeleted(true);
        propertyRepository.save(activeProperty);

        assertThrows(IllegalArgumentException.class, () -> {
            wishlistService.addToWishlist(userId, activeProperty.getId());
        });
    }

    @Test
    public void testRemoveFromWishlist_Success() {
        wishlistService.addToWishlist(userId, activeProperty.getId());
        assertThat(wishlistService.isInWishlist(userId, activeProperty.getId())).isTrue();

        wishlistService.removeFromWishlist(userId, activeProperty.getId());
        assertThat(wishlistService.isInWishlist(userId, activeProperty.getId())).isFalse();
    }

    @Test
    public void testBatchCheckWishlist() {
        wishlistService.addToWishlist(userId, activeProperty.getId());
        UUID otherId = UUID.randomUUID();

        Set<UUID> result = wishlistService.batchCheckWishlist(userId, List.of(activeProperty.getId(), otherId));
        assertThat(result).containsExactly(activeProperty.getId());
    }
}
