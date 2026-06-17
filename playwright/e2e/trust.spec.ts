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
            role: 'OWNER'
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

    await mockOwnerLogin(page);

    // Navigate to verify page
    await page.goto('/trust/verify');
    await expect(page.locator('h1')).toContainText('Owner Verification Center');
    
    // Step 0: Click Continue
    await page.click('button:has-text("Continue")');

    // Step 1: Select document type and upload ID Document
    await expect(page.locator('h2:has-text("Provide Government ID")')).toBeVisible();
    await page.selectOption('select', 'PAN');

    // Mock file upload
    const fileChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.border-dashed').click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles({
      name: 'pan_card.jpg',
      mimeType: 'image/jpeg',
      buffer: Buffer.from('mock-pan-image-content')
    });

    // Check file is selected
    await expect(page.locator('text=pan_card.jpg')).toBeVisible();
    await page.click('button:has-text("Next")');

    // Step 2: Upload Selfie
    await expect(page.locator('h2:has-text("Face Selfie Check")')).toBeVisible();

    const selfieChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.w-40.h-40').click();
    const selfieChooser = await selfieChooserPromise;
    await selfieChooser.setFiles({
      name: 'selfie.jpg',
      mimeType: 'image/jpeg',
      buffer: Buffer.from('mock-selfie-image-content')
    });

    // Mock submission API call
    await page.route('**/api/v1/trust/verification', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'verification-uuid-9999',
            userId: 'owner-uuid-1111',
            verificationStatus: 'SUBMITTED',
            verificationLevel: 'LEVEL_2_DOCUMENTS',
            verificationProvider: 'ROOMWALLAH_TRUST',
            submittedAt: new Date().toISOString(),
            approvedAt: null,
            rejectedAt: null,
            expiresAt: null,
            reviewerId: null,
            rejectionReason: null,
            version: 1,
            documents: []
          }
        })
      });
    });

    // Submit form
    await page.click('button:has-text("Submit Verification")');

    // Step 3: Verify Success Summary
    await expect(page.locator('h2:has-text("Application Submitted!")')).toBeVisible();
    await expect(page.locator('text=Document Type: PAN')).toBeVisible();
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

    await mockOwnerLogin(page);

    // Go to wizard
    await page.goto('/trust/verify');
    await page.click('button:has-text("Continue")');

    // Select Passport
    await page.selectOption('select', 'PASSPORT');

    // Reload page to verify local cache recovery
    await page.reload();

    await page.click('button:has-text("Continue")');
    await expect(page.locator('select')).toHaveValue('PASSPORT');
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
