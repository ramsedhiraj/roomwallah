import { apiClient } from './api';

// ─── Trust & Verification Types ──────────────────────────────

export type VerificationStatus =
  | 'SUBMITTED'
  | 'DOCUMENT_CHECK'
  | 'OCR'
  | 'FACE_MATCH'
  | 'IDENTITY_PROVIDER'
  | 'RISK_ASSESSMENT'
  | 'MANUAL_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

export type VerificationLevel =
  | 'LEVEL_0_UNVERIFIED'
  | 'LEVEL_1_BASIC'
  | 'LEVEL_2_DOCUMENTS'
  | 'LEVEL_3_FULL';

export type FraudSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ModerationCaseStatus = 'OPEN' | 'ASSIGNED' | 'IN_REVIEW' | 'ESCALATED' | 'RESOLVED' | 'CLOSED';
export type RiskDecision = 'ALLOW' | 'REQUIRE_REVIEW' | 'RESTRICT' | 'BLOCK';

export interface VerificationDocumentDto {
  id: string;
  mediaId: string;
  documentType: string;
  uploadedAt: string;
}

export interface OwnerVerificationDto {
  id: string;
  userId: string;
  verificationStatus: VerificationStatus;
  verificationLevel: VerificationLevel;
  verificationProvider: string | null;
  submittedAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  expiresAt: string | null;
  reviewerId: string | null;
  rejectionReason: string | null;
  version: number;
  documents: VerificationDocumentDto[];
}

export interface TrustFactor {
  name: string;
  scoreImpact: number;
  positive: boolean;
  description: string;
}

export interface TrustExplanationDto {
  userId: string;
  currentScore: number;
  scoreVersion: string;
  ruleVersion: string;
  algorithmVersion: string;
  calculatedAt: string;
  positiveFactors: TrustFactor[];
  negativeFactors: TrustFactor[];
}

export interface ModerationCaseDto {
  id: string;
  entityType: string;
  entityId: string;
  status: ModerationCaseStatus;
  assignedAdmin: string | null;
  priorityScore: number;
  createdAt: string;
  closedAt: string | null;
  notes: string | null;
  userName?: string;
  userEmail?: string;
}

export interface SubmitVerificationRequest {
  documentType: string;
  mediaId: string;
  selfieMediaId: string;
  idempotencyKey?: string;
}

// ─── Trust API Services ──────────────────────────────────────

export async function submitVerification(
  payload: SubmitVerificationRequest
): Promise<OwnerVerificationDto> {
  const headers: Record<string, string> = {};
  if (payload.idempotencyKey) {
    headers['Idempotency-Key'] = payload.idempotencyKey;
  }
  const response = await apiClient.post(
    '/trust/verification',
    {
      documentType: payload.documentType,
      mediaId: payload.mediaId,
      selfieMediaId: payload.selfieMediaId
    },
    { headers }
  );
  return response.data.data;
}

export async function getVerificationStatus(): Promise<OwnerVerificationDto | null> {
  const response = await apiClient.get('/trust/status');
  return response.data.data ?? null;
}

export async function getTrustScoreExplanation(): Promise<TrustExplanationDto | null> {
  const response = await apiClient.get('/trust/score');
  return response.data.data ?? null;
}

export async function getPublicUserTrust(userId: string): Promise<{
  userId: string;
  trustScore: number;
  badges: string[];
}> {
  const response = await apiClient.get(`/users/${userId}/trust`);
  return response.data.data;
}

// ─── Admin Trust API Services ───────────────────────────────

export async function getModerationCases(): Promise<ModerationCaseDto[]> {
  const response = await apiClient.get('/admin/trust/cases');
  return response.data.data ?? [];
}

export async function approveModerationCase(
  id: string,
  reason: string,
  idempotencyKey?: string
): Promise<OwnerVerificationDto> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  const response = await apiClient.post(
    `/admin/trust/${id}/approve`,
    { reason },
    { headers }
  );
  return response.data.data;
}

export async function rejectModerationCase(
  id: string,
  reason: string,
  idempotencyKey?: string
): Promise<OwnerVerificationDto> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  const response = await apiClient.post(
    `/admin/trust/${id}/reject`,
    { reason },
    { headers }
  );
  return response.data.data;
}

export async function triggerTrustRecalculation(): Promise<void> {
  await apiClient.post('/admin/trust/recalculate');
}

export const trustService = {
  submitVerification,
  getVerificationStatus,
  getTrustScoreExplanation,
  getPublicUserTrust,
  getModerationCases,
  approveModerationCase,
  rejectModerationCase,
  triggerTrustRecalculation
};

export default trustService;
