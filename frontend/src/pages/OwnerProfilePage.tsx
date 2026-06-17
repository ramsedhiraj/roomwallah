import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiClient } from '../services/api';

export default function OwnerProfilePage() {
  const { id } = useParams<{ id: string }>();
  const [owner, setOwner] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchOwnerProfile = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await apiClient.get(`/owners/${id}/public-profile`);
        setOwner(res.data.data);
      } catch (err: any) {
        console.error(err);
        setError(err.response?.data?.message || 'Failed to load owner profile.');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchOwnerProfile();
    }
  }, [id]);

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (error || !owner) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-center px-4">
        <h2 className="text-2xl font-bold text-slate-100 mb-2">Profile Not Found</h2>
        <p className="text-slate-400 mb-6">{error || 'The requested property owner profile could not be loaded.'}</p>
        <Link to="/" className="px-6 py-2.5 bg-primary text-primary-foreground font-semibold rounded-xl hover:bg-opacity-95 transition-all">
          Go Home
        </Link>
      </div>
    );
  }

  const joinDateFormatted = new Date(owner.joinDate).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <div className="max-w-2xl mx-auto py-12 px-4">
      <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-8 shadow-2xl backdrop-blur-md text-center space-y-6">
        
        {/* Avatar and Name */}
        <div className="space-y-4">
          <img
            src={owner.avatarUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(owner.displayName || 'Owner')}`}
            alt={owner.displayName}
            className="w-32 h-32 rounded-full object-cover border-4 border-slate-800 mx-auto shadow-lg"
          />
          
          <div className="space-y-1">
            <div className="flex items-center justify-center space-x-2">
              <h1 className="text-2xl font-extrabold text-slate-100">{owner.displayName}</h1>
              {owner.verifiedOwner && (
                <span className="w-5 h-5 bg-primary/20 text-primary rounded-full flex items-center justify-center text-xs" title="Verified Owner">
                  ✓
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400">Verified Property Owner</p>
          </div>
        </div>

        {/* Info Grid */}
        <div className="grid grid-cols-3 gap-4 py-6 border-y border-slate-850">
          <div className="text-center">
            <span className="text-xs text-slate-500 block mb-1">Member Since</span>
            <span className="text-sm font-bold text-slate-200">{joinDateFormatted}</span>
          </div>

          <div className="text-center">
            <span className="text-xs text-slate-500 block mb-1">Trust Score</span>
            <span className="text-sm font-bold text-emerald-400">{owner.trustScore}/100</span>
          </div>

          <div className="text-center">
            <span className="text-xs text-slate-500 block mb-1">Active Listings</span>
            <span className="text-sm font-bold text-slate-200">{owner.listingsCount} Properties</span>
          </div>
        </div>

        {/* Public Badges */}
        <div className="space-y-3 text-left">
          <h3 className="text-sm font-semibold text-slate-400">Security & Verifications</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex items-center space-x-3 p-3 bg-slate-950/40 border border-slate-850 rounded-xl">
              <span className={`w-2.5 h-2.5 rounded-full ${owner.verifiedOwner ? 'bg-emerald-500' : 'bg-slate-700'}`}></span>
              <div>
                <h4 className="text-xs font-bold text-slate-300">Identity Verification</h4>
                <p className="text-[10px] text-slate-500">{owner.verifiedOwner ? 'Govt Identity check completed' : 'Pending identity upload'}</p>
              </div>
            </div>

            <div className="flex items-center space-x-3 p-3 bg-slate-950/40 border border-slate-850 rounded-xl">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <div>
                <h4 className="text-xs font-bold text-slate-300">Contact Authenticated</h4>
                <p className="text-[10px] text-slate-500">Secure email/phone verified</p>
              </div>
            </div>
          </div>
        </div>

        {/* Action button */}
        <div className="pt-4">
          <Link
            to="/"
            className="inline-block px-6 py-3 bg-slate-800 hover:bg-slate-750 text-slate-300 font-semibold rounded-xl text-sm transition-all shadow-md"
          >
            Back to Home
          </Link>
        </div>

      </div>
    </div>
  );
}
