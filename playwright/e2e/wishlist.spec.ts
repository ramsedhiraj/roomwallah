import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 4 - Wishlist E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log('BROWSER CONSOLE:', msg.text()));
    page.on('request', req => console.log('BROWSER REQUEST:', req.method(), req.url()));
    page.on('requestfailed', req => console.log('BROWSER REQ FAILED:', req.url(), req.failure()?.errorText));

    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });

    // Mock recommendations to keep landing/property detail pages happy
    await page.route('**/api/v1/recommendations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: []
        })
      });
    });

    // Mock trending search queries
    await page.route('**/api/v1/search/trending*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: []
        })
      });
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

  test('1. Guest attempting to access wishlist is redirected to login', async ({ page }) => {
    await page.goto('/wishlist');
    await page.waitForURL('**/login**');
  });

  test('2. Tenant successfully adds and removes property from wishlist', async ({ page }) => {
    // Mock Search Results using wildcard to match query parameters correctly
    await page.route('**/api/v1/search*', async (route) => {
      const url = route.request().url();
      if (url.includes('/autocomplete') || url.includes('/trending')) {
        return route.continue();
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            results: [
              {
                propertyId: 'prop-uuid-9999',
                listingRef: 'RW-MUM-2026-999999',
                slug: 'luxury-apartment-bandra',
                title: 'Luxury Apartment Bandra',
                city: 'Mumbai',
                locality: 'Bandra West',
                price: 75000,
                propertyType: 'APARTMENT',
                listingPurpose: 'RENT',
                bedrooms: 2,
                bathrooms: 2,
                parkingCount: 1,
                furnishingStatus: 'FULLY_FURNISHED',
                petFriendly: true,
                trustScore: 85,
                ownerVerified: true,
                ownerBadge: 'PRO',
                mediaCount: 1,
                publishedAt: new Date().toISOString(),
                thumbnailUrl: null,
                latitude: 19.0544,
                longitude: 72.8402
              }
            ],
            nextCursor: null,
            totalCount: 1,
            executionTimeMs: 12
          }
        })
      });
    });

    // Mock Wishlist endpoints dynamically
    const wishlistItems = new Set<string>();

    await page.route('**/api/v1/wishlist**', async (route) => {
      const url = route.request().url();
      const method = route.request().method();

      if (url.includes('/check')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: Array.from(wishlistItems)
          })
        });
      } else if (url.includes('/prop-uuid-9999')) {
        if (method === 'POST') {
          wishlistItems.add('prop-uuid-9999');
          await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
        } else if (method === 'DELETE') {
          wishlistItems.delete('prop-uuid-9999');
          await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
        }
      } else {
        // GET /wishlist
        const items = Array.from(wishlistItems).map(id => ({
          propertyId: id,
          listingRef: 'RW-MUM-2026-999999',
          slug: 'luxury-apartment-bandra',
          title: 'Luxury Apartment Bandra',
          city: 'Mumbai',
          locality: 'Bandra West',
          price: 75000,
          propertyType: 'APARTMENT',
          listingPurpose: 'RENT',
          bedrooms: 2,
          bathrooms: 2,
          parkingCount: 1,
          furnishingStatus: 'FULLY_FURNISHED',
          petFriendly: true,
          trustScore: 85,
          ownerVerified: true,
          ownerBadge: 'PRO',
          mediaCount: 1,
          publishedAt: new Date().toISOString(),
          thumbnailUrl: null,
          latitude: 19.0544,
          longitude: 72.8402
        }));
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: items
          })
        });
      }
    });

    // Login as Tenant
    await mockTenantLogin(page);

    // Go to Search page
    await page.goto('/search');
    
    // Wait for property card to appear
    const card = page.locator('[data-testid="property-card"]');
    await expect(card).toBeVisible();

    // Verify Wishlist Button is present
    const heartBtn = page.locator('button[aria-label="Add to wishlist"]');
    await expect(heartBtn).toBeVisible();

    // Click Wishlist Button (Add to wishlist)
    await heartBtn.click();
    
    // Check if label changes to "Remove from wishlist"
    const removeBtn = page.locator('button[aria-label="Remove from wishlist"]');
    await expect(removeBtn).toBeVisible();

    // Navigate to Wishlist page
    await page.goto('/wishlist');

    // Verify the saved property card is visible in wishlist page
    const wishlistCard = page.locator('[data-testid="property-card"]');
    await expect(wishlistCard).toBeVisible();
    await expect(wishlistCard.locator('h3')).toContainText('Luxury Apartment Bandra');

    // Click remove from wishlist on the wishlist page
    const wishlistHeartBtn = wishlistCard.locator('button[aria-label="Remove from wishlist"]');
    await wishlistHeartBtn.click();

    // Verify empty state is displayed
    const emptyStateHeader = page.locator('h3:has-text("Your wishlist is empty")');
    await expect(emptyStateHeader).toBeVisible();
  });
});
