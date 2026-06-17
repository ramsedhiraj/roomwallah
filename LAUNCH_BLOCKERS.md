# MVP Launch Blockers - RoomWallah

**Status:** RED (Not Launch Ready)  
**Date:** June 17, 2026

This document identifies every issue preventing a public MVP launch, classified by severity, with a clear path to resolution.

---

## 1. Critical Blockers (Severity 1)
*Must be fixed before internal beta.*

### 🔴 Architectural Conflict: Entity Collision
- **Issue:** Duplicate `TrustScore` entities in `com.roomwallah.trust` and `com.roomwallah.verification` mapping to the same `trust_scores` table.
- **Business Impact:** Potential data corruption; users see inconsistent trust scores; platform loses credibility.
- **Technical Impact:** Hibernate/JPA runtime crashes or non-deterministic persistence behavior.
- **Estimated Effort:** 3-5 days.
- **Recommended Fix:** Merge the two modules into a single `com.roomwallah.verification` bounded context. Unify the `TrustScore` entity and repository.

### 🔴 Security: Non-functional JWT Validation
- **Issue:** `JwtService.java` is a stub returning `null` for username extraction.
- **Business Impact:** Any user can potentially impersonate any other user or bypass security entirely.
- **Technical Impact:** Breaking change for all authenticated endpoints.
- **Estimated Effort:** 1 day.
- **Recommended Fix:** Delegate all JWT logic to the functional `AccessTokenServiceImpl` or implement the `JwtService` using the same secret/logic.

### 🔴 Functional: Stubbed Verification Engine
- **Issue:** All identity verification adapters (Passport, DigiLocker, PAN) throw `UnsupportedOperationException`.
- **Business Impact:** The "broker-resistant" promise is impossible to fulfill. No user can be verified.
- **Technical Impact:** Runtime crashes when users attempt the verification wizard.
- **Estimated Effort:** 10-14 days.
- **Recommended Fix:** Implement at least one real identity provider (e.g., Aadhaar via a 3rd party API) to enable the "Verified User" status.

---

## 2. High Priority (Severity 2)
*Must be fixed before public MVP.*

### 🟠 Payment Logic is Purely Mocked
- **Issue:** Stripe and Razorpay adapters return static strings like "ch_mock_123".
- **Business Impact:** No revenue can be collected; no escrow security for tenants.
- **Technical Impact:** No real integration with external webhooks or payment states.
- **Estimated Effort:** 7-10 days.
- **Recommended Fix:** Implement Stripe "Test Mode" integration including webhook handlers for `payment_intent.succeeded`.

### 🟠 Fraud & Risk Engine Gaps
- **Issue:** `FraudService` returns `null` for risk evaluations.
- **Business Impact:** High risk of platform abuse by brokers or scammers.
- **Technical Impact:** Silent failures in trust score calculations.
- **Estimated Effort:** 3-5 days.
- **Recommended Fix:** Implement a default "Low Risk" return object and basic rule-based checks (e.g., duplicate phone/email detection).

---

## 3. Medium Priority (Severity 3)
*Can be addressed during early beta.*

### 🟡 Large Frontend Bundle Size
- **Issue:** Chunks > 500kB due to 100+ scaffolded pages.
- **Business Impact:** Poor mobile experience; slow initial load times for users on 4G/5G.
- **Technical Impact:** Build warnings; performance degradation.
- **Estimated Effort:** 2-3 days.
- **Recommended Fix:** Implement React code-splitting (`React.lazy`) for routes and remove redundant/empty page scaffolds.

### 🟡 Media Processing Mocks
- **Issue:** Virus scanning and image optimization are mocked.
- **Business Impact:** Risk of malware uploads; high storage costs and slow page loads due to unoptimized images.
- **Technical Impact:** Storage bloat; security vulnerability.
- **Estimated Effort:** 3-5 days.
- **Recommended Fix:** Integrate a basic library (like Thumbnailator for Java) for image resizing and a cloud-based virus scanner.

---

## 4. Low Priority (Severity 4)
*Post-launch improvements.*

### 🔵 AI Feature Stubs
- **Issue:** Semantic search and recommendations use local database stubs.
- **Business Impact:** Search is functional but not "magical" as marketed.
- **Technical Impact:** None (graceful fallback to SQL search).
- **Estimated Effort:** 14+ days.
- **Recommended Fix:** Gradually replace SQL fallbacks with vector search (e.g., pgvector).

---

# Prioritized Launch Roadmap

## Phase 1: Stabilization (Weeks 1-2)
1. **Fix Architectural Collision:** Unify `trust` and `verification` packages.
2. **Harden Security:** Ensure JWT validation is functional and unified.
3. **Clean Scaffolds:** Remove or hide empty frontend routes to reduce bundle size.

## Phase 2: Core Functionality (Weeks 3-5)
1. **Real Verification:** Integrate one real Identity Verification provider.
2. **Functional Payments:** Implement Stripe Test Mode and Escrow logic.
3. **Property Health:** Fix the `LazyInitializationException` risks in Media and Amenities.

## Phase 3: Risk & Operations (Weeks 6-7)
1. **Fraud Engine:** Implement basic fraud rules for broker detection.
2. **Admin Tools:** Enable real moderation workflows for property approvals.
3. **E2E Testing:** Run Playwright suite against the functional flows.

## Phase 4: Beta Launch (Week 8)
1. **Deploy to Staging:** Full environment test on AWS/GCP.
2. **Internal Testing:** 10 users test the full flow from Listing -> Verification -> Booking.
3. **Public MVP Launch.**
