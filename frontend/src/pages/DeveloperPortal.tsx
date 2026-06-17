import React, { useState } from 'react';
import { 
  LineChart, Line, BarChart, Bar, XAxis, YAxis, 
  CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  Terminal, Key, Activity, ShieldAlert, Cpu, 
  BookOpen, Code, ArrowUpRight, Download, RefreshCw 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

// Mock developer stats
const apiUsageData = [
  { day: 'Mon', requests: 12400, errors: 45, latency: 120, rateLimitBlocks: 12 },
  { day: 'Tue', requests: 14500, errors: 62, latency: 115, rateLimitBlocks: 18 },
  { day: 'Wed', requests: 13200, errors: 38, latency: 110, rateLimitBlocks: 5 },
  { day: 'Thu', requests: 15900, errors: 55, latency: 125, rateLimitBlocks: 28 },
  { day: 'Fri', requests: 18200, errors: 90, latency: 130, rateLimitBlocks: 34 },
  { day: 'Sat', requests: 11400, errors: 22, latency: 105, rateLimitBlocks: 8 },
  { day: 'Sun', requests: 9800, errors: 18, latency: 95, rateLimitBlocks: 4 },
];

export default function DeveloperPortal() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 800);
  };

  const handleExportUsageCSV = () => {
    const headers = 'Day,API Requests,Errors,Avg Latency (ms),Rate Limit Blocks (429)\n';
    const rows = apiUsageData.map(d => `${d.day},${d.requests},${d.errors},${d.latency},${d.rateLimitBlocks}`).join('\n');
    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", "developer_api_usage.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Terminal className="w-8 h-8 text-primary" />
            Developer Integration Portal
          </h1>
          <p className="text-muted-foreground text-sm">
            Manage RoomWallah platform APIs, review actuator keys performance, and trace HTTP response times.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleExportUsageCSV}
            className="flex items-center gap-1.5 px-3 py-1.5 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-xs font-semibold text-slate-450 hover:text-white"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export Usage CSV</span>
          </button>
          <button
            onClick={() => navigate('/developer/keys')}
            className="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-primary to-secondary text-white font-bold text-xs rounded-xl hover:opacity-95 shadow-md shadow-indigo-500/10 transition-all"
          >
            <Key className="w-3.5 h-3.5" />
            <span>Manage API Keys</span>
          </button>
        </div>
      </div>

      {/* Quota gauges and status info */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        {/* Quota limit card */}
        <div className="glass p-5 rounded-3xl border border-white/5 space-y-4">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span>API Quota Consumption</span>
            <span className="text-primary-light">32%</span>
          </div>
          <div>
            <div className="text-3xl font-black text-white">32,410 / 100,000</div>
            <p className="text-[10px] text-slate-500 mt-1">Queries reset on 01 Jul 2026. Rate limit: 60req/min.</p>
          </div>
          <div className="w-full bg-slate-950 border border-slate-900 h-2 rounded-full overflow-hidden">
            <div className="bg-primary h-full rounded-full w-[32%]" />
          </div>
        </div>

        {/* Avg Latency card */}
        <div className="glass p-5 rounded-3xl border border-white/5 space-y-4">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span>Average Latency</span>
            <span className="text-emerald-450 font-bold">-4ms YoY</span>
          </div>
          <div>
            <div className="text-3xl font-black text-white">114 ms</div>
            <p className="text-[10px] text-slate-500 mt-1">P99 response time across all endpoints. Server node: IN-WEST.</p>
          </div>
          <div className="flex gap-1.5 items-end h-6 pt-2">
            {[14, 18, 12, 16, 22, 10, 8].map((v, i) => (
              <div key={i} className="flex-1 bg-emerald-500/30 rounded-t" style={{ height: `${v * 4}%` }} />
            ))}
          </div>
        </div>

        {/* Errors card */}
        <div className="glass p-5 rounded-3xl border border-white/5 space-y-4">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span>HTTP Error Rate</span>
            <span className="text-rose-400 font-bold">0.42%</span>
          </div>
          <div>
            <div className="text-3xl font-black text-white">18 errors</div>
            <p className="text-[10px] text-slate-500 mt-1">Out of 4,210 requests logged today. Mostly 401 Unauthorized.</p>
          </div>
          <div className="flex gap-1.5 items-end h-6 pt-2">
            {[2, 8, 4, 12, 6, 2, 1].map((v, i) => (
              <div key={i} className="flex-1 bg-rose-500/30 rounded-t" style={{ height: `${v * 8}%` }} />
            ))}
          </div>
        </div>
      </div>

      {/* API Calls chart */}
      <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
        <div className="flex justify-between items-center flex-wrap gap-2">
          <div>
            <h2 className="text-base font-bold">API Traffic Trends</h2>
            <p className="text-xs text-muted-foreground">Historical Requests count vs Average Response Latency</p>
          </div>
          <button
            onClick={handleRefresh}
            className="p-2 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-slate-400"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-primary' : ''}`} />
          </button>
        </div>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={apiUsageData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
              <XAxis dataKey="day" stroke="#64748b" fontSize={11} />
              <YAxis stroke="#64748b" fontSize={11} />
              <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
              <Legend />
              <Line name="Requests" type="monotone" dataKey="requests" stroke="#6366f1" strokeWidth={2.5} activeDot={{ r: 6 }} />
              <Line name="Latency (ms)" type="monotone" dataKey="latency" stroke="#10b981" strokeWidth={2} />
              <Line name="Rate Limit Blocks (429)" type="monotone" dataKey="rateLimitBlocks" stroke="#f43f5e" strokeWidth={2} strokeDasharray="5 5" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Guide & SDKs Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="glass p-6 rounded-3xl border border-white/5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <h3 className="text-md font-bold text-white flex items-center gap-2">
              <Code className="w-5 h-5 text-indigo-400" />
              API Quick Start
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Authenticate all REST endpoints by attaching your secret API token as a Bearer credentials header inside the request configuration.
            </p>
          </div>
          <div className="p-4 bg-slate-950/80 border border-slate-900 rounded-xl font-mono text-[10px] space-y-1.5 text-slate-350">
            <div><span className="text-primary-light">curl</span> -X GET \</div>
            <div className="pl-4">"https://api.roomwallah.com/v1/listings?city=Mumbai" \</div>
            <div className="pl-4">-H <span className="text-amber-300">"Authorization: Bearer YOUR_API_KEY"</span></div>
          </div>
        </div>

        <div className="glass p-6 rounded-3xl border border-white/5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <h3 className="text-md font-bold text-white flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-indigo-400" />
              Available Endpoints
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Developers have read/write access to search profiles, properties metadata, and booking slots depending on configured scopes.
            </p>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex items-center gap-2 bg-slate-950/40 border border-slate-900 rounded-xl p-2.5">
              <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-mono text-[9px] font-bold">GET</span>
              <span className="font-semibold font-mono text-[10px] text-slate-200">/v1/properties</span>
            </div>
            <div className="flex items-center gap-2 bg-slate-950/40 border border-slate-900 rounded-xl p-2.5">
              <span className="px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-400 font-mono text-[9px] font-bold">POST</span>
              <span className="font-semibold font-mono text-[10px] text-slate-200">/v1/properties</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
