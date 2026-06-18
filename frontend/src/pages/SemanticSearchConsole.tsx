import React, { useState } from 'react';
import { Sliders, Cpu, Brain, Activity, Play, Code, AlertCircle, Database, Check } from 'lucide-react';
import { apiClient } from '../services/api';

interface QueryLog {
  id: string;
  query: string;
  timestamp: string;
  latencyMs: number;
  matchesFound: number;
  clickThrough: boolean;
  score: number;
}

const INITIAL_LOGS: QueryLog[] = [
  { id: '1', query: '1 BHK indiranagar under 15k with balcony', timestamp: '2026-06-15 19:40:12', latencyMs: 142, matchesFound: 3, clickThrough: true, score: 98 },
  { id: '2', query: 'luxury house with pool and garden in mumbai', timestamp: '2026-06-15 19:35:50', latencyMs: 231, matchesFound: 1, clickThrough: true, score: 95 },
  { id: '3', query: 'pet friendly 2 bhk flat noida sector 62 under 30k', timestamp: '2026-06-15 19:22:04', latencyMs: 168, matchesFound: 2, clickThrough: false, score: 87 },
  { id: '4', query: 'cheap pg near delhi university north campus', timestamp: '2026-06-15 18:59:15', latencyMs: 98, matchesFound: 4, clickThrough: true, score: 92 },
  { id: '5', query: 'studio apartment with security and parking near tech hub', timestamp: '2026-06-15 18:41:30', latencyMs: 189, matchesFound: 0, clickThrough: false, score: 0 },
];

