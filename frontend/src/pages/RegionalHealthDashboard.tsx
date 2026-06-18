import React, { useState, useEffect } from 'react';
import { ToggleLeft, ToggleRight, Server, Activity } from 'lucide-react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import { apiClient } from '../services/api';

export default function RegionalHealthDashboard() {
  const [regions, setRegions] = useState<any>({
    'US-EAST': true,
    'EU-WEST': true,
  });

  const fetchHealth = async () => {
    try {
      const res = await apiClient.get('/admin/regions');
      if (res.data && res.data.data) {
        setRegions(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching regional health from backend, using default simulated statuses");
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const handleToggleRegion = async (region: string, currentHealthy: boolean) => {
    try {
      await apiClient.post(`/admin/regions/${region}/health`, {
        healthy: !currentHealthy
      });
      fetchHealth();
    } catch (err) {
      setRegions({ ...regions, [region]: !currentHealthy });
    }
  };

  const latencyData = [
    { time: '00:00', 'US-EAST': 45, 'EU-WEST': 110 },
    { time: '04:00', 'US-EAST': 42, 'EU-WEST': 105 },
    { time: '08:00', 'US-EAST': 55, 'EU-WEST': 125 },
    { time: '12:00', 'US-EAST': 68, 'EU-WEST': 140 },
    { time: '16:00', 'US-EAST': 52, 'EU-WEST': 118 },
    { time: '20:00', 'US-EAST': 48, 'EU-WEST': 112 },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Multi-Region Routing & Failover
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Monitor regional storage bucket health (US-EAST vs EU-WEST) and simulate manual failovers.
          </p>
        </div>
        <Server className="w-8 h-8 text-indigo-400" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-1 space-y-4">
          <h3 className="text-base font-bold text-white mb-2">Storage Regions</h3>
          {Object.keys(regions).map(reg => (
            <div key={reg} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex justify-between items-center">
              <div className="space-y-1">
                <span className="font-semibold text-slate-200">{reg} Region</span>
                <span className={`block text-[10px] font-bold ${regions[reg] ? 'text-emerald-400' : 'text-rose-455 animate-pulse'}`}>
                  {regions[reg] ? 'ACTIVE / HEALTHY' : 'DEGRADED / FAILOVER'}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleToggleRegion(reg, regions[reg])}
                  className="transition-colors"
                >
                  {regions[reg] ? (
                    <ToggleRight className="w-9 h-9 text-indigo-500" />
                  ) : (
                    <ToggleLeft className="w-9 h-9 text-slate-600" />
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="lg:col-span-2">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-base font-bold text-white">Regional Storage Latency (ms)</h3>
              <Activity className="w-4 h-4 text-slate-500" />
            </div>
            <div className="h-[280px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={latencyData}>
                  <defs>
                    <linearGradient id="colorUs" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorEu" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="time" stroke="#94a3b8" fontSize={11} />
                  <YAxis stroke="#94a3b8" fontSize={11} />
                  <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                  <Area type="monotone" dataKey="US-EAST" stroke="#6366f1" fillOpacity={1} fill="url(#colorUs)" />
                  <Area type="monotone" dataKey="EU-WEST" stroke="#10b981" fillOpacity={1} fill="url(#colorEu)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
