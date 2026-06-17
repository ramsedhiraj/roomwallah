// Service worker for RoomWallah PWA
const CACHE_NAME = 'roomwallah-cache-v1';
const DATA_CACHE_NAME = 'roomwallah-data-cache-v1';

const PRECACHE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/favicon.ico',
  '/icons/icon-72x72.png',
  '/icons/icon-96x96.png',
  '/icons/icon-128x128.png',
  '/icons/icon-144x144.png',
  '/icons/icon-152x152.png',
  '/icons/icon-192x192.png',
  '/icons/icon-384x384.png',
  '/icons/icon-512x512.png'
];

// Install Event
self.addEventListener('install', (event: any) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('Precaching static assets...');
      return cache.addAll(PRECACHE_ASSETS);
    }).then(() => (self as any).skipWaiting())
  );
});

// Activate Event
self.addEventListener('activate', (event: any) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cache) => {
          if (cache !== CACHE_NAME && cache !== DATA_CACHE_NAME) {
            console.log('Clearing old cache:', cache);
            return caches.delete(cache);
          }
          return Promise.resolve(true);
        })
      );
    }).then(() => (self as any).clients.claim())
  );
});

// Fetch Event
self.addEventListener('fetch', (event: any) => {
  const requestUrl = new URL(event.request.url);

  // Stale-While-Revalidate for Search and Autocomplete API calls
  if (requestUrl.pathname.includes('/api/v1/search')) {
    event.respondWith(
      caches.open(DATA_CACHE_NAME).then((cache) => {
        return fetch(event.request)
          .then((response) => {
            // If response is valid, clone it and save to cache
            if (response.status === 200) {
              cache.put(event.request.url, response.clone());
            }
            return response;
          })
          .catch(() => {
            // If network fails, try getting from cache
            return cache.match(event.request.url).then((cachedResponse) => {
              if (cachedResponse) {
                return cachedResponse;
              }
              // Return a fallback JSON response if offline and not cached
              return new Response(
                JSON.stringify({
                  success: false,
                  message: 'You are currently offline. Please check your network connection.',
                  data: { results: [], nextCursor: null, totalCount: 0, executionTimeMs: 0 }
                }),
                {
                  headers: { 'Content-Type': 'application/json' },
                  status: 200 // Serve with 200 so frontend maps empty list gracefully
                }
              );
            });
          });
      })
    );
    return;
  }

  // Stale-While-Revalidate for general assets and UI static files
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        // Fetch fresh version in background and update cache
        fetch(event.request)
          .then((networkResponse) => {
            if (networkResponse.status === 200) {
              caches.open(CACHE_NAME).then((cache) => {
                cache.put(event.request, networkResponse);
              });
            }
          })
          .catch(() => {
            // Silently fail if background fetch fails
          });
        return cachedResponse;
      }

      return fetch(event.request).then((response) => {
        // Cache dynamic assets on-the-fly (e.g. images, stylesheets)
        if (
          response.status === 200 &&
          (event.request.destination === 'image' ||
            event.request.destination === 'style' ||
            event.request.destination === 'script' ||
            event.request.url.includes('unpkg.com'))
        ) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseClone);
          });
        }
        return response;
      });
    })
  );
});
