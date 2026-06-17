import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import * as zod from 'zod';
import { User, Mail, Phone, Lock, Sparkles, Loader2, ShieldCheck, ShieldAlert } from 'lucide-react';
import { apiClient } from '../services/api';
import { useState } from 'react';

const registerSchema = zod.object({
  fullName: zod.string().min(2, 'Name must be at least 2 characters'),
  email: zod.string().email('Invalid email address'),
  phone: zod.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid phone number format (e.g. +919876543210)'),
  password: zod.string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one digit')
    .regex(/[@#$%^&+=!]/, 'Password must contain at least one special character (@#$%^&+=!)'),
  role: zod.enum(['TENANT', 'OWNER'], {
    errorMap: () => ({ message: 'Please select a role' }),
  }),
});

type RegisterFormInputs = zod.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<RegisterFormInputs>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      role: 'TENANT',
    }
  });

  const selectedRole = watch('role');

  const onSubmit = async (data: RegisterFormInputs) => {
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await apiClient.post('/auth/register', data);
      setSuccess('Registration successful! Redirecting to login page...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err: any) {
      log.error("Register request failed", err);
      const msg = err.response?.data?.message || 'Registration failed. Email or phone number might already be in use.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[85vh] flex flex-col justify-center items-center px-4 py-8 relative">
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 rounded-full bg-indigo-500/10 blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-md glass p-8 rounded-2xl border border-slate-800 relative z-10 animate-fade-in">
        <div className="text-center mb-8 space-y-2">
          <div className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-medium border border-indigo-500/20">
            <Sparkles className="w-3 h-3" />
            <span>Join RoomWallah</span>
          </div>
          <h2 className="text-3xl font-extrabold bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Create Account
          </h2>
          <p className="text-slate-400 text-sm">
            Sign up directly as a tenant or verified homeowner
          </p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-start gap-3 text-red-400 text-sm animate-fade-in">
            <ShieldAlert className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-start gap-3 text-emerald-400 text-sm animate-fade-in">
            <ShieldCheck className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Role selector buttons */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-300 block">Select Role</label>
            <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                onClick={() => setValue('role', 'TENANT')}
                className={`py-3 rounded-xl border text-sm font-semibold transition-all ${
                  selectedRole === 'TENANT'
                    ? 'bg-primary text-white border-primary glow-indigo'
                    : 'bg-slate-950/20 border-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                Tenant
              </button>
              <button
                type="button"
                onClick={() => setValue('role', 'OWNER')}
                className={`py-3 rounded-xl border text-sm font-semibold transition-all ${
                  selectedRole === 'OWNER'
                    ? 'bg-primary text-white border-primary glow-indigo'
                    : 'bg-slate-950/20 border-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                Home Owner
              </button>
            </div>
            {errors.role && (
              <p className="text-xs text-red-400">{errors.role.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-300 block">Full Name</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                <User className="w-4 h-4" />
              </span>
              <input
                {...register('fullName')}
                type="text"
                placeholder="John Doe"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
              />
            </div>
            {errors.fullName && (
              <p className="text-xs text-red-400">{errors.fullName.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-300 block">Email Address</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                <Mail className="w-4 h-4" />
              </span>
              <input
                {...register('email')}
                type="email"
                placeholder="name@example.com"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
              />
            </div>
            {errors.email && (
              <p className="text-xs text-red-400">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-300 block">Phone Number</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                <Phone className="w-4 h-4" />
              </span>
              <input
                {...register('phone')}
                type="tel"
                placeholder="+919876543210"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
              />
            </div>
            {errors.phone && (
              <p className="text-xs text-red-400">{errors.phone.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-300 block">Password</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                <Lock className="w-4 h-4" />
              </span>
              <input
                {...register('password')}
                type="password"
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
              />
            </div>
            {errors.password && (
              <p className="text-xs text-red-400">{errors.password.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 hover:shadow-lg hover:shadow-indigo-500/10 active:scale-[0.99] transition-all flex items-center justify-center gap-2 mt-2"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Signing Up...
              </>
            ) : (
              'Create Account'
            )}
          </button>
        </form>

        <p className="mt-8 text-center text-sm text-slate-400">
          Already have an account?{" "}
          <Link to="/login" className="text-primary font-semibold hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

const log = {
  error: (...args: any[]) => console.error("[Register]", ...args)
};
