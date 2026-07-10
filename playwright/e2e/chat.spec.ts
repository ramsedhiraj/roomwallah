import { test, expect } from '@playwright/test';

test.describe('RoomWallah Phase 5 - Chat & Communication E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Add page logging listeners for debug
    page.on('console', msg => console.log('BROWSER CONSOLE:', msg.text()));
    page.on('request', req => console.log('BROWSER REQUEST:', req.method(), req.url()));
    page.on('requestfailed', req => console.log('BROWSER REQ FAILED:', req.url(), req.failure()?.errorText));

    await page.addInitScript(() => {
      window.localStorage.setItem('disable-service-worker', 'true');
    });

    // Mock global layouts resources to avoid connection refusal
    await page.route('**/api/v1/recommendations*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: [] }) });
    });
    await page.route('**/api/v1/search/trending*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: [] }) });
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
            role: 'TENANT'
          }
        })
      });
    });

    // Go to login page and login
    await page.goto('/login');
    await page.fill('input[placeholder*="name@example.com"]', 'rohan@tenant.com');
    await page.fill('input[placeholder="••••••••"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  }

  test('1. Guest attempting to access chat is redirected to login', async ({ page }) => {
    await page.goto('/chat');
    await page.waitForURL('**/login**');
    expect(page.url()).toContain('/login');
  });

  test('2. Tenant successfully initiates chat from bookings page and sends a message', async ({ page }) => {
    // Mock Bookings
    await page.route('**/api/v1/bookings/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'booking-uuid-1111',
              propertyId: 'property-uuid-9999',
              tenantId: 'tenant-uuid-3333',
              ownerId: 'owner-uuid-4444',
              status: 'CONFIRMED',
              priceAmount: 45000,
              priceCurrency: 'INR',
              notes: 'Looking forward to viewing!',
              createdAt: new Date().toISOString()
            }
          ]
        })
      });
    });

    // Mock Visits
    await page.route('**/api/v1/visits/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: [] })
      });
    });
    await page.route('**/api/v1/conversations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'conversation-uuid-2222',
              bookingId: 'booking-uuid-1111',
              tenantId: 'tenant-uuid-3333',
              ownerId: 'owner-uuid-4444',
              tenantName: 'Rohan Tenant',
              ownerName: 'Amit Owner',
              latestMessage: 'Hello, let us schedule a visit.',
              latestMessageTime: new Date().toISOString(),
              unreadCount: 0
            }
          ]
        })
      });
    });

    // Mock Chat messages list
    const messages = [
      {
        id: 'msg-uuid-1',
        senderId: 'owner-uuid-4444',
        content: 'Hello, let us schedule a visit.',
        read: true,
        createdAt: new Date(Date.now() - 60000).toISOString()
      }
    ];

    await page.route('**/api/v1/conversations/conversation-uuid-2222/messages', async (route) => {
      const method = route.request().method();
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ data: messages })
        });
      } else if (method === 'POST') {
        const body = JSON.parse(route.request().postData() || '{}');
        const newMsg = {
          id: `msg-uuid-${Date.now()}`,
          senderId: 'tenant-uuid-3333',
          content: body.content,
          read: false,
          createdAt: new Date().toISOString()
        };
        messages.push(newMsg);
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ data: newMsg })
        });
      }
    });

    await page.route('**/api/v1/conversations/conversation-uuid-2222/read', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
    });

    // Mock Notifications (avoid 401s on mount/header renders)
    await page.route('**/api/v1/notifications/inbox', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    });

    await mockTenantLogin(page);

    // Go to My Bookings page
    await page.goto('/bookings');
    await page.waitForSelector('text=Proposal ID: booking-');

    // Click "Chat with Owner" button
    const chatBtn = page.locator('button:has-text("Chat with Owner")');
    await expect(chatBtn).toBeVisible();
    await chatBtn.click();

    // Verify redirected to chat page with the conversation selected
    await page.waitForURL('**/chat/booking-uuid-1111');
    
    // Check conversation header
    await expect(page.locator('h3:has-text("Amit Owner")')).toBeVisible();
    
    // Check existing message is visible
    await expect(page.locator('.whitespace-pre-wrap:has-text("Hello, let us schedule a visit.")').first()).toBeVisible();

    // Send a message
    const input = page.locator('textarea[placeholder="Type your message..."]');
    await input.fill("Sure, tomorrow at 10 AM works!");
    await page.click('button[aria-label="Send Message"]');

    // Verify message is visible in chat history
    await expect(page.locator('.whitespace-pre-wrap:has-text("Sure, tomorrow at 10 AM works!")').first()).toBeVisible();
    await expect(input).toHaveValue('');
  });

  test('3. Notifications center fetches database alerts and navigates to chat', async ({ page }) => {
    // Mock Inbox Notifications list
    await page.route('**/api/v1/notifications/inbox', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'notif-uuid-1',
            userId: 'tenant-uuid-3333',
            title: 'New Message Received',
            message: 'Amit Owner sent a message: Hello, let us schedule a visit.',
            status: 'UNREAD',
            notificationType: 'CHAT',
            createdAt: new Date().toISOString()
          }
        ])
      });
    });

    await page.route('**/api/v1/notifications/inbox/notif-uuid-1/read', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true }) });
    });

    await mockTenantLogin(page);

    // Open notifications dropdown
    const bellBtn = page.locator('button[aria-label="Notifications Dropdown"]');
    await expect(bellBtn).toBeVisible();
    
    // Unread count should show "1"
    const badge = bellBtn.locator('span');
    await expect(badge).toHaveText('1');

    // Click to open dropdown
    await bellBtn.click();

    // Verify notification content is rendered
    const item = page.locator('span:has-text("New Message Received")');
    await expect(item).toBeVisible();
    await expect(page.locator('p:has-text("Amit Owner sent a message")')).toBeVisible();

    // Click notification item to navigate
    await item.click();

    // Verify redirected to chat page
    await page.waitForURL('**/chat');
    expect(page.url()).toContain('/chat');
  });

});
