import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { 
  Building2, 
  MapPin, 
  DollarSign, 
  Maximize2, 
  Info, 
  Save, 
  ArrowLeft,
  CheckCircle,
  AlertCircle,
  Check
} from 'lucide-react';
import { apiClient } from '../services/api';
import { PropertyType, ListingPurpose, FurnishingStatus, AreaUnit, PropertyVisibility } from '../types';

interface FormState {
  title: string;
  description: string;
  propertyType: PropertyType;
  listingPurpose: ListingPurpose;
  visibility: PropertyVisibility;
  priceAmount: string;
  priceCurrency: string;
  securityDepositAmount: string;
  securityDepositCurrency: string;
  maintenanceChargesAmount: string;
  maintenanceChargesCurrency: string;
  negotiable: boolean;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  country: string;
  zipCode: string;
  latitude: string;
  longitude: string;
  areaValue: string;
  areaUnit: AreaUnit;
  bedrooms: string;
  bathrooms: string;
  parkingCount: number;
  parkingType: string;
  furnishingStatus: FurnishingStatus | '';
  constructionYear: string;
  floorNumber: string;
  totalFloors: string;
  facingDirection: string;
  possessionStatus: string;
  petFriendly: boolean;
  availabilityDate: string;
  slug: string;
  amenities: string[];
}

const initialFormState: FormState = {
  title: '',
  description: '',
  propertyType: 'FLAT',
  listingPurpose: 'RENT',
  visibility: 'PUBLIC',
  priceAmount: '',
  priceCurrency: 'INR',
  securityDepositAmount: '',
  securityDepositCurrency: 'INR',
  maintenanceChargesAmount: '',
  maintenanceChargesCurrency: 'INR',
  negotiable: false,
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  country: 'India',
  zipCode: '',
  latitude: '',
  longitude: '',
  areaValue: '',
  areaUnit: 'SQ_FT',
  bedrooms: '',
  bathrooms: '',
  parkingCount: 0,
  parkingType: '',
  furnishingStatus: '',
  constructionYear: '',
  floorNumber: '',
  totalFloors: '',
  facingDirection: '',
  possessionStatus: '',
  petFriendly: false,
  availabilityDate: '',
  slug: '',
  amenities: []
};

const defaultAmenitiesList = [
  { key: 'LIFT', label: 'Elevator / Lift' },
  { key: 'BALCONY', label: 'Balcony' },
  { key: 'GYM', label: 'Gym / Fitness Center' },
  { key: 'POWER_BACKUP', label: 'Power Backup' },
  { key: 'SECURITY', label: '24/7 Security' },
  { key: 'SWIMMING_POOL', label: 'Swimming Pool' },
  { key: 'CLUBHOUSE', label: 'Club House' },
  { key: 'WIFI', label: 'Broadband Internet' },
  { key: 'WATER_HEATER', label: 'Water Heater / Geyser' },
  { key: 'AC', label: 'Air Conditioning' }
];

