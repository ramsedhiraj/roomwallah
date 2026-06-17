import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { 
  Building, 
  MapPin, 
  DollarSign, 
  Maximize, 
  Calendar, 
  BedDouble, 
  Bath, 
  Car, 
  Compass, 
  Activity, 
  Clock, 
  User as UserIcon, 
  ArrowLeft,
  Copy,
  Check,
  PawPrint,
  CheckCircle,
  EyeOff,
  Mail,
  MessageSquare,
  X,
  Loader2
} from 'lucide-react';
import { apiClient } from '../services/api';
import { Property } from '../types';
import SimilarListings from '../components/SimilarListings';
import RecommendedForYou from '../components/RecommendedForYou';

export default function PropertyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [property, setProperty] = useState<Property | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  
  // Contact Owner State
  const [isContactModalOpen, setIsContactModalOpen] = useState(false);
  const [inquiryText, setInquiryText] = useState('I am interested in this property. Please contact me.');
  const [contactPhone, setContactPhone] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactLoading, setContactLoading] = useState(false);
  const [contactSuccess, setContactSuccess] = useState<string | null>(null);
  const [contactError, setContactError] = useState<string | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    const fetchProperty = async () => {
      try {
        setLoading(true);
        const res = await apiClient.get(`/properties/${id}`);
        setProperty(res.data.data);
        setError(null);
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

  const handleCopyRef = () => {
    if (property) {
      navigator.clipboard.writeText(property.listingRef);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const formatAmenityLabel = (amenity: string) => {
    return amenity.split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
  };

  const handleContactSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!property) return;
    
    setContactLoading(true);
    setContactError(null);
    setContactSuccess(null);
    
    try {
      await apiClient.post('/leads', {
        propertyId: property.id,
        ownerId: property.ownerId,
        inquiryText,
        contactPhone: contactPhone || undefined,
        contactEmail: contactEmail || undefined
      });
      setContactSuccess('Your inquiry has been sent to the owner.');
      setTimeout(() => {
        setIsContactModalOpen(false);
        setContactSuccess(null);
      }, 3000);
    } catch (err: any) {
      console.error(err);
      setContactError(err.response?.data?.message || 'Failed to send inquiry. Please try again.');
    } finally {
      setContactLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 text-center">
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 mb-6 text-sm">
          {error || 'Listing not found or deleted'}
        </div>
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 hover:text-white hover:bg-slate-855 hover:border-slate-700 transition-all text-sm font-semibold"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Go Back</span>
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-in">
      <button 
        onClick={() => navigate(-1)} 
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm mb-6"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back</span>
      </button>

      {/* Main Grid: Details Left, Sidebar Right */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Column: Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main Title & Header */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8 space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-2.5 py-1 rounded bg-indigo-500/10 border border-indigo-500/20 text-xs font-bold text-indigo-400 uppercase tracking-widest">
                {property.propertyType.replace('_', ' ')}
              </span>
              <span className="px-2.5 py-1 rounded bg-emerald-500/10 border border-emerald-500/20 text-xs font-bold text-emerald-400 uppercase tracking-widest">
                For {property.listingPurpose}
              </span>
              {property.visibility !== 'PUBLIC' && (
                <span className="px-2.5 py-1 rounded bg-amber-500/10 border border-amber-500/20 text-xs font-bold text-amber-400 uppercase tracking-widest flex items-center gap-1">
                  <EyeOff className="w-3 h-3" />
                  {property.visibility}
                </span>
              )}
            </div>

            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white">
              {property.title}
            </h1>

            {property.slug && (
              <div className="text-xs text-indigo-400 font-mono bg-indigo-950/20 border border-indigo-900/30 px-3 py-1.5 rounded-lg inline-block">
                SEO Path: /properties/{property.slug}
              </div>
            )}

            <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-slate-900">
              <div className="flex items-center gap-2 text-slate-400 text-sm">
                <MapPin className="w-4 h-4 text-rose-500 shrink-0" />
                <span>
                  {property.address.line1 ? `${property.address.line1}, ` : ''}
                  {property.address.city}, {property.address.state}, {property.address.zipCode}
                </span>
              </div>

              {/* Public Ref Code badge */}
              <button 
                onClick={handleCopyRef}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-mono text-slate-350 transition-colors"
                title="Copy Listing Ref"
              >
                <span>REF: {property.listingRef}</span>
                {copied ? (
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
          </div>

          {/* Description */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8">
            <h3 className="text-lg font-bold text-slate-200 mb-4">Description</h3>
            <p className="text-slate-350 text-sm leading-relaxed whitespace-pre-wrap">
              {property.description || 'No description provided for this listing.'}
            </p>
          </div>

          {/* Key Specifications */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8">
            <h3 className="text-lg font-bold text-slate-200 mb-4">Key Specifications</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                <Maximize className="w-5 h-5 text-indigo-400" />
                <div>
                  <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Area Size</div>
                  <div className="text-sm font-bold text-slate-200">
                    {property.area.value} {property.area.unit.replace('_', ' ')}
                  </div>
                </div>
              </div>

              {property.bedrooms && (
                <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                  <BedDouble className="w-5 h-5 text-indigo-400" />
                  <div>
                    <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Bedrooms</div>
                    <div className="text-sm font-bold text-slate-200">{property.bedrooms} BHK</div>
                  </div>
                </div>
              )}

              {property.bathrooms && (
                <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                  <Bath className="w-5 h-5 text-indigo-400" />
                  <div>
                    <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Bathrooms</div>
                    <div className="text-sm font-bold text-slate-200">{property.bathrooms} Baths</div>
                  </div>
                </div>
              )}

              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                <Car className="w-5 h-5 text-indigo-400" />
                <div>
                  <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Parking</div>
                  <div className="text-sm font-bold text-slate-200">
                    {property.parkingCount > 0 ? `${property.parkingCount} (${property.parkingType || 'General'})` : 'No Parking'}
                  </div>
                </div>
              </div>

              {property.furnishingStatus && (
                <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                  <Building className="w-5 h-5 text-indigo-400" />
                  <div>
                    <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Furnishing</div>
                    <div className="text-sm font-bold text-slate-200 capitalize">
                      {property.furnishingStatus.toLowerCase().replace('_', ' ')}
                    </div>
                  </div>
                </div>
              )}

              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-900 flex items-center gap-3">
                <PawPrint className="w-5 h-5 text-indigo-400" />
                <div>
                  <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Pets Allowed</div>
                  <div className="text-sm font-bold text-slate-200">
                    {property.petFriendly ? 'Yes' : 'No'}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Extensible Amenities Section */}
          {property.amenities && property.amenities.length > 0 && (
            <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8 animate-fade-in">
              <h3 className="text-lg font-bold text-slate-200 mb-4">Amenities & Features</h3>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {property.amenities.map(amenity => (
                  <div 
                    key={amenity}
                    className="flex items-center gap-2 p-3 rounded-2xl bg-slate-950/40 border border-slate-900 text-sm text-slate-300"
                  >
                    <CheckCircle className="w-4 h-4 text-indigo-400 shrink-0" />
                    <span className="truncate">{formatAmenityLabel(amenity)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Enriched Optional Attributes */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8">
            <h3 className="text-lg font-bold text-slate-200 mb-4">Metadata & Features</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900">
                <span className="text-slate-400 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-indigo-400" /> Construction Year
                </span>
                <span className="font-semibold text-slate-200">{property.constructionYear || 'N/A'}</span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900">
                <span className="text-slate-400 flex items-center gap-2">
                  <Building className="w-4 h-4 text-indigo-400" /> Floor Level
                </span>
                <span className="font-semibold text-slate-200">
                  {property.floorNumber !== undefined ? `${property.floorNumber} / ${property.totalFloors || 'N/A'}` : 'N/A'}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900">
                <span className="text-slate-400 flex items-center gap-2">
                  <Compass className="w-4 h-4 text-indigo-400" /> Facing Direction
                </span>
                <span className="font-semibold text-slate-200 capitalize">{property.facingDirection || 'N/A'}</span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900">
                <span className="text-slate-400 flex items-center gap-2">
                  <Activity className="w-4 h-4 text-indigo-400" /> Possession Status
                </span>
                <span className="font-semibold text-slate-200 capitalize">{property.possessionStatus || 'N/A'}</span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900 sm:col-span-2">
                <span className="text-slate-400 flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-indigo-400" /> Availability Date
                </span>
                <span className="font-semibold text-slate-200">
                  {property.availabilityDate ? new Date(property.availabilityDate).toLocaleDateString('en-IN', {
                    day: 'numeric', month: 'long', year: 'numeric'
                  }) : 'Immediate'}
                </span>
              </div>

              {property.publishedAt && (
                <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900 sm:col-span-2 animate-fade-in">
                  <span className="text-slate-400 flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-emerald-400" /> Published On
                  </span>
                  <span className="font-semibold text-slate-200">
                    {new Date(property.publishedAt).toLocaleDateString('en-IN', {
                      day: 'numeric', month: 'long', year: 'numeric'
                    })}
                  </span>
                </div>
              )}

              {property.verifiedAt && (
                <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/50 border border-slate-900 sm:col-span-2 animate-fade-in">
                  <span className="text-slate-400 flex items-center gap-2">
                    <CheckCircle className="w-4 h-4 text-emerald-400" /> Verified On
                  </span>
                  <span className="font-semibold text-slate-200">
                    {new Date(property.verifiedAt).toLocaleDateString('en-IN', {
                      day: 'numeric', month: 'long', year: 'numeric'
                    })}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Pricing & Owner Details Sidebar */}
        <div className="space-y-6">
          {/* Pricing Box */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8 space-y-6">
            <div>
              <div className="text-xs text-slate-500 font-bold uppercase tracking-wider mb-1">Total Cost</div>
              <div className="flex items-baseline gap-1">
                <span className="text-3xl font-extrabold text-white">
                  ₹ {property.price.amount.toLocaleString('en-IN')}
                </span>
                <span className="text-slate-400 text-sm">/ {property.listingPurpose === 'RENT' ? 'month' : 'total'}</span>
              </div>
              {property.negotiable && (
                <span className="text-xs text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded mt-1.5 inline-block">
                  Price Negotiable
                </span>
              )}
            </div>

            {/* Additional Cost lines */}
            <div className="space-y-3 pt-4 border-t border-slate-900 text-sm">
              {property.securityDeposit && (
                <div className="flex justify-between items-center text-slate-350">
                  <span>Security Deposit:</span>
                  <span className="font-bold text-slate-200">
                    ₹ {property.securityDeposit.amount.toLocaleString('en-IN')}
                  </span>
                </div>
              )}

              {property.maintenanceCharges && (
                <div className="flex justify-between items-center text-slate-350">
                  <span>Maintenance Charges:</span>
                  <span className="font-bold text-slate-200">
                    ₹ {property.maintenanceCharges.amount.toLocaleString('en-IN')} /mo
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Owner Details Card */}
          <div className="glass border border-slate-800/80 rounded-3xl p-6 md:p-8 space-y-5 text-center">
            <div className="w-16 h-16 rounded-full bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-primary mx-auto">
              <UserIcon className="w-7 h-7" />
            </div>
            
            <div>
              <h3 className="font-bold text-slate-200">Property Owner</h3>
              <p className="text-xs text-slate-400 mt-1">Verified Owner on RoomWallah</p>
            </div>

            <Link 
              to={`/owners/${property.ownerId}`}
              className="flex items-center justify-center gap-1.5 w-full py-3 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-250 hover:text-white font-semibold transition-all text-sm mb-3"
            >
              <span>View Public Profile</span>
            </Link>

            <button
              onClick={() => setIsContactModalOpen(true)}
              className="flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-gradient-to-r from-primary to-secondary text-white font-bold shadow-lg shadow-indigo-500/20 hover:opacity-95 transition-all text-sm hover:-translate-y-0.5"
            >
              <MessageSquare className="w-4 h-4" />
              <span>Contact Owner</span>
            </button>
          </div>
        </div>
      </div>

      {/* Dynamic Recommendation & Similarity Widgets */}
      <div className="mt-12 pt-8 border-t border-slate-900 space-y-8">
        <SimilarListings 
          currentPropertyId={property.id} 
          propertyType={property.propertyType} 
          city={property.address.city} 
          locality={property.address.line2} 
        />
        <RecommendedForYou />
      </div>

      {/* Contact Owner Modal */}
      {isContactModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-slate-950 border border-slate-800 rounded-3xl w-full max-w-md overflow-hidden shadow-2xl">
            <div className="flex justify-between items-center p-6 border-b border-slate-900">
              <h3 className="text-xl font-bold text-white">Contact Owner</h3>
              <button 
                onClick={() => setIsContactModalOpen(false)}
                className="p-2 text-slate-400 hover:text-white hover:bg-slate-900 rounded-full transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form onSubmit={handleContactSubmit} className="p-6 space-y-5">
              {contactError && (
                <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm">
                  {contactError}
                </div>
              )}
              {contactSuccess && (
                <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
                  {contactSuccess}
                </div>
              )}

              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-300">Message</label>
                <textarea
                  required
                  rows={4}
                  value={inquiryText}
                  onChange={(e) => setInquiryText(e.target.value)}
                  className="w-full bg-slate-900/50 border border-slate-800 rounded-xl px-4 py-3 text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm resize-none"
                  placeholder="I am interested in this property..."
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-300">Your Phone (Optional)</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <MapPin className="w-4 h-4 text-slate-500" /> {/* Just a visual icon */}
                  </div>
                  <input
                    type="tel"
                    value={contactPhone}
                    onChange={(e) => setContactPhone(e.target.value)}
                    className="w-full bg-slate-900/50 border border-slate-800 rounded-xl pl-10 pr-4 py-3 text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                    placeholder="+91 99999 99999"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-300">Your Email (Optional)</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="w-4 h-4 text-slate-500" />
                  </div>
                  <input
                    type="email"
                    value={contactEmail}
                    onChange={(e) => setContactEmail(e.target.value)}
                    className="w-full bg-slate-900/50 border border-slate-800 rounded-xl pl-10 pr-4 py-3 text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                    placeholder="tenant@example.com"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={contactLoading || !!contactSuccess}
                className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold transition-all disabled:opacity-50 mt-4"
              >
                {contactLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <MessageSquare className="w-5 h-5" />}
                <span>Send Inquiry</span>
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
