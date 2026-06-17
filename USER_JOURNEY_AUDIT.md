# User Journey Audit - RoomWallah

**Status:** INCOMPLETE (65% Complete)  
**Role:** QA Lead & Product Manager Analysis  
**Goal:** Trace end-to-end flow from Owner onboarding to Tenant inquiry.

---

## 1. Journey Trace & Implementation Status

| Step | Backend Endpoint | Frontend Page | DB Tables | Status | Gaps / Bugs |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Owner Reg** | `POST /auth/register` | `/register` | `users`, `prefs` | 🟢 95% | None. |
| **2. Owner Login** | `POST /auth/login` | `/login` | `users`, `sessions`| 🟢 95% | None. |
| **3. Create Prop** | `POST /properties` | `/properties/create` | `properties` | 🟢 90% | Currently only saves as DRAFT. |
| **4. Upload Images** | `POST /media/upload` | `/properties/:id/media`| `property_media` | 🟡 80% | Uses `LocalMockObjectStorageAdapter`. |
| **5. Publish Prop** | `POST /properties/:id/submit` | `/properties/:id/edit` | `properties` | 🔴 20% | **UI BUG:** No "Submit for Review" button. |
| **6. Admin Approve** | `POST /admin/verifications/properties/:id/approve` | `/admin/verifications` | `properties` | 🟢 90% | Logic is sound and connected. |
| **7. Tenant Reg** | `POST /auth/register` | `/register` | `users` | 🟢 95% | Same as owner. |
| **8. Search Prop** | `GET /search` | `/search` | `properties` | 🟢 90% | Functional SQL search. |
| **9. View Prop** | `GET /properties/:id` | `/properties/:id` | `properties` | 🟢 95% | Functional. |
| **10. Contact Owner** | **MISSING** | `/properties/:id` | `leads` | 🔴 0% | **MISSING:** Lead Controller & UI Button. |

---

## 2. Blocking Issues

1.  **Broken "Publish" Workflow**: Owners can create drafts and upload images, but there is no button in the UI to submit the property for admin moderation. The backend endpoint exists, but the user is stuck in "Draft" mode.
2.  **Missing Lead Conversion**: Tenants can find properties but have no way to express interest or contact the owner. The `LeadService` is implemented in the backend, but it is not exposed via a REST Controller.
3.  **Mock Storage URLs**: Media uploads return a hardcoded Amazon S3 URL even when using local storage, which will break image rendering in local development unless a real bucket is configured.

---

## 3. Required Modifications

### Backend (Missing API)
- **File:** `backend/src/main/java/com/roomwallah/booking/presentation/controller/LeadController.java` (New File)
    - Need to expose `POST /api/v1/leads` to call `leadService.getOrCreateLead`.
    - Need to expose `GET /api/v1/leads/owner` for the owner dashboard.

### Frontend (Missing UI Interactions)
- **File:** `frontend/src/pages/CreatePropertyPage.tsx` & `PropertyMediaManagerPage.tsx`
    - Add "Submit for Verification" button that calls `POST /api/v1/properties/${id}/submit`.
- **File:** `frontend/src/pages/PropertyDetailPage.tsx`
    - Add "Contact Owner" or "I'm Interested" button.
    - Implement a simple modal to collect inquiry text.
    - Call the (to-be-created) Lead API.

---

## 4. Final Assessment
**Journey Completion: 65%**  
The project has a very high-quality foundation for 8 out of 10 steps. However, the "last mile" of the journey (submitting for review and tenant-to-owner contact) is completely missing from the implementation. 

**Estimated Effort to reach 100% Core Flow:** 4-6 developer days.
