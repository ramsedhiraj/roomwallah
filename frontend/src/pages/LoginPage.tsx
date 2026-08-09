import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import * as zod from 'zod';
import { Mail, Lock, ShieldAlert, Sparkles, Loader2 } from 'lucide-react';
import { apiClient } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useState } from 'react';

const loginSchema = zod.object({
  identity: zod.string().min(1, 'Email or Phone is required'),
  password: zod.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormInputs = zod.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const { setToken, setUser, setAuthenticated } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Forgot Password State
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotPasswordEmail, setForgotPasswordEmail] = useState('');
  const [isSubmittingRecovery, setIsSubmittingRecovery] = useState(false);
  const [recoverySuccess, setRecoverySuccess] = useState(false);
  const [recoveryError, setRecoveryError] = useState<string | null>(null);

  // Reset Password State
  const [resetCode, setResetCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmittingReset, setIsSubmittingReset] = useState(false);
  const [resetSuccess, setResetSuccess] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormInputs>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormInputs) => {
    setIsLoading(true);
    setError(null);
    try {
      // 1. Post credentials
      const response = await apiClient.post('/auth/login', data);
      const { accessToken, refreshToken } = response.data.data;

      // 2. Save tokens
      setToken(accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // 3. Fetch profile details
      const profileResponse = await apiClient.get('/auth/me');
      setUser(profileResponse.data.data);
      setAuthenticated(true);

      logInSuccess();
    } catch (err: any) {
      log.error("Login request failed", err);
      const msg = err.response?.data?.message || 'Invalid credentials. Please check your username/password.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingRecovery(true);
    setRecoveryError(null);
    try {
      await apiClient.post('/auth/forgot-password', { email: forgotPasswordEmail });
      setRecoverySuccess(true);
    } catch (err: any) {
      log.error("Forgot password request failed", err);
      const msg = err.response?.data?.message || 'Failed to send verification code. Please check your email and try again.';
      setRecoveryError(msg);
    } finally {
      setIsSubmittingRecovery(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingReset(true);
    setResetError(null);
    try {
      await apiClient.post('/auth/reset-password', {
        email: forgotPasswordEmail,
        code: resetCode,
        newPassword,
        confirmPassword,
      });
      setResetSuccess(true);
    } catch (err: any) {
      log.error("Reset password request failed", err);
      const msg = err.response?.data?.message || 'Failed to reset password. Please check your verification code and try again.';
      setResetError(msg);
    } finally {
      setIsSubmittingReset(false);
    }
  };

  const logInSuccess = () => {
    navigate('/', { replace: true });
  };

  const resetForgotPasswordState = () => {
    setShowForgotPassword(false);
    setRecoverySuccess(false);
    setRecoveryError(null);
    setForgotPasswordEmail('');
    setResetCode('');
    setNewPassword('');
    setConfirmPassword('');
    setResetSuccess(false);
    setResetError(null);
  };

  return (
    <div className="min-h-[75vh] flex flex-col justify-center items-center px-4 relative">
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 rounded-full bg-indigo-500/10 blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-md glass p-8 rounded-2xl border border-slate-800 relative z-10">
        {showForgotPassword ? (
          <div>
            <div className="text-center mb-8 space-y-2">
              <div className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-medium border border-indigo-500/20">
                <Sparkles className="w-3 h-3" />
                <span>Security Portal</span>
              </div>
              <h2 className="text-3xl font-extrabold bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
                Reset Password
              </h2>
              <p className="text-slate-400 text-sm">
                Recover access to your account
              </p>
            </div>

            {resetSuccess ? (
              <div className="space-y-4">
                <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-sm animate-fade-in">
                  Password reset successfully! You can now log in with your new password.
                </div>
                <button
                  type="button"
                  onClick={resetForgotPasswordState}
                  className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 transition-all text-sm"
                >
                  Proceed to Login
                </button>
              </div>
            ) : recoverySuccess ? (
              <div className="space-y-6 animate-fade-in">
                <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-sm">
                  If the email is registered, a verification code has been sent. Please check your inbox.
                </div>

                {resetError && (
                  <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-start gap-3 text-red-400 text-sm">
                    <ShieldAlert className="w-5 h-5 shrink-0 mt-0.5" />
                    <span>{resetError}</span>
                  </div>
                )}

                <form onSubmit={handleResetPassword} className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-300 block">Verification Code (OTP)</label>
                    <input
                      type="text"
                      required
                      value={resetCode}
                      onChange={(e) => setResetCode(e.target.value)}
                      placeholder="Enter 6-digit code"
                      className="w-full px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-300 block">New Password</label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                        <Lock className="w-4 h-4" />
                      </span>
                      <input
                        type="password"
                        required
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Min 8 chars with uppercase, digit & symbol"
                        className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-300 block">Confirm New Password</label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                        <Lock className="w-4 h-4" />
                      </span>
                      <input
                        type="password"
                        required
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Re-enter new password"
                        className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmittingReset}
                    className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 transition-all flex items-center justify-center gap-2"
                  >
                    {isSubmittingReset ? (
                      <>
                        <Loader2 className="w-5 h-5 animate-spin" />
                        Resetting Password...
                      </>
                    ) : (
                      'Confirm Password Reset'
                    )}
                  </button>
                </form>
              </div>
            ) : (
              <form onSubmit={handleForgotPassword} className="space-y-6">
                {recoveryError && (
                  <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-start gap-3 text-red-400 text-sm">
                    <ShieldAlert className="w-5 h-5 shrink-0 mt-0.5" />
                    <span>{recoveryError}</span>
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">Email Address</label>
                  <div className="relative">
                    <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                      <Mail className="w-4 h-4" />
                    </span>
                    <input
                      type="email"
                      required
                      value={forgotPasswordEmail}
                      onChange={(e) => setForgotPasswordEmail(e.target.value)}
                      placeholder="name@example.com"
                      className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isSubmittingRecovery}
                  className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 hover:shadow-lg hover:shadow-indigo-500/10 active:scale-[0.99] transition-all flex items-center justify-center gap-2"
                >
                  {isSubmittingRecovery ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      Sending Code...
                    </>
                  ) : (
                    'Send Verification Code'
                  )}
                </button>
              </form>
            )}

            <button
              onClick={resetForgotPasswordState}
              className="mt-6 text-sm text-primary hover:underline block text-center w-full"
            >
              Back to Login
            </button>
          </div>
        ) : (
          <div>
            <div className="text-center mb-8 space-y-2">
              <div className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-medium border border-indigo-500/20">
                <Sparkles className="w-3 h-3" />
                <span>Welcome Back</span>
              </div>
              <h2 className="text-3xl font-extrabold bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
                Account Login
              </h2>
              <p className="text-slate-400 text-sm">
                Access your broker-free property marketplace
              </p>
            </div>

            {error && (
              <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-start gap-3 text-red-400 text-sm animate-fade-in">
                <ShieldAlert className="w-5 h-5 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-300 block">Email or Phone</label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                    <Mail className="w-4 h-4" />
                  </span>
                  <input
                    {...register('identity')}
                    type="text"
                    placeholder="name@example.com or +91..."
                    className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                  />
                </div>
                {errors.identity && (
                  <p className="text-xs text-red-400">{errors.identity.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-sm font-semibold text-slate-300 block">Password</label>
                  <button
                    type="button"
                    onClick={() => setShowForgotPassword(true)}
                    className="text-xs text-primary hover:underline outline-none"
                  >
                    Forgot password?
                  </button>
                </div>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-500">
                    <Lock className="w-4 h-4" />
                  </span>
                  <input
                    {...register('password')}
                    type="password"
                    placeholder="••••••••"
                    className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-sm text-slate-100"
                  />
                </div>
                {errors.password && (
                  <p className="text-xs text-red-400">{errors.password.message}</p>
                )}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 hover:shadow-lg hover:shadow-indigo-500/10 active:scale-[0.99] transition-all flex items-center justify-center gap-2"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Signing In...
                  </>
                ) : (
                  'Sign In'
                )}
              </button>
            </form>

            <p className="mt-8 text-center text-sm text-slate-400">
              Don't have an account?{" "}
              <Link to="/register" className="text-primary font-semibold hover:underline">
                Sign up
              </Link>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
// simple logger helper stub
const log = {
  error: (...args: any[]) => console.error("[Login]", ...args)
};
