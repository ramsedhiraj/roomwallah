import React, { useState } from 'react';
import { 
  Bell, MessageSquare, ShieldAlert, Calendar, 
  DollarSign, Check, Trash2, Search, Filter, AlertTriangle 
} from 'lucide-react';
import { motion } from 'framer-motion';

interface NotificationLog {
  id: string;
  timestamp: string;
  title: string;
  message: string;
  category: 'BOOKING' | 'PAYMENT' | 'SECURITY' | 'TRUST' | 'CHAT';
  channel: 'EMAIL' | 'SMS' | 'WHATSAPP' | 'PUSH';
  status: 'DELIVERED' | 'READ' | 'FAILED';
  recipient: string;
}

const mockHistory: NotificationLog[] = [
  { id: 'lh-1', timestamp: '2026-06-15T18:40:00Z', title: 'Booking Confirmed', message: 'Your property visit slot for #PRP-10492 has been confirmed by the owner.', category: 'BOOKING', channel: 'WHATSAPP', status: 'READ', recipient: '+91 98765 43210' },
  { id: 'lh-2', timestamp: '2026-06-15T16:15:00Z', title: 'Security Alert', message: 'New device session detected for account.', category: 'SECURITY', channel: 'EMAIL', status: 'DELIVERED', recipient: 'tenant@example.com' },
  { id: 'lh-3', timestamp: '2026-06-15T16:02:15Z', title: 'Fraud Alert Raised', message: 'Payment rejected by gateway bank due to velocity card limits.', category: 'PAYMENT', channel: 'SMS', status: 'FAILED', recipient: '+91 98765 43210' },
  { id: 'lh-4', timestamp: '2026-06-14T12:00:00Z', title: 'Escrow Released', message: 'Escrow account payout of ₹45,000 completed.', category: 'PAYMENT', channel: 'EMAIL', status: 'READ', recipient: 'owner@example.com' },
  { id: 'lh-5', timestamp: '2026-06-13T09:30:00Z', title: 'Trust Documents Verified', message: 'Kyc submission for Rajesh Kumar matches Title deed checks.', category: 'TRUST', channel: 'PUSH', status: 'READ', recipient: 'Web Session' },
  { id: 'lh-6', timestamp: '2026-06-11T15:20:00Z', title: 'Booking Request Received', message: 'Tenant Rajesh Kumar requested a visit slot for 2 BHK Flat.', category: 'BOOKING', channel: 'WHATSAPP', status: 'READ', recipient: '+91 88888 77777' },
];

