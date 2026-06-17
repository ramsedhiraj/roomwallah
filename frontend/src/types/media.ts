export type MediaType = 'IMAGE' | 'VIDEO' | 'FLOOR_PLAN' | 'VIRTUAL_TOUR' | 'PANORAMA' | 'GLB' | 'GLTF' | 'USDZ' | 'OBJ';
export type ProcessingStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';
export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface PropertyMedia {
  id: string;
  propertyId: string;
  objectKey: string;
  mediaType: MediaType;
  processingStatus: ProcessingStatus;
  moderationStatus: ModerationStatus;
  displayOrder: number;
  isCover: boolean;
  mimeType: string;
  fileSize: number;
  width?: number;
  height?: number;
  durationSeconds?: number;
  url: string;
  createdAt: string;
  updatedAt: string;
}
