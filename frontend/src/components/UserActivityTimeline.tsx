import React from 'react';
import { 
  LogIn, ShieldAlert, Edit, Eye, 
  MapPin, Clock, ShieldCheck 
} from 'lucide-react';
import { motion } from 'framer-motion';

export interface ActivityItem {
  id: string;
  timestamp: string;
  action: string;
  description: string;
  category: 'SECURITY' | 'AUTHENTICATION' | 'MANAGEMENT' | 'TRUST' | 'MODERATION';
  ipAddress: string;
  location: string;
  device: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
}

interface Props {
  userId: string;
  userName: string;
  activities?: ActivityItem[];
}

const defaultActivities: ActivityItem[] = [
  {
    id: 'act-1',
    timestamp: '2026-06-15T18:32:00Z',
    action: 'Verify Request Submitted',
    description: 'Submitted Aadhaar card and title deeds for property #PRP-10492.',
    category: 'TRUST',
    ipAddress: '103.42.122.9',
    location: 'Mumbai, India',
    device: 'Chrome / Windows',
    severity: 'INFO',
  },
  {
    id: 'act-2',
    timestamp: '2026-06-15T16:15:22Z',
    action: 'Login Successful',
    description: 'Authenticated via multi-factor auth.',
    category: 'AUTHENTICATION',
    ipAddress: '103.42.122.9',
    location: 'Mumbai, India',
    device: 'Chrome / Windows',
    severity: 'INFO',
  },
  {
    id: 'act-3',
    timestamp: '2026-06-14T22:45:10Z',
    action: 'Password Rotate Triggered',
    description: 'Security password rotation completed successfully.',
    category: 'SECURITY',
    ipAddress: '103.42.122.9',
    location: 'Mumbai, India',
    device: 'Safari / iPhone 15',
    severity: 'WARNING',
  },
  {
    id: 'act-4',
    timestamp: '2026-06-12T11:04:30Z',
    action: 'Listing Visibility: PRIVATE',
    description: 'Changed visibility of property #PRP-10492 to private.',
    category: 'MANAGEMENT',
    ipAddress: '49.207.240.11',
    location: 'Bengaluru, India',
    device: 'Firefox / Mac OS',
    severity: 'INFO',
  },
  {
    id: 'act-5',
    timestamp: '2026-06-10T09:12:15Z',
    action: 'Suspicious IP Attempt Blocked',
    description: 'Login attempt from unrecognized IP address blocked by risk scanner.',
    category: 'SECURITY',
    ipAddress: '198.51.100.42',
    location: 'Frankfurt, Germany',
    device: 'Curl / Linux',
    severity: 'CRITICAL',
  },
];

export default function UserActivityTimeline({ userId, userName, activities = defaultActivities }: Props) {
  
  const getIcon = (category: string) => {
    switch (category) {
      case 'SECURITY':
        return <ShieldAlert className="w-4 h-4 text-rose-400" />;
      case 'AUTHENTICATION':
        return <LogIn className="w-4 h-4 text-indigo-400" />;
      case 'TRUST':
        return <ShieldCheck className="w-4 h-4 text-emerald-400" />;
      case 'MANAGEMENT':
        return <Edit className="w-4 h-4 text-purple-400" />;
      case 'MODERATION':
        return <Eye className="w-4 h-4 text-amber-400" />;
      default:
        return <Clock className="w-4 h-4 text-slate-400" />;
    }
  };

  const getSeverityClass = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-500/20 border-rose-500/40 text-rose-300';
      case 'WARNING':
        return 'bg-amber-500/20 border-amber-500/40 text-amber-300';
      default:
        return 'bg-slate-900 border-slate-800 text-slate-400';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between border-b border-slate-900 pb-4">
        <div>
          <h3 className="text-md font-bold text-slate-200">Activity Timeline</h3>
          <p className="text-xs text-muted-foreground">User: {userName} (ID: {userId.substring(0, 8)})</p>
        </div>
        <span className="text-[10px] uppercase font-bold tracking-wider px-2.5 py-1 rounded bg-slate-900 border border-slate-800 text-slate-400">
          Audited Session
        </span>
      </div>

      <div className="relative border-l border-slate-800 ml-3.5 space-y-6">
        {activities.map((act, idx) => (
          <motion.div
            key={act.id}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: idx * 0.05 }}
            className="relative pl-6"
          >
            {/* Timeline dot */}
            <div className="absolute left-[-17px] top-1 p-1 bg-slate-950 border border-slate-800 rounded-full flex items-center justify-center">
              {getIcon(act.category)}
            </div>

            <div className="glass rounded-xl p-4 border border-white/5 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-xs font-bold text-white">{act.action}</span>
                  <span className={`text-[9px] font-bold px-2 py-0.5 rounded border ${getSeverityClass(act.severity)}`}>
                    {act.severity}
                  </span>
                </div>
                <span className="text-[10px] text-slate-500 flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {new Date(act.timestamp).toLocaleString('en-IN', {
                    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
                  })}
                </span>
              </div>

              <p className="text-xs text-slate-350 leading-relaxed">{act.description}</p>

              <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[10px] text-slate-500 border-t border-slate-900 pt-2">
                <div className="flex items-center gap-1">
                  <MapPin className="w-3 h-3 text-indigo-400/60" />
                  <span>IP: {act.ipAddress} ({act.location})</span>
                </div>
                <div className="flex items-center gap-1">
                  <Clock className="w-3 h-3 text-slate-600" />
                  <span>Client: {act.device}</span>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
