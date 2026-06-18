import React, { useState } from 'react';
import { Brain, Activity, Sliders, Sparkles, TrendingUp, BarChart3, Check, AlertOctagon, RefreshCw, Trash2, Languages } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { apiClient } from '../services/api';
import { getTranslation, Locale } from '../utils/i18n';

interface DqEvent {
  id: string;
  eventName: string;
  errorReason: string;
  attempts: number;
  timestamp: string;
}

const INITIAL_DLQ_EVENTS: DqEvent[] = [
  { id: 'dlq-1', eventName: 'property.vectorized.indexing', errorReason: 'Milvus connection pool exhausted', attempts: 5, timestamp: '2026-06-15 19:30:10' },
  { id: 'dlq-2', eventName: 'tenant.recommendation.update', errorReason: 'Zustand auth store token refresh timeout', attempts: 4, timestamp: '2026-06-15 19:15:22' },
];

const RETRY_HISTOGRAM = [
  { attempt: '0 Retries', volume: 15482 },
  { attempt: '1 Retry', volume: 1420 },
  { attempt: '2 Retries', volume: 284 },
  { attempt: '3 Retries', volume: 65 },
  { attempt: '>3 (DLQ)', volume: 12 },
];

const FALLBACK_TREND = [
  { day: 'Mon', fallbackRate: 4.2 },
  { day: 'Tue', fallbackRate: 3.8 },
  { day: 'Wed', fallbackRate: 5.1 },
  { day: 'Thu', fallbackRate: 4.7 },
  { day: 'Fri', fallbackRate: 3.2 },
  { day: 'Sat', fallbackRate: 2.9 },
  { day: 'Sun', fallbackRate: 3.0 },
];

const LATENCY_TREND = [
  { time: '00:00', latencyMs: 142, load: 35 },
  { time: '04:00', latencyMs: 121, load: 12 },
  { time: '08:00', latencyMs: 184, load: 68 },
  { time: '12:00', latencyMs: 221, load: 94 },
  { time: '16:00', latencyMs: 198, load: 78 },
  { time: '20:00', latencyMs: 165, load: 52 },
];

const QUERY_TYPES = [
  { name: 'Semantic Search', value: 45, color: '#6366f1' },
  { name: 'Traditional Keyword', value: 30, color: '#3b82f6' },
  { name: 'Direct Field Filter', value: 25, color: '#10b981' },
];

