import React, { useState } from 'react';
import { 
  CheckCircle2, AlertTriangle, XCircle, ArrowLeft, 
  Clock, ShieldCheck, Mail, Send, Activity 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

interface SystemComponent {
  name: string;
  status: 'OPERATIONAL' | 'DEGRADED' | 'OUTAGE';
  uptime90d: number;
  history: ('UP' | 'DEGRADED' | 'DOWN')[]; // 30 days
}

const mockComponents: SystemComponent[] = [
  { name: 'RoomWallah Public Website', status: 'OPERATIONAL', uptime90d: 99.99, history: Array(30).fill('UP') },
  { name: 'Listing & Search Engine API', status: 'OPERATIONAL', uptime90d: 99.95, history: [...Array(14).fill('UP'), 'DEGRADED', ...Array(15).fill('UP')] },
  { name: 'Escrow Payment Coordinator', status: 'OPERATIONAL', uptime90d: 100.0, history: Array(30).fill('UP') },
  { name: 'WhatsApp & SMS Alert Dispatcher', status: 'OPERATIONAL', uptime90d: 99.82, history: [...Array(24).fill('UP'), 'DOWN', ...Array(5).fill('UP')] },
  { name: 'KYC Document Recognition Engine', status: 'DEGRADED', uptime90d: 98.75, history: [...Array(28).fill('UP'), 'DEGRADED', 'DEGRADED'] },
];

const mockIncidents = [
  { date: '12 June 2026', title: 'KYC Document Recognition Lag', status: 'RESOLVED', message: 'The OCR model container experienced CPU starvation, causing document evaluation times to stretch up to 10 minutes. The instances were scaled horizontally, resolving the delay.' },
  { date: '04 June 2026', title: 'SMS Gateway Dispatch Failure', status: 'RESOLVED', message: 'Our upstream telephony API provider experienced routing outages. Heartbeat checks routed SMS verification tokens to WhatsApp instead. Operational within 25 minutes.' },
];

export default function ServiceStatusPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [subscribed, setSubscribed] = useState(false);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OPERATIONAL':
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-450 uppercase">Operational</span>;
      case 'DEGRADED':
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/20 text-amber-400 uppercase">Degraded Performance</span>;
      default:
        return <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-rose-500/10 border border-rose-500/20 text-rose-450 uppercase">Major Outage</span>;
    }
  };

  const getHistoryDayColor = (dayStatus: string) => {
    switch (dayStatus) {
      case 'UP': return 'bg-emerald-550/60 hover:bg-emerald-450';
      case 'DEGRADED': return 'bg-amber-500/70 hover:bg-amber-400';
      default: return 'bg-rose-550/70 hover:bg-rose-450';
    }
  };

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setSubscribed(true);
    setEmail('');
    setTimeout(() => setSubscribed(false), 4000);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in relative">
      {/* Header banner */}
      <div className="p-6 rounded-3xl glass border border-emerald-550/20 bg-emerald-950/5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-3">
          <CheckCircle2 className="w-8 h-8 text-emerald-450 animate-pulse" />
          <div>
            <h1 className="text-xl font-black text-white">All Systems Operational</h1>
            <p className="text-xs text-slate-400 mt-0.5">RoomWallah systems are functioning normally. 100% uptime over past 24h.</p>
          </div>
        </div>
        <span className="text-[10px] uppercase font-bold tracking-widest px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-400">
          Uptime: 99.92%
        </span>
      </div>

      {/* Components List */}
      <div className="glass rounded-3xl p-6 md:p-8 border border-white/5 space-y-6">
        <h2 className="text-md font-bold text-slate-200 flex items-center gap-2">
          <Activity className="w-5 h-5 text-indigo-400" />
          Component Status & 30-Day History
        </h2>

        <div className="space-y-6">
          {mockComponents.map((comp, i) => (
            <div key={i} className="space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="font-bold text-slate-200">{comp.name}</span>
                <div className="flex items-center gap-3">
                  <span className="text-slate-500 font-semibold">{comp.uptime90d}% Uptime</span>
                  {getStatusBadge(comp.status)}
                </div>
              </div>

              {/* 30 day history blocks */}
              <div className="flex gap-1">
                {comp.history.map((dayStatus, dayIdx) => (
                  <div 
                    key={dayIdx} 
                    className={`h-6 flex-1 rounded ${getHistoryDayColor(dayStatus)} transition-colors`}
                    title={`Day -${30 - dayIdx}: ${dayStatus}`}
                  />
                ))}
              </div>
              <div className="flex justify-between text-[9px] text-slate-500 font-bold uppercase">
                <span>30 Days Ago</span>
                <span>Today</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Subscription box */}
      <div className="glass rounded-3xl p-6 border border-white/5 flex flex-col md:flex-row justify-between items-center gap-4">
        <div className="space-y-1 text-center md:text-left">
          <h3 className="text-sm font-bold text-white">Subscribe to Status Updates</h3>
          <p className="text-xs text-slate-400">Get automatic email/SMS alerts whenever an incident is raised.</p>
        </div>
        
        <form onSubmit={handleSubscribe} className="flex gap-2 w-full md:w-auto shrink-0 max-w-sm">
          <input
            type="email"
            required
            placeholder="sysops@company.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="flex-1 px-3.5 py-2 bg-slate-950 border border-slate-900 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-primary"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-primary hover:opacity-95 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shrink-0"
          >
            <Send className="w-3 h-3" />
            <span>Subscribe</span>
          </button>
        </form>
      </div>

      {/* Historical Incidents */}
      <div className="glass rounded-3xl p-6 md:p-8 border border-white/5 space-y-6">
        <h2 className="text-md font-bold text-slate-200 flex items-center gap-2">
          <Clock className="w-5 h-5 text-indigo-400" />
          Historical Incidents
        </h2>

        <div className="space-y-6 divide-y divide-slate-900/60">
          {mockIncidents.map((inc, i) => (
            <div key={i} className={`pt-4 ${i === 0 ? 'pt-0' : ''} space-y-2`}>
              <div className="flex justify-between items-center text-xs flex-wrap gap-2">
                <span className="font-bold text-slate-200">{inc.title}</span>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] text-slate-500">{inc.date}</span>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 font-bold text-[9px] uppercase">
                    {inc.status}
                  </span>
                </div>
              </div>
              <p className="text-xs text-slate-350 leading-relaxed">{inc.message}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Subscription Success Message */}
      <AnimatePresence>
        {subscribed && (
          <motion.div
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
            className="fixed bottom-6 right-6 p-4 bg-emerald-950 border border-emerald-500/30 text-emerald-300 rounded-2xl flex items-center gap-3 shadow-2xl z-55 text-xs"
          >
            <ShieldCheck className="w-5 h-5 text-emerald-450 shrink-0" />
            <div>
              <span className="font-bold text-white block">Subscribed Successfully</span>
              <span>You will receive outage reports on your email.</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
