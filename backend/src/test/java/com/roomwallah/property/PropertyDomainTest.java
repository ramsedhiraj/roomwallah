package com.roomwallah.property;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.entity.PropertyVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyDomainTest {

    @Test
    public void testPropertyInitialStatusAndDefaults() {
        Property property = new Property();
        assertEquals(PropertyStatus.DRAFT, property.getStatus());
        assertEquals(PropertyVisibility.PUBLIC, property.getVisibility());
        assertNull(property.getPublishedAt());
        assertNull(property.getVerifiedAt());
        assertNull(property.getArchivedAt());
        assertNull(property.getSlug());
    }

    @Test
    public void testValidStatusTransitions() {
        Property property = new Property();
        
        // DRAFT -> PENDING_VERIFICATION
        property.transitionTo(PropertyStatus.PENDING_VERIFICATION);
        assertEquals(PropertyStatus.PENDING_VERIFICATION, property.getStatus());

        // PENDING_VERIFICATION -> ACTIVE
        property.transitionTo(PropertyStatus.ACTIVE);
        assertEquals(PropertyStatus.ACTIVE, property.getStatus());

        // ACTIVE -> PAUSED
        property.transitionTo(PropertyStatus.PAUSED);
        assertEquals(PropertyStatus.PAUSED, property.getStatus());

        // PAUSED -> ACTIVE
        property.transitionTo(PropertyStatus.ACTIVE);
        assertEquals(PropertyStatus.ACTIVE, property.getStatus());

        // ACTIVE -> ARCHIVED
        property.transitionTo(PropertyStatus.ARCHIVED);
        assertEquals(PropertyStatus.ARCHIVED, property.getStatus());
    }

    @Test
    public void testInvalidStatusTransitions() {
        Property property = new Property();

        // DRAFT cannot go directly to ACTIVE
        assertThrows(IllegalStateException.class, () -> property.transitionTo(PropertyStatus.ACTIVE));

        // PENDING_VERIFICATION cannot go directly to PAUSED
        property.transitionTo(PropertyStatus.PENDING_VERIFICATION);
        assertThrows(IllegalStateException.class, () -> property.transitionTo(PropertyStatus.PAUSED));

        // ARCHIVED is terminal
        property.transitionTo(PropertyStatus.ARCHIVED);
        assertThrows(IllegalStateException.class, () -> property.transitionTo(PropertyStatus.ACTIVE));
    }

    @Test
    public void testListingRefGeneration() {
        Property property = new Property();
        property.generateListingRef("Pune");
        assertNotNull(property.getListingRef());
        assertTrue(property.getListingRef().startsWith("RW-PUNE-"));
        
        Property propertySpecial = new Property();
        propertySpecial.generateListingRef("New York City!");
        assertTrue(propertySpecial.getListingRef().startsWith("RW-NEWYORKCITY-"));

        Property propertyEmpty = new Property();
        propertyEmpty.generateListingRef("   ");
        assertTrue(propertyEmpty.getListingRef().startsWith("RW-IND-"));
    }
}