export default function AiAnalyticsDashboard() {
  const [locale, setLocale] = useState<Locale>('en-IN');
  const [modelName, setModelName] = useState('RoomBERT-v2');
  const [cacheEnabled, setCacheEnabled] = useState(true);
  const [isConfigSaved, setIsConfigSaved] = useState(false);
  const [dlqEvents, setDlqEvents] = useState<DqEvent[]>(INITIAL_DLQ_EVENTS);
  const [dlqLoading, setDlqLoading] = useState(false);

  const t = (key: Parameters<typeof getTranslation>[1]) => getTranslation(locale, key);

  const handleSaveConfig = async () => {
    setIsConfigSaved(true);
    setTimeout(() => setIsConfigSaved(false), 3000);

    try {
      await apiClient.post('/admin/ai-analytics/config', {
        modelName,
        cacheEnabled,
      });
    } catch (err) {
      console.warn('API config push failed, updated locally.');
    }
  };

  const handleRedriveDlq = async () => {
    setDlqLoading(true);
    setTimeout(() => {
      setDlqEvents([]);
      setDlqLoading(false);
    }, 1000);

    try {
      await apiClient.post('/admin/outbox/redrive', {});
    } catch (err) {
      console.warn('DLQ redrive failed, simulated locally.');
    }
  };

  const handlePurgeDlq = async () => {
    setDlqLoading(true);
    setTimeout(() => {
      setDlqEvents([]);
      setDlqLoading(false);
    }, 800);

    try {
      await apiClient.post('/admin/outbox/purge', {});
    } catch (err) {
      console.warn('DLQ purge failed, simulated locally.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Header */}
      <div className="border-b border-slate-800 pb-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Global AI Analytics & Insights
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Monitor model metrics, outbox status retry histograms, DLQ rates, and semantic fallback thresholds.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Language selector */}
          <div className="flex items-center space-x-1 bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-350">
            <Languages className="w-4 h-4 text-indigo-400" />
            <select
              value={locale}
              onChange={(e) => setLocale(e.target.value as Locale)}
              className="bg-transparent border-none focus:ring-0 focus:outline-none cursor-pointer"
            >
              <option value="en-IN" className="bg-slate-950">EN (India)</option>
              <option value="hi-IN" className="bg-slate-950">HI (India)</option>
              <option value="mr-IN" className="bg-slate-950">MR (India)</option>
            </select>
          </div>

          <span className="text-xs text-indigo-400 font-semibold bg-indigo-500/10 px-3 py-2 rounded-full border border-indigo-500/20">
            Active Core Engine: v1.8.4-prod
          </span>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">AI Precision Rate</span>
            <Brain className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">98.6%</p>
          <span className="text-[10px] text-emerald-450 font-semibold block mt-1">Within SLA threshold (95%)</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Duplicate Accuracy</span>
            <Sparkles className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">97.4%</p>
          <span className="text-[10px] text-emerald-450 font-semibold block mt-1">F-1 Match score rating</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Recommendation CTR</span>
            <TrendingUp className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">12.8%</p>
          <span className="text-[10px] text-emerald-450 font-semibold block mt-1">+1.8% conversion spike</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-slate-400 uppercase">Search Fallback Rate</span>
            <BarChart3 className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">3.7%</p>
          <span className="text-[10px] text-emerald-450 font-semibold block mt-1">Target fallback &lt; 5.0%</span>
        </div>
      </div>

      {/* Latency & Intent Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        {/* Line Chart: Latency Trend */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-2">
          <div className="mb-4 flex justify-between items-center">
            <div>
              <h2 className="text-base font-bold text-white">AI Inference Latency & Load</h2>
              <p className="text-xs text-slate-400">Response latency in milliseconds vs concurrent client load percentage</p>
            </div>
            <Activity className="w-4 h-4 text-slate-500" />
          </div>
          <div className="h-[260px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={LATENCY_TREND} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={11} />
                <YAxis stroke="#94a3b8" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Line type="monotone" dataKey="latencyMs" stroke="#6366f1" strokeWidth={2} name="Latency (ms)" activeDot={{ r: 6 }} />
                <Line type="monotone" dataKey="load" stroke="#10b981" strokeWidth={2} name="System Load (%)" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Pie Chart: Query Breakdown */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1">
          <div className="mb-4">
            <h2 className="text-base font-bold text-white">Search Intent Channels</h2>
            <p className="text-xs text-slate-400">Breakdown of user search channel types</p>
          </div>
          <div className="h-[210px] w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={QUERY_TYPES} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={4} dataKey="value">
                  {QUERY_TYPES.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          {/* Custom Legends list */}
          <div className="space-y-2 mt-4">
            {QUERY_TYPES.map((type) => (
              <div key={type.name} className="flex justify-between items-center text-xs">
                <span className="flex items-center gap-2 text-slate-350">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: type.color }} />
                  {type.name}
                </span>
                <span className="font-semibold text-slate-200">{type.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Outbox & SLO Metrics Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        {/* Outbox Event Retry Histogram */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1">
          <div className="mb-4">
            <h2 className="text-base font-bold text-white">Outbox Retry Status Histogram</h2>
            <p className="text-xs text-slate-400">Distribution of outbox event message dispatch retries</p>
          </div>
          <div className="h-[240px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={RETRY_HISTOGRAM} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="attempt" stroke="#94a3b8" fontSize={9} />
                <YAxis stroke="#94a3b8" fontSize={9} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Bar dataKey="volume" fill="#6366f1" radius={[4, 4, 0, 0]} name="Events Volume" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Search Fallback Rate SLO Chart */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1">
          <div className="mb-4">
            <h2 className="text-base font-bold text-white">Search Fallback Rate (%)</h2>
            <p className="text-xs text-slate-400">Semantic search fallback to BM25 keyword matching (SLO threshold: 5%)</p>
          </div>
          <div className="h-[240px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={FALLBACK_TREND} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="day" stroke="#94a3b8" fontSize={10} />
                <YAxis stroke="#94a3b8" fontSize={10} domain={[0, 8]} />
                <Tooltip contentStyle={{ backgroundColor: '#090d16', border: '1px solid #1e293b' }} />
                <Line type="monotone" dataKey="fallbackRate" stroke="#10b981" strokeWidth={2} name="Fallback Rate" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Dead Letter Queue (DLQ) Details */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1 flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-base font-bold text-white flex items-center gap-1.5">
                <AlertOctagon className="w-5 h-5 text-rose-500 animate-pulse-subtle" />
                <span>Dead Letter Queue (DLQ)</span>
              </h2>
              <span className="text-[10px] bg-rose-500/10 text-rose-450 border border-rose-500/25 px-2 py-0.5 rounded font-bold">
                {dlqEvents.length} Pending
              </span>
            </div>

            {dlqEvents.length > 0 ? (
              <div className="space-y-3">
                {dlqEvents.map(e => (
                  <div key={e.id} className="p-3 bg-slate-950 border border-slate-850 rounded-xl text-xs space-y-1">
                    <div className="flex justify-between font-semibold text-slate-200">
                      <span className="truncate max-w-[150px]">{e.eventName}</span>
                      <span className="text-[10px] text-rose-450">Attempts: {e.attempts}</span>
                    </div>
                    <p className="text-[10px] text-slate-450 italic leading-snug">{e.errorReason}</p>
                    <span className="block text-[8px] text-slate-500 text-right">{e.timestamp}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-10 bg-slate-950/40 border border-slate-850 border-dashed rounded-xl">
                <Check className="w-8 h-8 text-emerald-500 mb-2" />
                <p className="text-slate-400 text-xs font-semibold">DLQ Queue Clear</p>
                <p className="text-slate-500 text-[10px] mt-0.5">All outbox events processed successfully.</p>
              </div>
            )}
          </div>

          {dlqEvents.length > 0 && (
            <div className="flex items-center gap-2 pt-4 border-t border-slate-800/80 mt-4">
              <button
                onClick={handlePurgeDlq}
                disabled={dlqLoading}
                className="flex-1 py-2 bg-slate-950 border border-slate-850 hover:bg-slate-900 text-[10px] font-bold text-slate-450 hover:text-white rounded-lg transition-colors flex items-center justify-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Purge DLQ</span>
              </button>
              <button
                onClick={handleRedriveDlq}
                disabled={dlqLoading}
                className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-500 text-[10px] font-bold text-white rounded-lg transition-colors flex items-center justify-center gap-1 shadow-md shadow-indigo-950/20"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${dlqLoading ? 'animate-spin' : ''}`} />
                <span>Re-drive Queue</span>
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Tuning Config controller */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-3 flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="space-y-1 text-center md:text-left">
            <h3 className="text-sm font-bold text-white flex items-center justify-center md:justify-start gap-1.5">
              <Sliders className="w-4.5 h-4.5 text-indigo-400" />
              <span>Model Controller & Caching</span>
            </h3>
            <p className="text-xs text-slate-400">Deploy fine-tuned neural model checkpoints or toggle semantic caching layers globally.</p>
          </div>

          <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
            <div className="flex items-center gap-2.5">
              <label className="text-xs text-slate-400 uppercase font-semibold">Active Alg:</label>
              <select
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                className="bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
              >
                <option value="RoomBERT-v2">RoomBERT-v2 (Transformer Contextual)</option>
                <option value="RoomGNN-v3">RoomGNN-v3 (Graph-based matching)</option>
                <option value="Collaborative-Filtering-Base">CF-Base (Collaborative Matrix)</option>
              </select>
            </div>

            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                checked={cacheEnabled}
                onChange={(e) => setCacheEnabled(e.target.checked)}
                className="rounded bg-slate-950 border-slate-800 text-indigo-500 focus:ring-0"
              />
              <span className="text-xs text-slate-350 font-semibold">Cache Vector Embeddings</span>
            </label>

            <button
              onClick={handleSaveConfig}
              className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-5 py-2 rounded-xl text-xs transition-colors shrink-0 shadow-md"
            >
              {isConfigSaved ? (
                <span className="flex items-center gap-1"><Check className="w-3.5 h-3.5" /> Pushed</span>
              ) : (
                <span>{t('applyAdj')}</span>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
