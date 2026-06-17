import { useState, useEffect } from 'react';
import { apiClient } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useNavigate } from 'react-router-dom';

export default function SettingsPage() {
  const { logout } = useAuthStore();
  const navigate = useNavigate();
  
  const [preferences, setPreferences] = useState({
    darkModePreferred: false,
    emailNotificationsEnabled: true,
    pushNotificationsEnabled: true,
    marketingNotificationsEnabled: false,
    preferredLanguage: 'en',
    preferredContactMethod: 'EMAIL',
  });
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showDeactivateModal, setShowDeactivateModal] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchSettings = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/users/me');
      const data = res.data.data;
      setPreferences({
        darkModePreferred: data.darkModePreferred,
        emailNotificationsEnabled: data.emailNotificationsEnabled,
        pushNotificationsEnabled: data.pushNotificationsEnabled,
        marketingNotificationsEnabled: data.marketingNotificationsEnabled,
        preferredLanguage: data.preferredLanguage || 'en',
        preferredContactMethod: data.preferredContactMethod || 'EMAIL',
      });
    } catch (err: any) {
      console.error('Failed to load settings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  const handleToggle = async (key: keyof typeof preferences) => {
    const updated = {
      ...preferences,
      [key]: !preferences[key],
    };
    setPreferences(updated);
    await saveSettings(updated);
  };

  const handleSelectChange = async (key: keyof typeof preferences, value: string) => {
    const updated = {
      ...preferences,
      [key]: value,
    };
    setPreferences(updated);
    await saveSettings(updated);
  };

  const saveSettings = async (updatedPrefs: typeof preferences) => {
    try {
      setSaving(true);
      setStatusMessage(null);
      await apiClient.put('/users/me', updatedPrefs);
      setStatusMessage({ type: 'success', text: 'Settings saved successfully' });
    } catch (err: any) {
      console.error(err);
      setStatusMessage({ type: 'error', text: 'Failed to save settings' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async () => {
    try {
      await apiClient.delete('/users/me');
      logout();
      navigate('/login');
    } catch (err: any) {
      console.error(err);
      setStatusMessage({ type: 'error', text: err.response?.data?.message || 'Failed to deactivate account.' });
      setShowDeactivateModal(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-primary to-purple-400 bg-clip-text text-transparent mb-8">
        Account Settings
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

      <div className="space-y-6">
        {/* Preference Card */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 shadow-xl backdrop-blur-md space-y-4">
          <h2 className="text-lg font-bold text-slate-200 border-b border-slate-800 pb-2">Preferences</h2>

          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-sm font-semibold text-slate-300 block">Dark Visual Mode</span>
              <span className="text-xs text-slate-500">Enable premium dark theme preferences</span>
            </div>
            <button
              onClick={() => handleToggle('darkModePreferred')}
              className={`w-11 h-6 rounded-full transition-all relative ${
                preferences.darkModePreferred ? 'bg-primary' : 'bg-slate-700'
              }`}
            >
              <span className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-all ${
                preferences.darkModePreferred ? 'transform translate-x-5' : ''
              }`} />
            </button>
          </div>

          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-sm font-semibold text-slate-300 block">Preferred Language</span>
              <span className="text-xs text-slate-500">Interface localization settings</span>
            </div>
            <select
              value={preferences.preferredLanguage}
              onChange={(e) => handleSelectChange('preferredLanguage', e.target.value)}
              className="px-3 py-1.5 bg-slate-950 border border-slate-850 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-primary"
            >
              <option value="en">English</option>
              <option value="hi">Hindi (हिन्दी)</option>
              <option value="kn">Kannada (ಕನ್ನಡ)</option>
              <option value="ta">Tamil (தமிழ்)</option>
            </select>
          </div>
        </div>

        {/* Notifications Card */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 shadow-xl backdrop-blur-md space-y-4">
          <h2 className="text-lg font-bold text-slate-200 border-b border-slate-800 pb-2">Notifications</h2>

          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-sm font-semibold text-slate-300 block">Email Alerts</span>
              <span className="text-xs text-slate-500">Receive system alerts via your registered email</span>
            </div>
            <button
              onClick={() => handleToggle('emailNotificationsEnabled')}
              className={`w-11 h-6 rounded-full transition-all relative ${
                preferences.emailNotificationsEnabled ? 'bg-primary' : 'bg-slate-700'
              }`}
            >
              <span className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-all ${
                preferences.emailNotificationsEnabled ? 'transform translate-x-5' : ''
              }`} />
            </button>
          </div>

          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-sm font-semibold text-slate-300 block">Push Notifications</span>
              <span className="text-xs text-slate-500">Receive platform push alerts in your browser</span>
            </div>
            <button
              onClick={() => handleToggle('pushNotificationsEnabled')}
              className={`w-11 h-6 rounded-full transition-all relative ${
                preferences.pushNotificationsEnabled ? 'bg-primary' : 'bg-slate-700'
              }`}
            >
              <span className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-all ${
                preferences.pushNotificationsEnabled ? 'transform translate-x-5' : ''
              }`} />
            </button>
          </div>

          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-sm font-semibold text-slate-300 block">Marketing Newsletter</span>
              <span className="text-xs text-slate-500">Stay up to date with updates and promotions</span>
            </div>
            <button
              onClick={() => handleToggle('marketingNotificationsEnabled')}
              className={`w-11 h-6 rounded-full transition-all relative ${
                preferences.marketingNotificationsEnabled ? 'bg-primary' : 'bg-slate-700'
              }`}
            >
              <span className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-all ${
                preferences.marketingNotificationsEnabled ? 'transform translate-x-5' : ''
              }`} />
            </button>
          </div>
        </div>

        {/* Danger Zone Card */}
        <div className="bg-slate-900/60 border border-red-900/30 rounded-2xl p-6 shadow-xl backdrop-blur-md space-y-4">
          <h2 className="text-lg font-bold text-rose-500 border-b border-rose-950/30 pb-2">Danger Zone</h2>
          <p className="text-xs text-slate-400">
            Deactivating your account is a soft delete. Your listings will be hidden, and you will be signed out. 
            You can request recovery by contacting administrative support in the future.
          </p>
          <button
            onClick={() => setShowDeactivateModal(true)}
            className="px-4 py-2.5 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 hover:border-rose-500/50 text-rose-400 font-semibold rounded-xl text-sm transition-all"
          >
            Deactivate My Account
          </button>
        </div>
      </div>

      {/* Confirmation Modal */}
      {showDeactivateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-slate-100">Are you absolutely sure?</h3>
            <p className="text-xs text-slate-400">
              This will deactivate your account and log you out. This action can only be reversed by administrators.
            </p>
            <div className="flex space-x-3 justify-end pt-2">
              <button
                onClick={() => setShowDeactivateModal(false)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-750 text-slate-300 font-semibold rounded-xl text-xs transition-all"
              >
                Cancel
              </button>
              <button
                onClick={handleDeactivate}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white font-semibold rounded-xl text-xs shadow-lg transition-all"
              >
                Confirm Deactivation
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
