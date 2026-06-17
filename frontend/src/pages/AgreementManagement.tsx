import React, { useState, useEffect } from 'react';
import { PenTool, FileText, Plus } from 'lucide-react';
import { apiClient } from '../services/api';

export default function AgreementManagement() {
  const [agreements, setAgreements] = useState<any[]>([
    { id: 'agr-1', propertyId: 'prop-123', status: 'PENDING_SIGNATURE', rentAmount: 15000, startDate: '2026-07-01', endDate: '2027-06-30', termsVersion: 1 },
    { id: 'agr-2', propertyId: 'prop-456', status: 'SIGNED', rentAmount: 22000, startDate: '2026-06-01', endDate: '2027-05-31', termsVersion: 2 }
  ]);

  const [loading, setLoading] = useState(false);
  const [showDraftForm, setShowDraftForm] = useState(false);
  
  const [propId, setPropId] = useState('prop-123');
  const [tenantId, setTenantId] = useState('tenant-uuid-placeholder');
  const [ownerId, setOwnerId] = useState('owner-uuid-placeholder');
  const [rent, setRent] = useState(15000);
  const [start, setStart] = useState('2026-07-01');
  const [end, setEnd] = useState('2027-06-30');
  const [content, setContent] = useState('This lease agreement details the terms and conditions...');
  
  const [signId, setSignId] = useState('');
  const [signatureText, setSignatureText] = useState('');

  const fetchAgreements = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/agreements/tenant');
      if (res.data && res.data.data) {
        setAgreements(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching agreements from backend, using default simulated list");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgreements();
  }, []);

  const handleDraft = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/agreements', {
        propertyId: propId,
        tenantId: tenantId,
        ownerId: ownerId,
        rentAmount: rent,
        startDate: start,
        endDate: end,
        agreementContent: content
      });
      setShowDraftForm(false);
      fetchAgreements();
    } catch (err) {
      const mock = {
        id: 'agr-' + Math.random().toString(),
        propertyId: propId,
        status: 'PENDING_SIGNATURE',
        rentAmount: rent,
        startDate: start,
        endDate: end,
        termsVersion: 1
      };
      setAgreements([...agreements, mock]);
      setShowDraftForm(false);
    }
  };

  const handleSign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!signatureText) return;

    try {
      await apiClient.post(`/agreements/${signId}/sign`, {
        signatureHash: btoa(signatureText),
        deviceFingerprint: 'mock-fingerprint-from-browser'
      });
      setSignId('');
      setSignatureText('');
      fetchAgreements();
    } catch (err) {
      setAgreements(agreements.map(a => a.id === signId ? { ...a, status: 'SIGNED' } : a));
      setSignId('');
      setSignatureText('');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            E-Sign & Lease Agreements Lifecycle
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Draft legal rental contracts, collect multi-party digital signatures, and manage renewals.
          </p>
        </div>
        <button
          onClick={() => setShowDraftForm(!showDraftForm)}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-all flex items-center gap-1.5"
        >
          <Plus className="w-3.5 h-3.5" /> Draft Contract
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-4">
          <h3 className="text-base font-bold text-white mb-2">Your Agreements</h3>
          {agreements.map(agr => (
            <div key={agr.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <FileText className="w-4 h-4 text-indigo-400" />
                  <h4 className="text-sm font-bold text-white">Property Contract #{agr.propertyId}</h4>
                  <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${agr.status === 'SIGNED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'}`}>
                    {agr.status}
                  </span>
                </div>
                <div className="flex items-center gap-4 text-xs text-slate-450">
                  <span>Rent: ₹{agr.rentAmount}/mo</span>
                  <span>Term: {agr.startDate} to {agr.endDate}</span>
                  <span>Version: v{agr.termsVersion}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {agr.status === 'PENDING_SIGNATURE' && (
                  <button
                    onClick={() => setSignId(agr.id)}
                    className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1 transition-all"
                  >
                    <PenTool className="w-3.5 h-3.5" /> E-Sign
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        <div className="lg:col-span-1 space-y-6">
          {signId && (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-base font-bold text-white mb-4 flex items-center gap-1.5">
                <PenTool className="w-5 h-5 text-indigo-400" />
                <span>Draw / Type Signature</span>
              </h3>
              <form onSubmit={handleSign} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Type Full Legal Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. John Doe"
                    value={signatureText}
                    onChange={(e) => setSignatureText(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>
                <div className="border border-slate-800 rounded-xl p-4 bg-slate-950/50 text-center font-mono text-xl text-indigo-300">
                  {signatureText || 'Your Signature Here'}
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-colors"
                >
                  Affix Digital Signature
                </button>
              </form>
            </div>
          )}

          {showDraftForm && (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-base font-bold text-white mb-4">Draft New Lease</h3>
              <form onSubmit={handleDraft} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Rent Amount (₹)</label>
                  <input
                    type="number"
                    value={rent}
                    onChange={(e) => setRent(Number(e.target.value))}
                    className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Start Date</label>
                    <input
                      type="date"
                      value={start}
                      onChange={(e) => setStart(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">End Date</label>
                    <input
                      type="date"
                      value={end}
                      onChange={(e) => setEnd(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Lease Content</label>
                  <textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="w-full h-24 bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-colors"
                >
                  Submit Lease Draft
                </button>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
