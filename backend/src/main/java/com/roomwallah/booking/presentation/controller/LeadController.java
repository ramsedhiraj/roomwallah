package com.roomwallah.booking.presentation.controller;

import com.roomwallah.booking.application.facade.BookingFacade;
import com.roomwallah.booking.presentation.dto.LeadRequestDto;
import com.roomwallah.booking.presentation.dto.LeadResponseDto;
import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final BookingFacade bookingFacade;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponseDto>> createLead(@Valid @RequestBody LeadRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to create/get lead from tenant: {} for property: {}", currentUser.getId(), request.getPropertyId());

        String phone = request.getContactPhone() != null ? request.getContactPhone() : currentUser.getPhone();
        String email = request.getContactEmail() != null ? request.getContactEmail() : currentUser.getEmail();

        LeadResponseDto response = bookingFacade.getOrCreateLead(
                request.getPropertyId(),
                currentUser.getId(),
                request.getOwnerId(),
                request.getInquiryText(),
                phone,
                email
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lead interest recorded successfully"));
    }

    @GetMapping("/owner")
    public ResponseEntity<ApiResponse<List<LeadResponseDto>>> getOwnerLeads() {
        User currentUser = currentUserProvider.getCurrentUser();
        log.info("Request to get leads for owner: {}", currentUser.getId());
        
        List<LeadResponseDto> leads = bookingFacade.getOwnerLeads(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(leads, "Owner leads retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDto>> getLead(@PathVariable UUID id) {
        log.info("Request to get lead details: {}", id);
        LeadResponseDto lead = bookingFacade.getLead(id);
        return ResponseEntity.ok(ApiResponse.success(lead));
    }
}
