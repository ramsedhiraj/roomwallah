# MVP Minimum - 30-Day Launch Plan

**Goal:** Launch a functional "Broker-Resistant" directory for 10 Property Owners and 100 Tenants.  
**Focus:** Reliability and Simplicity. No AI, No Escrow, No OCR.

---

## 1. Feature Readiness Dashboard

| Feature | Completion % | Missing Tasks | Effort |
| :--- | :---: | :--- | :--- |
| **User Auth** | 90% | Cleanup error messages, Password reset flow. | 1 day |
| **Property Listing** | 85% | Multi-photo upload (local storage), Validation. | 3 days |
| **Property Search** | 75% | Multi-filter SQL support (Price, Type, City). | 2 days |
| **Property Details** | 90% | Final UI polish for amenities and maps. | 1 day |
| **Contact Owner** | 30% | "I'm Interested" button -> Create Lead -> Email Owner. | 4 days |
| **Admin Moderation** | 50% | Simple UI list to Approve/Reject properties. | 3 days |

---

## 2. Technical Execution (The "Launch" Path)

### 🟢 User Registration & Login (90% Ready)
- **Current State:** JWT and JPA persistence are solid.
- **Missing:** Simple frontend validation (email format, password strength).
- **Blocking Dependency:** None.

### 🟢 Property Listing & Details (85% Ready)
- **Current State:** CRUD and DTO mapping are functional.
- **Missing:** Ensure `LocalStorageAdapter` actually saves files to a disk folder (un-mock).
- **Blocking Dependency:** Storage directory permissions on server.

### 🟡 Property Search (75% Ready)
- **Current State:** Basic SQL search exists.
- **Missing:** Add price range and property type filters to `PostgreSqlSearchAdapter`.
- **Blocking Dependency:** None.

### 🔴 Contact Owner (30% Ready)
- **Current State:** `Lead` entity exists, but no user-facing "Contact" flow.
- **Missing:** 
    1. Create `POST /api/v1/leads` to link Tenant to Property.
    2. Configure a real SMTP server (SendGrid/Mailgun) in `application.yml`.
    3. Add "Contact Owner" button to `PropertyDetailPage.tsx`.
- **Blocking Dependency:** SMTP Credentials.

### 🔴 Admin Moderation (40% Ready)
- **Current State:** Controllers exist but frontend is mostly mock data.
- **Missing:** 
    1. Connect `AdminModerationDashboard.tsx` to the `PropertyController` admin endpoints.
    2. Implementation of `property.transitionTo(ACTIVE)` in service layer.
- **Blocking Dependency:** None.

---

## 3. 30-Day Execution Roadmap

### Week 1: Core Hardening
- **Days 1-3:** Implement real Local Storage for media (no more `return null` in storage).
- **Days 4-5:** Finalize the Property Listing wizard on the frontend.
- **Days 6-7:** Setup Production Database (PostgreSQL) and Flyway baseline.

### Week 2: Search & Discovery
- **Days 8-10:** Build the multi-filter Search API (Price/City/Type).
- **Days 11-13:** Polish the Search Results and Property Detail pages.
- **Days 14:** Internal "Alpha" testing of the Listing-to-Search flow.

### Week 3: Interaction & Moderation
- **Days 15-18:** Build the "Contact Owner" lead generation flow + Email notifications.
- **Days 19-21:** Build the Admin Moderation dashboard (Approve/Reject buttons).
- **Days 22-23:** Password Reset and Profile management cleanup.

### Week 4: Deployment & Launch
- **Days 24-25:** Deployment to DigitalOcean/Render (Manual deploy, not enterprise CI/CD).
- **Days 26-28:** Bug bashing and mobile responsiveness checks.
- **Days 29:** Final Sanity Check (Smoke tests).
- **Day 30:** **MVP LAUNCH.**

---

## 4. MVP Success Criteria
1. **Security**: Users can register and login securely.
2. **Value**: Owners can list a property and have it approved by an Admin.
3. **Connectivity**: Tenants can find a property and send their contact info to the Owner.
4. **Reliability**: No `LazyInitializationException` or `return null` in core paths.
