import { apiClient } from './api';

export interface TrustExplanationDto {
  overallScore: number;
  identityScore: number;
  propertyScore: number;
  reviewScore: number;
  activityScore: number;
  fraudPenalty: number;
  calculatedAt: string;
}

export async function getTrustScoreExplanation(): Promise<TrustExplanationDto | null> {
  const response = await apiClient.get('/verifications/trust-score');
  return response.data.data ?? null;
}

export const trustService = {
  getTrustScoreExplanation
};

export default trustService;
