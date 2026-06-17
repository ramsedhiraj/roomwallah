import { Routes, Route } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import LandingPage from '../pages/LandingPage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import ProfilePage from '../pages/ProfilePage';
import ChangePasswordPage from '../pages/ChangePasswordPage';
import SettingsPage from '../pages/SettingsPage';
import OwnerProfilePage from '../pages/OwnerProfilePage';
import ProtectedRoute from '../components/ProtectedRoute';
import MyListingsPage from '../pages/MyListingsPage';
import CreatePropertyPage from '../pages/CreatePropertyPage';
import EditPropertyPage from '../pages/EditPropertyPage';
import PropertyDetailPage from '../pages/PropertyDetailPage';
import PropertyMediaManagerPage from '../pages/PropertyMediaManagerPage';
import SearchPage from '../pages/SearchPage';
import VerificationWizardPage from '../pages/VerificationWizardPage';
import AdminModerationDashboard from '../pages/AdminModerationDashboard';

// Phase 8 Pages
import BookVisitPage from '../pages/BookVisitPage';
import MyBookingsPage from '../pages/MyBookingsPage';
import OwnerBookingDashboard from '../pages/OwnerBookingDashboard';
import VisitCalendar from '../pages/VisitCalendar';
import LeadInbox from '../pages/LeadInbox';
import BookingAdminDashboard from '../pages/BookingAdminDashboard';

// Phase 9 Pages
import CheckoutPage from '../pages/CheckoutPage';
import MyPaymentsPage from '../pages/MyPaymentsPage';
import PaymentStatusPage from '../pages/PaymentStatusPage';
import RefundCenterPage from '../pages/RefundCenterPage';
import InvoiceCenterPage from '../pages/InvoiceCenterPage';
import EarningsDashboardPage from '../pages/EarningsDashboardPage';
import PendingPayoutsPage from '../pages/PendingPayoutsPage';
import PaymentMonitorPage from '../pages/PaymentMonitorPage';
import FraudConsolePage from '../pages/FraudConsolePage';
import ReconciliationDashboardPage from '../pages/ReconciliationDashboardPage';
import WebhookEventsPage from '../pages/WebhookEventsPage';
import DisputeQueuePage from '../pages/DisputeQueuePage';
import FinanceAnalyticsPage from '../pages/FinanceAnalyticsPage';

// Phase 10 Pages
import AdminAnalyticsDashboard from '../pages/AdminAnalyticsDashboard';
import BusinessInsightsPage from '../pages/BusinessInsightsPage';
import AuditLogsPage from '../pages/AuditLogsPage';
import PreferencesPage from '../pages/PreferencesPage';
import NotificationHistory from '../pages/NotificationHistory';
import FraudDashboard from '../pages/FraudDashboard';
import RiskCasePage from '../pages/RiskCasePage';
import DeveloperPortal from '../pages/DeveloperPortal';
import ApiKeysPage from '../pages/ApiKeysPage';
import SystemHealthDashboard from '../pages/SystemHealthDashboard';
import ServiceStatusPage from '../pages/ServiceStatusPage';
import RecommendationConfig from '../pages/RecommendationConfig';
import CacheMonitoringDashboard from '../pages/CacheMonitoringDashboard';

// Phase 11 AI & Analytics Pages
import AiSearchExperience from '../pages/AiSearchExperience';
import SemanticSearchConsole from '../pages/SemanticSearchConsole';
import PersonalizedFeed from '../pages/PersonalizedFeed';
import OwnerPricingInsights from '../pages/OwnerPricingInsights';
import ListingHealthDashboard from '../pages/ListingHealthDashboard';
import DuplicateDetectionReview from '../pages/DuplicateDetectionReview';
import AiAnalyticsDashboard from '../pages/AiAnalyticsDashboard';
import AiPropertyAssistant from '../pages/AiPropertyAssistant';

