import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import ErrorBoundary from './components/ErrorBoundary.tsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);

// Register service worker for PWA capabilities
const disableSW = localStorage.getItem('disable-service-worker') === 'true';

if ('serviceWorker' in navigator && !disableSW) {
  let refreshing = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!refreshing) {
      refreshing = true;
      console.log('New Service Worker activated, refreshing client...');
      window.location.reload();
    }
  });

  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js')
      .then((registration) => {
        console.log('ServiceWorker registered with scope: ', registration.scope);
        // Force check for updated service worker script on server immediately
        registration.update().catch(() => {});
      })
      .catch((err) => {
        console.log('ServiceWorker registration failed: ', err);
      });
  });
}

