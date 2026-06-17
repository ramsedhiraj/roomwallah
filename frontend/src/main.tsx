import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// Register service worker for PWA capabilities
const disableSW = localStorage.getItem('disable-service-worker') === 'true';

if ('serviceWorker' in navigator && !disableSW) {
  if (import.meta.env.PROD) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/service-worker.js')
        .then((registration) => {
          console.log('ServiceWorker registration successful with scope: ', registration.scope);
        })
        .catch((err) => {
          console.log('ServiceWorker registration failed: ', err);
        });
    });
  } else if (import.meta.env.DEV) {
    // Allow service worker in development for testing
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/service-worker.js')
        .then((registration) => {
          console.log('ServiceWorker (Dev) registered: ', registration.scope);
        })
        .catch((err) => {
          console.log('ServiceWorker (Dev) registration failed: ', err);
        });
    });
  }
}