// Phase 12 Pages
import CostDashboard from '../pages/CostDashboard';
import ExperimentDashboard from '../pages/ExperimentDashboard';
import SearchEvaluationDashboard from '../pages/SearchEvaluationDashboard';
import DataQualityDashboard from '../pages/DataQualityDashboard';
import PluginManager from '../pages/PluginManager';
import RegionalHealthDashboard from '../pages/RegionalHealthDashboard';
import DocumentVault from '../pages/DocumentVault';
import AgreementManagement from '../pages/AgreementManagement';
import TrustScoreConsole from '../pages/TrustScoreConsole';
import VerificationQueue from '../pages/VerificationQueue';

export default function AppRoutes() {
  return (
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
          <Route path="settings" element={<SettingsPage />} />
          <Route path="change-password" element={<ChangePasswordPage />} />
          <Route path="listings" element={<MyListingsPage />} />
          <Route path="listings/create" element={<CreatePropertyPage />} />
          <Route path="listings/edit/:id" element={<EditPropertyPage />} />
          <Route path="listings/media/:id" element={<PropertyMediaManagerPage />} />
          <Route path="trust/verify" element={<VerificationWizardPage />} />
          <Route path="admin/trust" element={<AdminModerationDashboard />} />
          
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
          <Route path="admin/analytics" element={<AdminAnalyticsDashboard />} />
          <Route path="admin/insights" element={<BusinessInsightsPage />} />
          <Route path="admin/audit-logs" element={<AuditLogsPage />} />
          <Route path="admin/fraud" element={<FraudDashboard />} />
          <Route path="admin/fraud/risk-cases/:id" element={<RiskCasePage />} />
          <Route path="admin/system-health" element={<SystemHealthDashboard />} />
          <Route path="admin/recommendations" element={<RecommendationConfig />} />
          <Route path="admin/cache" element={<CacheMonitoringDashboard />} />

          {/* Phase 11 AI & Analytics Routes */}
          <Route path="search/ai" element={<AiSearchExperience />} />
          <Route path="personalized-feed" element={<PersonalizedFeed />} />
          <Route path="listings/pricing-insights" element={<OwnerPricingInsights />} />
          <Route path="listings/health" element={<ListingHealthDashboard />} />
          <Route path="admin/duplicates" element={<DuplicateDetectionReview />} />
          <Route path="admin/semantic-search" element={<SemanticSearchConsole />} />
          <Route path="admin/ai-analytics" element={<AiAnalyticsDashboard />} />
          <Route path="assistant" element={<AiPropertyAssistant isPage={true} />} />

          {/* Phase 12 Enterprise Scale, Trust, Compliance & Ecosystem Routes */}
          <Route path="admin/costs" element={<CostDashboard />} />
          <Route path="admin/experiments" element={<ExperimentDashboard />} />
          <Route path="admin/search/evaluations" element={<SearchEvaluationDashboard />} />
          <Route path="admin/data-quality" element={<DataQualityDashboard />} />
          <Route path="admin/plugins" element={<PluginManager />} />
          <Route path="admin/regions" element={<RegionalHealthDashboard />} />
          <Route path="vault/documents" element={<DocumentVault />} />
          <Route path="agreements" element={<AgreementManagement />} />
          <Route path="trust/console" element={<TrustScoreConsole />} />
          <Route path="admin/verification-queue" element={<VerificationQueue />} />
        </Route>

        {/* Public Owner Profiles */}
        <Route path="owners/:id" element={<OwnerProfilePage />} />
      </Route>
      <Route path="*" element={
        <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
          <h1 className="text-6xl font-extrabold text-primary mb-4">404</h1>
          <p className="text-xl text-muted-foreground mb-6">The page you are looking for does not exist.</p>
          <a href="/" className="px-6 py-3 bg-primary text-primary-foreground font-semibold rounded-lg hover:bg-opacity-95 transition-all">
            Return Home
          </a>
        </div>
      } />
    </Routes>
  );
}
