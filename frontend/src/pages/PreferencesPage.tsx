import React, { useState } from 'react';
import { 
  Bell, Save, Mail, MessageSquare, AlertTriangle, 
  Clock, ShieldAlert, Sparkles, CheckCircle2 
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface PreferenceToggle {
  key: string;
  title: string;
  description: string;
  email: boolean;
  sms: boolean;
  whatsapp: boolean;
  push: boolean;
}

export default function PreferencesPage() {
  const [preferences, setPreferences] = useState<PreferenceToggle[]>([
    { key: 'booking', title: 'Booking & Visits Alerts', description: 'Real-time schedule notifications, host confirmations, and visit cancellations.', email: true, sms: true, whatsapp: true, push: true },
    { key: 'payout', title: 'Escrow & Financial Updates', description: 'Escrow fund initiation, receipt confirmation, and monthly rental disbursements.', email: true, sms: false, whatsapp: true, push: false },
    { key: 'verification', title: 'Landlord Trust Auditing', description: 'Status of physical verified matches and KYC document evaluations.', email: true, sms: true, whatsapp: false, push: true },
    { key: 'chat', title: 'Direct Messaging Alerts', description: 'Immediate notifications for landlord/tenant chat exchanges.', email: false, sms: false, whatsapp: true, push: true },
    { key: 'security', title: 'Security & Device Toggles', description: 'Suspicious IP access alarms, password updates, and API key audits.', email: true, sms: true, whatsapp: true, push: true },
  ]);

  const [frequency, setFrequency] = useState<'INSTANT' | 'DAILY' | 'WEEKLY' | 'NONE'>('INSTANT');
  const [showToast, setShowToast] = useState(false);

  const handleToggle = (itemKey: string, channel: 'email' | 'sms' | 'whatsapp' | 'push') => {
    setPreferences(prev => prev.map(item => {
      if (item.key === itemKey) {
        return { ...item, [channel]: !item[channel] };
      }
      return item;
    }));
  };

  const handleSave = () => {
    setShowToast(true);
    setTimeout(() => setShowToast(false), 3000);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6 text-slate-100 relative">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
          <Bell className="w-8 h-8 text-primary" />
          Notification Preferences
        </h1>
        <p className="text-muted-foreground text-sm">
          Customize delivery channels, frequencies, and alerts triggers for your account.
        </p>
      </div>

      {/* Main Form container */}
      <div className="glass rounded-3xl p-6 md:p-8 border border-white/5 space-y-8">
        
        {/* Delivery Frequency selection */}
        <div className="space-y-4 pb-6 border-b border-slate-900">
          <h2 className="text-base font-bold text-slate-200 flex items-center gap-2">
            <Clock className="w-5 h-5 text-indigo-400" />
            General Alert Digest Frequency
          </h2>
          <p className="text-xs text-slate-400">Controls how often aggregated summaries are dispatched to you.</p>
          
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {(['INSTANT', 'DAILY', 'WEEKLY', 'NONE'] as const).map((freq) => (
              <button
                key={freq}
                onClick={() => setFrequency(freq)}
                className={`py-3.5 rounded-xl border font-bold text-xs capitalize transition-all ${
                  frequency === freq 
                    ? 'bg-primary border-primary text-white shadow-md' 
                    : 'bg-slate-950 border-slate-900 text-slate-400 hover:border-slate-850 hover:text-slate-200'
                }`}
              >
                {freq.toLowerCase()} Delivery
              </button>
            ))}
          </div>
        </div>

        {/* Detailed Notification matrix */}
        <div className="space-y-6">
          <h2 className="text-base font-bold text-slate-200 flex items-center gap-2">
            <Mail className="w-5 h-5 text-indigo-400" />
            Channel Dispatch Rules
          </h2>
          <p className="text-xs text-slate-400">Choose which channels you want to use for specific category alerts.</p>
          
          <div className="space-y-5">
            {preferences.map((item) => (
              <div 
                key={item.key} 
                className="p-5 rounded-2xl bg-slate-950/40 border border-slate-900 hover:border-slate-850/60 transition-all space-y-4"
              >
                {/* Description details */}
                <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-2">
                  <div>
                    <h3 className="text-sm font-bold text-slate-200">{item.title}</h3>
                    <p className="text-xs text-slate-450 mt-1">{item.description}</p>
                  </div>
                </div>

                {/* Checklist options */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
                  {(['email', 'sms', 'whatsapp', 'push'] as const).map((channel) => (
                    <button
                      key={channel}
                      onClick={() => handleToggle(item.key, channel)}
                      className={`flex items-center justify-between px-4 py-2.5 rounded-xl border text-xs font-bold transition-all ${
                        item[channel] 
                          ? 'bg-slate-900 border-primary/40 text-primary-light text-slate-200' 
                          : 'bg-slate-950/20 border-slate-900 text-slate-500 hover:border-slate-850'
                      }`}
                    >
                      <span className="uppercase">{channel}</span>
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                        item[channel] 
                          ? 'bg-primary border-primary text-white' 
                          : 'border-slate-800 bg-slate-900'
                      }`}>
                        {item[channel] && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Action Bottom */}
        <div className="pt-6 border-t border-slate-900 flex justify-end">
          <button
            onClick={handleSave}
            className="px-6 py-3 bg-gradient-to-r from-primary to-secondary text-white font-semibold rounded-xl flex items-center gap-2 hover:opacity-95 shadow-md shadow-indigo-500/10 transition-all hover:translate-y-[-1px] active:translate-y-[0px]"
          >
            <Save className="w-4 h-4" />
            <span>Save Preferences</span>
          </button>
        </div>
      </div>

      {/* Toast Notification */}
      <AnimatePresence>
        {showToast && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="fixed bottom-6 right-6 p-4 bg-emerald-950/90 border border-emerald-500/30 text-emerald-300 rounded-2xl flex items-center gap-3 shadow-2xl z-50 text-xs"
          >
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            <div>
              <span className="font-bold text-white block">Preferences Updated</span>
              <span>Your configuration modifications are active.</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
