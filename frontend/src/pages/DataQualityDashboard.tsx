import React, { useState, useEffect } from 'react';
import { ShieldCheck, EyeOff, AlertTriangle, CheckSquare } from 'lucide-react';
import { apiClient } from '../services/api';

export default function DataQualityDashboard() {
  const [tasks, setTasks] = useState<any[]>([
    { id: 'rem-1', targetType: 'PROPERTY', targetId: 'prop-123', issueType: 'BROKEN_IMAGE', status: 'PENDING', description: 'Listing photo URL returned HTTP 404' },
    { id: 'rem-2', targetType: 'PROPERTY', targetId: 'prop-456', issueType: 'COORDINATE_MISMATCH', status: 'PENDING', description: 'Street address does not align with geopoint location coordinates' },
    { id: 'rem-3', targetType: 'PROPERTY', targetId: 'prop-789', issueType: 'STALE_LISTING', status: 'PENDING', description: 'No bookings or edits in past 180 days' }
  ]);

  const [filter, setFilter] = useState('ALL');

  const fetchTasks = async () => {
    try {
      const res = await apiClient.get('/admin/data-quality/remediation-tasks');
      if (res.data && res.data.data) {
        setTasks(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching quality remediation tasks, using default simulated data");
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  const handleResolve = async (id: string) => {
    try {
      await apiClient.post(`/admin/data-quality/remediation-tasks/${id}/resolve`, {});
      fetchTasks();
    } catch (err) {
      setTasks(tasks.map(t => t.id === id ? { ...t, status: 'RESOLVED' } : t));
    }
  };

  const handleIgnore = async (id: string) => {
    try {
      await apiClient.post(`/admin/data-quality/remediation-tasks/${id}/ignore`, {});
      fetchTasks();
    } catch (err) {
      setTasks(tasks.map(t => t.id === id ? { ...t, status: 'IGNORED' } : t));
    }
  };

  const filteredTasks = tasks.filter(t => {
    if (filter === 'ALL') return true;
    return t.status === filter;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Listing Data Quality & Remediation
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Automated quality scanning tracking coordinates mismatches, dead image links, and stale listings.
          </p>
        </div>
        <ShieldCheck className="w-8 h-8 text-indigo-400" />
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-bold text-white">Remediation Task Queue</h3>
          <div className="flex gap-2">
            {['ALL', 'PENDING', 'RESOLVED', 'IGNORED'].map(st => (
              <button
                key={st}
                onClick={() => setFilter(st)}
                className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${filter === st ? 'bg-indigo-600 text-white shadow-md' : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-850'}`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        {filteredTasks.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider">
                  <th className="py-3 px-4">Target Type</th>
                  <th className="py-3 px-4">Target ID</th>
                  <th className="py-3 px-4">Issue Type</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-850 text-slate-350">
                {filteredTasks.map(t => (
                  <tr key={t.id} className="hover:bg-slate-950/20 transition-colors">
                    <td className="py-4 px-4 font-semibold text-slate-200">{t.targetType}</td>
                    <td className="py-4 px-4 font-mono text-slate-400">{t.targetId}</td>
                    <td className="py-4 px-4">
                      <span className="flex items-center gap-1 text-amber-455 font-semibold">
                        <AlertTriangle className="w-3.5 h-3.5" /> {t.issueType}
                      </span>
                    </td>
                    <td className="py-4 px-4 max-w-[300px] truncate">{t.description}</td>
                    <td className="py-4 px-4">
                      <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${t.status === 'PENDING' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : t.status === 'RESOLVED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'}`}>
                        {t.status}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-right">
                      {t.status === 'PENDING' && (
                        <div className="inline-flex gap-2">
                          <button
                            onClick={() => handleResolve(t.id)}
                            className="p-1.5 bg-emerald-950/20 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-900 hover:text-white rounded transition-all"
                            title="Mark Resolved"
                          >
                            <CheckSquare className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleIgnore(t.id)}
                            className="p-1.5 bg-slate-950 border border-slate-850 hover:bg-slate-900 text-slate-400 hover:text-white rounded transition-colors"
                            title="Ignore"
                          >
                            <EyeOff className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-10 bg-slate-950/40 border border-slate-850 border-dashed rounded-xl">
            <ShieldCheck className="w-8 h-8 text-emerald-500 mb-2 mx-auto" />
            <p className="text-slate-400 text-xs font-semibold">No Data Quality Issues</p>
            <p className="text-slate-500 text-[10px] mt-0.5">Your property listings are clean and complete.</p>
          </div>
        )}
      </div>
    </div>
  );
}
