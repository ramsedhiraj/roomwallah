import React, { useState } from 'react';
import { 
  FileText, Search, Filter, ShieldAlert, AlertTriangle, 
  CheckCircle2, ArrowRight, User, Eye, X, RefreshCw 
} from 'lucide-react';
import UserActivityTimeline, { ActivityItem } from '../components/UserActivityTimeline';
import { motion, AnimatePresence } from 'framer-motion';

// Mock system audit events
interface AuditEvent {
  id: string;
  timestamp: string;
  userId: string;
  userName: string;
  action: string;
  category: 'SECURITY' | 'FINANCE' | 'TRUST' | 'SYSTEM' | 'AUTHENTICATION';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  description: string;
  ipAddress: string;
}

const auditEventsMock: AuditEvent[] = [
  { id: 'ev-101', timestamp: '2026-06-15T18:32:00Z', userId: 'usr-9281', userName: 'Rajesh Kumar', action: 'Verify Document Submitted', category: 'TRUST', severity: 'INFO', description: 'User Rajesh Kumar submitted KYC papers for review.', ipAddress: '103.42.122.9' },
  { id: 'ev-102', timestamp: '2026-06-15T18:24:11Z', userId: 'usr-4412', userName: 'Priyah Sharma', action: 'Escrow Released', category: 'FINANCE', severity: 'INFO', description: 'Admin released escrow funds of ₹45,000 for booking #BKG-9921.', ipAddress: '192.168.1.104' },
  { id: 'ev-103', timestamp: '2026-06-15T17:40:05Z', userId: 'usr-8891', userName: 'System Engine', action: 'Database Cluster Failover', category: 'SYSTEM', severity: 'WARNING', description: 'Database cluster secondary replicated node became primary automatically.', ipAddress: '10.0.4.15' },
  { id: 'ev-104', timestamp: '2026-06-15T17:10:59Z', userId: 'usr-1250', userName: 'John Doe', action: 'API Key Revoked', category: 'SECURITY', severity: 'WARNING', description: 'Revoked production api key with suffix "...a823".', ipAddress: '157.48.22.84' },
  { id: 'ev-105', timestamp: '2026-06-15T16:02:15Z', userId: 'usr-7721', userName: 'Vikram Singh', action: 'Fraud Alert Triggered', category: 'SECURITY', severity: 'CRITICAL', description: 'Risk score 88% triggered due to mismatch billing address country vs login IP.', ipAddress: '203.192.208.5' },
  { id: 'ev-106', timestamp: '2026-06-15T14:48:33Z', userId: 'usr-3029', userName: 'Amit Patel', action: 'Listing Deleted', category: 'TRUST', severity: 'INFO', description: 'Owner Amit Patel deleted listing #PRP-40182.', ipAddress: '115.240.92.14' },
  { id: 'ev-107', timestamp: '2026-06-15T13:21:40Z', userId: 'usr-7721', userName: 'Vikram Singh', action: 'Multiple Login Failures', category: 'AUTHENTICATION', severity: 'CRITICAL', description: '5 login attempts failed from IP 203.192.208.5 within 2 minutes.', ipAddress: '203.192.208.5' },
];

