import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import ProtectedRoute from '../components/ProtectedRoute';

// Lazy load all pages for optimal bundle size & code splitting
const LandingPage = lazy(() => import('../pages/LandingPage'));
const LoginPage = lazy(() => import('../pages/LoginPage'));
const RegisterPage = lazy(() => import('../pages/RegisterPage'));
const ProfilePage = lazy(() => import('../pages/ProfilePage'));
const ChangePasswordPage = lazy(() => import('../pages/ChangePasswordPage'));
const SettingsPage = lazy(() => import('../pages/SettingsPage'));
const OwnerProfilePage = lazy(() => import('../pages/OwnerProfilePage'));
const MyListingsPage = lazy(() => import('../pages/MyListingsPage'));
const CreatePropertyPage = lazy(() => import('../pages/CreatePropertyPage'));
const EditPropertyPage = lazy(() => import('../pages/EditPropertyPage'));
const PropertyDetailPage = lazy(() => import('../pages/PropertyDetailPage'));
const PropertyMediaManagerPage = lazy(() => import('../pages/PropertyMediaManagerPage'));
const SearchPage = lazy(() => import('../pages/SearchPage'));
const WishlistPage = lazy(() => import('../pages/WishlistPage'));
const ChatPage = lazy(() => import('../pages/ChatPage'));

// Phase 8 Pages
const BookVisitPage = lazy(() => import('../pages/BookVisitPage'));
const MyBookingsPage = lazy(() => import('../pages/MyBookingsPage'));
const OwnerBookingDashboard = lazy(() => import('../pages/OwnerBookingDashboard'));
const VisitCalendar = lazy(() => import('../pages/VisitCalendar'));
const LeadInbox = lazy(() => import('../pages/LeadInbox'));
const BookingAdminDashboard = lazy(() => import('../pages/BookingAdminDashboard'));

// Phase 9 Pages
const CheckoutPage = lazy(() => import('../pages/CheckoutPage'));
const MyPaymentsPage = lazy(() => import('../pages/MyPaymentsPage'));
const PaymentStatusPage = lazy(() => import('../pages/PaymentStatusPage'));
const RefundCenterPage = lazy(() => import('../pages/RefundCenterPage'));
const InvoiceCenterPage = lazy(() => import('../pages/InvoiceCenterPage'));
const EarningsDashboardPage = lazy(() => import('../pages/EarningsDashboardPage'));
const PendingPayoutsPage = lazy(() => import('../pages/PendingPayoutsPage'));
const PaymentMonitorPage = lazy(() => import('../pages/PaymentMonitorPage'));
const FraudConsolePage = lazy(() => import('../pages/FraudConsolePage'));
const ReconciliationDashboardPage = lazy(() => import('../pages/ReconciliationDashboardPage'));
const WebhookEventsPage = lazy(() => import('../pages/WebhookEventsPage'));
const DisputeQueuePage = lazy(() => import('../pages/DisputeQueuePage'));
const FinanceAnalyticsPage = lazy(() => import('../pages/FinanceAnalyticsPage'));

// Phase 10 Pages
const AuditLogsPage = lazy(() => import('../pages/AuditLogsPage'));
const PreferencesPage = lazy(() => import('../pages/PreferencesPage'));
const NotificationHistory = lazy(() => import('../pages/NotificationHistory'));
const FraudDashboard = lazy(() => import('../pages/FraudDashboard'));
const RiskCasePage = lazy(() => import('../pages/RiskCasePage'));
const DeveloperPortal = lazy(() => import('../pages/DeveloperPortal'));
const ApiKeysPage = lazy(() => import('../pages/ApiKeysPage'));
const SystemHealthDashboard = lazy(() => import('../pages/SystemHealthDashboard'));
const ServiceStatusPage = lazy(() => import('../pages/ServiceStatusPage'));
const CacheMonitoringDashboard = lazy(() => import('../pages/CacheMonitoringDashboard'));

// Phase 11 AI & Analytics Pages
const OwnerPricingInsights = lazy(() => import('../pages/OwnerPricingInsights'));
const ListingHealthDashboard = lazy(() => import('../pages/ListingHealthDashboard'));

// Phase 12 Pages
const CostDashboard = lazy(() => import('../pages/CostDashboard'));
const ExperimentDashboard = lazy(() => import('../pages/ExperimentDashboard'));
const DataQualityDashboard = lazy(() => import('../pages/DataQualityDashboard'));
const PluginManager = lazy(() => import('../pages/PluginManager'));
const RegionalHealthDashboard = lazy(() => import('../pages/RegionalHealthDashboard'));
const DocumentVault = lazy(() => import('../pages/DocumentVault'));
const AgreementManagement = lazy(() => import('../pages/AgreementManagement'));

