import React, { useState, useEffect } from 'react';
import { 
  Zap, Database, RefreshCw, RefreshCw as WarmupIcon, 
  Trash2, Terminal, AlertTriangle, ShieldCheck, CheckCircle2 
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface InvalidationLog {
  id: string;
  timestamp: string;
  key: string;
  reason: 'RECORD_MODIFIED' | 'EXPIRED' | 'MANUAL_PURGE';
  layer: 'L1' | 'L2' | 'BOTH';
  deliveryStatus: 'COMPLETED' | 'RETRACTED';
}

const initialLogs: InvalidationLog[] = [
  { id: 'inv-1', timestamp: '18:52:10', key: 'property_listing:PRP-10492', reason: 'RECORD_MODIFIED', layer: 'BOTH', deliveryStatus: 'COMPLETED' },
  { id: 'inv-2', timestamp: '18:51:44', key: 'recommendations:usr-9281', reason: 'MANUAL_PURGE', layer: 'L1', deliveryStatus: 'COMPLETED' },
  { id: 'inv-3', timestamp: '18:50:02', key: 'search_cache_city:BLR:size:20', reason: 'EXPIRED', layer: 'L2', deliveryStatus: 'COMPLETED' },
  { id: 'inv-4', timestamp: '18:48:19', key: 'property_listing:PRP-30291', reason: 'RECORD_MODIFIED', layer: 'BOTH', deliveryStatus: 'COMPLETED' },
];

export default function CacheMonitoringDashboard() {
  const [logs, setLogs] = useState<InvalidationLog[]>(initialLogs);
  const [l1Hits, setL1Hits] = useState(96.2);
  const [l2Hits, setL2Hits] = useState(88.4);
  const [stampedeLocks, setStampedeLocks] = useState(2);
  const [showConfirmPurge, setShowConfirmPurge] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Simulate invalidation events in real-time
  useEffect(() => {
    const timer = setInterval(() => {
      const reasons: Array<'RECORD_MODIFIED' | 'EXPIRED' | 'MANUAL_PURGE'> = ['RECORD_MODIFIED', 'EXPIRED'];
      const layers: Array<'L1' | 'L2' | 'BOTH'> = ['L1', 'L2', 'BOTH'];
      const properties = ['PRP-40182', 'PRP-20291', 'PRP-9921', 'PRP-0182'];
      
      const newLog: InvalidationLog = {
        id: `inv-${Date.now()}`,
        timestamp: new Date().toLocaleTimeString('en-IN'),
        key: `property_listing:${properties[Math.floor(Math.random() * properties.length)]}`,
        reason: reasons[Math.floor(Math.random() * reasons.length)],
        layer: layers[Math.floor(Math.random() * layers.length)],
        deliveryStatus: 'COMPLETED'
      };

      setLogs(prev => [newLog, ...prev.slice(0, 7)]);
      
      // Slightly fluctuate metrics
      setL1Hits(prev => Math.min(99.5, Math.max(92.0, parseFloat((prev + (Math.random() - 0.5) * 0.4).toFixed(1)))));
      setL2Hits(prev => Math.min(95.0, Math.max(84.0, parseFloat((prev + (Math.random() - 0.5) * 0.6).toFixed(1)))));
    }, 5000);

    return () => clearInterval(timer);
  }, []);

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handlePurgeAll = () => {
    setLoading(true);
    setShowConfirmPurge(false);
    setTimeout(() => {
      setL1Hits(0.0);
      setL2Hits(0.0);
      setStampedeLocks(0);
      setLogs([]);
      setLoading(false);
      triggerToast('All caching layers purged. L1/L2 caches reset to cold.');
    }, 1000);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in relative">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Zap className="w-8 h-8 text-primary" />
            L1/L2 Cache Administration
          </h1>
          <p className="text-muted-foreground text-sm">
            Monitor Caffeine (L1 local JVM) and Redis (L2 distributed shared) memory hit ratios, invalidation channels, and stampede locks.
          </p>
        </div>

        <button
          onClick={() => setShowConfirmPurge(true)}
          className="flex items-center gap-1.5 px-4 py-2.5 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-xl shadow-md transition-all active:scale-[0.98]"
        >
          <Trash2 className="w-4 h-4" />
          <span>Purge Caching Layers</span>
        </button>
      </div>

      {/* Visual Gauges */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Caffeine L1 gauge */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between items-center text-center">
          <div className="w-full flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span className="flex items-center gap-1.5"><Database className="w-4 h-4 text-indigo-400" />Caffeine L1 Cache</span>
            <span className="text-indigo-400 font-bold">Local JVM</span>
          </div>
          <div className="relative w-24 h-24 flex items-center justify-center">
            <svg className="w-full h-full transform -rotate-90">
              <circle cx="48" cy="48" r="40" className="stroke-slate-900 fill-none" strokeWidth="8" />
              <circle cx="48" cy="48" r="40" className="stroke-indigo-500 fill-none" strokeWidth="8" strokeDasharray="251.2" strokeDashoffset={251.2 - (251.2 * l1Hits) / 100} />
            </svg>
            <div className="absolute text-center">
              <span className="text-lg font-black text-white">{l1Hits}%</span>
              <span className="text-[8px] text-slate-500 block font-bold uppercase">Hit Ratio</span>
            </div>
          </div>
          <p className="text-[10px] text-slate-500">Keys warm: 842. Memory size: ~8.4 MB. Latency: &lt;1ms.</p>
        </div>

        {/* Redis L2 gauge */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between items-center text-center">
          <div className="w-full flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span className="flex items-center gap-1.5"><Zap className="w-4 h-4 text-pink-400" />Redis L2 Cache</span>
            <span className="text-pink-400 font-bold">Shared Cluster</span>
          </div>
          <div className="relative w-24 h-24 flex items-center justify-center">
            <svg className="w-full h-full transform -rotate-90">
              <circle cx="48" cy="48" r="40" className="stroke-slate-900 fill-none" strokeWidth="8" />
              <circle cx="48" cy="48" r="40" className="stroke-pink-500 fill-none" strokeWidth="8" strokeDasharray="251.2" strokeDashoffset={251.2 - (251.2 * l2Hits) / 100} />
            </svg>
            <div className="absolute text-center">
              <span className="text-lg font-black text-white">{l2Hits}%</span>
              <span className="text-[8px] text-slate-500 block font-bold uppercase">Hit Ratio</span>
            </div>
          </div>
          <p className="text-[10px] text-slate-500">Keys warm: 12,981. Memory size: ~42.9 MB. Latency: 2ms.</p>
        </div>

        {/* Stampede Lock / Warmups */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between text-xs">
          <h3 className="font-bold text-slate-200 uppercase text-[10px] tracking-wider">Stampede & Database Shields</h3>
          
          <div className="space-y-3">
            <div className="flex justify-between items-center p-3 bg-slate-950/40 border border-slate-900 rounded-xl">
              <div>
                <span className="font-bold text-white block">Active Stampede Locks</span>
                <span className="text-[9px] text-slate-500 mt-0.5 block">Mutex keys shielding double DB reads</span>
              </div>
              <span className={`text-base font-black px-2.5 py-0.5 rounded-lg ${stampedeLocks > 0 ? 'bg-amber-500/10 text-amber-400' : 'bg-slate-900 text-slate-400'}`}>
                {stampedeLocks}
              </span>
            </div>

            <div className="flex justify-between items-center p-3 bg-slate-950/40 border border-slate-900 rounded-xl">
              <div>
                <span className="font-bold text-white block">Warm-up Pools</span>
                <span className="text-[9px] text-slate-500 mt-0.5 block">Active async search data caching tasks</span>
              </div>
              <span className="text-xs font-bold text-indigo-400 flex items-center gap-1">
                <WarmupIcon className="w-3.5 h-3.5 animate-spin" />
                <span>2 Cities</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Invalidation Trace logs */}
      <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
        <h2 className="text-base font-bold flex items-center gap-2">
          <Terminal className="w-5 h-5 text-indigo-400" />
          Cache Invalidation Broadcast Stream
        </h2>
        <p className="text-xs text-muted-foreground">Trace messages broadcasted over Redis Pub/Sub channels to clear Caffeine L1 nodes.</p>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-950/60 border-b border-slate-900 text-slate-400 font-bold">
                <th className="p-4">Timestamp</th>
                <th className="p-4">Invalidated Key</th>
                <th className="p-4">Purge Reason</th>
                <th className="p-4">Target Layer</th>
                <th className="p-4 text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-900/60">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-900/30 transition-colors">
                  <td className="p-4 text-slate-400 font-mono">{log.timestamp}</td>
                  <td className="p-4 font-mono font-bold text-slate-200">{log.key}</td>
                  <td className="p-4">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      log.reason === 'RECORD_MODIFIED' ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' : 'bg-slate-900 text-slate-400'
                    }`}>
                      {log.reason.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="p-4 font-extrabold text-pink-400">{log.layer}</td>
                  <td className="p-4 text-center">
                    <span className="text-emerald-450 font-bold flex items-center justify-center gap-1">
                      <div className="w-1.5 h-1.5 bg-emerald-400 rounded-full" />
                      <span>{log.deliveryStatus}</span>
                    </span>
                  </td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-slate-550 font-semibold">
                    No active invalidation logs recorded.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Confirm purge modal */}
      <AnimatePresence>
        {showConfirmPurge && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.6 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowConfirmPurge(false)}
              className="fixed inset-0 bg-slate-950 z-40"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-slate-950 border border-slate-900 p-6 rounded-2xl z-50 text-xs space-y-4"
            >
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-2xl w-fit text-rose-500">
                <AlertTriangle className="w-6 h-6 animate-pulse" />
              </div>
              <div className="space-y-2">
                <h3 className="text-base font-extrabold text-white">Purge All Platform Caches?</h3>
                <p className="text-slate-400 leading-relaxed">
                  This forces immediate cold restarts across Caffeine L1 and Redis L2 memory layers. The subsequent database read operations will experience high load spikes (stampede danger).
                </p>
              </div>
              <div className="flex gap-3 justify-end pt-4 border-t border-slate-900 font-semibold">
                <button
                  onClick={() => setShowConfirmPurge(false)}
                  className="px-4 py-2 border border-slate-800 rounded-xl hover:bg-slate-900 text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  onClick={handlePurgeAll}
                  className="px-4 py-2 bg-rose-655 bg-rose-600 hover:bg-rose-700 text-white rounded-xl shadow-md"
                >
                  Yes, Purge Caches
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
            className="fixed bottom-6 right-6 p-4 bg-indigo-950/90 border border-indigo-500/30 text-indigo-300 rounded-2xl flex items-center gap-3 shadow-2xl z-55 text-xs"
          >
            <CheckCircle2 className="w-5 h-5 text-indigo-400 shrink-0" />
            <div>
              <span className="font-bold text-white block">Purge Executed</span>
              <span>{toastMessage}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