export default function AuditLogsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | 'SECURITY' | 'FINANCE' | 'TRUST' | 'SYSTEM' | 'AUTHENTICATION'>('ALL');
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'INFO' | 'WARNING' | 'CRITICAL'>('ALL');
  
  // Side drawer state
  const [selectedUser, setSelectedUser] = useState<{ id: string; name: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [showLedgerReport, setShowLedgerReport] = useState(false);

  const filteredLogs = auditEventsMock.filter(log => {
    const matchesSearch = log.userName.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          log.action.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          log.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          log.userId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = categoryFilter === 'ALL' || log.category === categoryFilter;
    const matchesSeverity = severityFilter === 'ALL' || log.severity === severityFilter;
    return matchesSearch && matchesCategory && matchesSeverity;
  });

  const getSeverityBadge = (sev: string) => {
    switch (sev) {
      case 'CRITICAL':
        return <span className="px-2 py-0.5 rounded-full bg-rose-500/20 border border-rose-500/30 text-rose-400 font-bold text-[10px]">CRITICAL</span>;
      case 'WARNING':
        return <span className="px-2 py-0.5 rounded-full bg-amber-500/20 border border-amber-500/30 text-amber-400 font-bold text-[10px]">WARNING</span>;
      default:
        return <span className="px-2 py-0.5 rounded-full bg-slate-900 border border-slate-800 text-slate-400 font-bold text-[10px]">INFO</span>;
    }
  };

  const getCategoryColor = (cat: string) => {
    switch (cat) {
      case 'SECURITY': return 'text-rose-400';
      case 'FINANCE': return 'text-emerald-400';
      case 'TRUST': return 'text-indigo-400';
      case 'SYSTEM': return 'text-sky-400';
      default: return 'text-purple-400';
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6 animate-fade-in text-slate-100 relative">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight flex items-center gap-3">
            <FileText className="w-8 h-8 text-primary" />
            System Audit Log Console
          </h1>
          <p className="text-muted-foreground text-sm">
            Auditing operations, database state checks, security alarms, and admin actions.
          </p>
        </div>
      </div>

      {/* Audit Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="glass rounded-2xl p-5 border border-white/5 flex items-center gap-4">
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl text-primary">
            <FileText className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs text-slate-400 font-bold uppercase tracking-wider">Total Audits</div>
            <div className="text-2xl font-black mt-1">12,842</div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/5 flex items-center gap-4">
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs text-slate-400 font-bold uppercase tracking-wider">Security Alerts (24h)</div>
            <div className="text-2xl font-black mt-1">3</div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/5 flex items-center gap-4">
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl text-amber-400">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs text-slate-400 font-bold uppercase tracking-wider">Warnings Raised</div>
            <div className="text-2xl font-black mt-1">14</div>
        </div>
      </div>
    </div>

      {/* Ledger Integrity verifier status */}
      <div className="glass rounded-2xl p-4 border border-emerald-550/20 bg-emerald-950/5 text-xs space-y-4">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <div className="flex items-center gap-2.5">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-ping shrink-0" />
            <div>
              <span className="font-extrabold text-white">Cryptographic Ledger integrity: Verified</span>
              <span className="text-slate-400 block mt-0.5 font-mono">SHA-256 Ledger chain root: 0x8df5a8e1cb7b4b1a4032d8495a4bb1006e8b28f8952cc914f6b219e4811a2bc7</span>
            </div>
          </div>
          <div className="flex items-center gap-3 shrink-0 self-stretch sm:self-auto justify-between sm:justify-start">
            <button
              onClick={() => setShowLedgerReport(!showLedgerReport)}
              className="text-xs text-primary font-bold hover:underline"
            >
              {showLedgerReport ? 'Hide Report' : 'View Full Report'}
            </button>
            <span className="px-2.5 py-1 bg-emerald-550/10 border border-emerald-500/30 text-emerald-400 rounded-xl font-bold uppercase text-[9px] tracking-wider shrink-0">
              Chain Status: Green
            </span>
          </div>
        </div>

        {/* Expandable validation report */}
        <AnimatePresence>
          {showLedgerReport && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-900 pt-4 mt-2 space-y-4 overflow-hidden"
            >
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-[10px] text-slate-500">
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Chain Version</span>
                  <span className="font-semibold text-slate-200 block mt-0.5">v2.4 (SHA-256 Ledger)</span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Last Verified</span>
                  <span className="font-semibold text-slate-200 block mt-0.5">2026-06-15 18:50:32</span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Log Blocks Audited</span>
                  <span className="font-semibold text-slate-200 block mt-0.5">14,892 blocks</span>
                </div>
                <div>
                  <span className="font-bold text-slate-400 block uppercase">Tamper Detections</span>
                  <span className="font-semibold text-emerald-400 block mt-0.5">0 incidents detected</span>
                </div>
              </div>

              {/* Detail alerts check */}
              <div className="space-y-2">
                <span className="font-bold text-slate-400 block uppercase text-[10px]">Validation Checks</span>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                    <div className="text-[10px] font-bold text-emerald-450 uppercase">Log Reordering Check</div>
                    <p className="text-[10px] text-slate-450 leading-relaxed">No logs reordering or sequential timestamp offsets detected in block chain.</p>
                  </div>
                  <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                    <div className="text-[10px] font-bold text-emerald-450 uppercase">Deleted Records Check</div>
                    <p className="text-[10px] text-slate-450 leading-relaxed">0 deleted entries. Sequential ID index chain verified without block drops.</p>
                  </div>
                  <div className="p-3 bg-slate-950/60 border border-slate-900 rounded-xl space-y-1">
                    <div className="text-[10px] font-bold text-emerald-450 uppercase">Unauthorized Modifications</div>
                    <p className="text-[10px] text-slate-450 leading-relaxed">0 signature mismatch events. All historical hashes conform to verified payloads.</p>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Advanced Filters */}
      <div className="glass rounded-2xl p-6 border border-white/5 space-y-4">
        <h2 className="text-sm font-bold text-slate-200">Refine Audit Query</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="relative">
            <Search className="absolute left-3.5 top-3 w-4 h-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search user, action, or log message..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-900 rounded-xl text-sm text-slate-200 focus:outline-none focus:border-primary transition-all"
            />
          </div>

          <div className="flex items-center gap-2 bg-slate-950 border border-slate-900 rounded-xl px-3 py-2">
            <span className="text-xs text-slate-500 font-bold shrink-0">Category:</span>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value as any)}
              className="w-full bg-transparent border-none text-xs text-slate-350 focus:outline-none"
            >
              <option value="ALL">All Categories</option>
              <option value="SECURITY">Security</option>
              <option value="FINANCE">Finance</option>
              <option value="TRUST">Trust</option>
              <option value="SYSTEM">System</option>
              <option value="AUTHENTICATION">Authentication</option>
            </select>
          </div>

          <div className="flex items-center gap-2 bg-slate-950 border border-slate-900 rounded-xl px-3 py-2">
            <span className="text-xs text-slate-500 font-bold shrink-0">Severity:</span>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value as any)}
              className="w-full bg-transparent border-none text-xs text-slate-350 focus:outline-none"
            >
              <option value="ALL">All Severities</option>
              <option value="INFO">Info</option>
              <option value="WARNING">Warning</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="glass rounded-2xl border border-white/5 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-950/60 border-b border-slate-900 text-slate-400 font-bold">
                <th className="p-4">Timestamp</th>
                <th className="p-4">Category</th>
                <th className="p-4">User</th>
                <th className="p-4">Action</th>
                <th className="p-4">Severity</th>
                <th className="p-4">IP Address</th>
                <th className="p-4 text-center">Inspect</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-900/60">
              {filteredLogs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-900/30 transition-colors">
                  <td className="p-4 text-slate-450 whitespace-nowrap">
                    {new Date(log.timestamp).toLocaleString('en-IN', {
                      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit'
                    })}
                  </td>
                  <td className={`p-4 font-bold ${getCategoryColor(log.category)}`}>
                    {log.category}
                  </td>
                  <td className="p-4">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-slate-950 border border-slate-800 flex items-center justify-center text-[10px] text-primary">
                        <User className="w-3.5 h-3.5" />
                      </div>
                      <div>
                        <span className="font-semibold text-slate-200">{log.userName}</span>
                        <div className="text-[10px] text-slate-500 font-mono">{log.userId}</div>
                      </div>
                    </div>
                  </td>
                  <td className="p-4">
                    <div>
                      <span className="font-bold text-slate-200">{log.action}</span>
                      <p className="text-[11px] text-slate-450 mt-0.5 line-clamp-1">{log.description}</p>
                    </div>
                  </td>
                  <td className="p-4">{getSeverityBadge(log.severity)}</td>
                  <td className="p-4 text-slate-400 font-mono">{log.ipAddress}</td>
                  <td className="p-4 text-center">
                    <button
                      onClick={() => setSelectedUser({ id: log.userId, name: log.userName })}
                      className="p-1.5 rounded-lg border border-slate-800 bg-slate-900 hover:border-slate-700 hover:text-white transition-all inline-flex items-center justify-center text-slate-400"
                      title="Inspect User Timeline"
                    >
                      <Eye className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
              {filteredLogs.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-slate-500 font-semibold">
                    No matching audit entries found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Slid-in Drawer for User Timeline */}
      <AnimatePresence>
        {selectedUser && (
          <>
            {/* Backdrop overlay */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.5 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelectedUser(null)}
              className="fixed inset-0 bg-slate-950 z-40"
            />
            {/* Slide drawer */}
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="fixed top-0 right-0 h-full w-full sm:w-[450px] bg-slate-950 border-l border-slate-900 shadow-2xl p-6 overflow-y-auto z-50 text-slate-100"
            >
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-lg font-bold">User Inspector</h2>
                <button
                  onClick={() => setSelectedUser(null)}
                  className="p-1.5 bg-slate-900 border border-slate-850 hover:border-slate-700 text-slate-400 hover:text-white rounded-xl transition-all"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <UserActivityTimeline userId={selectedUser.id} userName={selectedUser.name} />
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