export default function NotificationHistory() {
  const [notifications, setNotifications] = useState<NotificationLog[]>(mockHistory);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | 'BOOKING' | 'PAYMENT' | 'SECURITY' | 'TRUST' | 'CHAT'>('ALL');
  const [channelFilter, setChannelFilter] = useState<'ALL' | 'EMAIL' | 'SMS' | 'WHATSAPP' | 'PUSH'>('ALL');

  const filtered = notifications.filter(n => {
    const matchesSearch = n.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          n.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          n.recipient.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = categoryFilter === 'ALL' || n.category === categoryFilter;
    const matchesChannel = channelFilter === 'ALL' || n.channel === channelFilter;
    return matchesSearch && matchesCategory && matchesChannel;
  });

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'BOOKING': return <Calendar className="w-4 h-4 text-indigo-400" />;
      case 'PAYMENT': return <DollarSign className="w-4 h-4 text-emerald-400" />;
      case 'SECURITY': return <ShieldAlert className="w-4 h-4 text-rose-400" />;
      case 'TRUST': return <Check className="w-4 h-4 text-purple-400" />;
      default: return <MessageSquare className="w-4 h-4 text-sky-400" />;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'READ': return <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 font-bold text-[9px]">Read</span>;
      case 'DELIVERED': return <span className="px-2 py-0.5 rounded-full bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 font-bold text-[9px]">Delivered</span>;
      default: return <span className="px-2 py-0.5 rounded-full bg-rose-500/20 border border-rose-500/30 text-rose-400 font-bold text-[9px] flex items-center gap-0.5"><AlertTriangle className="w-2.5 h-2.5" />Failed</span>;
    }
  };

  const clearNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-6 text-slate-100 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <Bell className="w-8 h-8 text-primary animate-pulse" />
            Notification Dispatch History
          </h1>
          <p className="text-muted-foreground text-sm">
            Auditing communication alerts, messaging status logs, and multi-channel delivery rates.
          </p>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="glass p-4 rounded-xl border border-white/5 text-center">
          <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Total Dispatched</div>
          <p className="text-2xl font-black mt-1 text-white">{notifications.length}</p>
        </div>
        <div className="glass p-4 rounded-xl border border-white/5 text-center">
          <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Delivery Rate</div>
          <p className="text-2xl font-black mt-1 text-emerald-400">
            {notifications.length > 0 
              ? `${Math.round(((notifications.length - notifications.filter(n => n.status === 'FAILED').length) / notifications.length) * 100)}%`
              : '100%'}
          </p>
        </div>
        <div className="glass p-4 rounded-xl border border-white/5 text-center">
          <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">WhatsApp Alerts</div>
          <p className="text-2xl font-black mt-1 text-indigo-400">
            {notifications.filter(n => n.channel === 'WHATSAPP').length}
          </p>
        </div>
        <div className="glass p-4 rounded-xl border border-white/5 text-center">
          <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Failed Audits</div>
          <p className="text-2xl font-black mt-1 text-rose-400">
            {notifications.filter(n => n.status === 'FAILED').length}
          </p>
        </div>
      </div>

      {/* Filters bar */}
      <div className="glass rounded-2xl p-5 border border-white/5 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 w-4 h-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search recipient or content..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-950 border border-slate-900 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-primary"
            />
          </div>

          <div className="flex items-center gap-2 bg-slate-950 border border-slate-900 rounded-xl px-3 py-2 text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value as any)}
              className="w-full bg-transparent border-none text-slate-350 focus:outline-none"
            >
              <option value="ALL">All Categories</option>
              <option value="BOOKING">Booking</option>
              <option value="PAYMENT">Payment</option>
              <option value="SECURITY">Security</option>
              <option value="TRUST">Trust</option>
            </select>
          </div>

          <div className="flex items-center gap-2 bg-slate-950 border border-slate-900 rounded-xl px-3 py-2 text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <select
              value={channelFilter}
              onChange={(e) => setChannelFilter(e.target.value as any)}
              className="w-full bg-transparent border-none text-slate-350 focus:outline-none"
            >
              <option value="ALL">All Channels</option>
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
              <option value="WHATSAPP">WhatsApp</option>
              <option value="PUSH">Web Push</option>
            </select>
          </div>
        </div>
      </div>

      {/* List items */}
      <div className="space-y-4">
        {filtered.map((item, idx) => (
          <motion.div
            key={item.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: idx * 0.05 }}
            className="p-5 rounded-2xl glass border border-white/5 flex flex-col sm:flex-row justify-between items-start gap-4"
          >
            <div className="flex gap-4 items-start flex-1">
              <div className="p-3 bg-slate-900 border border-slate-800 rounded-xl shrink-0 text-slate-400">
                {getCategoryIcon(item.category)}
              </div>
              <div className="space-y-1.5 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-extrabold text-slate-200 text-sm leading-none">{item.title}</h3>
                  <span className="text-[9px] bg-slate-900 border border-slate-850 px-2 py-0.5 rounded font-mono text-slate-400">
                    Channel: {item.channel}
                  </span>
                </div>
                <p className="text-xs text-slate-350 leading-relaxed max-w-2xl">{item.message}</p>
                <div className="text-[10px] text-slate-500 font-semibold flex items-center gap-1.5">
                  <span>Recipient: <span className="font-mono text-slate-400">{item.recipient}</span></span>
                  <span>·</span>
                  <span>{new Date(item.timestamp).toLocaleString('en-IN')}</span>
                </div>
              </div>
            </div>

            <div className="flex sm:flex-col justify-between items-end gap-3 w-full sm:w-auto shrink-0 border-t sm:border-t-0 border-slate-900 pt-3 sm:pt-0">
              {getStatusBadge(item.status)}
              <button
                onClick={() => clearNotification(item.id)}
                className="p-2 border border-slate-850 bg-slate-900/40 hover:bg-rose-950/20 hover:border-rose-900/30 text-slate-400 hover:text-rose-400 rounded-xl transition-all"
                title="Delete from log"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </motion.div>
        ))}
        {filtered.length === 0 && (
          <div className="text-center py-12 glass rounded-2xl text-slate-550 border border-white/5">
            No notification logs match the selected query.
          </div>
        )}
      </div>
    </div>
  );
}
