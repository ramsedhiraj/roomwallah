import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as zod from 'zod';
import { apiClient } from '../services/api';

const passwordSchema = zod.object({
  currentPassword: zod.string().min(1, 'Current password is required'),
  newPassword: zod.string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number')
    .regex(/[@#$%^&+=!]/, 'Password must contain at least one special character (@#$%^&+=!)'),
  confirmPassword: zod.string().min(1, 'Please confirm your new password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type PasswordFormValues = zod.infer<typeof passwordSchema>;

export default function ChangePasswordPage() {
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { register, handleSubmit, reset, watch, formState: { errors, isSubmitting } } = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
  });

  const newPasswordValue = watch('newPassword', '');

  const onSubmit = async (values: PasswordFormValues) => {
    try {
      setStatusMessage(null);
      await apiClient.post('/users/me/change-password', values);
      setStatusMessage({ type: 'success', text: 'Password changed successfully!' });
      reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      console.error(err);
      setStatusMessage({ type: 'error', text: err.response?.data?.message || 'Failed to change password.' });
    }
  };

  // Check individual password criteria
  const hasMinLength = newPasswordValue.length >= 8;
  const hasUppercase = /[A-Z]/.test(newPasswordValue);
  const hasLowercase = /[a-z]/.test(newPasswordValue);
  const hasNumber = /[0-9]/.test(newPasswordValue);
  const hasSpecial = /[@#$%^&+=!]/.test(newPasswordValue);

  return (
    <div className="max-w-md mx-auto py-12 px-4">
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-8 shadow-xl backdrop-blur-md space-y-6">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight bg-gradient-to-r from-primary to-purple-400 bg-clip-text text-transparent mb-1">
            Change Password
          </h1>
          <p className="text-sm text-slate-400">Update your account security settings</p>
        </div>

        {statusMessage && (
          <div className={`p-4 rounded-xl text-sm border font-medium ${
            statusMessage.type === 'success' 
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' 
              : 'bg-rose-500/10 border-rose-500/30 text-rose-400'
          }`}>
            {statusMessage.text}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-400 mb-1">Current Password</label>
            <input
              type="password"
              {...register('currentPassword')}
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
            />
            {errors.currentPassword && <p className="text-xs text-rose-400 mt-1">{errors.currentPassword.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400 mb-1">New Password</label>
            <input
              type="password"
              {...register('newPassword')}
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
            />
            {errors.newPassword && <p className="text-xs text-rose-400 mt-1">{errors.newPassword.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400 mb-1">Confirm New Password</label>
            <input
              type="password"
              {...register('confirmPassword')}
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-850 rounded-xl text-slate-200 focus:outline-none focus:border-primary transition-all"
            />
            {errors.confirmPassword && <p className="text-xs text-rose-400 mt-1">{errors.confirmPassword.message}</p>}
          </div>

          {/* Password Strength Checklist */}
          <div className="bg-slate-950/40 p-4 rounded-xl border border-slate-850 text-xs space-y-2">
            <span className="text-slate-400 font-semibold block mb-1">New Password Requirements:</span>
            <div className="flex items-center space-x-2">
              <span className={`w-1.5 h-1.5 rounded-full ${hasMinLength ? 'bg-emerald-500' : 'bg-slate-700'}`}></span>
              <span className={hasMinLength ? 'text-slate-300' : 'text-slate-500'}>At least 8 characters</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className={`w-1.5 h-1.5 rounded-full ${hasUppercase && hasLowercase ? 'bg-emerald-500' : 'bg-slate-700'}`}></span>
              <span className={(hasUppercase && hasLowercase) ? 'text-slate-300' : 'text-slate-500'}>Uppercase & lowercase letters</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className={`w-1.5 h-1.5 rounded-full ${hasNumber ? 'bg-emerald-500' : 'bg-slate-700'}`}></span>
              <span className={hasNumber ? 'text-slate-300' : 'text-slate-500'}>At least one number</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className={`w-1.5 h-1.5 rounded-full ${hasSpecial ? 'bg-emerald-500' : 'bg-slate-700'}`}></span>
              <span className={hasSpecial ? 'text-slate-300' : 'text-slate-500'}>At least one special character (@#$%^&+=!)</span>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 bg-primary hover:bg-opacity-95 text-primary-foreground font-semibold rounded-xl shadow-lg transition-all disabled:opacity-50 mt-2"
          >
            {isSubmitting ? 'Updating Password...' : 'Change Password'}
          </button>
        </form>
      </div>
    </div>
  );
}
