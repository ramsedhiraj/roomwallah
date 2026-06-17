import { apiClient } from './api';

// ─── Verification Types ──────────────────────────────────────

export interface VerificationRequest {
  id: string;
  userId: string;
  provider: string;
  requestStatus: string;
  verifiedName: string | null;
  confidenceScore: number | null;
  submittedAt: string;
  completedAt: string | null;
  expiresAt: string | null;
  rejectionReason: string | null;
  verificationVersion: number;
}

export interface TrustScore {
  overallScore: number;
  identityScore: number;
  propertyScore: number;
  reviewScore: number;
  activityScore: number;
  fraudPenalty: number;
  calculatedAt: string;
}

export interface FraudSignal {
  id: string;
  userId: string;
  signalType: string;
  severity: string;
  brokerRiskScore: number;
  description: string;
  createdAt: string;
}

export interface VerificationDecisionAudit {
  id: string;
  verificationRequestId: string;
  adminId: string;
  previousStatus: string | null;
  newStatus: string | null;
  decisionReason: string;
  correlationId: string;
  createdAt: string;
}

// ─── Verification API ────────────────────────────────────────

export async function submitIdentityVerification(
  provider: string,
  code: string,
  idempotencyKey?: string
): Promise<VerificationRequest> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  const response = await apiClient.post(
    '/verifications/identity',
    { provider, code },
    { headers }
  );
  return response.data.data;
}

export async function getActiveVerification(): Promise<VerificationRequest | null> {
  const response = await apiClient.get('/verifications/active');
  return response.data.data ?? null;
}

export async function getMyTrustScore(): Promise<TrustScore | null> {
  const response = await apiClient.get('/verifications/trust-score');
  return response.data.data ?? null;
}

export async function getMyFraudSignals(): Promise<FraudSignal[]> {
  const response = await apiClient.get('/verifications/fraud-signals');
  return response.data.data ?? [];
}

// ─── Admin Verification API ─────────────────────────────────

export async function getPendingVerifications(): Promise<VerificationRequest[]> {
  const response = await apiClient.get('/admin/verifications/pending');
  return response.data.data ?? [];
}

export async function getAllFraudSignals(): Promise<FraudSignal[]> {
  const response = await apiClient.get('/admin/verifications/fraud-signals');
  return response.data.data ?? [];
}

export async function approveVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/approve`, { reason });
  return response.data.data;
}

export async function rejectVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/reject`, { reason });
  return response.data.data;
}

export async function escalateVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/escalate`, { reason });
  return response.data.data;
}

export async function reopenVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/reopen`, { reason });
  return response.data.data;
}

export async function revokeVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/revoke`, { reason });
  return response.data.data;
}

export async function expireVerification(id: string, reason: string): Promise<VerificationRequest> {
  const response = await apiClient.post(`/admin/verifications/${id}/expire`, { reason });
  return response.data.data;
}

export async function getDecisionHistory(id: string): Promise<VerificationDecisionAudit[]> {
  const response = await apiClient.get(`/admin/verifications/${id}/history`);
  return response.data.data ?? [];
}
