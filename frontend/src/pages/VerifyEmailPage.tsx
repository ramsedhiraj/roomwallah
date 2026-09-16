import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Mail, CheckCircle, AlertCircle, Loader2, Sparkles, Send } from 'lucide-react';
import { apiClient } from '../services/api';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [isLoading, setIsLoading] = useState(true);
  const [isSuccess, setIsSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Resend Verification State
  const [resendEmail, setResendEmail] = useState('');
  const [isResending, setIsResending] = useState(false);
  const [resendSuccessMessage, setResendSuccessMessage] = useState<string | null>(null);
  const [resendErrorMessage, setResendErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    const verifyToken = async () => {
      if (!token || token.trim() === '') {
        if (isMounted) {
          setIsLoading(false);
          setIsSuccess(false);
          setErrorMessage('No verification token provided. Please click the link received in your email.');
        }
        return;
      }

      setIsLoading(true);
      setErrorMessage(null);

      try {
        await apiClient.get(`/auth/verify-email?token=${encodeURIComponent(token.trim())}`);
        if (isMounted) {
          setIsSuccess(true);
        }
      } catch (err: any) {
        if (isMounted) {
          setIsSuccess(false);
          if (!err.response) {
            setErrorMessage('Unable to connect to the server. Please check your internet connection.');
          } else {
            const msg = err.response?.data?.message || 'Verification link is invalid or expired.';
            setErrorMessage(msg);
          }
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    verifyToken();

    return () => {
      isMounted = false;
    };
  }, [token]);

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resendEmail || resendEmail.trim() === '') return;

    setIsResending(true);
    setResendSuccessMessage(null);
    setResendErrorMessage(null);

    try {
      await apiClient.post('/auth/resend-verification', { email: resendEmail.trim() });
      setResendSuccessMessage('Verification email sent! Please check your inbox and click the verification link.');
    } catch (err: any) {
      if (!err.response) {
        setResendErrorMessage('Unable to connect to the server. Please try again.');
      } else {
        const msg = err.response?.data?.message || 'Failed to resend verification email. Please try again.';
        setResendErrorMessage(msg);
      }
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="min-h-[75vh] flex flex-col justify-center items-center px-4 relative">
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 rounded-full bg-indigo-500/10 blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-md glass p-8 rounded-2xl border border-slate-800 relative z-10 animate-fade-in">
        <div className="text-center mb-6 space-y-2">
          <div className="inline-flex items-center space-x-1 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-medium border border-indigo-500/20">
            <Sparkles className="w-3 h-3" />
            <span>Email Verification</span>
          </div>
          <h2 className="text-3xl font-extrabold bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Account Activation
          </h2>
        </div>

        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center space-y-4 text-center">
            <div className="relative">
              <div className="w-16 h-16 rounded-full border-4 border-indigo-500/20 border-t-primary animate-spin"></div>
              <Mail className="w-6 h-6 text-primary absolute inset-0 m-auto" />
            </div>
            <p className="text-slate-300 font-medium text-base">Verifying your email...</p>
            <p className="text-xs text-slate-500">Please wait while we validate your activation token</p>
          </div>
        ) : isSuccess ? (
          <div className="py-6 space-y-6 text-center animate-fade-in">
            <div className="mx-auto w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CheckCircle className="w-8 h-8" />
            </div>
            <div className="space-y-2">
              <h3 className="text-xl font-bold text-white">Email Verified!</h3>
              <p className="text-sm text-slate-300">
                Your email has been verified successfully.
              </p>
              <p className="text-xs text-slate-400">
                You can now log in to your RoomWallah account.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="w-full py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl hover:opacity-95 transition-all text-sm"
            >
              Continue to Login
            </button>
          </div>
        ) : (
          <div className="py-4 space-y-6 animate-fade-in">
            <div className="text-center space-y-3">
              <div className="mx-auto w-16 h-16 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400">
                <AlertCircle className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-bold text-white">Verification Failed</h3>
              <p className="text-sm text-red-400 bg-red-500/10 p-3 rounded-xl border border-red-500/20">
                {errorMessage || 'Verification link is invalid or expired.'}
              </p>
            </div>

            <div className="border-t border-slate-800 pt-5 space-y-4">
              <p className="text-xs text-slate-300 text-center font-medium">
                Need a new verification link? Enter your email address below:
              </p>

              {resendSuccessMessage && (
                <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-xs text-emerald-400">
                  {resendSuccessMessage}
                </div>
              )}

              {resendErrorMessage && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-xs text-red-400">
                  {resendErrorMessage}
                </div>
              )}

              <form onSubmit={handleResend} className="space-y-3">
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500">
                    <Mail className="w-4 h-4" />
                  </span>
                  <input
                    type="email"
                    required
                    value={resendEmail}
                    onChange={(e) => setResendEmail(e.target.value)}
                    placeholder="name@example.com"
                    className="w-full pl-9 pr-4 py-2.5 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition-all text-xs text-slate-100"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isResending}
                  className="w-full py-2.5 bg-slate-900 border border-slate-700 text-slate-200 text-xs font-semibold rounded-xl hover:bg-slate-800 transition-all flex items-center justify-center gap-2"
                >
                  {isResending ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      Sending Link...
                    </>
                  ) : (
                    <>
                      <Send className="w-3.5 h-3.5" />
                      Resend Verification Email
                    </>
                  )}
                </button>
              </form>
            </div>

            <div className="pt-2 text-center">
              <Link to="/login" className="text-xs text-primary hover:underline font-semibold">
                Back to Login
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}