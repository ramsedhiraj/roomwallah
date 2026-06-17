import React, { useState, useEffect } from 'react';
import { 
  AreaChart, Area, LineChart, Line, XAxis, YAxis, 
  CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  Cpu, HardDrive, Database, Zap, RefreshCw, 
  CheckCircle2, AlertTriangle, XCircle, Clock 
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

// Mock actuator stats
const performanceMetrics = [
  { time: '18:00', apiLatency: 120, dbLatency: 18, redisLatency: 2 },
  { time: '18:10', apiLatency: 114, dbLatency: 22, redisLatency: 3 },
  { time: '18:20', apiLatency: 145, dbLatency: 35, redisLatency: 2 },
  { time: '18:30', apiLatency: 130, dbLatency: 15, redisLatency: 1 },
  { time: '18:40', apiLatency: 110, dbLatency: 12, redisLatency: 2 },
  { time: '18:50', apiLatency: 115, dbLatency: 14, redisLatency: 2 },
];

interface LogTrace {
  id: string;
  timestamp: string;
  service: string;
  level: 'INFO' | 'WARN' | 'ERROR';
  message: string;
}

const initialLogTraces: LogTrace[] = [
  { id: 't-1', timestamp: '18:52:10', service: 'AUTH-SVC', level: 'INFO', message: 'JWT token validated for session usr_882918' },
  { id: 't-2', timestamp: '18:52:05', service: 'PAY-SVC', level: 'INFO', message: 'Stripe Webhook processed successfully. Event: payment_intent.succeeded' },
  { id: 't-3', timestamp: '18:51:50', service: 'DB-POOL', level: 'WARN', message: 'HikariCP active connections reached 82% threshold' },
  { id: 't-4', timestamp: '18:51:30', service: 'SEARCH-IDX', level: 'INFO', message: 'Indexed property #PRP-10492 in 45ms' },
  { id: 't-5', timestamp: '18:51:11', service: 'SYS-MON', level: 'INFO', message: 'JVM Garbage Collection completed. GC pause: 12ms. Recovered 480MB.' },
];

export default function SystemHealthDashboard() {
  const [logs, setLogs] = useState<LogTrace[]>(initialLogTraces);
  const [loading, setLoading] = useState(false);
  const [healthStatus, setHealthStatus] = useState<'HEALTHY' | 'DEGRADED' | 'CRITICAL'>('HEALTHY');

  // Simulate live logging trace
  useEffect(() => {
    const timer = setInterval(() => {
      const services = ['AUTH-SVC', 'PAY-SVC', 'DB-POOL', 'SEARCH-IDX', 'SYS-MON', 'AGR-AGNT'];
      const levels: Array<'INFO' | 'WARN' | 'ERROR'> = ['INFO', 'INFO', 'INFO', 'WARN', 'INFO'];
      const messages = [
        'Checked out database connection from Hikari pool',
        'Redis cache hit ratio evaluated at 96.4%',
        'SSE channel heartbeat broadcasted to active tenants',
        'Moderator audit file integrity check completed successfully',
        'Escrow payout released trigger completed in 182ms',
        'JVM memory utilization garbage collector check: OK'
      ];
      
      const newLog: LogTrace = {
        id: `t-${Date.now()}`,
        timestamp: new Date().toLocaleTimeString('en-IN'),
        service: services[Math.floor(Math.random() * services.length)],
        level: levels[Math.floor(Math.random() * levels.length)],
        message: messages[Math.floor(Math.random() * messages.length)]
      };

      setLogs(prev => [newLog, ...prev.slice(0, 8)]);
    }, 4000);

    return () => clearInterval(timer);
  }, []);

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setHealthStatus('HEALTHY');
    }, 800);
  };

  const getServiceStatusIcon = (status: 'UP' | 'DEGRADED' | 'DOWN') => {
    switch (status) {
      case 'UP':
        return <CheckCircle2 className="w-5 h-5 text-emerald-450 shrink-0" />;
      case 'DEGRADED':
        return <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />;
      default:
        return <XCircle className="w-5 h-5 text-rose-500 shrink-0" />;
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Zap className="w-8 h-8 text-primary animate-pulse" />
            System Health Monitoring
          </h1>
          <p className="text-muted-foreground text-sm">
            Live actuator telemetry, JVM memory allocations, connection pools, and latencies.
          </p>
        </div>
        
        <button
          onClick={handleRefresh}
          className="flex items-center gap-1.5 px-4 py-2 border border-slate-800 rounded-xl bg-slate-900/50 hover:bg-slate-900 transition-all text-xs font-semibold text-slate-400 hover:text-white"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-primary' : ''}`} />
          <span>Sync Actuator telemetry</span>
        </button>
      </div>

      {/* Overview Status Banner */}
      <div className="glass rounded-3xl p-5 border border-white/5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-2xl">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-slate-450 uppercase font-bold tracking-widest block">System cluster status</span>
            <h2 className="text-lg font-black text-white">All Platform Services Operational</h2>
          </div>
        </div>
        <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/15 border border-emerald-500/30 text-emerald-450 rounded-xl text-xs font-extrabold">
          <span>Cluster Health: UP (100%)</span>
        </div>
      </div>

      {/* Actuator Telemetry Gauges */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* JVM Memory Gauge */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span className="flex items-center gap-1.5"><Cpu className="w-4 h-4 text-primary" />JVM Heap Memory</span>
            <span className="text-slate-205">26% allocated</span>
          </div>
          <div className="py-2 flex justify-center items-center relative h-32">
            <svg className="w-24 h-24 transform -rotate-90">
              <circle cx="48" cy="48" r="40" className="stroke-slate-900 fill-none" strokeWidth="8" />
              <circle cx="48" cy="48" r="40" className="stroke-primary fill-none" strokeWidth="8" strokeDasharray="251.2" strokeDashoffset={251.2 - (251.2 * 26) / 100} />
            </svg>
            <div className="absolute text-center">
              <span className="text-base font-black text-white">528 MB</span>
              <span className="text-[9px] text-slate-500 block font-bold">Of 2048 MB</span>
            </div>
          </div>
          <p className="text-[10px] text-slate-500 text-center">GC pause averages 14ms. Thread allocation: 42 active.</p>
        </div>

        {/* Database Connection Pool */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span className="flex items-center gap-1.5"><Database className="w-4 h-4 text-indigo-400" />HikariCP Connection Pool</span>
            <span className="text-indigo-400 font-bold">16% active</span>
          </div>
          <div className="py-2 flex justify-center items-center relative h-32">
            <svg className="w-24 h-24 transform -rotate-90">
              <circle cx="48" cy="48" r="40" className="stroke-slate-900 fill-none" strokeWidth="8" />
              <circle cx="48" cy="48" r="40" className="stroke-indigo-500 fill-none" strokeWidth="8" strokeDasharray="251.2" strokeDashoffset={251.2 - (251.2 * 16) / 100} />
            </svg>
            <div className="absolute text-center">
              <span className="text-base font-black text-white">8 Active</span>
              <span className="text-[9px] text-slate-500 block font-bold">Of 50 Max</span>
            </div>
          </div>
          <p className="text-[10px] text-slate-500 text-center">Idle connections: 42. Queue wait timeout: 0ms.</p>
        </div>

        {/* Redis Cache Hit Ratio */}
        <div className="glass rounded-3xl p-5 border border-white/5 space-y-4 flex flex-col justify-between">
          <div className="flex justify-between items-center text-xs font-bold text-slate-400 uppercase tracking-wider">
            <span className="flex items-center gap-1.5"><Zap className="w-4 h-4 text-pink-400" />Redis Cache Hit Ratio</span>
            <span className="text-pink-400 font-bold">94.6% Hits</span>
          </div>
          <div className="py-2 flex justify-center items-center relative h-32">
            <svg className="w-24 h-24 transform -rotate-90">
              <circle cx="48" cy="48" r="40" className="stroke-slate-900 fill-none" strokeWidth="8" />
              <circle cx="48" cy="48" r="40" className="stroke-pink-500 fill-none" strokeWidth="8" strokeDasharray="251.2" strokeDashoffset={251.2 - (251.2 * 94.6) / 100} />
            </svg>
            <div className="absolute text-center">
              <span className="text-base font-black text-white">94.6%</span>
              <span className="text-[9px] text-slate-500 block font-bold">Hit Efficiency</span>
            </div>
          </div>
          <p className="text-[10px] text-slate-500 text-center">Active keys: 1,842. Memory consumption: 12.4 MB.</p>
        </div>
      </div>

      {/* Latency graphs */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Core Latency Chart */}
        <div className="lg:col-span-2 glass rounded-3xl p-6 border border-white/5 space-y-4">
          <div>
            <h2 className="text-base font-bold">Actuator Response Latency</h2>
            <p className="text-xs text-muted-foreground">Response time stats (ms) monitored across API, Database, and Cache</p>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={performanceMetrics} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" opacity={0.3} />
                <XAxis dataKey="time" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px' }} />
                <Legend />
                <Area name="API Gateway Latency" type="monotone" dataKey="apiLatency" stroke="#6366f1" fill="#6366f1" fillOpacity={0.15} />
                <Area name="Database Latency" type="monotone" dataKey="dbLatency" stroke="#a855f7" fill="#a855f7" fillOpacity={0.05} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Services status checklist */}
        <div className="glass rounded-3xl p-6 border border-white/5 space-y-4 flex flex-col justify-between">
          <h2 className="text-base font-bold">Microservice Health status</h2>
          
          <div className="space-y-3 flex-1 mt-2">
            {[
              { name: 'Gateway Routing Proxy', status: 'UP', desc: 'Active routes mapping' },
              { name: 'User Authentication Svc', status: 'UP', desc: 'Actuator session validation' },
              { name: 'Payment Escrow Engine', status: 'UP', desc: 'Secure bank API connection' },
              { name: 'ElasticSearch Broker Node', status: 'DEGRADED', desc: 'High indexing memory usage' },
              { name: 'Notification Dispatch Hub', status: 'UP', desc: 'WhatsApp & SMS channels healthy' },
            ].map((svc, i) => (
              <div key={i} className="flex justify-between items-center p-3 bg-slate-950/40 border border-slate-900 rounded-xl">
                <div>
                  <div className="text-xs font-bold text-slate-200">{svc.name}</div>
                  <span className="text-[10px] text-slate-500 mt-0.5 block">{svc.desc}</span>
                </div>
                {getServiceStatusIcon(svc.status as any)}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Live trace logger */}
      <div className="glass rounded-3xl p-6 border border-white/5 space-y-4">
        <div className="flex justify-between items-center">
          <h2 className="text-base font-bold">Live System Trace Console</h2>
          <div className="flex items-center gap-1.5 text-[10px] text-slate-550 font-bold uppercase tracking-wider">
            <Clock className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <span>Polling actuator logs...</span>
          </div>
        </div>

        <div className="p-5 bg-slate-950/80 border border-slate-900 rounded-xl font-mono text-[10px] space-y-2.5 max-h-56 overflow-y-auto">
          {logs.map((log) => (
            <div key={log.id} className="flex items-start gap-2.5 leading-relaxed">
              <span className="text-indigo-400 shrink-0">[{log.timestamp}]</span>
              <span className={`px-1.5 py-0.5 rounded font-bold text-[8px] shrink-0 ${
                log.level === 'ERROR' ? 'bg-rose-500/20 text-rose-400' : log.level === 'WARN' ? 'bg-amber-500/20 text-amber-300' : 'bg-slate-900 text-slate-400'
              }`}>
                {log.level}
              </span>
              <span className="text-sky-350 shrink-0 font-bold">[{log.service}]</span>
              <span className="text-slate-300 break-all">{log.message}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
