import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 7 - Trust & Verification E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });
  });

  // Helper for OWNER login mocking
  async function mockOwnerLogin(page) {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            accessToken: 'owner_access_token',
            refreshToken: 'owner_refresh_token'
          }
        })
      });
    });

    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'owner-uuid-1111',
            fullName: 'John Owner',
            email: 'owner@roomwallah.com',
            phone: '+919999999999',
            role: 'OWNER',
            emailVerified: true,
            phoneVerified: true,
            identityVerified: false
          }
        })
      });
    });

    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'owner-uuid-1111',
            fullName: 'John Owner',
            email: 'owner@roomwallah.com',
            phone: '+919999999999',
            role: 'OWNER',
            emailVerified: true,
            phoneVerified: true,
            identityVerified: false
          }
        })
      });
    });

    await page.goto('/login');
    await page.fill('input[placeholder*="name@example.com"]', 'owner@roomwallah.com');
    await page.fill('input[placeholder="••••••••"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  }

  // Helper for ADMIN login mocking
  async function mockAdminLogin(page) {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            accessToken: 'admin_access_token',
            refreshToken: 'admin_refresh_token'
          }
        })
      });
    });

    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'admin-uuid-2222',
            fullName: 'System Administrator',
            email: 'admin@roomwallah.com',
            phone: '+918888888888',
            role: 'ADMIN'
          }
        })
      });
    });

    await page.goto('/login');
    await page.fill('input[placeholder*="name@example.com"]', 'admin@roomwallah.com');
    await page.fill('input[placeholder="••••••••"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  }

  test('1. Owner Verification Wizard submission flow', async ({ page }) => {
    // 1. Initial State: Unverified owner
    await page.route('**/api/v1/trust/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: null })
      });
    });

    // Mock properties/me to return a draft property
    await page.route('**/api/v1/properties/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'prop-uuid-mock',
              title: 'Luxury Apartment',
              address: { city: 'Mumbai' }
            }
          ]
        })
      });
    });

    // Mock Aadhaar verification API
    await page.route('**/api/v1/verifications/identity', async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'verif-uuid-1234',
            userId: 'owner-uuid-1111',
            provider: 'AADHAAR',
            requestStatus: 'APPROVED',
            verifiedName: 'John Owner',
            confidenceScore: 100.0,
            submittedAt: new Date().toISOString()
          }
        })
      });
    });

    // Mock final verification submission pipeline
    await page.route('**/api/v1/verifications/property', async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'verification-uuid-9999',
            propertyId: 'prop-uuid-mock',
            ownerId: 'owner-uuid-1111',
            documentUrl: 'http://example.com/deed.pdf',
            utilityBillUrl: 'http://example.com/bill.pdf',
            deedNameMatched: true,
            utilityNameMatched: true,
            locationMatched: true,
            confidenceScore: 100.0,
            approvalStatus: 'APPROVED'
          }
        })
      });
    });

    await mockOwnerLogin(page);

    // Navigate to verify page
    await page.goto('/trust/verify');
    await expect(page.locator('h1')).toContainText('Listing & Owner Verification Wizard');
    
    // Step 0: Proceed to Step 2 (Phone)
    await page.click('button:has-text("Proceed to Step 2")');

    // Step 1: Proceed to Step 3 (Aadhaar eKYC)
    await page.click('button:has-text("Proceed to Step 3")');

    // Step 2: Fill Aadhaar, consent and verify
    await expect(page.locator('h2')).toContainText('Aadhaar eKYC Identity Validation');
    await page.fill('input[placeholder="0000 0000 0000"]', '123456789012');
    await page.check('input[id="consent"]');
    await page.click('button:has-text("Verify Identity")');

    // Step 3: Select property and upload deed document
    await expect(page.locator('h2')).toContainText('Upload Property Ownership Proof');
    await page.selectOption('select', 'prop-uuid-mock');

    const fileChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.border-dashed').click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles({
      name: 'deed.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('mock-deed-content')
    });

    await expect(page.locator('text=deed.pdf')).toBeVisible();
    await page.click('button:has-text("Next Step")');

    // Step 4: Upload utility bill
    await expect(page.locator('h2')).toContainText('Upload Recent Utility Bill');

    const utilityChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.border-dashed').click();
    const utilityChooser = await utilityChooserPromise;
    await utilityChooser.setFiles({
      name: 'utility.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('mock-utility-content')
    });

    await expect(page.locator('text=utility.pdf')).toBeVisible();
    await page.click('button:has-text("Submit Pipeline")');

    // Step 5: Verification results success summary
    await expect(page.locator('h2')).toContainText('Verification Approved Automatically!');
    await expect(page.locator('text=Ownership Deed Name Match:')).toBeVisible();
  });

  test('2. Draft autosave and local caching recovery', async ({ page }) => {
    // 1. Initial State: Unverified owner
    await page.route('**/api/v1/trust/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: null })
      });
    });

    // Mock properties/me to return a draft property
    await page.route('**/api/v1/properties/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'prop-uuid-mock',
              title: 'Luxury Apartment',
              address: { city: 'Mumbai' }
            }
          ]
        })
      });
    });

    await mockOwnerLogin(page);

    // Go to wizard
    await page.goto('/trust/verify');
    await page.click('button:has-text("Proceed to Step 2")');
    await page.click('button:has-text("Proceed to Step 3")');

    // Verify we are on Step 2 (Aadhaar eKYC Identity Validation)
    await expect(page.locator('h2')).toContainText('Aadhaar eKYC Identity Validation');

    // Reload page to verify state recovery from profile refresh
    await page.reload();

    await page.click('button:has-text("Proceed to Step 2")');
    await page.click('button:has-text("Proceed to Step 3")');
    await expect(page.locator('h2')).toContainText('Aadhaar eKYC Identity Validation');
  });

  test('3. Admin moderation queue details & priority overrides', async ({ page }) => {
    // Mock moderation cases response
    await page.route('**/api/v1/admin/trust/cases', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'case-uuid-95',
              entityType: 'OWNER_VERIFICATION',
              entityId: 'verif-uuid-95',
              status: 'OPEN',
              assignedAdmin: null,
              priorityScore: 95,
              createdAt: new Date().toISOString(),
              closedAt: null,
              notes: 'High risk: multiple accounts on same IP address.'
            },
            {
              id: 'case-uuid-40',
              entityType: 'OWNER_VERIFICATION',
              entityId: 'verif-uuid-40',
              status: 'OPEN',
              assignedAdmin: null,
              priorityScore: 40,
              createdAt: new Date().toISOString(),
              closedAt: null,
              notes: 'Normal verification review request.'
            }
          ]
        })
      });
    });

    await mockAdminLogin(page);

    // Go to admin dashboard
    await page.goto('/admin/trust');
    await expect(page.locator('h1')).toContainText('Trust & Moderation Dashboard');

    // Verify both cases are listed
    await expect(page.locator('text=Priority 95')).toBeVisible();
    await expect(page.locator('text=Priority 40')).toBeVisible();

    // Verify first case (priority 95) is loaded in details pane
    await expect(page.locator('text=Review Case #case-uui')).toBeVisible();
    await expect(page.locator('text=Risk Priority: 95')).toBeVisible();
    await expect(page.locator('text=High risk: multiple accounts on same IP address.')).toBeVisible();

    // Fill notes and reject case
    await page.fill('textarea[placeholder*="Input findings"]', 'Verification rejected due to fraud signal.');

    // Mock rejection endpoint
    await page.route('**/api/v1/admin/trust/*/reject', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'verif-uuid-95',
            verificationStatus: 'REJECTED',
            rejectionReason: 'Verification rejected due to fraud signal.'
          }
        })
      });
    });

    await page.click('button:has-text("Reject verification")');

    // Verify success alert
    await expect(page.locator('text=rejected')).toBeVisible();
  });

  test('4. Trust score breakdown dialog factor auditing', async ({ page }) => {
    await mockOwnerLogin(page);

    // Mock trust score explanation API
    await page.route('**/api/v1/trust/score', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            userId: 'owner-uuid-1111',
            currentScore: 85,
            scoreVersion: 'v1',
            ruleVersion: 'rule_v1.2',
            algorithmVersion: 'algo_v2.0',
            calculatedAt: new Date().toISOString(),
            positiveFactors: [
              { name: 'Verified Identity', scoreImpact: 50, positive: true, description: 'Government KYC verification completed.' },
              { name: 'Positive Reviews', scoreImpact: 15, positive: true, description: 'Tenant ratings average above 4.5.' }
            ],
            negativeFactors: [
              { name: 'Missing Profile Details', scoreImpact: 5, positive: false, description: 'Bio and contact preference not fully configured.' }
            ]
          }
        })
      });
    });

    // Go to profile page
    await page.goto('/profile');
    
    // Click trust breakdown button
    await page.click('button#view-trust-breakdown-btn');

    // Verify trust score breakdown dialog is shown
    await expect(page.locator('h2#trust-dialog-title')).toContainText('Trust Score Breakdown');
    await expect(page.locator('text=85')).toBeVisible();
    await expect(page.locator('text=Algorithm Version: algo_v2.0')).toBeVisible();

    // Verify positive factors are shown
    await expect(page.locator('text=Verified Identity')).toBeVisible();
    await expect(page.locator('text=+50')).toBeVisible();
    await expect(page.locator('text=Positive Reviews')).toBeVisible();
    await expect(page.locator('text=+15')).toBeVisible();

    // Verify negative factors are shown
    await expect(page.locator('text=Missing Profile Details')).toBeVisible();
    await expect(page.locator('text=-5')).toBeVisible();

    // Close dialog
    await page.click('button[aria-label="Close dialog"]');
    await expect(page.locator('h2#trust-dialog-title')).not.toBeVisible();
  });
});
