export type PropertyType = 'ROOM' | 'FLAT' | 'APARTMENT' | 'HOUSE' | 'VILLA' | 'PG' | 'HOSTEL' | 'OFFICE' | 'SHOP' | 'COMMERCIAL_SPACE' | 'LAND';
export type ListingPurpose = 'RENT' | 'SALE';
export type PropertyStatus = 'DRAFT' | 'PENDING_VERIFICATION' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED' | 'REJECTED' | 'SOLD' | 'RENTED';
export type FurnishingStatus = 'UNFURNISHED' | 'SEMI_FURNISHED' | 'FULLY_FURNISHED';
export type AreaUnit = 'SQ_FT' | 'SQ_M' | 'ACRE' | 'HECTARE';
export type PropertyVisibility = 'PUBLIC' | 'PRIVATE' | 'HIDDEN';

export interface Money {
  amount: number;
  currency: string;
}

export interface Address {
  line1: string;
  line2?: string;
  city: string;
  state: string;
  country: string;
  zipCode: string;
}

export interface GeoLocation {
  latitude: number;
  longitude: number;
}

export interface AreaMeasurement {
  value: number;
  unit: AreaUnit;
}

export interface Property {
  id: string;
  listingRef: string;
  ownerId: string;
  title: string;
  description?: string;
  propertyType: PropertyType;
  listingPurpose: ListingPurpose;
  status: PropertyStatus;
  visibility: PropertyVisibility;
  price: Money;
  securityDeposit?: Money;
  maintenanceCharges?: Money;
  negotiable: boolean;
  address: Address;
  geoLocation?: GeoLocation;
  area: AreaMeasurement;
  bedrooms?: number;
  bathrooms?: number;
  parkingCount: number;
  parkingType?: string;
  furnishingStatus?: FurnishingStatus;
  constructionYear?: number;
  floorNumber?: number;
  totalFloors?: number;
  facingDirection?: string;
  possessionStatus?: string;
  petFriendly: boolean;
  availabilityDate?: string;
  
  // Lifecycle Timestamps
  publishedAt?: string;
  verifiedAt?: string;
  archivedAt?: string;

  // Moderation Metadata
  moderationStatus?: string;
  moderationReason?: string;
  reviewedBy?: string;
  reviewedAt?: string;

  // SEO Friendly Slug
  slug?: string;

  // Extensible amenities list
  amenities?: string[];

  createdAt: string;
  updatedAt: string;
  version: number;
}
