package com.roomwallah.partner.controller;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partner/properties")
@RequiredArgsConstructor
public class PartnerSearchController {

    private final PropertyRepository propertyRepository;

    private void checkScope(String requiredScope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        boolean hasScope = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("SCOPE_" + requiredScope) || a.equalsIgnoreCase("SCOPE_READ"));
        if (!hasScope) {
            throw new AccessDeniedException("Access Denied: Missing scope " + requiredScope);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Property>> searchProperties(@RequestParam(required = false) String city) {
        checkScope("READ");
        List<Property> properties = propertyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .filter(p -> city == null || (p.getAddress() != null && city.equalsIgnoreCase(p.getAddress().getCity())))
                .toList();
        return ResponseEntity.ok(properties);
    }
}
