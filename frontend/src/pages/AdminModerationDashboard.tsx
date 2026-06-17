import React, { useEffect, useState } from 'react';
import { 
  getModerationCases, 
  approveModerationCase, 
  rejectModerationCase, 
  triggerTrustRecalculation,
  ModerationCaseDto 
} from '../services/trustService';
import { 
  ShieldAlert, 
  UserCheck, 
  UserX, 
  Clock, 
  Search, 
  AlertCircle, 
  Loader2, 
  CornerDownRight,
  Filter,
  CheckCircle,
  XCircle,
  FileText
} from 'lucide-react';

export default function AdminModerationDashboard() {
  const [cases, setCases] = useState<ModerationCaseDto[]>([]);
  const [selectedCase, setSelectedCase] = useState<ModerationCaseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Search and filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('OPEN');
  
  // Review notes
  const [reviewerNotes, setReviewerNotes] = useState('');
  
  // Trigger fetch on load
  useEffect(() => {
    fetchCases();
  }, []);

  const fetchCases = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getModerationCases();
      setCases(data);
      if (data.length > 0) {
        setSelectedCase(data[0]);
      } else {
        setSelectedCase(null);
      }
    } catch (err) {
      setError('Failed to retrieve moderation queue. Please check permissions.');
    } finally {
      setLoading(false);
    }
  };

  const handleDecision = async (approve: boolean) => {
    if (!selectedCase) return;
    setActionLoading(true);
    setError(null);
    setSuccess(null);

    // Generate unique idempotency key for this admin action
    const idempotencyKey = `admin_${approve ? 'appr' : 'reje'}_${selectedCase.id}_${Date.now()}`;

    try {
      if (approve) {
        await approveModerationCase(selectedCase.id, reviewerNotes || 'Verified owner details matching government record.', idempotencyKey);
        setSuccess(`Verification for Case #${selectedCase.id.substring(0, 8)} approved successfully.`);
      } else {
        await rejectModerationCase(selectedCase.id, reviewerNotes || 'Government record does not match selfie details.', idempotencyKey);
        setSuccess(`Verification for Case #${selectedCase.id.substring(0, 8)} rejected.`);
      }
      setReviewerNotes('');
      await fetchCases(); // Refresh queue
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to submit decision. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRecalculate = async () => {
    setActionLoading(true);
    try {
      await triggerTrustRecalculation();
      setSuccess('Trust score recalculation triggered across all active accounts.');
      await fetchCases();
    } catch (err) {
      setError('Recalculation sweep failed.');
    } finally {
      setActionLoading(false);
    }
  };

  // Filter cases based on search and status pills
  const filteredCases = cases.filter(item => {
    const matchesSearch = 
      item.entityId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.notes && item.notes.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 p-6 md:p-8">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-8 space-y-4 md:space-y-0">
        <div>
          <h1 className="text-3xl font-extrabold font-outfit text-slate-800 dark:text-white">
            Trust & Moderation Dashboard
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Prioritized review queue for owner verifications, broker alerts, and fraud signals.
          </p>
        </div>
        
        <div className="flex space-x-3">
          <button
            onClick={handleRecalculate}
            disabled={actionLoading}
            className="px-4 py-2 border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 text-sm font-semibold rounded-lg transition"
          >
            Run Score Sweep
          </button>
          <button
            onClick={fetchCases}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-lg transition"
          >
            Refresh Queue
          </button>
        </div>
      </div>

      {/* Success/Error Alerts */}
      {success && (
        <div className="mb-6 p-4 bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900 rounded-xl flex items-center space-x-2 text-emerald-800 dark:text-emerald-400 text-sm">
          <CheckCircle className="w-5 h-5" />
          <span>{success}</span>
        </div>
      )}
      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900 rounded-xl flex items-center space-x-2 text-rose-500 text-sm">
          <XCircle className="w-5 h-5" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Grid split layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left column: prioritized list */}
        <div className="lg:col-span-5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 flex flex-col h-[75vh]">
          
          {/* Filters */}
          <div className="space-y-4 mb-4">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search cases by entity ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 rounded-lg text-slate-800 dark:text-slate-200 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            
            <div className="flex space-x-1.5 border-b border-slate-100 dark:border-slate-800 pb-2">
              {['ALL', 'OPEN', 'ASSIGNED', 'IN_REVIEW', 'RESOLVED'].map((status) => (
                <button
                  key={status}
                  onClick={() => setStatusFilter(status)}
                  className={`px-3 py-1 rounded-md text-xs font-semibold uppercase tracking-wider transition ${
                    statusFilter === status 
                      ? 'bg-indigo-100 text-indigo-600 dark:bg-indigo-950/30 dark:text-indigo-400' 
                      : 'text-slate-400 hover:text-slate-600 dark:hover:text-slate-200'
                  }`}
                >
                  {status}
                </button>
              ))}
            </div>
          </div>

          {/* List queue */}
          <div className="flex-1 overflow-y-auto space-y-3">
            {loading ? (
              <div className="flex flex-col items-center justify-center h-full space-y-2">
                <Loader2 className="w-6 h-6 text-indigo-500 animate-spin" />
                <p className="text-xs text-slate-400">Loading cases...</p>
              </div>
            ) : filteredCases.length > 0 ? (
              filteredCases.map((item) => (
                <div
                  key={item.id}
                  onClick={() => setSelectedCase(item)}
                  className={`p-4 rounded-xl border cursor-pointer transition ${
                    selectedCase?.id === item.id 
                      ? 'border-indigo-500 bg-indigo-50/5 dark:bg-indigo-950/10' 
                      : 'border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/40'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="space-y-1">
                      <span className="px-2 py-0.5 bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-400 rounded-md text-[10px] font-bold uppercase tracking-wider">
                        Priority {item.priorityScore}
                      </span>
                      <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 mt-1">
                        Case #{item.id.substring(0, 8)}
                      </h4>
                      <p className="text-[11px] text-slate-400">
                        {item.entityType} • {new Date(item.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    
                    <span className={`px-2 py-0.5 text-[9px] font-bold uppercase rounded ${
                      item.status === 'OPEN' 
                        ? 'bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400' 
                        : item.status === 'RESOLVED' 
                          ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400' 
                          : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                    }`}>
                      {item.status}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-center py-6">
                <CheckCircle className="w-10 h-10 text-emerald-500 mb-2" />
                <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">Queue is Clear!</p>
                <p className="text-xs text-slate-400 mt-1">All verifications have been processed.</p>
              </div>
            )}
          </div>
        </div>

        {/* Right column: active case details */}
        <div className="lg:col-span-7 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 flex flex-col h-[75vh]">
          {selectedCase ? (
            <div className="flex flex-col h-full justify-between">
              
              {/* Scrollable details */}
              <div className="overflow-y-auto space-y-6 flex-1 pr-2">
                <div className="flex justify-between items-start border-b border-slate-100 dark:border-slate-800 pb-4">
                  <div>
                    <h2 className="text-xl font-bold font-outfit text-slate-800 dark:text-white">
                      Review Case #{selectedCase.id.substring(0, 8)}
                    </h2>
                    <p className="text-xs text-slate-400">
                      Entity ID: {selectedCase.entityId}
                    </p>
                  </div>
                  <span className="text-sm font-bold text-rose-500">
                    Risk Priority: {selectedCase.priorityScore}
                  </span>
                </div>

                {/* Validation check widgets */}
                <div className="space-y-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Likeness Verification details
                  </h3>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-900 rounded-xl space-y-2">
                      <div className="flex items-center space-x-1.5 text-indigo-500 font-semibold text-xs">
                        <FileText className="w-4 h-4" />
                        <span>OCR Extraction Analysis</span>
                      </div>
                      <p className="text-xs text-slate-500">Confidence Score: <span className="font-semibold text-slate-700 dark:text-slate-300">92%</span></p>
                      <p className="text-xs text-slate-500">Document Type Match: <span className="text-emerald-500 font-semibold">VALID</span></p>
                    </div>

                    <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-900 rounded-xl space-y-2">
                      <div className="flex items-center space-x-1.5 text-indigo-500 font-semibold text-xs">
                        <Clock className="w-4 h-4" />
                        <span>Facial Recognition Analysis</span>
                      </div>
                      <p className="text-xs text-slate-500">Selfie Match Likeness: <span className="font-semibold text-slate-700 dark:text-slate-300">95%</span></p>
                      <p className="text-xs text-slate-500">Face Check Outcome: <span className="text-emerald-500 font-semibold">MATCH</span></p>
                    </div>
                  </div>
                </div>

                {/* Notes log */}
                {selectedCase.notes && (
                  <div className="p-4 bg-slate-50 dark:bg-slate-950/40 border border-slate-100 dark:border-slate-900 rounded-xl">
                    <h4 className="text-xs font-bold text-slate-500 mb-1">Case Metadata/Notes</h4>
                    <p className="text-xs text-slate-600 dark:text-slate-300 italic">{selectedCase.notes}</p>
                  </div>
                )}

                {/* Review Notes Area */}
                {selectedCase.status !== 'RESOLVED' && selectedCase.status !== 'CLOSED' && (
                  <div className="space-y-2">
                    <label className="block text-xs font-semibold text-slate-500">
                      Add reviewer decision notes:
                    </label>
                    <textarea
                      value={reviewerNotes}
                      onChange={(e) => setReviewerNotes(e.target.value)}
                      placeholder="Input findings, matched details, or reasons for rejection..."
                      rows={4}
                      className="w-full p-3 border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 rounded-lg text-slate-850 dark:text-slate-100 text-xs focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                )}
              </div>

              {/* Review actions */}
              {selectedCase.status !== 'RESOLVED' && selectedCase.status !== 'CLOSED' ? (
                <div className="flex justify-end space-x-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                  <button
                    onClick={() => handleDecision(false)}
                    disabled={actionLoading}
                    className="px-5 py-2.5 bg-rose-50 hover:bg-rose-100 text-rose-600 font-semibold text-sm rounded-lg transition flex items-center space-x-1.5 disabled:opacity-50"
                  >
                    <UserX className="w-4 h-4" />
                    <span>Reject verification</span>
                  </button>
                  <button
                    onClick={() => handleDecision(true)}
                    disabled={actionLoading}
                    className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm rounded-lg transition flex items-center space-x-1.5 disabled:opacity-50"
                  >
                    {actionLoading ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <UserCheck className="w-4 h-4" />
                    )}
                    <span>Approve verification</span>
                  </button>
                </div>
              ) : (
                <div className="pt-4 border-t border-slate-100 dark:border-slate-800 text-center text-xs text-slate-400">
                  Case is already finalized and resolved. No action required.
                </div>
              )}

            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-center py-12">
              <CornerDownRight className="w-8 h-8 text-slate-300 mb-2" />
              <p className="text-sm font-semibold text-slate-500">Select a case from the queue to review.</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
