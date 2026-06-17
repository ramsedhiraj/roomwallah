import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 8 - Booking, Scheduling & CRM E2E Tests', () => {

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
            identityVerified: true
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

  test('1. Tenant schedules a physical property visit', async ({ page }) => {
    const propertyId = 'prop-uuid-5555';
    
    // Mock property details
    await page.route(`**/api/v1/properties/${propertyId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: propertyId,
            title: 'Spacious 2BHK Flat in HSR Layout',
            ownerId: 'owner-uuid-1111',
            propertyType: 'FLAT',
            price: { amount: 25000, currency: 'INR' },
            address: { locality: 'Sector 3', city: 'Bangalore' }
          }
        })
      });
    });

    // Mock slots availability
    await page.route(`**/api/v1/admin/calendar/slots?propertyId=${propertyId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'slot-uuid-99',
              propertyId: propertyId,
              startTime: new Date(Date.now() + 86400000).toISOString(), // 24 hours later
              endTime: new Date(Date.now() + 90000000).toISOString(),
              maxBookings: 2,
              currentBookings: 0,
              status: 'AVAILABLE'
            }
          ]
        })
      });
    });

    await mockTenantLogin(page);

    // Go to scheduling page
    await page.goto(`/properties/${propertyId}/book-visit`);
    await expect(page.locator('h1')).toContainText('Schedule a Property Visit');

    // Click slot
    await page.click('button:has-text("slots left")');

    // Fill notes
    await page.fill('textarea[placeholder*="Enter any questions"]', 'I would like to verify parking details.');

    // Mock schedule visit response
    await page.route('**/api/v1/visits', async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'visit-uuid-123',
            propertyId: propertyId,
            tenantId: 'tenant-uuid-3333',
            visitSlotId: 'slot-uuid-99',
            status: 'SCHEDULED'
          }
        })
      });
    });

    // Mock tenant visits listing page
    await page.route('**/api/v1/visits/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'visit-uuid-123',
              propertyId: propertyId,
              tenantId: 'tenant-uuid-3333',
              visitSlotId: 'slot-uuid-99',
              status: 'SCHEDULED',
              startTime: new Date(Date.now() + 86400000).toISOString(),
              endTime: new Date(Date.now() + 90000000).toISOString(),
              notes: 'I would like to verify parking details.'
            }
          ]
        })
      });
    });

    await page.route('**/api/v1/bookings/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: [] })
      });
    });

    // Submit schedule visit
    await page.click('button:has-text("Confirm Visit Schedule")');
    await page.waitForURL('/visits/me');

    // Check scheduling was created successfully
    await expect(page.locator('text=Visit ID: visit-uu')).toBeVisible();
    await expect(page.locator('text=Active')).toBeVisible();
  });

  test('2. Owner approves tenant booking request', async ({ page }) => {
    // Mock owner bookings list
    await page.route('**/api/v1/admin/bookings', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'booking-uuid-777',
              propertyId: 'prop-uuid-5555',
              tenantId: 'tenant-uuid-3333',
              ownerId: 'owner-uuid-1111',
              status: 'PENDING',
              priceAmount: 22000,
              priceCurrency: 'INR',
              notes: 'Ready to sign contract immediately.',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    await mockOwnerLogin(page);

    // Go to owner dashboard
    await page.goto('/listings/bookings');
    await expect(page.locator('h1')).toContainText('Owner Bookings');

    // Verify pending booking request
    await expect(page.locator('text=Proposal ID: booking-')).toBeVisible();
    await expect(page.locator('text=₹22,000')).toBeVisible();

    // Mock approve API response
    await page.route('**/api/v1/admin/bookings/*/approve', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            id: 'booking-uuid-777',
            status: 'CONFIRMED'
          }
        })
      });
    });

    // Mock refreshed list
    await page.route('**/api/v1/admin/bookings', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'booking-uuid-777',
              propertyId: 'prop-uuid-5555',
              tenantId: 'tenant-uuid-3333',
              ownerId: 'owner-uuid-1111',
              status: 'CONFIRMED',
              priceAmount: 22000,
              priceCurrency: 'INR',
              notes: 'Ready to sign contract immediately.',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    // Click Approve
    page.once('dialog', dialog => dialog.accept());
    await page.click('button:has-text("Approve")');

    // Verify confirmation status
    await expect(page.locator('span:text-is("CONFIRMED")')).toBeVisible();
  });

  test('3. CRM lead inbox overview and internal notes auditing', async ({ page }) => {
    const leadId = 'lead-uuid-888';
    
    // Mock qualified leads list
    await page.route('**/api/v1/admin/leads', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: leadId,
              propertyId: 'prop-uuid-5555',
              tenantId: 'tenant-uuid-3333',
              ownerId: 'owner-uuid-1111',
              status: 'NEW',
              inquiryText: 'Looking for a quiet flat near main street.',
              contactPhone: '+919999999999',
              contactEmail: 'rohan@tenant.com',
              leadScore: 90,
              leadScoreExplanation: 'Verified Identity: +50 | Verified Badge: +20 | Completions: +20',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    // Mock initial notes
    await page.route(`**/api/v1/admin/leads/${leadId}/notes`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'note-uuid-1',
              leadId: leadId,
              authorId: 'owner-uuid-1111',
              content: 'Spoke with tenant, they sound very interested.',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    // Mock admin list
    await page.route('**/api/v1/users', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: [] })
      });
    });

    await mockOwnerLogin(page);

    // Go to lead inbox
    await page.goto('/listings/leads');
    await expect(page.locator('h1')).toContainText('CRM Lead Inbox');

    // Verify lead score indicator & explanation
    await expect(page.locator('text=Score: 90')).toBeVisible();
    await expect(page.locator('text=Verified Identity: +50')).toBeVisible();

    // Verify existing notes log
    await expect(page.locator('text=Spoke with tenant')).toBeVisible();

    // Add note
    await page.fill('input[placeholder*="internal audit note"]', 'Tenant requested a second visit.');

    // Mock note addition response
    await page.route(`**/api/v1/admin/leads/${leadId}/notes`, async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'note-uuid-2',
              leadId: leadId,
              authorId: 'owner-uuid-1111',
              content: 'Tenant requested a second visit.',
              createdAt: new Date().toISOString()
            },
            {
              id: 'note-uuid-1',
              leadId: leadId,
              authorId: 'owner-uuid-1111',
              content: 'Spoke with tenant, they sound very interested.',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    await page.click('button:has-text("Log Note")');

    // Verify note is appended to timeline
    await expect(page.locator('text=Tenant requested a second visit.')).toBeVisible();
  });
});
