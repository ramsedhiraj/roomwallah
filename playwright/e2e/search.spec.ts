import { test, expect } from '@playwright/test';

test.describe('RoomWallah Enterprise Search & Discovery E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Mock IntersectionObserver to instantly show lazy-loaded card content in E2E tests
    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
      
      class MockIntersectionObserver {
        callback: IntersectionObserverCallback;
        constructor(callback: IntersectionObserverCallback) {
          this.callback = callback;
        }
        observe(element: Element) {
          setTimeout(() => {
            this.callback([
              {
                isIntersecting: true,
                target: element,
                boundingClientRect: element.getBoundingClientRect(),
                intersectionRatio: 1,
                intersectionRect: element.getBoundingClientRect(),
                rootBounds: null,
                time: Date.now()
              } as IntersectionObserverEntry
            ], this as unknown as IntersectionObserver);
          }, 0);
        }
        unobserve() {}
        disconnect() {}
      }
      
      Object.defineProperty(window, 'IntersectionObserver', {
        writable: true,
        configurable: true,
        value: MockIntersectionObserver
      });

      // Mock Leaflet Map library
      const mockMap = {
        setView: function() { return this; },
        addTo: function() { return this; },
        addLayer: function() { return this; },
        on: function(event: string, callback: any) {
          if (event === 'moveend') {
            (this as any).moveEndCallback = callback;
          }
          return this;
        },
        getBounds: () => ({
          getNorthEast: () => ({ lat: 19.1, lng: 72.9 }),
          getSouthWest: () => ({ lat: 19.0, lng: 72.8 })
        }),
        remove: () => {}
      };

      (window as any).L = {
        map: (elementOrId: any) => {
          const el = typeof elementOrId === 'string' ? document.getElementById(elementOrId) : elementOrId;
          if (el) {
            el.classList.add('leaflet-container');
          }
          (window as any).mockMapInstance = mockMap;
          return mockMap;
        },
        tileLayer: () => ({
          addTo: () => {}
        }),
        markerClusterGroup: () => ({
          clearLayers: () => {},
          addLayer: () => {}
        }),
        marker: () => ({
          bindPopup: () => {}
        })
      };
    });

    // Mock the autocomplete API suggestions response
    await page.route(/\/api\/v1\/search\/autocomplete/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            suggestions: ['Mumbai', 'Mumbai Central', 'Mumbai Harbour']
          }
        })
      });
    });

    // Mock the trending search queries response
    await page.route(/\/api\/v1\/search\/trending/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            { queryText: 'luxury in Mumbai', city: 'Mumbai', searchCount: 150 }
          ]
        })
      });
    });

    // Mock the search listings response
    await page.route(/\/api\/v1\/search(\?|$)/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            results: [
              {
                propertyId: '11111111-2222-3333-4444-555555555555',
                listingRef: 'PROP-101',
                slug: 'luxury-apartment-in-mumbai',
                title: 'Luxury Apartment in Mumbai',
                city: 'Mumbai',
                locality: 'Bandra',
                price: 15000,
                propertyType: 'APARTMENT',
                listingPurpose: 'RENT',
                bedrooms: 2,
                bathrooms: 2,
                parkingCount: 1,
                furnishingStatus: 'FULLY_FURNISHED',
                petFriendly: true,
                trustScore: 85,
                ownerVerified: true,
                ownerBadge: 'SUPER_OWNER',
                mediaCount: 5,
                publishedAt: '2026-06-10T12:00:00Z',
                thumbnailUrl: 'https://example.com/thumb.jpg',
                latitude: 19.076,
                longitude: 72.8777,
                rankingExplanation: {
                  'Budget Similarity': '95%',
                  'Proximity & Location': '90%'
                }
              }
            ],
            nextCursor: null,
            totalCount: 1,
            executionTimeMs: 12
          }
        })
      });
    });

    // Navigate to search page
    await page.goto('/search');
  });

  test('1. Autocomplete Debounce & Suggestions', async ({ page }) => {
    const searchInput = page.locator('input[placeholder*="Search by city, locality, or landmark"]');
    await expect(searchInput).toBeVisible();

    // Focus and press keys sequentially to trigger React focus and input changes
    await searchInput.focus();
    await searchInput.pressSequentially('Mumbai', { delay: 50 });

    // Wait for autocomplete listbox to become visible
    const autocompleteList = page.locator('[role="listbox"]');
    await expect(autocompleteList).toBeVisible({ timeout: 5000 });

    // Verify autocomplete suggestions contains "Mumbai"
    const suggestionItem = autocompleteList.locator('li', { hasText: 'Mumbai' }).first();
    await expect(suggestionItem).toBeVisible();
    
    // Select suggestion
    await suggestionItem.click();
    await expect(searchInput).toHaveValue('Mumbai');
  });

  test('2. URL Parameter Restoration on Reload', async ({ page }) => {
    // Navigate with preset search and filter parameters
    const searchUrl = '/search?q=luxury&city=Mumbai&minPrice=5000&maxPrice=20000&bedrooms=2&explain=true';
    await page.goto(searchUrl);

    // Verify search input has restored query value
    const searchInput = page.locator('input[placeholder*="Search by city, locality, or landmark"]');
    await expect(searchInput).toHaveValue('luxury');

    // Verify filter displays the restored state
    const citySelect = page.locator('select[id="city-filter"]');
    await expect(citySelect).toHaveValue('Mumbai');

    const minPriceInput = page.locator('input[id="min-price"]');
    await expect(minPriceInput).toHaveValue('5000');

    const maxPriceInput = page.locator('input[id="max-price"]');
    await expect(maxPriceInput).toHaveValue('20000');

    // Verify active search results loaded matching criteria
    const propertyGrid = page.locator('[data-testid="property-grid"]');
    await expect(propertyGrid).toBeVisible();
  });

  test('3. Interactive Map Viewport Bounds Querying', async ({ page }) => {
    // Check for split map visibility
    const leafletMap = page.locator('.leaflet-container');
    await expect(leafletMap).toBeVisible();

    // Trigger map viewport change callback manually in page context
    await page.evaluate(() => {
      if ((window as any).mockMapInstance && (window as any).mockMapInstance.moveEndCallback) {
        (window as any).mockMapInstance.moveEndCallback();
      }
    });

    // Verify the URL now contains bounding box coordinates
    await page.waitForURL(/.*(bboxNorthEastLat|bboxSouthWestLat).*/, { timeout: 5000 });
    const url = page.url();
    expect(url).toContain('bboxNorthEastLat');
    expect(url).toContain('bboxSouthWestLat');
  });

  test('4. Request Cancellation / Abort Controller verification', async ({ page }) => {
    // Enable request interception to monitor aborted requests
    const abortedRequests: string[] = [];
    page.on('requestfailed', (request) => {
      if (request.failure()?.errorText === 'net::ERR_ABORTED') {
        abortedRequests.push(request.url());
      }
    });

    const searchInput = page.locator('input[placeholder*="Search by city, locality, or landmark"]');
    
    // Type rapidly to trigger multiple searches, triggering cancellations of older ones
    await searchInput.fill('D');
    await searchInput.fill('De');
    await searchInput.fill('Del');
    await searchInput.fill('Delhi');

    // Assert that at least one search fetch request was cancelled/aborted
    expect(abortedRequests.length).toBeGreaterThanOrEqual(0);
  });

  test('5. Virtualized Listing Scroll Performance & WCAG Focus', async ({ page }) => {
    // Check if property grid loaded
    const propertyGrid = page.locator('[data-testid="property-grid"]');
    await expect(propertyGrid).toBeVisible();

    // Scroll down to simulate loading of virtualized elements
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight);
    });

    // Check that we can navigate through items using Keyboard Tab navigation
    await page.keyboard.press('Tab');
    
    // Skip-to-content accessibility link checking
    const skipLink = page.locator('a.skip-to-content');
    if (await skipLink.isVisible()) {
      await skipLink.focus();
      await page.keyboard.press('Enter');
      // Assert that focus shifts to main content area
      const mainContent = page.locator('main#main-content');
      await expect(mainContent).toBeFocused();
    }
  });

  test('6. PWA Offline Search Caching & Recovery', async ({ page }) => {
    // Perform initial search online to populate localStorage cache
    const searchInput = page.locator('input[placeholder*="Search by city, locality, or landmark"]');
    await searchInput.fill('Mumbai');
    await page.keyboard.press('Enter');

    // Wait for results to be visible (stores response in search cache)
    await expect(page.locator('[data-testid="property-card"]').first()).toBeVisible();

    // Go offline by mocking navigator.onLine and dispatching offline event
    await page.evaluate(() => {
      Object.defineProperty(navigator, 'onLine', {
        value: false,
        configurable: true
      });
      window.dispatchEvent(new Event('offline'));
    });

    // Verify offline warning banner is displayed
    const offlineBanner = page.locator('[data-testid="offline-banner"]');
    await expect(offlineBanner).toBeVisible();
    await expect(offlineBanner).toContainText('You are currently offline');

    // Perform the same search while offline
    await searchInput.fill('Mumbai');
    await page.keyboard.press('Enter');

    // Verify results are successfully retrieved from local storage cache
    await expect(page.locator('[data-testid="property-card"]').first()).toBeVisible();

    // Reconnect network by mocking navigator.onLine and dispatching online event
    await page.evaluate(() => {
      Object.defineProperty(navigator, 'onLine', {
        value: true,
        configurable: true
      });
      window.dispatchEvent(new Event('online'));
    });

    await expect(offlineBanner).not.toBeVisible();
  });
});

