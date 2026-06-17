import React, { useState, useEffect } from 'react';
import { ToggleLeft, ToggleRight, Puzzle, Plus, Check } from 'lucide-react';
import { apiClient } from '../services/api';

export default function PluginManager() {
  const [plugins, setPlugins] = useState<any[]>([
    { id: 'tax-calc', pluginName: 'GST Tax Calculator Hook', version: '1.0.2', status: 'ACTIVE', permissions: 'READ_LEDGER, WRITE_TAX_LOG' },
    { id: 'insurance-broker', pluginName: 'Rent Insurance API Extension', version: '2.1.0', status: 'INACTIVE', permissions: 'READ_AGREEMENTS' }
  ]);

  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [newId, setNewId] = useState('');
  const [newName, setNewName] = useState('');
  const [newVer, setNewVer] = useState('1.0.0');
  const [newPerms, setNewPerms] = useState('');

  const fetchPlugins = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/admin/plugins');
      if (res.data && res.data.data) {
        setPlugins(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching plugins, using default simulated data");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlugins();
  }, []);

  const handleToggle = async (id: string, currentStatus: string) => {
    const action = currentStatus === 'ACTIVE' ? 'deactivate' : 'activate';
    try {
      await apiClient.post(`/admin/plugins/${id}/${action}`, {});
      fetchPlugins();
    } catch (err) {
      setPlugins(plugins.map(p => p.id === id ? { ...p, status: currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' } : p));
    }
  };

  const handleInstall = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newId || !newName) return;

    try {
      await apiClient.post('/admin/plugins', {
        id: newId,
        name: newName,
        version: newVer,
        permissions: newPerms
      });
      setShowForm(false);
      setNewId('');
      setNewName('');
      setNewPerms('');
      fetchPlugins();
    } catch (err) {
      const mock = {
        id: newId,
        pluginName: newName,
        version: newVer,
        status: 'INACTIVE',
        permissions: newPerms
      };
      setPlugins([...plugins, mock]);
      setShowForm(false);
      setNewId('');
      setNewName('');
      setNewPerms('');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Plugin & Integration Ecosystem
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Install and manage sandbox-isolated plugins and webhooks extending platform capabilities.
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-all flex items-center gap-1.5"
        >
          <Plus className="w-3.5 h-3.5" /> Install Plugin
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-4">
          {plugins.map(p => (
            <div key={p.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex justify-between items-center gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <Puzzle className="w-4 h-4 text-indigo-400" />
                  <h3 className="text-sm font-bold text-white">{p.pluginName}</h3>
                  <span className="text-[9px] text-slate-500 font-mono">v{p.version}</span>
                </div>
                <p className="text-xs text-slate-400 leading-snug">Permissions: <span className="font-mono text-[10px] text-slate-300">{p.permissions || 'None'}</span></p>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleToggle(p.id, p.status)}
                  className="transition-colors hover:text-white"
                >
                  {p.status === 'ACTIVE' ? (
                    <ToggleRight className="w-9 h-9 text-indigo-500" />
                  ) : (
                    <ToggleLeft className="w-9 h-9 text-slate-600" />
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>

        {showForm && (
          <div className="lg:col-span-1">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-base font-bold text-white mb-4">Install Plugin</h3>
              <form onSubmit={handleInstall} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Plugin ID</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. rent-ins"
                    value={newId}
                    onChange={(e) => setNewId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Plugin Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Rental Insurance Extension"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Required Scopes</label>
                  <input
                    type="text"
                    placeholder="e.g. READ_AGREEMENTS"
                    value={newPerms}
                    onChange={(e) => setNewPerms(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-colors flex items-center justify-center gap-1"
                >
                  <Check className="w-3.5 h-3.5" /> Register Plugin
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
