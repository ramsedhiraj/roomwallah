import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 9 - Payment, Escrow, Webhook & Dispute E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });
  });

  // Helper for TENANT login mocking
  async function mockTenantLogin(page) {
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            accessToken: 'tenant_access_token',
            refreshToken: 'tenant_refresh_token'
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
            id: 'tenant-uuid-3333',
            fullName: 'Rohan Tenant',
            email: 'rohan@tenant.com',
            phone: '+918888888888',
            role: 'TENANT'
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
            id: 'tenant-uuid-3333',
            fullName: 'Rohan Tenant',
            email: 'rohan@tenant.com',
            phone: '+918888888888',
            role: 'TENANT',
            emailVerified: true,
            phoneVerified: true,
            identityVerified: true
          }
        })
      });
    });

    await page.goto('/login');
    await page.fill('input[placeholder*="name@example.com"]', 'rohan@tenant.com');
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

  test('1. Tenant successfully completes checkout and view status', async ({ page }) => {
    const bookingId = 'booking-uuid-7777';
    const paymentId = 'pay-uuid-8888';

    // Mock Booking details call
    await page.route(`**/api/v1/bookings/${bookingId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: bookingId,
            propertyId: 'prop-uuid-5555',
            tenantId: 'tenant-uuid-3333',
            priceAmount: 10000.00,
            priceCurrency: 'INR',
            status: 'PENDING_PAYMENT'
          }
        })
      });
    });

    // Mock Payment Initiation
    await page.route('**/api/v1/payments', async (route) => {
      expect(route.request().method()).toBe('POST');
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: paymentId,
            bookingId: bookingId,
            amount: 10236.00, // Amount + Fee + GST
            currency: 'INR',
            status: 'PENDING',
            gatewayProvider: 'RAZORPAY'
          }
        })
      });
    });

    // Mock Payment Capture
    await page.route(`**/api/v1/admin/payments/${paymentId}/capture`, async (route) => {
      expect(route.request().method()).toBe('POST');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: paymentId,
            bookingId: bookingId,
            amount: 10236.00,
            currency: 'INR',
            status: 'CAPTURED',
            gatewayProvider: 'RAZORPAY',
            gatewayPaymentId: 'gw_razor_mock_captured'
          }
        })
      });
    });

    // Run tenant login
    await mockTenantLogin(page);

    // Goto checkout
    await page.goto(`/checkout/${bookingId}`);

    // Verify title/pricing details
    await expect(page.locator('h1')).toContainText('Complete Your Payment');
    await expect(page.locator('span.text-indigo-300').filter({ hasText: '₹10,236' })).toBeVisible();

    // Select Razorpay and complete payment
    await page.click('text=Razorpay');
    await page.click('button[aria-label="Complete payment"]');

    // Should transition to success screen
    await expect(page.locator('h2')).toContainText('Payment Successful!');
    await expect(page.locator('text=pay-uuid-8888')).toBeVisible();
  });

  test('2. Admin monitors platform transactions and verifies webhook retries', async ({ page }) => {
    const paymentId = 'pay-uuid-8888';
    const webhookId = 'web-uuid-9999';

    // Mock Admin Payments List
    await page.route('**/api/v1/admin/payments', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: paymentId,
              bookingId: 'booking-uuid-7777',
              amount: 10236.00,
              currency: 'INR',
              status: 'CAPTURED',
              gatewayProvider: 'RAZORPAY',
              riskScore: 25,
              riskDecision: 'PASS'
            }
          ]
        })
      });
    });

    // Mock Webhooks list
    await page.route('**/api/v1/admin/payments/webhooks', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: webhookId,
              gatewayProvider: 'STRIPE',
              eventType: 'charge.succeeded',
              payloadJson: '{"id": "evt_123"}',
              processed: true,
              createdAt: '2026-06-15T10:00:00Z'
            }
          ]
        })
      });
    });

    // Mock Webhook retry endpoint
    await page.route(`**/api/v1/admin/payments/webhooks/${webhookId}/retry`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true
        })
      });
    });

    // Run admin login
    await mockAdminLogin(page);

    // Goto monitor
    await page.goto('/admin/payments');
    await expect(page.locator('h1')).toContainText('Real-time Payment Stream');
    await expect(page.locator('text=pay-uuid-8888')).toBeVisible();

    // Goto webhooks console
    await page.goto('/admin/payments/webhooks');
    await expect(page.locator('h1')).toContainText('Gateway Webhook Events');
    await expect(page.locator('text=charge.succeeded')).toBeVisible();

    // Expand webhook details
    await page.click('td >> text=charge.succeeded');
    await expect(page.locator('text=Raw Webhook Payload JSON')).toBeVisible();

    // Trigger retry
    const dialogPromise = page.waitForEvent('dialog');
    await page.click('button:has-text("Retry")');
    const dialog = await dialogPromise;
    expect(dialog.message()).toContain('Webhook event processing retried');
    await dialog.accept();
  });
});
