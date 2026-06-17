# Property Submission Report

**Date:** June 17, 2026
**Status:** SUCCESS
**Role:** Full-Stack Engineering

---

## 1. Summary of Changes
Implemented the missing "Submit for Review" workflow, allowing property owners to transition their listings from `DRAFT` to `PENDING_VERIFICATION`. This bridges a critical gap identified in the user journey audit.

---

## 2. Frontend Implementations

### UI Updates (`PropertyMediaManagerPage.tsx`)
- **Action Button**: Added a "Submit for Review" button to the header area, visible only when the property is in the `DRAFT` status.
- **State Management**: Introduced `actionLoading` state to disable the button and show a spinner during the API call, preventing double submissions.
- **Confirmation Flow**: Added a browser `window.confirm` dialog to ensure the user intentionally wants to lock their property for review.
- **Feedback**: Added success and error notifications upon completion of the API call, followed by a data refresh to reflect the new `PENDING_VERIFICATION` status.

---

## 3. Backend Implementations & Testing

### Verification
- **Endpoint**: Verified that `POST /api/v1/properties/{id}/submit` exists in the `PropertyController` and functions correctly.
- **Integration Test**: Created `PropertyControllerTest.java` as a `@SpringBootTest` utilizing `MockMvc`.
    - Simulated an authenticated owner submitting a property.
    - Verified the `200 OK` response.
    - Verified the JSON payload structure ensures the status is updated to `PENDING_VERIFICATION`.

---

## 4. Next Steps
- **Admin Flow Check**: Ensure that properties in `PENDING_VERIFICATION` successfully appear in the `/admin/verifications` dashboard (verified conceptually in previous audits, but should be smoke-tested end-to-end).
- **Email Notifications**: Consider adding an event listener to send an email to the owner confirming their submission is under review.