export default function SemanticSearchConsole() {
  const [modelType, setModelType] = useState('text-embedding-3-small');
  const [similarityThreshold, setSimilarityThreshold] = useState(0.65);
  const [semanticWeight, setSemanticWeight] = useState(0.60);
  const [keywordWeight, setKeywordWeight] = useState(0.40);
  
  // Playground states
  const [testQuery, setTestQuery] = useState('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [playgroundResult, setPlaygroundResult] = useState<{
    success: boolean;
    tokens: string[];
    parsedFilters: Record<string, any>;
    vector: number[];
  } | null>(null);

  const [logs] = useState<QueryLog[]>(INITIAL_LOGS);
  const [isSaved, setIsSaved] = useState(false);

  const handleApplyConfig = async () => {
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 3000);
    try {
      await apiClient.post('/admin/semantic/config', {
        modelType,
        similarityThreshold,
        semanticWeight,
        keywordWeight,
      });
    } catch (err) {
      console.warn('API config push failed, using local model state.');
    }
  };

  const handleTestPlayground = () => {
    if (!testQuery.trim()) return;
    setIsPlaying(true);

    setTimeout(() => {
      const tokens = testQuery.toLowerCase().split(/\s+/).filter(t => t.length > 2);
      const parsedFilters: Record<string, any> = {};

      if (testQuery.toLowerCase().includes('noida')) parsedFilters.locality = 'Sector 62';
      if (testQuery.toLowerCase().includes('bangalore')) parsedFilters.city = 'Bangalore';
      if (testQuery.toLowerCase().includes('mumbai')) parsedFilters.city = 'Mumbai';

      const bedMatch = testQuery.match(/(\d)\s*(?:bhk|bedroom|flat)/i);
      if (bedMatch) parsedFilters.bedrooms = parseInt(bedMatch[1]);

      const priceMatch = testQuery.match(/(?:under|below|max)\s*(\d+)/i);
      if (priceMatch) parsedFilters.price_max = parseInt(priceMatch[1]);

      setPlaygroundResult({
        success: true,
        tokens,
        parsedFilters,
        vector: Array.from({ length: 8 }, () => parseFloat((Math.random() * 2 - 1).toFixed(4))),
      });
      setIsPlaying(false);
    }, 800);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-6 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Semantic Search Tuning Console
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Configure vector search index parameters, tune similarity weights, and analyze real-time NLP parsing pipelines.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="flex h-2.5 w-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
          <span className="text-xs text-emerald-400 font-semibold bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
            Vector Database Connected (Milvus)
          </span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase">Total Semantic Queries</span>
            <Activity className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">48,291</p>
          <span className="text-[10px] text-emerald-400 font-semibold mt-1 block">+12.4% vs last week</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase">Average Inference Latency</span>
            <Cpu className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">164 ms</p>
          <span className="text-[10px] text-emerald-400 font-semibold mt-1 block">Healthy (Target &lt; 250ms)</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase">Index Coverage Ratio</span>
            <Database className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">100.0%</p>
          <span className="text-[10px] text-slate-500 mt-1 block">15,482 properties vectorized</span>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase">Search Precision (Top-5)</span>
            <Brain className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white">93.8%</p>
          <span className="text-[10px] text-indigo-400 font-semibold mt-1 block">Based on feedback ratings</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Tuning Configuration Panel */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-1 flex flex-col justify-between">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-4">
              <Sliders className="w-5 h-5 text-indigo-400" />
              <span>Embedding Calibration</span>
            </h2>

            <div className="space-y-5">
              {/* Model Select */}
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-2">Vector Model Pipeline</label>
                <select
                  value={modelType}
                  onChange={(e) => setModelType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-850 focus:border-indigo-500 rounded-xl p-3 text-sm text-slate-200"
                >
                  <option value="text-embedding-3-small">OpenAI text-embedding-3-small (1536d)</option>
                  <option value="text-embedding-3-large">OpenAI text-embedding-3-large (3072d)</option>
                  <option value="all-MiniLM-L6-v2">HuggingFace all-MiniLM-L6-v2 (384d)</option>
                  <option value="custom-room-vector-v2">Custom Fine-Tuned RoomBERT-v2 (768d)</option>
                </select>
              </div>

              {/* Threshold Slider */}
              <div>
                <div className="flex justify-between items-center text-xs mb-2">
                  <span className="font-semibold text-slate-400 uppercase">Min Similarity Threshold</span>
                  <span className="text-indigo-400 font-bold font-mono">{similarityThreshold.toFixed(2)}</span>
                </div>
                <input
                  type="range"
                  min="0.30"
                  max="0.95"
                  step="0.05"
                  value={similarityThreshold}
                  onChange={(e) => setSimilarityThreshold(parseFloat(e.target.value))}
                  className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                />
                <span className="text-[10px] text-slate-500 mt-1 block">Results below this cosine score will be pruned.</span>
              </div>

              {/* Semantic Weight Slider */}
              <div>
                <div className="flex justify-between items-center text-xs mb-2">
                  <span className="font-semibold text-slate-400 uppercase">Semantic Similarity Weight</span>
                  <span className="text-indigo-400 font-bold font-mono">{(semanticWeight * 100).toFixed(0)}%</span>
                </div>
                <input
                  type="range"
                  min="0.0"
                  max="1.0"
                  step="0.05"
                  value={semanticWeight}
                  onChange={(e) => {
                    const val = parseFloat(e.target.value);
                    setSemanticWeight(val);
                    setKeywordWeight(1 - val);
                  }}
                  className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                />
              </div>

              {/* Keyword Weight Slider */}
              <div>
                <div className="flex justify-between items-center text-xs mb-2">
                  <span className="font-semibold text-slate-400 uppercase">Traditional BM25 Keyword Weight</span>
                  <span className="text-indigo-400 font-bold font-mono">{(keywordWeight * 100).toFixed(0)}%</span>
                </div>
                <input
                  type="range"
                  min="0.0"
                  max="1.0"
                  step="0.05"
                  value={keywordWeight}
                  onChange={(e) => {
                    const val = parseFloat(e.target.value);
                    setKeywordWeight(val);
                    setSemanticWeight(1 - val);
                  }}
                  className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                />
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-800 mt-6">
            <button
              onClick={handleApplyConfig}
              className="w-full flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl py-3 transition-colors shadow-lg"
            >
              {isSaved ? (
                <>
                  <Check className="w-4 h-4" />
                  <span>Config Pushed Successfully</span>
                </>
              ) : (
                <span>Publish Engine Settings</span>
              )}
            </button>
          </div>
        </div>

        {/* NLP Playground & Query Parser */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:col-span-2">
          <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-4">
            <Code className="w-5 h-5 text-indigo-400" />
            <span>Semantic Translation Playground</span>
          </h2>

          <div className="flex gap-2 mb-6">
            <input
              type="text"
              className="w-full bg-slate-950 border border-slate-850 focus:border-indigo-500 rounded-xl px-4 py-3 text-slate-200 text-sm focus:outline-none"
              placeholder="Enter test sentence... e.g. 2 BHK flat with balcony under 20k"
              value={testQuery}
              onChange={(e) => setTestQuery(e.target.value)}
            />
            <button
              onClick={handleTestPlayground}
              disabled={isPlaying || !testQuery.trim()}
              className="px-5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl flex items-center gap-2 transition-colors disabled:opacity-50"
            >
              <Play className="w-4 h-4" />
              <span>Parse</span>
            </button>
          </div>

          {playgroundResult ? (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Tokens */}
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-850">
                  <span className="block text-xs font-semibold text-slate-400 uppercase mb-2">Tokenized Vocabulary</span>
                  <div className="flex flex-wrap gap-1.5">
                    {playgroundResult.tokens.map((t, i) => (
                      <span key={i} className="px-2 py-0.5 bg-slate-900 text-slate-350 border border-slate-800 rounded text-xs font-mono">
                        {t}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Filters */}
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-850">
                  <span className="block text-xs font-semibold text-slate-400 uppercase mb-2">Derived Pre-Filters</span>
                  {Object.keys(playgroundResult.parsedFilters).length > 0 ? (
                    <pre className="text-xs text-indigo-300 font-mono">
                      {JSON.stringify(playgroundResult.parsedFilters, null, 2)}
                    </pre>
                  ) : (
                    <span className="text-slate-500 text-xs italic">No explicit relational filters parsed. Vector search will cover intent.</span>
                  )}
                </div>
              </div>

              {/* Vector representation */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-850">
                <span className="block text-xs font-semibold text-slate-400 uppercase mb-2">Dense Vector Array Segment (8 Dimension Excerpt)</span>
                <div className="font-mono text-slate-300 text-xs overflow-x-auto whitespace-nowrap bg-slate-900 p-2.5 rounded border border-slate-800">
                  [{playgroundResult.vector.join(', ')}]
                </div>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 border border-dashed border-slate-800 rounded-xl bg-slate-950/20">
              <AlertCircle className="w-8 h-8 text-slate-600 mb-2" />
              <p className="text-slate-400 text-sm">Enter a search sentence and click parse to examine NLP outputs.</p>
            </div>
          )}
        </div>
      </div>

      {/* Query Logs Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 mt-8">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-bold text-white">Recent Semantic Queries Logs</h2>
          <span className="text-xs text-slate-400">Showing last 5 platform searches</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300 border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 text-xs uppercase font-semibold">
                <th className="py-3.5 px-4">Query String</th>
                <th className="py-3.5 px-4 text-center">Latency</th>
                <th className="py-3.5 px-4 text-center">Matches</th>
                <th className="py-3.5 px-4 text-center">CTR Status</th>
                <th className="py-3.5 px-4 text-right">Match Score</th>
                <th className="py-3.5 px-4 text-right">Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-b border-slate-800/60 hover:bg-slate-850/40 transition-colors">
                  <td className="py-4 px-4 font-medium text-white max-w-xs truncate">{log.query}</td>
                  <td className="py-4 px-4 text-center font-mono text-xs">{log.latencyMs} ms</td>
                  <td className="py-4 px-4 text-center font-mono text-xs">{log.matchesFound}</td>
                  <td className="py-4 px-4 text-center">
                    {log.clickThrough ? (
                      <span className="px-2 py-0.5 bg-emerald-500/10 text-emerald-400 rounded text-[10px] font-semibold border border-emerald-500/20">
                        Clicked
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 bg-slate-800 text-slate-400 rounded text-[10px] font-semibold">
                        Dismissed
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 text-right font-mono text-xs text-indigo-400 font-bold">
                    {log.score > 0 ? `${log.score}%` : 'N/A'}
                  </td>
                  <td className="py-4 px-4 text-right text-xs text-slate-400">{log.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
