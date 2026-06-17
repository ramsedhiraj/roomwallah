package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.user.entity.User;

import java.util.List;

public interface OwnerPropertyService {
    List<Property> getOwnerProperties(User owner);
    long countActiveListings(User owner);
}
