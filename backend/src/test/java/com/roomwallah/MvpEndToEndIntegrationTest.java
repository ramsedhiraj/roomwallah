package com.roomwallah;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.presentation.dto.LoginRequest;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.property.domain.entity.ListingPurpose;
import com.roomwallah.property.domain.entity.PropertyType;
import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.property.presentation.dto.MoneyDto;
import com.roomwallah.property.presentation.dto.AddressDto;
import com.roomwallah.property.domain.entity.AreaUnit;
import com.roomwallah.property.presentation.dto.AreaMeasurementDto;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.entity.User;
import com.roomwallah.booking.presentation.dto.LeadRequestDto;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MvpEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Test
    public void executeMvpFlow() throws Exception {
        // 1. Register Owner
        RegisterRequest ownerReg = RegisterRequest.builder()
                .fullName("Test Owner")
                .email("owner.e2e@example.com")
                .phone("+918888888888")
                .password("Password123!")
                .role(UserRole.OWNER)
                .build();
        
        MvcResult ownerRegResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isOk())
                .andReturn();
                
        String ownerIdStr = JsonPath.read(ownerRegResult.getResponse().getContentAsString(), "$.data.id");
        UUID ownerId = UUID.fromString(ownerIdStr);

        User ownerUser = userRepository.findById(ownerId).orElseThrow();
        ownerUser.setEmailVerified(true);
        userRepository.save(ownerUser);

        // 2. Login Owner
        LoginRequest ownerLogin = new LoginRequest("owner.e2e@example.com", "Password123!");
        MvcResult ownerLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String ownerToken = JsonPath.read(ownerLoginResult.getResponse().getContentAsString(), "$.data.accessToken");


        // 3. Create Property
        AddressDto address = new AddressDto("Apt 1", "Main St", "City", "State", "Country", "123456");
        CreatePropertyRequest createProp = CreatePropertyRequest.builder()
                .title("Luxury Apartment")
                .propertyType(PropertyType.APARTMENT)
                .listingPurpose(ListingPurpose.RENT)
                .price(new MoneyDto(new BigDecimal("50000"), "INR"))
                .address(address)
                .area(new AreaMeasurementDto(new BigDecimal("1000.0"), AreaUnit.SQ_FT))
                .build();

        MvcResult createPropResult = mockMvc.perform(post("/api/v1/properties")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        
        String propertyIdStr = JsonPath.read(createPropResult.getResponse().getContentAsString(), "$.data.id");
        UUID propertyId = UUID.fromString(propertyIdStr);

        // 4. Submit Property
        mockMvc.perform(post("/api/v1/properties/" + propertyId + "/submit")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // 5. Register Admin
        RegisterRequest adminReg = RegisterRequest.builder()
                .fullName("Test Admin")
                .email("admin.e2e@example.com")
                .phone("+917777777777")
                .password("Password123!")
                .role(UserRole.ADMIN)
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReg)))
                .andExpect(status().isOk());

        User adminUser = userRepository.findByEmail("admin.e2e@example.com").orElseThrow();
        adminUser.setEmailVerified(true);
        userRepository.save(adminUser);

        // Login Admin
        LoginRequest adminLogin = new LoginRequest("admin.e2e@example.com", "Password123!");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(adminLoginResult.getResponse().getContentAsString(), "$.data.accessToken");

        // 6. Admin Approve Property
        
        // Setup: Verify owner email and phone directly in the DB to satisfy publication rules
        User owner = userRepository.findById(ownerId).orElseThrow();
        owner.setEmailVerified(true);
        owner.setPhoneVerified(true);
        owner.setIdentityVerified(true);
        userRepository.save(owner);

        mockMvc.perform(post("/api/v1/properties/" + propertyId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 7. Register Tenant
        RegisterRequest tenantReg = RegisterRequest.builder()
                .fullName("Test Tenant")
                .email("tenant.e2e@example.com")
                .phone("+916666666666")
                .password("Password123!")
                .role(UserRole.TENANT)
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantReg)))
                .andExpect(status().isOk());

        User tenantUser = userRepository.findByEmail("tenant.e2e@example.com").orElseThrow();
        tenantUser.setEmailVerified(true);
        userRepository.save(tenantUser);

        // Login Tenant
        LoginRequest tenantLogin = new LoginRequest("tenant.e2e@example.com", "Password123!");
        MvcResult tenantLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenantLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String tenantToken = JsonPath.read(tenantLoginResult.getResponse().getContentAsString(), "$.data.accessToken");

        // 8. Tenant Search Property
        mockMvc.perform(get("/api/v1/search?propertyType=APARTMENT")
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                // Ensure search works and doesn't fail
                .andExpect(jsonPath("$.data").exists());

        // 9. Tenant Get Property
        mockMvc.perform(get("/api/v1/properties/" + propertyId)
                .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(propertyId.toString()));

        // 10. Tenant Contact Owner
        LeadRequestDto leadRequest = LeadRequestDto.builder()
                .propertyId(propertyId)
                .ownerId(ownerId)
                .inquiryText("I would like to rent this place.")
                .contactPhone("+916666666666")
                .contactEmail("tenant.e2e@example.com")
                .build();

        mockMvc.perform(post("/api/v1/leads")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.propertyId").value(propertyId.toString()));
                
        // Verify Owner sees lead
        mockMvc.perform(get("/api/v1/leads/owner")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].propertyId").value(propertyId.toString()));
    }
}
