import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 11 - AI Features & Analytics E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Disable service worker for predictable mocking
    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });
  });

  // Helper for Renter/Admin login mocking
  async function mockLogin(page, role = 'TENANT') {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            accessToken: 'mock_access_token',
            refreshToken: 'mock_refresh_token'
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
            id: 'tenant-uuid-1111',
            fullName: 'Test User',
            email: 'user@roomwallah.com',
            phone: '+919999999999',
            role: role
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
            id: 'tenant-uuid-1111',
            fullName: 'Test User',
            email: 'user@roomwallah.com',
            phone: '+919999999999',
            role: role,
            emailVerified: true,
            phoneVerified: true,
            identityVerified: true
          }
        })
      });
    });

    // Mock generic API endpoints returning success
    await page.route('**/api/v1/ai/search', async (route) => {
      await route.fulfill({ status: 404, body: '' }); // Fallback to local
    });

    await page.route('**/api/v1/ai/chat', async (route) => {
      await route.fulfill({ status: 404, body: '' }); // Fallback to local
    });

    await page.goto('/login');
    await page.fill('input[placeholder*="name@example.com"]', 'user@roomwallah.com');
    await page.fill('input[placeholder="••••••••"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  }

  test('1. Renter performs semantic natural language search & adjusts AI weights', async ({ page }) => {
    await mockLogin(page);

    // Navigate to AI Search page
    await page.goto('/search/ai');
    await expect(page.locator('h1')).toContainText('Natural Language AI Search');

    // Input semantic query
    const searchInput = page.locator('[data-testid="semantic-search-input"]');
    await searchInput.fill('2 BHK Noida Sector 62 with gym under 30000');
    
    // Perform search
    await page.click('[data-testid="semantic-search-btn"]');

    // Verify results and match score gauges load
    await expect(page.locator('text=Matched Properties')).toBeVisible();
    await expect(page.locator('text=AI Match Insights')).first().toBeVisible();

    // Toggle parsed intent & vector schema display
    await page.click('text=View Parsed Intent & Vector Schema');
    await expect(page.locator('text=Mock Dense Vector Embedding')).toBeVisible();

    // Open weight calibration and change range slider
    await page.click('text=Adjust AI Weight Preferences');
    const budgetSlider = page.locator('input[type="range"]').first();
    await budgetSlider.fill('50');

    // Confirm recalibration occurred
    await expect(page.locator('text=Budget Match Weight').first()).toBeVisible();
  });

  test('2. Renter interacts with the chatbot drawer, switches sessions, and changes locale', async ({ page }) => {
    await mockLogin(page);

    // Toggle the chat assistant drawer
    const toggleBtn = page.locator('[data-testid="ai-assistant-toggle"]');
    await expect(toggleBtn).toBeVisible();
    await toggleBtn.click();

    // Check if drawer is visible
    const chatDrawer = page.locator('[data-testid="ai-chat-drawer"]');
    await expect(chatDrawer).toBeVisible();
    await expect(chatDrawer.locator('text=AI Property Assistant')).toBeVisible();

    // Input chat message
    const chatInput = page.locator('[data-testid="ai-chat-input"]');
    await chatInput.fill('Estimate average rent in Indiranagar');
    await page.click('[data-testid="ai-chat-send-btn"]');

    // Validate simulated reply appeared
    await expect(chatDrawer.locator('text=Average rent in Bangalore Indiranagar is')).toBeVisible({ timeout: 5000 });

    // Deletion support: Clear history
    await page.click('text=Clear Session History');
    await expect(chatDrawer.locator('text=No conversation history')).toBeVisible();

    // Switch between sessions
    await page.click('text=Bangalore Studio Search');
    await expect(chatDrawer.locator('text=Bangalore, Gym, Tech Hub')).toBeVisible();

    // Localization toggle: Switch language to Hindi
    const langSelect = chatDrawer.locator('select').first();
    await langSelect.selectOption('hi-IN');
    await expect(chatDrawer.locator('text=एआई संपत्ति सहायक')).toBeVisible();
  });

  test('3. Owner analyzes pricing insights and resolves listing health score issues', async ({ page }) => {
    await mockLogin(page, 'OWNER');

    // Navigate to pricing insights
    await page.goto('/listings/pricing-insights');
    await expect(page.locator('h1')).toContainText('Dynamic Pricing & Market Insights');
    await expect(page.locator('text=Suggested Rent Adjustments')).toBeVisible();

    // Click dynamic price suggestion apply
    const applyBtn = page.locator('text=Apply Adjustment').first();
    await applyBtn.click();
    await expect(page.locator('text=Applied').first()).toBeVisible();

    // Go to Listing Health dashboard
    await page.goto('/listings/health');
    await expect(page.locator('h1')).toContainText('Listing Health & Completeness Dashboard');

    // Check health alert warning box
    const alertBox = page.locator('[data-testid="health-alert-box"]').first();
    await expect(alertBox).toBeVisible();
    await expect(alertBox).toContainText('outdated availability calendar');

    // Click Resolve on checklist item
    const resolveBtn = page.locator('text=Resolve').first();
    await resolveBtn.click();

    // Resolve task wizard dialog fills
    await expect(page.locator('text=Health Optimization Wizard')).toBeVisible();
    await page.fill('textarea', 'Completed 3D tour link at roomwallah.com/tour/3d');
    await page.click('text=Complete Optimization');

    // Verify score changed / task completed
    await expect(page.locator('text=Health Optimization Wizard')).not.toBeVisible();
  });

  test('4. Admin reviews duplicate queue clusters and acts on candidates', async ({ page }) => {
    await mockLogin(page, 'ADMIN');

    // Go to Duplicate Review dashboard
    await page.goto('/admin/duplicates');
    await expect(page.locator('h1')).toContainText('AI Duplicate Detection Review');
    await expect(page.locator('text=Suspected Duplicate')).toBeVisible();

    // Verify duplicate cluster cards side by side
    await expect(page.locator('text=Candidate A')).toBeVisible();
    await expect(page.locator('text=Candidate B')).toBeVisible();

    // Take action: dismiss/whistelist
    await page.click('[data-testid="duplicate-dismiss-btn"]');
    
    // Toast success feedback shows
    await expect(page.locator('text=whitelisted and marked as separate')).toBeVisible();
  });
});
