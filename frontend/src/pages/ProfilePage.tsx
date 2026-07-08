import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as zod from 'zod';
import { apiClient } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { Link } from 'react-router-dom';
import TrustExplanationDialog from '../components/TrustExplanationDialog';

const profileSchema = zod.object({
  fullName: zod.string().min(2, 'Name must be at least 2 characters').max(100, 'Name must not exceed 100 characters'),
  bio: zod.string().max(1000, 'Bio must not exceed 1000 characters').optional().nullable(),
  dateOfBirth: zod.string().optional().nullable(),
  gender: zod.string().max(50, 'Gender must not exceed 50 characters').optional().nullable(),
  darkModePreferred: zod.boolean().optional(),
  emailNotificationsEnabled: zod.boolean().optional(),
  pushNotificationsEnabled: zod.boolean().optional(),
  marketingNotificationsEnabled: zod.boolean().optional(),
  preferredLanguage: zod.string().optional(),
  preferredContactMethod: zod.string().optional(),
});

type ProfileFormValues = zod.infer<typeof profileSchema>;

export default function ProfilePage() {
  const { user, setUser } = useAuthStore();
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isTrustDialogOpen, setIsTrustDialogOpen] = useState(false);

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
  });

  const fetchProfile = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/users/me');
      const data = res.data.data;
      setProfile(data);
      
      // Update local state and Zustand user
      if (user) {
        setUser({
          ...user,
          fullName: data.fullName,
        });
      }

      // Pre-fill form fields
      reset({
        fullName: data.fullName || '',
        bio: data.bio || '',
        dateOfBirth: data.dateOfBirth || '',
        gender: data.gender || '',
        darkModePreferred: data.darkModePreferred,
        emailNotificationsEnabled: data.emailNotificationsEnabled,
        pushNotificationsEnabled: data.pushNotificationsEnabled,
        marketingNotificationsEnabled: data.marketingNotificationsEnabled,
        preferredLanguage: data.preferredLanguage || 'en',
        preferredContactMethod: data.preferredContactMethod || 'EMAIL',
      });
    } catch (err: any) {
      logError('Failed to fetch profile', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const onSubmit = async (values: ProfileFormValues) => {
    try {
      setStatusMessage(null);
      const res = await apiClient.put('/users/me', values);
      setProfile(res.data.data);
      if (user) {
        setUser({
          ...user,
          fullName: res.data.data.fullName,
        });
      }
      setStatusMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (err: any) {
      logError('Failed to update profile', err);
      setStatusMessage({ type: 'error', text: err.response?.data?.message || 'Failed to update profile.' });
    }
  };

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const file = e.target.files[0];
    
    // File validation
    if (file.size > 5 * 1024 * 1024) {
      setStatusMessage({ type: 'error', text: 'File is too large. Max size is 5MB.' });
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      setUploading(true);
      setStatusMessage(null);
      const res = await apiClient.post('/users/me/avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      setProfile((prev: any) => ({
        ...prev,
        avatarUrl: res.data.data.avatarUrl,
      }));
      
      setStatusMessage({ type: 'success', text: 'Avatar uploaded successfully!' });
    } catch (err: any) {
      logError('Failed to upload avatar', err);
      setStatusMessage({ type: 'error', text: err.response?.data?.message || 'Failed to upload avatar.' });
    } finally {
      setUploading(false);
    }
  };

  const logError = (context: string, err: any) => {
    console.error(context, err);
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-primary to-purple-400 bg-clip-text text-transparent mb-8">
        My Profile & Preferences
      </h1>

      {statusMessage && (
        <div className={`p-4 mb-6 rounded-lg text-sm border font-medium ${
          statusMessage.type === 'success' 
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' 
            : 'bg-rose-500/10 border-rose-500/30 text-rose-400'
        }`}>
          {statusMessage.text}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Left Side: Avatar & Verification */}
        <div className="md:col-span-1 space-y-6">
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 text-center shadow-xl backdrop-blur-md">
            <div className="relative group w-32 h-32 mx-auto mb-4">
              <img
                src={profile?.avatarUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(profile?.fullName || 'User')}`}
                alt="Avatar"
                className="w-full h-full rounded-full object-cover border-4 border-slate-800 group-hover:opacity-75 transition-all"
              />
              <label className="absolute inset-0 flex items-center justify-center rounded-full bg-black/60 opacity-0 group-hover:opacity-100 cursor-pointer transition-all">
                <span className="text-xs font-semibold text-white">
                  {uploading ? 'Uploading...' : 'Change Photo'}
                </span>
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleAvatarChange}
                  disabled={uploading}
                />
              </label>
            </div>
            
            <h2 className="text-xl font-bold text-slate-100">{profile?.fullName}</h2>
            <p className="text-sm text-slate-400 capitalize mb-4">{profile?.role.toLowerCase()}</p>

            {/* Verification Status Cards */}
            <div className="space-y-2 text-left mt-6 pt-6 border-t border-slate-800">
              <div className="flex items-center justify-between p-2 rounded bg-slate-950/40 text-xs">
                <span className="text-slate-400">Email Verification</span>
                <span className={`px-2 py-0.5 rounded font-semibold ${
                  profile?.emailVerified ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'
                }`}>
                  {profile?.emailVerified ? 'Verified' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between p-2 rounded bg-slate-950/40 text-xs">
                <span className="text-slate-400">Phone Verification</span>
                <span className={`px-2 py-0.5 rounded font-semibold ${
                  profile?.phoneVerified ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'
                }`}>
                  {profile?.phoneVerified ? 'Verified' : 'Pending'}
                </span>
              </div>
              <div className="flex items-center justify-between p-2 rounded bg-slate-950/40 text-xs">
                <span className="text-slate-400">Identity (KYC)</span>
                <span className={`px-2 py-0.5 rounded font-semibold ${
                  profile?.identityVerified ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'
                }`}>
                  {profile?.identityVerified ? 'Verified' : 'Unverified'}
                </span>
              </div>
              
              <div className="mt-4 pt-4 border-t border-slate-800 space-y-2">
                <button
                  type="button"
                  onClick={() => setIsTrustDialogOpen(true)}
                  className="w-full py-2 bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-400 border border-indigo-500/20 text-xs font-semibold rounded-lg transition-all"
                  id="view-trust-breakdown-btn"
                >
                  View Trust Score Breakdown
                </button>
                <Link
                  to="/trust/verify"
                  className="w-full py-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 text-xs font-semibold rounded-lg transition-all text-center block"
                  id="verify-documents-btn"
                >
                  Submit Verification Documents
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Profile Forms & Preferences */}
        <div className="md:col-span-2 space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            
            {/* Profile Info Section */}
            <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-6 shadow-xl backdrop-blur-md space-y-4">
              <h3 className="text-lg font-semibold text-slate-200 mb-2">Personal Information</h3>
              
              <div>
                <label className="block text-sm font-medium text-slate-400 mb-1">Full Name</label>
                <input
                  type="text"
                  {...register('fullName')}
                  className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
                />
                {errors.fullName && <p className="text-xs text-rose-400 mt-1">{errors.fullName.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-400 mb-1">Bio</label>
                <textarea
                  rows={3}
                  {...register('bio')}
                  placeholder="Tell potential tenants or owners a bit about yourself..."
                  className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all resize-none"
                />
                {errors.bio && <p className="text-xs text-rose-400 mt-1">{errors.bio.message}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-1">Date of Birth</label>
                  <input
                    type="date"
                    {...register('dateOfBirth')}
                    className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
                  />
                  {errors.dateOfBirth && <p className="text-xs text-rose-400 mt-1">{errors.dateOfBirth.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-1">Gender</label>
                  <select
                    {...register('gender')}
                    className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
                  >
                    <option value="">Select Gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                  {errors.gender && <p className="text-xs text-rose-400 mt-1">{errors.gender.message}</p>}
                </div>
              </div>
            </div>

            {/* Preferences Section */}
            <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-6 shadow-xl backdrop-blur-md space-y-4">
              <h3 className="text-lg font-semibold text-slate-200 mb-2">System Preferences</h3>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-1">Preferred Language</label>
                  <select
                    {...register('preferredLanguage')}
                    className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
                  >
                    <option value="en">English</option>
                    <option value="hi">Hindi (हिन्दी)</option>
                    <option value="kn">Kannada (ಕನ್ನಡ)</option>
                    <option value="ta">Tamil (தமிழ்)</option>
                    <option value="te">Telugu (తెలుగు)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-1">Preferred Contact Method</label>
                  <select
                    {...register('preferredContactMethod')}
                    className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
                  >
                    <option value="EMAIL">Email Alerts</option>
                    <option value="PHONE">Direct Phone Call</option>
                    <option value="WHATSAPP">WhatsApp Messaging</option>
                  </select>
                </div>
              </div>

              {/* Notification Toggles */}
              <div className="space-y-3 pt-4 border-t border-slate-800">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-semibold text-slate-200">Dark Mode</h4>
                    <p className="text-xs text-slate-400">Toggle dark visual mode preferences</p>
                  </div>
                  <input
                    type="checkbox"
                    {...register('darkModePreferred')}
                    className="w-4 h-4 rounded text-primary focus:ring-primary focus:ring-opacity-50"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-semibold text-slate-200">Email Notifications</h4>
                    <p className="text-xs text-slate-400">Receive system alerts via email</p>
                  </div>
                  <input
                    type="checkbox"
                    {...register('emailNotificationsEnabled')}
                    className="w-4 h-4 rounded text-primary focus:ring-primary focus:ring-opacity-50"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-semibold text-slate-200">Push Notifications</h4>
                    <p className="text-xs text-slate-400">Receive browser push notifications</p>
                  </div>
                  <input
                    type="checkbox"
                    {...register('pushNotificationsEnabled')}
                    className="w-4 h-4 rounded text-primary focus:ring-primary focus:ring-opacity-50"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-semibold text-slate-200">Marketing Communications</h4>
                    <p className="text-xs text-slate-400">Receive platform newsletters and promotions</p>
                  </div>
                  <input
                    type="checkbox"
                    {...register('marketingNotificationsEnabled')}
                    className="w-4 h-4 rounded text-primary focus:ring-primary focus:ring-opacity-50"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-6 py-2.5 bg-primary hover:bg-opacity-95 text-primary-foreground font-semibold rounded-xl shadow-lg transition-all disabled:opacity-50"
              >
                {isSubmitting ? 'Saving...' : 'Save Profile Changes'}
              </button>
            </div>

          </form>
        </div>
      </div>
      
      <TrustExplanationDialog 
        isOpen={isTrustDialogOpen} 
        onClose={() => setIsTrustDialogOpen(false)} 
      />
    </div>
  );
}
