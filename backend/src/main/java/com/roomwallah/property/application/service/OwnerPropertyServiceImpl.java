package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerPropertyServiceImpl implements OwnerPropertyService {

    private final PropertyRepository propertyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Property> getOwnerProperties(User owner) {
        return propertyRepository.findByOwnerIdAndDeletedFalse(owner.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveListings(User owner) {
        return propertyRepository.countByOwnerIdAndStatusAndDeletedFalse(owner.getId(), PropertyStatus.ACTIVE);
    }
}
