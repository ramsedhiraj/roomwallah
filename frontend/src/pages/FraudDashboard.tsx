import React, { useState } from 'react';
import { 
  BarChart, Bar, LineChart, Line, XAxis, YAxis, 
  CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  ShieldAlert, RefreshCw, AlertTriangle, CheckCircle, 
  Users, Globe, Lock, ArrowUpRight, ArrowRight, Eye 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

// Mock Data
const riskScoreDistribution = [
  { range: '0-20 (Low)', transactions: 1450 },
  { range: '21-40 (Norm)', transactions: 380 },
  { range: '41-60 (Med)', transactions: 85 },
  { range: '61-80 (High)', transactions: 24 },
  { range: '81-100 (Crit)', transactions: 8 },
];

const riskAlertsTimeline = [
  { day: 'Mon', highRiskAlerts: 2, lockedAttempts: 1 },
  { day: 'Tue', highRiskAlerts: 4, lockedAttempts: 2 },
  { day: 'Wed', highRiskAlerts: 1, lockedAttempts: 0 },
  { day: 'Thu', highRiskAlerts: 5, lockedAttempts: 3 },
  { day: 'Fri', highRiskAlerts: 3, lockedAttempts: 1 },
  { day: 'Sat', highRiskAlerts: 8, lockedAttempts: 5 },
  { day: 'Sun', highRiskAlerts: 2, lockedAttempts: 1 },
];

interface RiskCase {
  id: string;
  userId: string;
  userName: string;
  bookingId: string;
  amount: number;
  riskScore: number;
  triggerReason: string;
  ipAddress: string;
  country: string;
  status: 'UNDER_REVIEW' | 'RESOLVED_CLEAN' | 'LOCKED_BLOCKED';
  createdAt: string;
  rulesetVersion: string;
}

const initialCases: RiskCase[] = [
  { id: 'RC-1092', userId: 'usr-9281', userName: 'Rajesh Kumar', bookingId: 'BKG-5219', amount: 45000, riskScore: 82, triggerReason: 'Geo-IP country (Frankfurt, DE) differs from billing (IN)', ipAddress: '198.51.100.42', country: 'DE', status: 'UNDER_REVIEW', createdAt: '2026-06-15T18:32:00Z', rulesetVersion: 'v1.4.2' },
  { id: 'RC-1093', userId: 'usr-4412', userName: 'Priyah Sharma', bookingId: 'BKG-4412', amount: 28000, riskScore: 71, triggerReason: 'Multi-account card footprint (used by 3 tenant IDs)', ipAddress: '103.42.122.9', country: 'IN', status: 'UNDER_REVIEW', createdAt: '2026-06-15T17:15:00Z', rulesetVersion: 'v1.4.2' },
  { id: 'RC-1094', userId: 'usr-1250', userName: 'John Doe', bookingId: 'BKG-0182', amount: 12000, riskScore: 48, triggerReason: 'Velocity checks: 4 transactions within 10 minutes', ipAddress: '157.48.22.84', country: 'US', status: 'RESOLVED_CLEAN', createdAt: '2026-06-14T22:45:00Z', rulesetVersion: 'v1.4.0' },
  { id: 'RC-1095', userId: 'usr-7721', userName: 'Vikram Singh', bookingId: 'BKG-6632', amount: 95000, riskScore: 95, triggerReason: 'Card fingerprint flagged by Stripe Radar blocklist', ipAddress: '203.192.208.5', country: 'IN', status: 'LOCKED_BLOCKED', createdAt: '2026-06-13T11:04:00Z', rulesetVersion: 'v1.4.2' },
];

export default function FraudDashboard() {
  const [cases, setCases] = useState<RiskCase[]>(initialCases);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'UNDER_REVIEW' | 'RESOLVED_CLEAN' | 'LOCKED_BLOCKED'>('ALL');
  const navigate = useNavigate();

  const filteredCases = cases.filter(c => activeFilter === 'ALL' || c.status === activeFilter);

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <ShieldAlert className="w-8 h-8 text-rose-500 animate-pulse" />
            Fraud Prevention Dashboard
          </h1>
          <p className="text-muted-foreground text-sm">
            AI velocity risk indicators, multi-account footprint checks, and IP proxy analysis.
          </p>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { title: 'Under Review', value: cases.filter(c => c.status === 'UNDER_REVIEW').length, color: 'border-l-amber-500 text-amber-400', desc: 'Requires manual analysis' },
          { title: 'Total Flagged', value: cases.length, color: 'border-l-rose-500 text-rose-450', desc: 'Alerts raised this week' },
          { title: 'Overridden Clean', value: cases.filter(c => c.status === 'RESOLVED_CLEAN').length, color: 'border-l-emerald-500 text-emerald-450', desc: 'False positive validations' },
          { title: 'Accounts Locked', value: cases.filter(c => c.status === 'LOCKED_BLOCKED').length, color: 'border-l-rose-700 text-rose-300', desc: 'Permanent platform bans' },
        ].map((stat, idx) => (
          <div key={idx} className={`glass p-5 rounded-2xl border-l-4 ${stat.color} shadow-md`}>
            <div className="text-[10px] uppercase font-bold text-slate-400 tracking-widest">{stat.title}</div>
            <p className="text-3xl font-extrabold mt-1.5">{stat.value}</p>
            <p className="text-[10px] text-slate-500 mt-1">{stat.desc}</p>
          </div>
        ))}
      </div>

      {/* Visual Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Risk Score distribution */}
        <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
          <h2 className="text-sm font-bold">Transaction Risk Distribution</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={riskScoreDistribution} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                <XAxis dataKey="range" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
                <Bar name="Transactions Count" dataKey="transactions" fill="#f43f5e" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Anomalies timeline */}
        <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
          <h2 className="text-sm font-bold">Weekly Alert Spikes</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={riskAlertsTimeline} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                <XAxis dataKey="day" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
                <Legend />
                <Line name="High Risk Alerts" type="monotone" dataKey="highRiskAlerts" stroke="#ef4444" strokeWidth={2.5} />
                <Line name="Banned Attempts" type="monotone" dataKey="lockedAttempts" stroke="#a855f7" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Rules engine overview */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
        <h2 className="text-sm font-bold">Active Risk Evaluation Rules</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-slate-950/40 border border-slate-900 p-4 rounded-xl flex gap-3">
            <Globe className="w-5 h-5 text-indigo-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-bold text-white">Geo-IP Country Check</div>
              <p className="text-[10px] text-slate-450 mt-1">Triggers +40% score mismatch if billing country differs from access IP origin.</p>
            </div>
          </div>
          <div className="bg-slate-950/40 border border-slate-900 p-4 rounded-xl flex gap-3">
            <RefreshCw className="w-5 h-5 text-pink-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-bold text-white">Velocity Card Limits</div>
              <p className="text-[10px] text-slate-450 mt-1">Triggers +25% score if &gt;3 checkout attempts are completed within 15 mins.</p>
            </div>
          </div>
          <div className="bg-slate-950/40 border border-slate-900 p-4 rounded-xl flex gap-3">
            <Users className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-bold text-white">Multi-Account Cards</div>
              <p className="text-[10px] text-slate-450 mt-1">Triggers +50% score if a single credit card hash is shared across different accounts.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Active Incidents stream */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
        <div className="flex justify-between items-center flex-wrap gap-2">
          <h2 className="text-base font-bold">Risk Assessment Queue</h2>
          <div className="flex bg-slate-950 border border-slate-900 rounded-xl p-1 text-[10px] font-bold">
            {(['ALL', 'UNDER_REVIEW', 'RESOLVED_CLEAN', 'LOCKED_BLOCKED'] as const).map(f => (
              <button
                key={f}
                onClick={() => setActiveFilter(f)}
                className={`px-3 py-1 rounded-lg transition-all uppercase ${
                  activeFilter === f ? 'bg-primary text-white' : 'text-slate-450 hover:text-slate-200'
                }`}
              >
                {f.replace(/_/g, ' ')}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-3">
          {filteredCases.map(c => {
            const isHigh = c.riskScore > 65;
            return (
              <div 
                key={c.id}
                className={`p-4 rounded-xl border flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-slate-950/20 ${
                  c.status === 'UNDER_REVIEW' 
                    ? (isHigh ? 'border-rose-500/20 hover:border-rose-500/40 bg-rose-950/5' : 'border-amber-500/20 hover:border-amber-500/40 bg-amber-950/5')
                    : 'border-slate-900 hover:border-slate-850'
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${
                      c.status === 'UNDER_REVIEW' 
                        ? (isHigh ? 'bg-rose-500/20 text-rose-300' : 'bg-amber-500/20 text-amber-300')
                        : (c.status === 'RESOLVED_CLEAN' ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400')
                    }`}>
                      {c.status.replace(/_/g, ' ')}
                    </span>
                    <span className="font-mono text-slate-500 text-[10px]">{c.id}</span>
                    <span className="text-[10px] text-slate-400">· User: {c.userName} ({c.userId})</span>
                    <span className="text-[9px] bg-slate-900 border border-slate-800 text-indigo-400 px-1.5 py-0.5 rounded font-mono font-semibold">Ruleset: {c.rulesetVersion}</span>
                  </div>
                  <p className="text-xs text-slate-200 font-semibold mt-1">{c.triggerReason}</p>
                  <div className="text-[10px] text-slate-500 flex items-center gap-2">
                    <span>IP Address: <span className="font-mono text-slate-450">{c.ipAddress} ({c.country})</span></span>
                    <span>·</span>
                    <span>Amount: <span className="font-bold text-slate-400">₹{c.amount.toLocaleString()}</span></span>
                  </div>
                </div>

                <div className="flex items-center gap-4 shrink-0 w-full sm:w-auto justify-between border-t sm:border-none border-slate-900 pt-3 sm:pt-0">
                  <div className="text-right">
                    <div className={`text-xl font-black ${isHigh ? 'text-rose-450' : 'text-amber-400'}`}>
                      {c.riskScore}%
                    </div>
                    <div className="text-[9px] uppercase tracking-wider text-slate-500 font-bold">Risk Score</div>
                  </div>
                  <button
                    onClick={() => navigate(`/admin/fraud/risk-cases/${c.id}`)}
                    className="px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-semibold flex items-center gap-1.5 hover:text-white"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    Review
                  </button>
                </div>
              </div>
            )})}`
          {filteredCases.length === 0 && (
            <div className="text-center py-12 text-slate-550 border border-slate-900 rounded-xl">
              No incidents in this queue matches filters.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
