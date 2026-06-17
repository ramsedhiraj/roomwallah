import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Plus, 
  MapPin, 
  DollarSign, 
  FileText, 
  ExternalLink, 
  Eye, 
  Edit3, 
  Trash2, 
  Archive, 
  Pause, 
  Play, 
  CheckCircle,
  Copy,
  Check,
  Image as ImageIcon
} from 'lucide-react';
import { apiClient } from '../services/api';
import { Property, PropertyStatus } from '../types';

export default function MyListingsPage() {
  const [properties, setProperties] = useState<Property[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const fetchProperties = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/properties/me');
      setProperties(res.data.data || []);
      setError(null);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to fetch your property listings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProperties();
  }, []);

  const handleCopyRef = (ref: string) => {
    navigator.clipboard.writeText(ref);
    setCopiedId(ref);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleAction = async (id: string, action: 'submit' | 'publish' | 'pause' | 'archive' | 'delete') => {
    try {
      setError(null);
      setSuccessMsg(null);
      if (action === 'delete') {
        if (window.confirm('Are you sure you want to delete this listing? This will perform a soft-delete.')) {
          await apiClient.delete(`/properties/${id}`);
          setSuccessMsg('Listing deleted successfully');
        } else {
          return;
        }
      } else {
        await apiClient.post(`/properties/${id}/${action}`);
        setSuccessMsg(`Listing ${action}ed successfully`);
      }
      fetchProperties();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || `Failed to perform action: ${action}`);
    }
  };

  const getStatusColor = (status: PropertyStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
      case 'DRAFT':
        return 'bg-blue-500/10 text-blue-400 border border-blue-500/20';
      case 'PENDING_VERIFICATION':
        return 'bg-amber-500/10 text-amber-400 border border-amber-500/20';
      case 'PAUSED':
        return 'bg-slate-500/10 text-slate-400 border border-slate-500/20';
      case 'REJECTED':
        return 'bg-rose-500/10 text-rose-400 border border-rose-500/20';
      case 'ARCHIVED':
      default:
        return 'bg-gray-500/10 text-gray-500 border border-gray-500/20';
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
            My Property Listings
          </h1>
          <p className="text-slate-400 mt-1">
            Manage your broker-free properties, monitor verification status, and publish listings.
          </p>
        </div>
        <Link 
          to="/listings/create" 
          className="flex items-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-primary to-secondary text-white font-semibold shadow-lg shadow-indigo-500/10 hover:opacity-95 transition-all hover:translate-y-[-1px] active:translate-y-[0px] text-sm"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Property</span>
        </Link>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm">
          {error}
        </div>
      )}

      {successMsg && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
          {successMsg}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
      ) : properties.length === 0 ? (
        <div className="glass border border-slate-800 rounded-3xl p-12 text-center max-w-xl mx-auto mt-8">
          <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-primary mx-auto mb-4">
            <FileText className="w-8 h-8" />
          </div>
          <h3 className="text-lg font-bold text-slate-200">No properties found</h3>
          <p className="text-slate-400 text-sm mt-2 mb-6">
            You haven't added any listings yet. Create a draft property listing to get started.
          </p>
          <Link 
            to="/listings/create" 
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-200 font-semibold hover:bg-slate-850 hover:border-slate-700 transition-all text-sm"
          >
            Create First Listing
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {properties.map((prop) => (
            <div 
              key={prop.id} 
              className="glass border border-slate-800/80 rounded-2xl overflow-hidden hover:border-slate-700 transition-all duration-300 flex flex-col group"
            >
              {/* Card Header (Preview Mock Image) */}
              <div className="h-44 bg-gradient-to-br from-slate-900 via-indigo-950/20 to-slate-950 relative flex items-center justify-center overflow-hidden border-b border-slate-900">
                <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:14px_24px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)]"></div>
                <div className="text-center p-4 z-10 transition-transform group-hover:scale-105 duration-300">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest">{prop.propertyType}</span>
                  <h4 className="text-slate-200 font-bold mt-1 line-clamp-2 px-4">{prop.title}</h4>
                </div>
                
                {/* Floating Status Pill */}
                <div className="absolute top-4 right-4 z-20">
                  <span className={`px-2.5 py-1 rounded-full text-xs font-semibold uppercase tracking-wider ${getStatusColor(prop.status)}`}>
                    {prop.status.replace('_', ' ')}
                  </span>
                </div>
              </div>

              {/* Card Body */}
              <div className="p-5 flex-grow flex flex-col justify-between">
                <div>
                  {/* Stable Ref Bubble */}
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-xs text-slate-500 font-medium">LISTING REF:</span>
                    <button 
                      onClick={() => handleCopyRef(prop.listingRef)}
                      className="flex items-center gap-1.5 px-2 py-1 rounded bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-mono text-slate-300 transition-colors"
                      title="Copy Listing Ref"
                    >
                      <span>{prop.listingRef}</span>
                      {copiedId === prop.listingRef ? (
                        <Check className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
                      ) : (
                        <Copy className="w-3.5 h-3.5" />
                      )}
                    </button>
                  </div>

                  {/* Core Details */}
                  <div className="grid grid-cols-2 gap-4 mb-4 text-sm text-slate-400">
                    <div className="flex items-center gap-1.5">
                      <DollarSign className="w-4 h-4 text-emerald-500" />
                      <span className="font-bold text-slate-200">
                        {prop.price?.amount?.toLocaleString('en-IN')} /mo
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-rose-500" />
                      <span className="truncate">{prop.address.city}, {prop.address.state}</span>
                    </div>
                  </div>

                  {/* Optional Metadata Summary */}
                  <div className="flex flex-wrap gap-2 mb-6">
                    {prop.bedrooms && (
                      <span className="px-2 py-0.5 rounded bg-slate-950 border border-slate-900 text-xs text-slate-400">
                        {prop.bedrooms} BHK
                      </span>
                    )}
                    {prop.area && (
                      <span className="px-2 py-0.5 rounded bg-slate-950 border border-slate-900 text-xs text-slate-400">
                        {prop.area.value} {prop.area.unit.replace('_', ' ')}
                      </span>
                    )}
                    {prop.furnishingStatus && (
                      <span className="px-2 py-0.5 rounded bg-slate-950 border border-slate-900 text-xs text-slate-400">
                        {prop.furnishingStatus.replace('_', ' ')}
                      </span>
                    )}
                  </div>
                </div>

                {/* Card Actions */}
                <div className="pt-4 border-t border-slate-900 flex flex-wrap items-center justify-between gap-2.5">
                  <div className="flex items-center gap-1.5">
                    <Link 
                      to={`/properties/${prop.id}`} 
                      className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
                      title="View Public Listing"
                    >
                      <Eye className="w-4 h-4" />
                    </Link>
                    
                    {prop.status !== 'ARCHIVED' && (
                      <>
                        <Link 
                          to={`/listings/edit/${prop.id}`} 
                          className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
                          title="Edit Details"
                        >
                          <Edit3 className="w-4 h-4" />
                        </Link>
                        <Link 
                          to={`/listings/media/${prop.id}`} 
                          className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-indigo-400 hover:border-indigo-500/20 transition-all"
                          title="Manage Media & Assets"
                        >
                          <ImageIcon className="w-4 h-4" />
                        </Link>
                      </>
                    )}

                    {prop.status !== 'ARCHIVED' && (
                      <button 
                        onClick={() => handleAction(prop.id, 'archive')}
                        className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-amber-400 transition-colors"
                        title="Archive Listing"
                      >
                        <Archive className="w-4 h-4" />
                      </button>
                    )}

                    <button 
                      onClick={() => handleAction(prop.id, 'delete')}
                      className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-rose-500 transition-colors"
                      title="Soft Delete Listing"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Context State Action */}
                  <div className="flex items-center">
                    {prop.status === 'DRAFT' && (
                      <button
                        onClick={() => handleAction(prop.id, 'submit')}
                        className="px-3 py-1.5 text-xs font-semibold rounded bg-amber-500/10 border border-amber-500/20 text-amber-400 hover:bg-amber-500/20 transition-colors flex items-center gap-1"
                      >
                        Submit Verification
                      </button>
                    )}

                    {prop.status === 'PENDING_VERIFICATION' && (
                      <button
                        onClick={() => handleAction(prop.id, 'publish')}
                        className="px-3 py-1.5 text-xs font-semibold rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 hover:bg-emerald-500/20 transition-colors flex items-center gap-1"
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                        Approve & Publish (Admin)
                      </button>
                    )}

                    {prop.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleAction(prop.id, 'pause')}
                        className="px-3 py-1.5 text-xs font-semibold rounded bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors flex items-center gap-1"
                      >
                        <Pause className="w-3.5 h-3.5" />
                        Pause Listing
                      </button>
                    )}

                    {prop.status === 'PAUSED' && (
                      <button
                        onClick={() => handleAction(prop.id, 'publish')}
                        className="px-3 py-1.5 text-xs font-semibold rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 hover:bg-emerald-500/20 transition-colors flex items-center gap-1"
                      >
                        <Play className="w-3.5 h-3.5" />
                        Resume Listing
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
