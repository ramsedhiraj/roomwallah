import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 10 - Enterprise Platform & Operations E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });
  });

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
            id: 'admin-uuid-0000',
            fullName: 'Platform Admin',
            email: 'admin@roomwallah.com',
            phone: '+910000000000',
            role: 'ADMIN'
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
            id: 'admin-uuid-0000',
            fullName: 'Platform Admin',
            email: 'admin@roomwallah.com',
            phone: '+910000000000',
            role: 'ADMIN',
            emailVerified: true,
            phoneVerified: true,
            identityVerified: true
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

  test('1. Admin views platform metrics and business analytics', async ({ page }) => {
    await mockAdminLogin(page);

    // Navigate to Analytics dashboard
    await page.goto('/admin/analytics');
    await expect(page.locator('h1')).toContainText('Admin Analytics Dashboard');
    await expect(page.locator('text=Active Listings')).toBeVisible();
    await expect(page.locator('text=3,842')).toBeVisible();

    // Navigate to Business Insights
    await page.goto('/admin/insights');
    await expect(page.locator('h1')).toContainText('Market & Business Insights');
    await expect(page.locator('text=Supply vs Demand Mapping')).toBeVisible();
    await expect(page.locator('text=Export CSV')).toBeVisible();
  });

  test('2. Admin modifies recommendation configurator weights', async ({ page }) => {
    await mockAdminLogin(page);

    // Navigate to recommendation configuration
    await page.goto('/admin/recommendations');
    await expect(page.locator('h1')).toContainText('Recommendation Model Configurator');
    await expect(page.locator('text=Preview Recommendation Rank')).toBeVisible();

    // Verify weights sliders are on the page
    await expect(page.locator('text=Budget Similarity Weight')).toBeVisible();
    await expect(page.locator('text=Proximity & Distance Weight')).toBeVisible();

    // Trigger publish weights check
    await page.click('button:has-text("Publish Weights")');
    await expect(page.locator('text=Scoring Weights Published')).toBeVisible();
  });

  test('3. Admin inspects audit logs and cryptographic integrity verification', async ({ page }) => {
    await mockAdminLogin(page);

    // Navigate to Audit Logs page
    await page.goto('/admin/audit-logs');
    await expect(page.locator('h1')).toContainText('System Audit Log Console');
    
    // Check integrity check banner
    await expect(page.locator('text=Cryptographic Ledger integrity: Verified')).toBeVisible();

    // Expand Full report
    await page.click('text=View Full Report');
    await expect(page.locator('text=Log Reordering Check')).toBeVisible();
    await expect(page.locator('text=Deleted Records Check')).toBeVisible();

    // Filter audits
    await page.fill('input[placeholder*="Search user"]', 'Rajesh');
    await expect(page.locator('span.font-semibold').filter({ hasText: 'Rajesh Kumar' })).toBeVisible();
    await expect(page.locator('text=John Doe')).not.toBeVisible();

    // View slide-in drawer inspect user timeline
    await page.click('button[title="Inspect User Timeline"]');
    await expect(page.locator('h2').filter({ hasText: 'User Inspector' })).toBeVisible();
    await expect(page.locator('text=Activity Timeline')).toBeVisible();
  });

  test('4. Admin monitors developer key rotation and cache stampede protection', async ({ page }) => {
    await mockAdminLogin(page);

    // Navigate to api key management
    await page.goto('/developer/keys');
    await expect(page.locator('h1')).toContainText('API Credentials Manager');
    await expect(page.locator('text=Production Mobile Client')).toBeVisible();
    await expect(page.locator('text=days remaining').first()).toBeVisible(); // Expiry timeline

    // Navigate to Cache Monitoring
    await page.goto('/admin/cache');
    await expect(page.locator('h1')).toContainText('L1/L2 Cache Administration');
    await expect(page.locator('text=Caffeine L1 Cache')).toBeVisible();
    await expect(page.locator('text=Redis L2 Cache')).toBeVisible();
    await expect(page.locator('text=Hit Ratio').first()).toBeVisible();
  });
});