export default function EditPropertyPage() {
  const { id } = useParams<{ id: string }>();
  const [formData, setFormData] = useState<FormState>(initialFormState);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProperty = async () => {
      try {
        setLoading(true);
        const res = await apiClient.get(`/properties/${id}`);
        const prop = res.data.data;
        if (prop) {
          setFormData({
            title: prop.title || '',
            description: prop.description || '',
            propertyType: prop.propertyType || 'FLAT',
            listingPurpose: prop.listingPurpose || 'RENT',
            visibility: prop.visibility || 'PUBLIC',
            priceAmount: prop.price?.amount?.toString() || '',
            priceCurrency: prop.price?.currency || 'INR',
            securityDepositAmount: prop.securityDeposit?.amount?.toString() || '',
            securityDepositCurrency: prop.securityDeposit?.currency || 'INR',
            maintenanceChargesAmount: prop.maintenanceCharges?.amount?.toString() || '',
            maintenanceChargesCurrency: prop.maintenanceCharges?.currency || 'INR',
            negotiable: prop.negotiable || false,
            addressLine1: prop.address?.line1 || '',
            addressLine2: prop.address?.line2 || '',
            city: prop.address?.city || '',
            state: prop.address?.state || '',
            country: prop.address?.country || 'India',
            zipCode: prop.address?.zipCode || '',
            latitude: prop.geoLocation?.latitude?.toString() || '',
            longitude: prop.geoLocation?.longitude?.toString() || '',
            areaValue: prop.area?.value?.toString() || '',
            areaUnit: prop.area?.unit || 'SQ_FT',
            bedrooms: prop.bedrooms?.toString() || '',
            bathrooms: prop.bathrooms?.toString() || '',
            parkingCount: prop.parkingCount || 0,
            parkingType: prop.parkingType || '',
            furnishingStatus: prop.furnishingStatus || '',
            constructionYear: prop.constructionYear?.toString() || '',
            floorNumber: prop.floorNumber?.toString() || '',
            totalFloors: prop.totalFloors?.toString() || '',
            facingDirection: prop.facingDirection || '',
            possessionStatus: prop.possessionStatus || '',
            petFriendly: prop.petFriendly || false,
            availabilityDate: prop.availabilityDate || '',
            slug: prop.slug || '',
            amenities: prop.amenities || []
          });
        }
      } catch (err: any) {
        console.error(err);
        setError(err.response?.data?.message || 'Failed to fetch property details');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchProperty();
    }
  }, [id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    
    setFormData(prev => ({
      ...prev,
      [name]: isCheckbox ? (e.target as HTMLInputElement).checked : value
    }));
  };

  const handleParkingCount = (change: number) => {
    setFormData(prev => ({
      ...prev,
      parkingCount: Math.max(0, prev.parkingCount + change)
    }));
  };

  const handleAmenityChange = (amenityKey: string) => {
    setFormData(prev => {
      const isSelected = prev.amenities.includes(amenityKey);
      const amenities = isSelected
        ? prev.amenities.filter(a => a !== amenityKey)
        : [...prev.amenities, amenityKey];
      return { ...prev, amenities };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    const payload = {
      title: formData.title,
      description: formData.description || null,
      propertyType: formData.propertyType,
      listingPurpose: formData.listingPurpose,
      visibility: formData.visibility,
      price: {
        amount: parseFloat(formData.priceAmount),
        currency: formData.priceCurrency
      },
      securityDeposit: formData.securityDepositAmount ? {
        amount: parseFloat(formData.securityDepositAmount),
        currency: formData.securityDepositCurrency
      } : null,
      maintenanceCharges: formData.maintenanceChargesAmount ? {
        amount: parseFloat(formData.maintenanceChargesAmount),
        currency: formData.maintenanceChargesCurrency
      } : null,
      negotiable: formData.negotiable,
      address: {
        line1: formData.addressLine1 || null,
        line2: formData.addressLine2 || null,
        city: formData.city,
        state: formData.state,
        country: formData.country,
        zipCode: formData.zipCode
      },
      geoLocation: (formData.latitude && formData.longitude) ? {
        latitude: parseFloat(formData.latitude),
        longitude: parseFloat(formData.longitude)
      } : null,
      area: {
        value: parseFloat(formData.areaValue),
        unit: formData.areaUnit
      },
      bedrooms: formData.bedrooms ? parseInt(formData.bedrooms) : null,
      bathrooms: formData.bathrooms ? parseInt(formData.bathrooms) : null,
      parkingCount: formData.parkingCount,
      parkingType: formData.parkingType || null,
      furnishingStatus: formData.furnishingStatus || null,
      constructionYear: formData.constructionYear ? parseInt(formData.constructionYear) : null,
      floorNumber: formData.floorNumber ? parseInt(formData.floorNumber) : null,
      totalFloors: formData.totalFloors ? parseInt(formData.totalFloors) : null,
      facingDirection: formData.facingDirection || null,
      possessionStatus: formData.possessionStatus || null,
      petFriendly: formData.petFriendly,
      availabilityDate: formData.availabilityDate || null,
      slug: formData.slug || null,
      amenities: formData.amenities
    };

    try {
      await apiClient.put(`/properties/${id}`, payload);
      setSuccess(true);
      setTimeout(() => {
        navigate('/listings');
      }, 2000);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Update failed. Please check validation errors.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 animate-fade-in">
      <div className="mb-6">
        <button 
          onClick={() => navigate('/listings')} 
          className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Listings</span>
        </button>
      </div>

      <div className="glass border border-slate-800 rounded-3xl p-6 md:p-8">
        <div className="border-b border-slate-900 pb-6 mb-8">
          <h1 className="text-2xl font-extrabold tracking-tight bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
            Edit Property Listing
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Modify listing details, prices, and specifications. Changes will be updated live.
          </p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center min-h-[300px]">
            <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : success ? (
          <div className="text-center py-12">
            <CheckCircle className="w-16 h-16 text-emerald-500 mx-auto mb-4 animate-bounce" />
            <h2 className="text-2xl font-bold text-slate-200">Listing Updated Successfully!</h2>
            <p className="text-slate-400 mt-2">Redirecting to your listings dashboard...</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-8">
            {error && (
              <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2.5">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            {/* Section 1: Basic Details */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <Building2 className="w-5 h-5" />
                <span>Basic Details</span>
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="md:col-span-2 space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Property Title *</label>
                  <input
                    type="text"
                    name="title"
                    value={formData.title}
                    onChange={handleChange}
                    required
                    placeholder="e.g. Modern 2 BHK Apartment near IT Park"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">SEO Friendly Slug</label>
                  <input
                    type="text"
                    name="slug"
                    value={formData.slug}
                    onChange={handleChange}
                    placeholder="e.g. 2bhk-flat-baner (Optional)"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="md:col-span-3 space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Description</label>
                  <textarea
                    name="description"
                    value={formData.description}
                    onChange={handleChange}
                    rows={4}
                    placeholder="Describe amenities, location advantages, house rules, etc."
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all resize-none"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Property Type *</label>
                  <select
                    name="propertyType"
                    value={formData.propertyType}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-300 text-sm transition-all"
                  >
                    <option value="ROOM">Room</option>
                    <option value="FLAT">Flat / Apartment</option>
                    <option value="APARTMENT">Apartment Complex</option>
                    <option value="HOUSE">Independent House</option>
                    <option value="VILLA">Villa</option>
                    <option value="PG">PG (Paying Guest)</option>
                    <option value="HOSTEL">Hostel</option>
                    <option value="OFFICE">Office Space</option>
                    <option value="SHOP">Shop</option>
                    <option value="COMMERCIAL_SPACE">Commercial Space</option>
                    <option value="LAND">Land / Plot</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Listing Purpose *</label>
                  <select
                    name="listingPurpose"
                    value={formData.listingPurpose}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-300 text-sm transition-all"
                  >
                    <option value="RENT">Rent</option>
                    <option value="SALE">Sale</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Listing Visibility *</label>
                  <select
                    name="visibility"
                    value={formData.visibility}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-300 text-sm transition-all"
                  >
                    <option value="PUBLIC">Public</option>
                    <option value="PRIVATE">Private</option>
                    <option value="HIDDEN">Hidden</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Section 2: Pricing Details */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <DollarSign className="w-5 h-5" />
                <span>Pricing Model</span>
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Price Amount *</label>
                  <input
                    type="number"
                    name="priceAmount"
                    value={formData.priceAmount}
                    onChange={handleChange}
                    required
                    min="0"
                    placeholder="e.g. 15000"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Security Deposit</label>
                  <input
                    type="number"
                    name="securityDepositAmount"
                    value={formData.securityDepositAmount}
                    onChange={handleChange}
                    min="0"
                    placeholder="Optional (e.g. 30000)"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Maintenance Charges</label>
                  <input
                    type="number"
                    name="maintenanceChargesAmount"
                    value={formData.maintenanceChargesAmount}
                    onChange={handleChange}
                    min="0"
                    placeholder="Optional (e.g. 1500)"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="sm:col-span-3 py-1 flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="negotiable"
                    name="negotiable"
                    checked={formData.negotiable}
                    onChange={handleChange}
                    className="w-4.5 h-4.5 rounded bg-slate-950 border border-slate-800 focus:ring-indigo-500 accent-indigo-500 cursor-pointer"
                  />
                  <label htmlFor="negotiable" className="text-sm text-slate-350 cursor-pointer font-medium select-none">
                    Price is Negotiable
                  </label>
                </div>
              </div>
            </div>

            {/* Section 3: Address / Location Details */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <MapPin className="w-5 h-5" />
                <span>Location Details</span>
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Address Line 1</label>
                  <input
                    type="text"
                    name="addressLine1"
                    value={formData.addressLine1}
                    onChange={handleChange}
                    placeholder="Street Address, Block Number"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Address Line 2</label>
                  <input
                    type="text"
                    name="addressLine2"
                    value={formData.addressLine2}
                    onChange={handleChange}
                    placeholder="Apartment name, Suite, Phase"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4 md:col-span-2">
                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">City *</label>
                    <input
                      type="text"
                      name="city"
                      value={formData.city}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Pune"
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">State *</label>
                    <input
                      type="text"
                      name="state"
                      value={formData.state}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Maharashtra"
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">ZIP Code *</label>
                    <input
                      type="text"
                      name="zipCode"
                      value={formData.zipCode}
                      onChange={handleChange}
                      required
                      placeholder="e.g. 411001"
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Country *</label>
                    <input
                      type="text"
                      name="country"
                      value={formData.country}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 md:col-span-2">
                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Latitude</label>
                    <input
                      type="number"
                      step="any"
                      name="latitude"
                      value={formData.latitude}
                      onChange={handleChange}
                      placeholder="Optional"
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Longitude</label>
                    <input
                      type="number"
                      step="any"
                      name="longitude"
                      value={formData.longitude}
                      onChange={handleChange}
                      placeholder="Optional"
                      className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Section 4: Specifications */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <Maximize2 className="w-5 h-5" />
                <span>Property Specifications</span>
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Area Value *</label>
                  <input
                    type="number"
                    step="any"
                    name="areaValue"
                    value={formData.areaValue}
                    onChange={handleChange}
                    required
                    placeholder="e.g. 1200"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Area Unit *</label>
                  <select
                    name="areaUnit"
                    value={formData.areaUnit}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-300 text-sm transition-all"
                  >
                    <option value="SQ_FT">Square Feet (sq ft)</option>
                    <option value="SQ_M">Square Meters (sq m)</option>
                    <option value="ACRE">Acre</option>
                    <option value="HECTARE">Hectare</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Bedrooms (BHK)</label>
                  <input
                    type="number"
                    name="bedrooms"
                    value={formData.bedrooms}
                    onChange={handleChange}
                    placeholder="e.g. 2"
                    min="0"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Bathrooms</label>
                  <input
                    type="number"
                    name="bathrooms"
                    value={formData.bathrooms}
                    onChange={handleChange}
                    placeholder="e.g. 2"
                    min="0"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Furnishing Status</label>
                  <select
                    name="furnishingStatus"
                    value={formData.furnishingStatus}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-350 text-sm transition-all"
                  >
                    <option value="">Select Status</option>
                    <option value="UNFURNISHED">Unfurnished</option>
                    <option value="SEMI_FURNISHED">Semi-Furnished</option>
                    <option value="FULLY_FURNISHED">Fully-Furnished</option>
                  </select>
                </div>

                <div className="space-y-1.5 flex flex-col justify-between">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Parking Count</label>
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => handleParkingCount(-1)}
                      className="w-10 h-10 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 flex items-center justify-center text-slate-300 font-bold"
                    >
                      -
                    </button>
                    <span className="text-slate-200 font-bold text-sm w-8 text-center">{formData.parkingCount}</span>
                    <button
                      type="button"
                      onClick={() => handleParkingCount(1)}
                      className="w-10 h-10 rounded-lg bg-slate-950 border border-slate-800 hover:border-slate-700 flex items-center justify-center text-slate-300 font-bold"
                    >
                      +
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Section 5: Extensible Amenities */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <Building2 className="w-5 h-5" />
                <span>Amenities & Features</span>
              </h2>

              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                {defaultAmenitiesList.map(amenity => {
                  const isSelected = formData.amenities.includes(amenity.key);
                  return (
                    <button
                      key={amenity.key}
                      type="button"
                      onClick={() => handleAmenityChange(amenity.key)}
                      className={`flex items-center gap-2.5 p-3.5 rounded-xl border text-sm font-semibold transition-all text-left ${
                        isSelected 
                          ? 'bg-indigo-500/10 border-indigo-500 text-indigo-300 shadow-md shadow-indigo-500/5' 
                          : 'bg-slate-950 border-slate-850 hover:border-slate-750 text-slate-400 hover:text-slate-300'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded flex items-center justify-center border transition-all ${
                        isSelected ? 'bg-indigo-500 border-indigo-500 text-white' : 'border-slate-700'
                      }`}>
                        {isSelected && <Check className="w-3 h-3" />}
                      </div>
                      <span>{amenity.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Section 6: Metadata */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-indigo-400 flex items-center gap-2 border-b border-slate-900 pb-2">
                <Info className="w-5 h-5" />
                <span>Enriched Metadata (Optional)</span>
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Construction Year</label>
                  <input
                    type="number"
                    name="constructionYear"
                    value={formData.constructionYear}
                    onChange={handleChange}
                    placeholder="e.g. 2020"
                    min="1900"
                    max={new Date().getFullYear() + 5}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Floor Number</label>
                  <input
                    type="number"
                    name="floorNumber"
                    value={formData.floorNumber}
                    onChange={handleChange}
                    placeholder="e.g. 4"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Total Floors</label>
                  <input
                    type="number"
                    name="totalFloors"
                    value={formData.totalFloors}
                    onChange={handleChange}
                    placeholder="e.g. 10"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Facing Direction</label>
                  <input
                    type="text"
                    name="facingDirection"
                    value={formData.facingDirection}
                    onChange={handleChange}
                    placeholder="e.g. East, North-East"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Possession Status</label>
                  <input
                    type="text"
                    name="possessionStatus"
                    value={formData.possessionStatus}
                    onChange={handleChange}
                    placeholder="e.g. Immediate, Dec 2026"
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-200 text-sm transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Availability Date</label>
                  <input
                    type="date"
                    name="availabilityDate"
                    value={formData.availabilityDate}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 focus:border-indigo-500 outline-none text-slate-350 text-sm transition-all"
                  />
                </div>

                <div className="sm:col-span-3 py-1 flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="petFriendly"
                    name="petFriendly"
                    checked={formData.petFriendly}
                    onChange={handleChange}
                    className="w-4.5 h-4.5 rounded bg-slate-950 border border-slate-800 focus:ring-indigo-500 accent-indigo-500 cursor-pointer"
                  />
                  <label htmlFor="petFriendly" className="text-sm text-slate-350 cursor-pointer font-medium select-none">
                    Pet Friendly Listing
                  </label>
                </div>
              </div>
            </div>

            {/* Actions Footer */}
            <div className="pt-6 border-t border-slate-900 flex justify-between items-center gap-4 flex-wrap">
              <Link
                to={`/listings/media/${id}`}
                className="px-5 py-3 rounded-xl border border-indigo-500/30 hover:border-indigo-500/50 bg-indigo-500/5 text-indigo-300 hover:text-indigo-200 transition-all text-sm font-semibold animate-pulse"
              >
                Manage Media Assets
              </Link>
              <div className="flex items-center gap-4">
                <button
                  type="button"
                  onClick={() => navigate('/listings')}
                  className="px-5 py-3 rounded-xl border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white transition-all text-sm font-semibold"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-primary to-secondary text-white font-semibold shadow-lg shadow-indigo-500/10 hover:opacity-95 disabled:opacity-50 transition-all text-sm"
                >
                  {saving ? (
                    <div className="w-4.5 h-4.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  ) : (
                    <Save className="w-4 h-4" />
                  )}
                  <span>Save Changes</span>
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
