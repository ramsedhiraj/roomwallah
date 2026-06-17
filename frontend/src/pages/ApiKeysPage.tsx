import React, { useState } from 'react';
import { 
  Key, Plus, RotateCw, Trash2, Edit3, Eye, Copy, 
  Check, ShieldAlert, ArrowLeft, CheckCircle2, X 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

interface ApiKeyItem {
  id: string;
  name: string;
  prefix: string;
  scopes: string[];
  quota: string;
  expiresAt: string;
  lastUsedAt: string;
  errorRate: string;
  status: 'ACTIVE' | 'REVOKED';
  autoRotateReminders: boolean;
  rotationIntervalDays: number;
  lastRotatedAt: string;
}

const initialKeys: ApiKeyItem[] = [
  { id: 'key-1', name: 'Production Mobile Client', prefix: 'rw_live_a8f9', scopes: ['read:listings', 'read:bookings'], quota: '18,420 / 50,000', expiresAt: '2027-06-15T00:00:00Z', lastUsedAt: '2026-06-15T18:50:11Z', errorRate: '0.2%', status: 'ACTIVE', autoRotateReminders: true, rotationIntervalDays: 90, lastRotatedAt: '2026-05-12T00:00:00Z' },
  { id: 'key-2', name: 'CRM Integration Sync', prefix: 'rw_live_f022', scopes: ['read:listings', 'write:listings', 'read:bookings', 'read:payments'], quota: '13,990 / 50,000', expiresAt: '2026-12-31T00:00:00Z', lastUsedAt: '2026-06-15T17:42:05Z', errorRate: '0.6%', status: 'ACTIVE', autoRotateReminders: false, rotationIntervalDays: 90, lastRotatedAt: '2026-06-01T00:00:00Z' },
];

export default function ApiKeysPage() {
  const navigate = useNavigate();
  const [keys, setKeys] = useState<ApiKeyItem[]>(initialKeys);
  const [showCreateModal, setShowCreateModal] = useState(false);
  
  // Create Key Form state
  const [newName, setNewName] = useState('');
  const [newScopes, setNewScopes] = useState<string[]>(['read:listings']);
  const [newExpiry, setNewExpiry] = useState('365'); // days
  const [autoRotate, setAutoRotate] = useState(true);
  const [rotateDays, setRotateDays] = useState(90);

  const getExpirationCountdown = (expiryStr: string) => {
    const diffTime = new Date(expiryStr).getTime() - new Date().getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    if (diffDays <= 0) return 'Expired';
    return `${diffDays} days remaining`;
  };

  const getRotationTimeline = (lastRotatedStr: string, interval: number, enabled: boolean) => {
    if (!enabled) return 'Disabled';
    const lastRotated = new Date(lastRotatedStr);
    const nextRotated = new Date(lastRotated);
    nextRotated.setDate(nextRotated.getDate() + interval);
    
    const diffTime = nextRotated.getTime() - new Date().getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    return `Due in ${diffDays > 0 ? diffDays : 0} days (${nextRotated.toLocaleDateString('en-IN')})`;
  };
  
  // Show key value once modal
  const [newlyCreatedKeyVal, setNewlyCreatedKeyVal] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Scopes options
  const scopeOptions = [
    { value: 'read:listings', label: 'Read Listings', desc: 'Allows searching and details reading for properties.' },
    { value: 'write:listings', label: 'Write Listings', desc: 'Allows owners to insert/update properties.' },
    { value: 'read:bookings', label: 'Read Bookings', desc: 'Allows fetching user booking records.' },
    { value: 'read:payments', label: 'Read Payments', desc: 'Allows auditing payout escrow status.' },
  ];

  const handleScopeCheckbox = (scopeVal: string) => {
    setNewScopes(prev => 
      prev.includes(scopeVal) ? prev.filter(s => s !== scopeVal) : [...prev, scopeVal]
    );
  };

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleCreateKeySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    // Generate simulated key
    const generatedToken = `rw_live_${Math.random().toString(36).substring(2, 12)}${Math.random().toString(36).substring(2, 12)}`;
    const prefix = generatedToken.substring(0, 12);
    
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + parseInt(newExpiry));

    const newKeyItem: ApiKeyItem = {
      id: `key-${Date.now()}`,
      name: newName,
      prefix: prefix,
      scopes: [...newScopes],
      quota: '0 / 50,000',
      expiresAt: expiryDate.toISOString(),
      lastUsedAt: 'Never',
      errorRate: '0.0%',
      status: 'ACTIVE',
      autoRotateReminders: autoRotate,
      rotationIntervalDays: rotateDays,
      lastRotatedAt: new Date().toISOString(),
    };

    setKeys(prev => [...prev, newKeyItem]);
    setNewName('');
    setNewScopes(['read:listings']);
    setShowCreateModal(false);
    setNewlyCreatedKeyVal(generatedToken);
  };

  const handleRevokeKey = (id: string) => {
    setKeys(prev => prev.filter(k => k.id !== id));
    triggerToast('API Key revoked successfully.');
  };

  const handleRotateKey = (id: string) => {
    const rotatedToken = `rw_live_rot_${Math.random().toString(36).substring(2, 12)}`;
    setKeys(prev => prev.map(k => {
      if (k.id === id) {
        return {
          ...k,
          prefix: rotatedToken.substring(0, 12),
          lastUsedAt: 'Never',
        };
      }
      return k;
    }));
    setNewlyCreatedKeyVal(rotatedToken);
    triggerToast('API Key rotated. Old key revoked immediately.');
  };

  const copyToClipboard = (val: string) => {
    navigator.clipboard.writeText(val);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in relative">
      {/* Back to portal link */}
      <button 
        onClick={() => navigate('/developer')} 
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-xs"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Portal Overview</span>
      </button>

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Key className="w-8 h-8 text-primary" />
            API Credentials Manager
          </h1>
          <p className="text-muted-foreground text-sm">
            Generate and manage access tokens for third-party integrations and platforms sync.
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-1.5 px-4 py-2.5 bg-gradient-to-r from-primary to-secondary text-white font-bold text-xs rounded-xl hover:opacity-95 shadow-md shadow-indigo-500/10 transition-all hover:translate-y-[-1px] active:translate-y-[0px]"
        >
          <Plus className="w-4 h-4" />
          <span>Generate New Key</span>
        </button>
      </div>

      {/* Warning Alert */}
      <div className="bg-rose-950/20 border border-rose-900/30 rounded-2xl p-4 flex items-start gap-3 text-slate-350 text-xs leading-relaxed">
        <ShieldAlert className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-white block">Credential Security warning</span>
          <span>Your API keys carry full administrative permissions for property listings. Never commit secret tokens to frontend source repositories or push to public folders.</span>
        </div>
      </div>

      {/* Keys list */}
      <div className="space-y-4">
        {keys.map((k) => (
          <div key={k.id} className="glass rounded-2xl p-5 border border-white/5 flex flex-col md:flex-row justify-between items-start gap-4">
            <div className="space-y-3 flex-1">
              <div className="flex items-center gap-2.5 flex-wrap">
                <h3 className="text-sm font-bold text-slate-200">{k.name}</h3>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 font-bold text-[9px]">ACTIVE</span>
                <span className="text-[10px] text-slate-500 font-mono font-bold">{k.prefix}...</span>
              </div>

              {/* Scopes Badges */}
              <div className="flex flex-wrap gap-1.5">
                {k.scopes.map(s => (
                  <span key={s} className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-400 text-[10px] font-mono">
                    {s}
                  </span>
                ))}
              </div>

              {/* Specs */}
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 text-[10px] text-slate-500 pt-1">
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Monthly Quota</span>
                  <span className="font-semibold text-slate-200 block mt-0.5">{k.quota}</span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Last Authentication</span>
                  <span className="font-semibold text-slate-250 block mt-0.5">
                    {k.lastUsedAt !== 'Never' ? new Date(k.lastUsedAt).toLocaleString('en-IN') : 'Never'}
                  </span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Expiration</span>
                  <span className="font-semibold text-slate-200 block mt-0.5">
                    {new Date(k.expiresAt).toLocaleDateString('en-IN', {
                      day: 'numeric', month: 'short', year: 'numeric'
                    })}
                    <span className="text-primary-light block text-[9px] font-bold mt-0.5">{getExpirationCountdown(k.expiresAt)}</span>
                  </span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Auto-Rotation Status</span>
                  <span className={`font-semibold block mt-0.5 ${k.autoRotateReminders ? 'text-indigo-400 font-bold' : 'text-slate-400'}`}>
                    {getRotationTimeline(k.lastRotatedAt, k.rotationIntervalDays, k.autoRotateReminders)}
                  </span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Api error rate</span>
                  <span className="font-semibold text-rose-400 block mt-0.5">{k.errorRate}</span>
                </div>
              </div>
            </div>

            {/* Actions panel */}
            <div className="flex sm:flex-row md:flex-col gap-2 w-full md:w-auto shrink-0 border-t md:border-t-0 border-slate-900 pt-3 md:pt-0 justify-end">
              <button
                onClick={() => handleRotateKey(k.id)}
                className="flex items-center gap-1.5 px-3 py-2 border border-slate-800 bg-slate-900/40 hover:bg-slate-900 rounded-xl text-xs text-slate-350 hover:text-white transition-all flex-1 md:flex-none justify-center"
              >
                <RotateCw className="w-3.5 h-3.5" />
                <span>Rotate Key</span>
              </button>
              <button
                onClick={() => handleRevokeKey(k.id)}
                className="flex items-center gap-1.5 px-3 py-2 border border-slate-800 bg-slate-900/40 hover:bg-rose-950/20 hover:border-rose-900/30 rounded-xl text-xs text-slate-450 hover:text-rose-400 transition-all flex-1 md:flex-none justify-center"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Revoke</span>
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Create Key Modal */}
      <AnimatePresence>
        {showCreateModal && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.6 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowCreateModal(false)}
              className="fixed inset-0 bg-slate-950 z-40"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-xl bg-slate-950 border border-slate-900 p-6 rounded-2xl z-50 text-xs space-y-4"
            >
              <div className="flex justify-between items-center pb-2 border-b border-slate-900">
                <h3 className="text-base font-extrabold text-white">Generate API Key</h3>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="p-1 rounded hover:bg-slate-900 text-slate-500 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <form onSubmit={handleCreateKeySubmit} className="space-y-4 pt-2">
                <div className="space-y-1.5">
                  <label className="font-bold text-slate-300">Key Name Reference</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Jenkins Deploy Server, App-Sync Client"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-900 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-primary"
                  />
                </div>

                <div className="space-y-2">
                  <label className="font-bold text-slate-300 block">Select Key Scopes</label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {scopeOptions.map((opt) => {
                      const isChecked = newScopes.includes(opt.value);
                      return (
                        <div
                          key={opt.value}
                          onClick={() => handleScopeCheckbox(opt.value)}
                          className={`p-3 rounded-xl border cursor-pointer flex gap-3 transition-all ${
                            isChecked ? 'border-primary bg-indigo-950/5 text-slate-200' : 'border-slate-900 bg-slate-950/20 text-slate-400 hover:border-slate-850'
                          }`}
                        >
                          <div className={`w-4.5 h-4.5 border rounded flex items-center justify-center shrink-0 mt-0.5 transition-all ${
                            isChecked ? 'bg-primary border-primary text-white' : 'border-slate-800 bg-slate-900'
                          }`}>
                            {isChecked && <Check className="w-3.5 h-3.5" />}
                          </div>
                          <div>
                            <span className="font-bold block text-xs">{opt.label}</span>
                            <span className="text-[10px] text-slate-500 mt-0.5 block">{opt.desc}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="font-bold text-slate-300">Expiration Period</label>
                  <select
                    value={newExpiry}
                    onChange={(e) => setNewExpiry(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-900 rounded-xl text-xs text-slate-350 focus:outline-none"
                  >
                    <option value="30">30 Days (Short Term)</option>
                    <option value="90">90 Days</option>
                    <option value="365">365 Days (Standard)</option>
                    <option value="730">2 Years</option>
                  </select>
                </div>

                {/* Auto rotation settings */}
                <div className="p-4 rounded-xl bg-slate-950/40 border border-slate-900 space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-slate-200 block">Automatic Key Rotation reminders</span>
                      <span className="text-[10px] text-slate-500 mt-0.5 block">Trigger email warnings when rotation due timeline approaches.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setAutoRotate(!autoRotate)}
                      className={`w-10 h-6 rounded-full p-1 transition-all flex items-center ${
                        autoRotate ? 'bg-primary justify-end' : 'bg-slate-900 border border-slate-800 justify-start'
                      }`}
                    >
                      <div className="w-4 h-4 bg-white rounded-full" />
                    </button>
                  </div>
                  {autoRotate && (
                    <div className="space-y-1.5 animate-fade-in pt-1">
                      <label className="text-[10px] font-bold text-slate-400 uppercase">Rotation Frequency</label>
                      <select
                        value={rotateDays}
                        onChange={(e) => setRotateDays(parseInt(e.target.value))}
                        className="w-full px-3 py-2 bg-slate-950 border border-slate-900 rounded-xl text-[11px] text-slate-350 focus:outline-none"
                      >
                        <option value="30">Every 30 Days</option>
                        <option value="60">Every 60 Days</option>
                        <option value="90">Every 90 Days (Recommended)</option>
                      </select>
                    </div>
                  )}
                </div>

                <div className="flex gap-3 justify-end border-t border-slate-900 pt-4 font-semibold text-xs">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="px-4 py-2 border border-slate-800 rounded-xl hover:bg-slate-900 text-slate-400 hover:text-white"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-primary hover:opacity-95 text-white rounded-xl shadow-md shadow-indigo-950/25"
                  >
                    Generate Credentials
                  </button>
                </div>
              </form>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Secret key displays modal */}
      <AnimatePresence>
        {newlyCreatedKeyVal && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.6 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-slate-950 z-50"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-lg bg-slate-950 border border-slate-900 p-6 rounded-2xl z-55 text-xs space-y-4 shadow-2xl"
            >
              <div className="space-y-1">
                <h3 className="text-base font-extrabold text-white">API Key Generated</h3>
                <p className="text-slate-450 text-[11px] leading-relaxed">
                  Please copy this key and save it securely. For security reasons, <span className="text-white font-bold">you will not be able to view it again.</span>
                </p>
              </div>

              {/* Key panel copy */}
              <div className="p-3 bg-slate-950 border border-slate-900 rounded-xl flex items-center justify-between gap-3 text-xs">
                <span className="font-mono text-primary-light font-bold select-all break-all pr-2">{newlyCreatedKeyVal}</span>
                <button
                  onClick={() => copyToClipboard(newlyCreatedKeyVal)}
                  className="p-2 border border-slate-800 bg-slate-900 hover:border-slate-700 text-slate-400 hover:text-white rounded-xl shrink-0 transition-all"
                  title="Copy Key Token"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>

              <div className="flex justify-end pt-2 border-t border-slate-900">
                <button
                  onClick={() => setNewlyCreatedKeyVal(null)}
                  className="px-4 py-2 bg-primary text-white font-bold rounded-xl hover:opacity-95"
                >
                  I have saved this key
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Toast Notification */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="fixed bottom-6 right-6 p-4 bg-indigo-950/90 border border-indigo-500/30 text-indigo-300 rounded-2xl flex items-center gap-3 shadow-2xl z-50 text-xs"
          >
            <CheckCircle2 className="w-5 h-5 text-indigo-400 shrink-0" />
            <div>
              <span className="font-bold text-white block">Operation Successful</span>
              <span>{toastMessage}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