// Premium staggered pulsing loader matching RoomWallah dark-theme design tokens
const PageLoader = () => (
  <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 bg-[#090d16]">
    <div className="flex items-center justify-center space-x-2">
      <div className="w-4.5 h-4.5 rounded-full bg-primary animate-bounce [animation-delay:-0.3s]"></div>
      <div className="w-4.5 h-4.5 rounded-full bg-secondary animate-bounce [animation-delay:-0.15s]"></div>
      <div className="w-4.5 h-4.5 rounded-full bg-indigo-500 animate-bounce"></div>
    </div>
    <p className="mt-5 text-sm text-slate-400 font-medium tracking-wide">Loading RoomWallah...</p>
  </div>
);

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<LandingPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          
          {/* Public Property Details */}
          <Route path="properties/:id" element={<PropertyDetailPage />} />

          {/* Public Search Page */}
          <Route path="search" element={<SearchPage />} />

          {/* Public System Status Page */}
          <Route path="status" element={<ServiceStatusPage />} />
          
          {/* Protected Routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="profile" element={<ProfilePage />} />
            <Route path="wishlist" element={<WishlistPage />} />
            <Route path="chat" element={<ChatPage />} />
            <Route path="chat/:id" element={<ChatPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="change-password" element={<ChangePasswordPage />} />
            <Route path="listings" element={<MyListingsPage />} />
            <Route path="listings/create" element={<CreatePropertyPage />} />
            <Route path="listings/edit/:id" element={<EditPropertyPage />} />
            <Route path="listings/media/:id" element={<PropertyMediaManagerPage />} />
            {/* Phase 8 Bounded Context Routes */}
            <Route path="properties/:id/book-visit" element={<BookVisitPage />} />
            <Route path="bookings" element={<MyBookingsPage />} />
            <Route path="visits/me" element={<MyBookingsPage />} />
            <Route path="listings/bookings" element={<OwnerBookingDashboard />} />
            <Route path="listings/calendar" element={<VisitCalendar />} />
            <Route path="listings/leads" element={<LeadInbox />} />
            <Route path="admin/bookings" element={<BookingAdminDashboard />} />

            {/* Phase 9 Payment Routes — Tenant */}
            <Route path="checkout/:bookingId" element={<CheckoutPage />} />
            <Route path="payments" element={<MyPaymentsPage />} />
            <Route path="payments/:paymentId/status" element={<PaymentStatusPage />} />
            <Route path="refunds" element={<RefundCenterPage />} />
            <Route path="invoices" element={<InvoiceCenterPage />} />

            {/* Phase 9 Payment Routes — Owner */}
            <Route path="earnings" element={<EarningsDashboardPage />} />
            <Route path="payouts" element={<PendingPayoutsPage />} />

            {/* Phase 9 Payment Routes — Admin */}
            <Route path="admin/payments" element={<PaymentMonitorPage />} />
            <Route path="admin/payments/fraud" element={<FraudConsolePage />} />
            <Route path="admin/payments/reconcile" element={<ReconciliationDashboardPage />} />
            <Route path="admin/payments/webhooks" element={<WebhookEventsPage />} />
            <Route path="admin/payments/disputes" element={<DisputeQueuePage />} />
            <Route path="admin/payments/analytics" element={<FinanceAnalyticsPage />} />

            {/* Phase 10 Enterprise Production Platform Routes */}
            <Route path="settings/preferences" element={<PreferencesPage />} />
            <Route path="notifications" element={<NotificationHistory />} />
            
            {/* Developer Routes */}
            <Route path="developer" element={<DeveloperPortal />} />
            <Route path="developer/keys" element={<ApiKeysPage />} />

            {/* Admin Enterprise Routes */}
            <Route path="admin/audit-logs" element={<AuditLogsPage />} />
            <Route path="admin/fraud" element={<FraudDashboard />} />
            <Route path="admin/fraud/risk-cases/:id" element={<RiskCasePage />} />
            <Route path="admin/system-health" element={<SystemHealthDashboard />} />
            <Route path="admin/cache" element={<CacheMonitoringDashboard />} />

            {/* Phase 11 AI & Analytics Routes */}
            <Route path="listings/pricing-insights" element={<OwnerPricingInsights />} />
            <Route path="listings/health" element={<ListingHealthDashboard />} />

            {/* Phase 12 Enterprise Scale, Trust, Compliance & Ecosystem Routes */}
            <Route path="admin/costs" element={<CostDashboard />} />
            <Route path="admin/experiments" element={<ExperimentDashboard />} />
            <Route path="admin/data-quality" element={<DataQualityDashboard />} />
            <Route path="admin/plugins" element={<PluginManager />} />
            <Route path="admin/regions" element={<RegionalHealthDashboard />} />
            <Route path="vault/documents" element={<DocumentVault />} />
            <Route path="agreements" element={<AgreementManagement />} />
          </Route>

          {/* Public Owner Profiles */}
          <Route path="owners/:id" element={<OwnerProfilePage />} />
        </Route>
        <Route path="*" element={
          <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 bg-[#090d16]">
            <h1 className="text-6xl font-extrabold text-primary mb-4">404</h1>
            <p className="text-xl text-slate-400 mb-6">The page you are looking for does not exist.</p>
            <a href="/" className="px-6 py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 shadow-md transition-all">
              Return Home
            </a>
          </div>
        } />
      </Routes>
    </Suspense>
  );
}
