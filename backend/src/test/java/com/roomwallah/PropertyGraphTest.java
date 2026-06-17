package com.roomwallah;

import com.roomwallah.graph.PropertyGraphService;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.valueobject.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PropertyGraphTest {

    @Mock
    private PropertyRepository propertyRepository;

    private PropertyGraphService propertyGraphService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        propertyGraphService = new PropertyGraphService(propertyRepository);
    }

    @Test
    public void testKnowledgeGraphTraversal() {
        UUID propertyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Property property = new Property();
        property.setId(propertyId);
        property.setTitle("Luxury Apartment Noida");
        property.setOwnerId(ownerId);
        property.setAddress(new Address("Noida Sec 137", "Noida", "UP", "India", "201305", "Noida"));
        property.setAmenities(Set.of("Gym", "Swimming Pool", "Power Backup"));

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        var result = propertyGraphService.traverseGraph(propertyId);

        assertNotNull(result);
        
        // Assert nodes list exists and maps correct types
        assertNotNull(result.getNodes());
        assertTrue(result.getNodes().size() >= 5);
        
        boolean hasProperty = result.getNodes().stream().anyMatch(n -> "PROPERTY".equals(n.getType()));
        boolean hasOwner = result.getNodes().stream().anyMatch(n -> "OWNER".equals(n.getType()));
        boolean hasLocality = result.getNodes().stream().anyMatch(n -> "LOCALITY".equals(n.getType()));
        boolean hasAmenity = result.getNodes().stream().anyMatch(n -> "AMENITY".equals(n.getType()));
        boolean hasSchool = result.getNodes().stream().anyMatch(n -> "SCHOOL".equals(n.getType()));
        boolean hasTransit = result.getNodes().stream().anyMatch(n -> "TRANSIT".equals(n.getType()));

        assertTrue(hasProperty);
        assertTrue(hasOwner);
        assertTrue(hasLocality);
        assertTrue(hasAmenity);
        assertTrue(hasSchool);
        assertTrue(hasTransit);

        // Assert edges (relationships)
        assertNotNull(result.getEdges());
        assertTrue(result.getEdges().size() >= 4);

        boolean ownsRelation = result.getEdges().stream().anyMatch(e -> "OWNED_BY".equals(e.getRelationship()));
        boolean locatedRelation = result.getEdges().stream().anyMatch(e -> "LOCATED_IN".equals(e.getRelationship()));
        boolean amenityRelation = result.getEdges().stream().anyMatch(e -> "HAS_AMENITY".equals(e.getRelationship()));

        assertTrue(ownsRelation);
        assertTrue(locatedRelation);
        assertTrue(amenityRelation);

        // Assert explanation strings are filled for AI assistant retrieval
        assertNotNull(result.getExplanationPaths());
        assertFalse(result.getExplanationPaths().isEmpty());
    }
}
