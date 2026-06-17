import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { 
  Mail,
  Phone,
  UserCheck,
  FileText, 
  UploadCloud, 
  AlertCircle, 
  CheckCircle2, 
  Loader2, 
  ChevronRight, 
  ChevronLeft,
  ShieldAlert,
  Building,
  Check,
  RefreshCw,
  Clock
} from 'lucide-react';

interface PropertyItem {
  id: string;
  title: string;
  address?: {
    line1?: string;
    city?: string;
  };
}

export default function VerificationWizardPage() {
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();
  const [activeStep, setActiveStep] = useState(0);
  
  // Verification states
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Step 1: Email OTP
  const [emailVal, setEmailVal] = useState(user?.email || '');
  const [emailOtp, setEmailOtp] = useState('');
  const [emailCooldown, setEmailCooldown] = useState(0);
  const [isEmailRequested, setIsEmailRequested] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(user?.emailVerified || false);

  // Step 2: Mobile OTP
  const [phoneVal, setPhoneVal] = useState(user?.phone || '');
  const [phoneOtp, setPhoneOtp] = useState('');
  const [phoneCooldown, setPhoneCooldown] = useState(0);
  const [isPhoneRequested, setIsPhoneRequested] = useState(false);
  const [isPhoneVerified, setIsPhoneVerified] = useState(user?.phoneVerified || false);

  // Step 3: Aadhaar eKYC
  const [aadhaarVal, setAadhaarVal] = useState('');
  const [aadhaarConsent, setAadhaarConsent] = useState(false);
  const [isAadhaarVerified, setIsAadhaarVerified] = useState(user?.identityVerified || false);

  // Step 4 & 5: Properties & documents
  const [myProperties, setMyProperties] = useState<PropertyItem[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [deedFile, setDeedFile] = useState<File | null>(null);
  const [deedUrl, setDeedUrl] = useState('');
  const [utilityFile, setUtilityFile] = useState<File | null>(null);
  const [utilityUrl, setUtilityUrl] = useState('');
  const [verificationResult, setVerificationResult] = useState<any>(null);

  // Cooldown timer hooks
  useEffect(() => {
    if (emailCooldown > 0) {
      const timer = setTimeout(() => setEmailCooldown(emailCooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [emailCooldown]);

  useEffect(() => {
    if (phoneCooldown > 0) {
      const timer = setTimeout(() => setPhoneCooldown(phoneCooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [phoneCooldown]);

  // Fetch properties and initial user state
  useEffect(() => {
    fetchProperties();
    refreshProfile();
  }, []);

  const refreshProfile = async () => {
    try {
      const profileResponse = await apiClient.get('/auth/me');
      const latestUser = profileResponse.data.data;
      setUser(latestUser);
      setIsEmailVerified(latestUser.emailVerified);
      setIsPhoneVerified(latestUser.phoneVerified);
      setIsAadhaarVerified(latestUser.identityVerified);
      if (latestUser.email) setEmailVal(latestUser.email);
      if (latestUser.phone) setPhoneVal(latestUser.phone);
    } catch (err) {
      console.error("Failed to refresh user profile data", err);
    }
  };

  const fetchProperties = async () => {
    try {
      const res = await apiClient.get('/properties/me');
      const data = res.data.data || [];
      setMyProperties(data);
      if (data.length > 0) {
        setSelectedPropertyId(data[0].id);
      }
    } catch (err) {
      console.error("Failed to fetch current user's listings", err);
    }
  };

  // --- Handlers ---
  const handleRequestEmailOtp = async () => {
    if (!emailVal) return setError("Please specify an email address.");
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/verifications/email/request', { target: emailVal });
      setIsEmailRequested(true);
      setEmailCooldown(60);
      setSuccessMsg("Verification code dispatched to your email address!");
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to dispatch email verification OTP code.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyEmailOtp = async () => {
    if (!emailOtp || emailOtp.length !== 6) return setError("Specify a valid 6-digit OTP code.");
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/verifications/email/verify', { code: emailOtp });
      setIsEmailVerified(true);
      setSuccessMsg("Email verified successfully!");
      setTimeout(() => {
        setSuccessMsg(null);
        setActiveStep(1);
      }, 1000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Incorrect or expired verification code.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequestPhoneOtp = async () => {
    if (!phoneVal) return setError("Please specify a phone number.");
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/verifications/mobile/request', { target: phoneVal });
      setIsPhoneRequested(true);
      setPhoneCooldown(60);
      setSuccessMsg("Verification code dispatched via SMS!");
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to dispatch SMS verification OTP code.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyPhoneOtp = async () => {
    if (!phoneOtp || phoneOtp.length !== 6) return setError("Specify a valid 6-digit OTP code.");
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/verifications/mobile/verify', { code: phoneOtp });
      setIsPhoneVerified(true);
      setSuccessMsg("Phone number verified successfully!");
      setTimeout(() => {
        setSuccessMsg(null);
        setActiveStep(2);
      }, 1000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Incorrect or expired verification code.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyAadhaar = async () => {
    if (!aadhaarVal || !aadhaarVal.match(/^\d{12}$/)) {
      return setError("Please specify a valid 12-digit Aadhaar ID number.");
    }
    if (!aadhaarConsent) {
      return setError("You must check the consent box to verify.");
    }
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/verifications/identity', {
        provider: 'aadhaar',
        code: aadhaarVal
      });
      setIsAadhaarVerified(true);
      setSuccessMsg("Aadhaar eKYC verification successful!");
      setTimeout(() => {
        setSuccessMsg(null);
        setActiveStep(3);
      }, 1000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Aadhaar eKYC verification failed.");
    } finally {
      setSubmitting(false);
    }
  };

  // Mock document file uploads to storage
  const uploadMockFile = async (file: File, prefix: string) => {
    // Generate a mock URL representing uploaded object in vault
    return `https://roomwallah-vault.s3.amazonaws.com/docs/${prefix}_${UUID()}_${file.name}`;
  };

  const UUID = () => Math.random().toString(36).substring(2, 15);

  const handleDeedNext = async () => {
    if (!deedFile && !deedUrl) {
      return setError("Please select or upload your property deeds document.");
    }
    setError(null);
    setSubmitting(true);
    try {
      if (deedFile) {
        const url = await uploadMockFile(deedFile, "deed");
        setDeedUrl(url);
      }
      setActiveStep(4);
    } catch (err) {
      setError("Failed to upload document file. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitVerificationPipeline = async () => {
    if (!selectedPropertyId) {
      return setError("Please select a property listing to verify.");
    }
    if (!utilityFile && !utilityUrl) {
      return setError("Please upload your electricity or utility bill.");
    }
    setError(null);
    setSubmitting(true);
    try {
      let finalUtilityUrl = utilityUrl;
      if (utilityFile) {
        finalUtilityUrl = await uploadMockFile(utilityFile, "utility");
        setUtilityUrl(finalUtilityUrl);
      }

      const res = await apiClient.post('/verifications/property', {
        propertyId: selectedPropertyId,
        documentUrl: deedUrl,
        utilityBillUrl: finalUtilityUrl
      });

      setVerificationResult(res.data.data);
      setActiveStep(5); // Show result page
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to submit property verification details.");
    } finally {
      setSubmitting(false);
    }
  };

  const steps = [
    { title: 'Email OTP', desc: 'Verify email' },
    { title: 'Mobile OTP', desc: 'Verify phone' },
    { title: 'Aadhaar eKYC', desc: 'Verify identity' },
    { title: 'Sale Deeds', desc: 'Upload ownership' },
    { title: 'Utility Bills', desc: 'Verify address' },
  ];

  return (
    <div className="min-h-[85vh] py-10 px-4 md:px-8 max-w-4xl mx-auto">
      
      {/* Stepper Header */}
      <div className="mb-10 text-center md:text-left">
        <h1 className="text-3xl font-extrabold font-outfit text-slate-800 dark:text-white mb-2">
          Listing & Owner Verification Wizard
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Complete the security validation steps to verify your ownership, unlock your badge, and publish active properties.
        </p>

        {/* 5-Step Progress Tracker */}
        <div className="mt-8 grid grid-cols-2 sm:grid-cols-5 gap-4">
          {steps.map((step, idx) => {
            const isCompleted = activeStep > idx;
            const isActive = activeStep === idx;
            return (
              <div 
                key={idx}
                className={`p-3 rounded-xl border transition-all text-left ${
                  isActive 
                    ? 'bg-indigo-600/10 border-indigo-500 text-indigo-400 font-semibold' 
                    : isCompleted 
                      ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400' 
                      : 'bg-slate-900/40 border-slate-800 text-slate-500'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[10px] uppercase tracking-wider font-bold">Step {idx + 1}</span>
                  {isCompleted && <Check className="w-3.5 h-3.5 text-emerald-400" />}
                </div>
                <p className="text-xs font-bold truncate text-slate-200">{step.title}</p>
                <p className="text-[10px] text-slate-400 truncate mt-0.5">{step.desc}</p>
              </div>
            );
          })}
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-xl flex items-center space-x-3 text-red-400 text-sm animate-fade-in">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center space-x-3 text-emerald-400 text-sm animate-fade-in">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Steps Content Card */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 md:p-8 shadow-sm">
        
        {/* STEP 0: Email OTP */}
        {activeStep === 0 && (
          <div className="space-y-6">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                <Mail className="w-5 h-5 text-indigo-500" />
                <span>Verify Registered Email Address</span>
              </h2>
              <p className="text-xs text-slate-400">Validate ownership of your profile email to send notification logs.</p>
            </div>

            {isEmailVerified ? (
              <div className="p-8 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-center space-y-3">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
                <h3 className="font-bold text-slate-100">Email Address Verified</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto">
                  Your registered email address <strong>{emailVal}</strong> has been successfully validated.
                </p>
                <button
                  onClick={() => setActiveStep(1)}
                  className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition mt-2"
                >
                  Proceed to Step 2
                </button>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">Email Address</label>
                  <div className="flex gap-3">
                    <input
                      type="email"
                      value={emailVal}
                      onChange={(e) => setEmailVal(e.target.value)}
                      placeholder="name@example.com"
                      disabled={isEmailRequested}
                      className="flex-1 px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition text-sm text-slate-100"
                    />
                    <button
                      onClick={handleRequestEmailOtp}
                      disabled={submitting || emailCooldown > 0}
                      className="px-5 py-3 bg-slate-900 border border-slate-800 hover:bg-slate-800 rounded-xl text-xs font-semibold transition text-slate-200 disabled:opacity-50"
                    >
                      {emailCooldown > 0 ? `Resend (${emailCooldown}s)` : 'Request OTP'}
                    </button>
                  </div>
                </div>

                {isEmailRequested && (
                  <div className="space-y-4 p-5 bg-slate-950/20 border border-slate-800 rounded-xl animate-slide-up">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-300 block">Enter 6-Digit Email OTP</label>
                      <input
                        type="text"
                        maxLength={6}
                        value={emailOtp}
                        onChange={(e) => setEmailOtp(e.target.value)}
                        placeholder="000000"
                        className="w-full text-center tracking-widest font-mono text-xl px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl outline-none transition text-slate-100"
                      />
                    </div>
                    <button
                      onClick={handleVerifyEmailOtp}
                      disabled={submitting || emailOtp.length !== 6}
                      className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition flex items-center justify-center gap-1"
                    >
                      {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Confirm Email Verification'}
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* STEP 1: Phone OTP */}
        {activeStep === 1 && (
          <div className="space-y-6">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                <Phone className="w-5 h-5 text-indigo-500" />
                <span>Verify Phone Number</span>
              </h2>
              <p className="text-xs text-slate-400">Validate ownership of your mobile number to trigger instant SMS alerts.</p>
            </div>

            {isPhoneVerified ? (
              <div className="p-8 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-center space-y-3">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
                <h3 className="font-bold text-slate-100">Phone Number Verified</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto">
                  Your mobile number <strong>{phoneVal}</strong> has been successfully validated.
                </p>
                <div className="flex justify-center gap-3 mt-4">
                  <button
                    onClick={() => setActiveStep(0)}
                    className="px-4 py-2 border border-slate-800 rounded-xl text-sm hover:bg-slate-800 transition"
                  >
                    Back
                  </button>
                  <button
                    onClick={() => setActiveStep(2)}
                    className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition"
                  >
                    Proceed to Step 3
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">Mobile Phone Number</label>
                  <div className="flex gap-3">
                    <input
                      type="text"
                      value={phoneVal}
                      onChange={(e) => setPhoneVal(e.target.value)}
                      placeholder="+91..."
                      disabled={isPhoneRequested}
                      className="flex-1 px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition text-sm text-slate-100"
                    />
                    <button
                      onClick={handleRequestPhoneOtp}
                      disabled={submitting || phoneCooldown > 0}
                      className="px-5 py-3 bg-slate-900 border border-slate-800 hover:bg-slate-800 rounded-xl text-xs font-semibold transition text-slate-200 disabled:opacity-50"
                    >
                      {phoneCooldown > 0 ? `Resend (${phoneCooldown}s)` : 'Request OTP'}
                    </button>
                  </div>
                </div>

                {isPhoneRequested && (
                  <div className="space-y-4 p-5 bg-slate-950/20 border border-slate-800 rounded-xl animate-slide-up">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-300 block">Enter 6-Digit SMS OTP</label>
                      <input
                        type="text"
                        maxLength={6}
                        value={phoneOtp}
                        onChange={(e) => phoneOtp.length <= 6 && setPhoneOtp(e.target.value)}
                        placeholder="000000"
                        className="w-full text-center tracking-widest font-mono text-xl px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl outline-none transition text-slate-100"
                      />
                    </div>
                    <button
                      onClick={handleVerifyPhoneOtp}
                      disabled={submitting || phoneOtp.length !== 6}
                      className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition flex items-center justify-center gap-1"
                    >
                      {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Confirm Phone Verification'}
                    </button>
                  </div>
                )}
                
                <div className="pt-4 border-t border-slate-800">
                  <button
                    onClick={() => setActiveStep(0)}
                    className="px-5 py-2.5 border border-slate-800 rounded-xl text-slate-300 font-semibold text-sm hover:bg-slate-800 transition"
                  >
                    Back
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* STEP 2: Aadhaar eKYC */}
        {activeStep === 2 && (
          <div className="space-y-6">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                <UserCheck className="w-5 h-5 text-indigo-500" />
                <span>Aadhaar eKYC Identity Validation</span>
              </h2>
              <p className="text-xs text-slate-400">Securely check your profile identity credentials against the UIDAI registry.</p>
            </div>

            {isAadhaarVerified ? (
              <div className="p-8 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-center space-y-3">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto" />
                <h3 className="font-bold text-slate-100">Identity Verified</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto">
                  Your Aadhaar eKYC identity is fully verified. Your profile now features the verified owner badge.
                </p>
                <div className="flex justify-center gap-3 mt-4">
                  <button
                    onClick={() => setActiveStep(1)}
                    className="px-4 py-2 border border-slate-800 rounded-xl text-sm hover:bg-slate-800 transition"
                  >
                    Back
                  </button>
                  <button
                    onClick={() => setActiveStep(3)}
                    className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition"
                  >
                    Proceed to Step 4
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">12-Digit Aadhaar Card Number</label>
                  <input
                    type="text"
                    maxLength={12}
                    value={aadhaarVal}
                    onChange={(e) => setAadhaarVal(e.target.value.replace(/\D/g, ''))}
                    placeholder="0000 0000 0000"
                    className="w-full px-4 py-3 bg-slate-950/40 border border-slate-800 focus:border-primary rounded-xl focus:ring-1 focus:ring-primary outline-none transition text-sm text-slate-100 font-mono text-center tracking-widest"
                  />
                </div>

                <div className="p-4 bg-slate-950/20 border border-slate-800 rounded-xl flex items-start gap-3">
                  <input
                    type="checkbox"
                    id="consent"
                    checked={aadhaarConsent}
                    onChange={(e) => setAadhaarConsent(e.target.checked)}
                    className="mt-1 accent-indigo-500 rounded cursor-pointer"
                  />
                  <label htmlFor="consent" className="text-xs text-slate-400 leading-normal cursor-pointer select-none">
                    I explicitly consent to RoomWallah using my Aadhaar details for eKYC verification. I understand this data is encrypted securely in AES-256 GCM and is only used to verify my listing account.
                  </label>
                </div>

                <div className="flex justify-between pt-4 border-t border-slate-800">
                  <button
                    onClick={() => setActiveStep(1)}
                    className="px-5 py-2.5 border border-slate-800 rounded-xl text-slate-300 font-semibold text-sm hover:bg-slate-800 transition"
                  >
                    Back
                  </button>
                  <button
                    onClick={handleVerifyAadhaar}
                    disabled={submitting || aadhaarVal.length !== 12 || !aadhaarConsent}
                    className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition flex items-center gap-1.5 disabled:opacity-50"
                  >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <>Verify Identity <ChevronRight className="w-4 h-4" /></>}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* STEP 3: Property deeds */}
        {activeStep === 3 && (
          <div className="space-y-6">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                <Building className="w-5 h-5 text-indigo-500" />
                <span>Upload Property Ownership Proof</span>
              </h2>
              <p className="text-xs text-slate-400">Submit Registry Deeds or Sale Deeds matching your registered profile name.</p>
            </div>

            {myProperties.length === 0 ? (
              <div className="p-6 bg-slate-950/20 border border-slate-800 rounded-xl text-center space-y-3">
                <Building className="w-12 h-12 text-slate-600 mx-auto" />
                <h3 className="font-semibold text-slate-300">No Draft Properties Found</h3>
                <p className="text-xs text-slate-400 max-w-sm mx-auto">
                  You need to create a property listing draft before starting its document validation.
                </p>
                <button
                  onClick={() => navigate('/listings/create')}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-xl text-xs font-bold transition text-white"
                >
                  Create Listing Draft
                </button>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">Select Listing to Verify</label>
                  <select
                    value={selectedPropertyId}
                    onChange={(e) => setSelectedPropertyId(e.target.value)}
                    className="w-full p-3 border border-slate-800 rounded-xl bg-slate-950 text-slate-200 text-sm outline-none focus:border-indigo-500"
                  >
                    {myProperties.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.title} {p.address?.city ? `(${p.address.city})` : ''}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-slate-300 block">Sale Deeds or Registry Deed Document</label>
                  
                  <div
                    className={`border-2 border-dashed rounded-xl p-8 text-center flex flex-col items-center justify-center cursor-pointer transition ${
                      deedFile ? 'border-emerald-500 bg-emerald-500/5' : 'border-slate-800 hover:border-slate-700'
                    }`}
                    onClick={() => document.getElementById('deed-upload')?.click()}
                  >
                    <input
                      type="file"
                      id="deed-upload"
                      className="hidden"
                      accept="image/*,application/pdf"
                      onChange={(e) => e.target.files?.[0] && setDeedFile(e.target.files[0])}
                    />
                    
                    {deedFile ? (
                      <div className="space-y-2">
                        <FileText className="w-12 h-12 text-emerald-500 mx-auto" />
                        <p className="text-sm font-semibold text-slate-200">{deedFile.name}</p>
                        <p className="text-xs text-slate-500">{(deedFile.size / 1024 / 1024).toFixed(2)} MB • Click to replace</p>
                      </div>
                    ) : (
                      <div className="space-y-2">
                        <UploadCloud className="w-12 h-12 text-slate-500 mx-auto" />
                        <p className="text-sm font-semibold text-slate-300">Drag & drop ownership document or click to browse</p>
                        <p className="text-xs text-slate-500">Supports PDF, PNG, JPG up to 10MB</p>
                      </div>
                    )}
                  </div>
                </div>

                <div className="flex justify-between pt-4 border-t border-slate-800">
                  <button
                    onClick={() => setActiveStep(2)}
                    className="px-5 py-2.5 border border-slate-800 rounded-xl text-slate-300 font-semibold text-sm hover:bg-slate-800 transition"
                  >
                    Back
                  </button>
                  <button
                    onClick={handleDeedNext}
                    disabled={submitting || (!deedFile && !deedUrl)}
                    className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition flex items-center gap-1.5 disabled:opacity-50"
                  >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <>Next Step <ChevronRight className="w-4 h-4" /></>}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* STEP 4: Utility bill upload */}
        {activeStep === 4 && (
          <div className="space-y-6">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
                <FileText className="w-5 h-5 text-indigo-500" />
                <span>Upload Recent Utility Bill</span>
              </h2>
              <p className="text-xs text-slate-400">Submit a recent electricity or water bill to cross-reference location coordinates and address.</p>
            </div>

            <div className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-300 block">Electricity / Water / Internet Bill</label>
                
                <div
                  className={`border-2 border-dashed rounded-xl p-8 text-center flex flex-col items-center justify-center cursor-pointer transition ${
                    utilityFile ? 'border-emerald-500 bg-emerald-500/5' : 'border-slate-800 hover:border-slate-700'
                  }`}
                  onClick={() => document.getElementById('utility-upload')?.click()}
                >
                  <input
                    type="file"
                    id="utility-upload"
                    className="hidden"
                    accept="image/*,application/pdf"
                    onChange={(e) => e.target.files?.[0] && setUtilityFile(e.target.files[0])}
                  />
                  
                  {utilityFile ? (
                    <div className="space-y-2">
                      <FileText className="w-12 h-12 text-emerald-500 mx-auto" />
                      <p className="text-sm font-semibold text-slate-200">{utilityFile.name}</p>
                      <p className="text-xs text-slate-500">{(utilityFile.size / 1024 / 1024).toFixed(2)} MB • Click to replace</p>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <UploadCloud className="w-12 h-12 text-slate-500 mx-auto" />
                      <p className="text-sm font-semibold text-slate-300">Drag & drop utility bill or click to browse</p>
                      <p className="text-xs text-slate-500">Supports PDF, PNG, JPG up to 10MB</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="flex justify-between pt-4 border-t border-slate-800">
                <button
                  onClick={() => setActiveStep(3)}
                  className="px-5 py-2.5 border border-slate-800 rounded-xl text-slate-300 font-semibold text-sm hover:bg-slate-800 transition"
                >
                  Back
                </button>
                <button
                  onClick={handleSubmitVerificationPipeline}
                  disabled={submitting || (!utilityFile && !utilityUrl)}
                  className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-sm transition flex items-center gap-1.5 disabled:opacity-50"
                >
                  {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <>Submit Pipeline <ChevronRight className="w-4 h-4" /></>}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* STEP 5: Verification Results */}
        {activeStep === 5 && verificationResult && (
          <div className="space-y-6 text-center py-4">
            {verificationResult.approvalStatus === 'APPROVED' ? (
              <CheckCircle2 className="w-16 h-16 text-emerald-500 mx-auto animate-bounce" />
            ) : (
              <Clock className="w-16 h-16 text-amber-500 mx-auto animate-pulse" />
            )}
            
            <div className="space-y-2">
              <h2 className="text-2xl font-bold font-outfit text-slate-800 dark:text-white">
                {verificationResult.approvalStatus === 'APPROVED' 
                  ? 'Verification Approved Automatically!' 
                  : 'Submitted to Admin Queue'}
              </h2>
              <p className="text-sm text-slate-500 max-w-md mx-auto">
                {verificationResult.approvalStatus === 'APPROVED'
                  ? 'The Jaccard similarity metrics and GPS city-boundary parameters passed all rule thresholds successfully.'
                  : 'The comparison scores fell below auto-approval parameters. An administrator will review your deeds manually.'}
              </p>
            </div>

            <div className="p-5 bg-slate-950/40 border border-slate-800 rounded-2xl max-w-md mx-auto text-left space-y-3">
              <h3 className="text-xs uppercase tracking-wider font-bold text-slate-400 mb-2">Automated Check Results:</h3>
              
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">Ownership Deed Name Match:</span>
                <span className={verificationResult.deedNameMatched ? 'text-emerald-400 font-semibold' : 'text-red-400'}>
                  {verificationResult.deedNameMatched ? 'PASSED' : 'MISMATCH'}
                </span>
              </div>

              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">Utility Address/Name Match:</span>
                <span className={verificationResult.utilityNameMatched ? 'text-emerald-400 font-semibold' : 'text-red-400'}>
                  {verificationResult.utilityNameMatched ? 'PASSED' : 'MISMATCH'}
                </span>
              </div>

              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">GPS City Bounds Match:</span>
                <span className={verificationResult.locationMatched ? 'text-emerald-400 font-semibold' : 'text-red-400'}>
                  {verificationResult.locationMatched ? 'PASSED' : 'MISMATCH'}
                </span>
              </div>

              <div className="border-t border-slate-800 pt-2 flex justify-between items-center text-sm font-semibold">
                <span className="text-slate-300">Confidence Score:</span>
                <span className={verificationResult.confidenceScore >= 70 ? 'text-emerald-400' : 'text-amber-400'}>
                  {verificationResult.confidenceScore}%
                </span>
              </div>

              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-400">Final Decision:</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                  verificationResult.approvalStatus === 'APPROVED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'
                }`}>
                  {verificationResult.approvalStatus}
                </span>
              </div>
            </div>

            <div className="flex justify-center gap-4 pt-6">
              <button
                onClick={() => navigate('/listings')}
                className="px-5 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-sm rounded-xl transition"
              >
                Go to My Listings
              </button>
              <button
                onClick={() => {
                  setDeedFile(null);
                  setDeedUrl('');
                  setUtilityFile(null);
                  setUtilityUrl('');
                  setVerificationResult(null);
                  setActiveStep(0);
                }}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl transition flex items-center space-x-1"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Verify Another</span>
              </button>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
