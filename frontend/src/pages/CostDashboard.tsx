import React, { useState, useEffect } from 'react';
import { Cpu, DollarSign, Activity, Percent, AlertCircle } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { apiClient } from '../services/api';

export default function CostDashboard() {
  const [stats, setStats] = useState<any>({
    totalCostUsd: 452.80,
    monthlyLimitUsd: 500.00,
    requestCount: 1250,
    failureCount: 12,
    totalTokens: 2584100,
    averageLatencyMs: 180,
    failureRate: 0.0096,
    avgSemanticSearchConfidence: 0.94,
    recommendationCtr: 0.12,
    safetyViolations: 0,
    humanOverrides: 2,
    cacheHits: 850,
    cacheMisses: 400,
    vectorStorageCost: 12.50,
    avgHallucinationRisk: 0.02
  });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await apiClient.get('/admin/ai-analytics/dashboard');
        if (res.data && res.data.data) {
          setStats(res.data.data);
        }
      } catch (e) {
        console.warn("Failed fetching live AI cost stats, using simulated values");
      }
    };
    fetchStats();
  }, []);

  const costData = [
    { name: 'Mon', Cost: 65.40, VectorCost: 1.80 },
    { name: 'Tue', Cost: 58.20, VectorCost: 1.80 },
    { name: 'Wed', Cost: 72.10, VectorCost: 1.80 },
    { name: 'Thu', Cost: 84.50, VectorCost: 1.80 },
    { name: 'Fri', Cost: 90.20, VectorCost: 2.10 },
    { name: 'Sat', Cost: 42.60, VectorCost: 1.60 },
    { name: 'Sun', Cost: 39.80, VectorCost: 1.60 },
  ];

  const totalSpend = Number(stats.totalCostUsd) + Number(stats.vectorStorageCost || 0);
  const percentLimit = (totalSpend / Number(stats.monthlyLimitUsd)) * 100;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            AI & Infrastructure Cost Governance
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Real-time tracking of LLM requests, vector store indexing costs, and cache hit metrics.
          </p>
        </div>
        <span className="text-xs text-emerald-400 font-semibold bg-emerald-500/10 px-3 py-2 rounded-full border border-emerald-500/20">
          Budget Control Active
        </span>
      </div>

      {percentLimit > 90 && (
        <div className="bg-rose-500/15 border border-rose-500/30 rounded-2xl p-4 mb-8 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-rose-400 mt-0.5 shrink-0" />
          <div>
            <h4 className="text-sm font-bold text-rose-300">ALERT: Tenant AI Budget Threshold Exceeded</h4>
            <p className="text-xs text-rose-400 mt-1">
              Your overall AI expenditures have crossed 90% of the monthly budget limit. Optimization measures and caching are strongly recommended to avoid service restrictions.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">LLM Generation Spend</span>
            <DollarSign className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">${Number(stats.totalCostUsd).toFixed(2)}</p>
          <span className="text-[10px] text-slate-500">Accumulated this month</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Vector Storage Cost</span>
            <Cpu className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">${Number(stats.vectorStorageCost || 12.5).toFixed(2)}</p>
          <span className="text-[10px] text-slate-500">pgvector database size allocation</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Cache Utilization</span>
            <Percent className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">
            {((stats.cacheHits / (stats.cacheHits + stats.cacheMisses || 1)) * 100).toFixed(1)}%
          </p>
          <span className="text-[10px] text-emerald-450">{stats.cacheHits} Hits / {stats.cacheMisses} Misses</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Tokens Exchanged</span>
            <Activity className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">{(stats.totalTokens / 1_000_000).toFixed(2)}M</p>
          <span className="text-[10px] text-slate-500">Total input + output tokens</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-2">
          <h2 className="text-base font-bold text-white mb-4">Daily Cost Breakdown</h2>
          <div className="h-[280px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={costData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="name" stroke="#94a3b8" fontSize={11} />
                <YAxis stroke="#94a3b8" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="Cost" fill="#6366f1" radius={[4, 4, 0, 0]} name="LLM Tokens ($)" />
                <Bar dataKey="VectorCost" fill="#10b981" radius={[4, 4, 0, 0]} name="Vector Storage ($)" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1 flex flex-col justify-between">
          <div>
            <h2 className="text-base font-bold text-white mb-2">Monthly Budget Cap</h2>
            <p className="text-xs text-slate-400 mb-6">Cap allocation limit per tenant instance.</p>
            <div className="w-full bg-slate-850 rounded-full h-4 mb-4 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${percentLimit > 90 ? 'bg-rose-500' : 'bg-indigo-600'}`}
                style={{ width: `${Math.min(100, percentLimit)}%` }}
              />
            </div>
            <div className="flex justify-between text-xs font-semibold text-slate-350">
              <span>Spent: ${totalSpend.toFixed(2)}</span>
              <span>Limit: ${Number(stats.monthlyLimitUsd).toFixed(2)}</span>
            </div>
          </div>
          <div className="bg-slate-950 p-4 border border-slate-850 rounded-xl mt-6">
            <h4 className="text-xs font-bold text-white mb-2">Cost Governance Policies</h4>
            <ul className="text-[10px] text-slate-450 space-y-1.5 list-disc pl-4">
              <li>Automatic caching of embeddings in Redis.</li>
              <li>Semantic cache layer matching query threshold &gt; 0.90.</li>
              <li>SLA limits rate limiting active on prompt executions.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
