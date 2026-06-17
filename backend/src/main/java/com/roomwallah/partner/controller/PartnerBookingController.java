package com.roomwallah.partner.controller;

import com.roomwallah.booking.application.service.BookingService;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.presentation.dto.BookingRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partner/bookings")
@RequiredArgsConstructor
public class PartnerBookingController {

    private final BookingService bookingService;

    private void checkScope(String requiredScope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        boolean hasScope = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("SCOPE_" + requiredScope));
        if (!hasScope) {
            throw new AccessDeniedException("Access Denied: Missing scope " + requiredScope);
        }
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam UUID tenantId,
            @RequestBody BookingRequestDto request
    ) {
        checkScope("BOOKING");
        Booking booking = bookingService.createBooking(tenantId, request);
        return ResponseEntity.ok(booking);
    }
}
