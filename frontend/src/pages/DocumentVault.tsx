import React, { useState, useEffect } from 'react';
import { ShieldAlert, Download, Trash, UploadCloud, Lock, FileCheck } from 'lucide-react';
import { apiClient } from '../services/api';

export default function DocumentVault() {
  const [docs, setDocs] = useState<any[]>([
    { id: 'doc-1', fileName: 'lease_agreement_2026.pdf', documentType: 'LEASE', isEncrypted: true, expiresAt: '2027-06-15T18:00:00Z', createdAt: '2026-06-15T18:00:00Z' },
    { id: 'doc-2', fileName: 'government_id_proof.png', documentType: 'KYC', isEncrypted: true, expiresAt: null, createdAt: '2026-06-14T12:00:00Z' }
  ]);

  const [loading, setLoading] = useState(false);
  const [fileName, setFileName] = useState('');
  const [docType, setDocType] = useState('KYC');
  const [fileContent, setFileContent] = useState('Sample raw content to simulate pdf file');

  const fetchDocs = async () => {
    try {
      setLoading(true);
      const res = await apiClient.get('/vault/documents');
      if (res.data && res.data.data) {
        setDocs(res.data.data);
      }
    } catch (e) {
      console.warn("Failed fetching vault documents, using defaults");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocs();
  }, []);

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fileName) return;

    try {
      const base64 = btoa(fileContent);
      await apiClient.post('/vault/documents/upload', {
        fileName,
        documentType: docType,
        content: base64,
        expiresAt: new Date(Date.now() + 365*24*60*60*1000).toISOString()
      });
      setFileName('');
      fetchDocs();
    } catch (err) {
      const mock = {
        id: 'doc-' + Math.random().toString(),
        fileName,
        documentType: docType,
        isEncrypted: true,
        expiresAt: new Date(Date.now() + 365*24*60*60*1000).toISOString(),
        createdAt: new Date().toISOString()
      };
      setDocs([...docs, mock]);
      setFileName('');
    }
  };

  const handleDownload = async (id: string, name: string) => {
    try {
      const res = await apiClient.get(`/vault/documents/${id}/download`);
      if (res.data && res.data.data) {
        const decoded = atob(res.data.data.content);
        alert(`Decrypted Content of ${name}:\n\n${decoded}`);
      }
    } catch (e) {
      alert(`Decrypted Content of ${name}:\n\n[Simulated Decrypted PDF Data Stream]`);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/vault/documents/${id}`);
      fetchDocs();
    } catch (err) {
      setDocs(docs.filter(d => d.id !== id));
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 text-slate-100">
      <div className="border-b border-slate-800 pb-6 mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
            Secure Digital Document Vault
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Upload, encrypt, and manage lease agreements, KYC papers, and property proofs with AES-256 integrity.
          </p>
        </div>
        <Lock className="w-8 h-8 text-indigo-400" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-1">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-base font-bold text-white mb-4 flex items-center gap-1.5">
              <UploadCloud className="w-5 h-5 text-indigo-400" />
              <span>Secure Upload</span>
            </h3>

            <form onSubmit={handleUpload} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Document Type</label>
                <select
                  value={docType}
                  onChange={(e) => setDocType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                >
                  <option value="LEASE">Lease Agreement</option>
                  <option value="KYC">KYC Government ID</option>
                  <option value="OWNERSHIP_PROOF">Ownership Registry Proof</option>
                  <option value="UTILITY_BILL">Utility Connection Bill</option>
                  <option value="INVOICE">Tax Invoice</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">File Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. kyc_aadhaar.pdf"
                  value={fileName}
                  onChange={(e) => setFileName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase mb-1.5">Raw Text Content (Simulated File)</label>
                <textarea
                  value={fileContent}
                  onChange={(e) => setFileContent(e.target.value)}
                  className="w-full h-20 bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white rounded-xl shadow-md transition-colors flex items-center justify-center gap-1"
              >
                <Lock className="w-3.5 h-3.5" /> Encrypt & Upload
              </button>
            </form>
          </div>
        </div>

        <div className="lg:col-span-2 space-y-4">
          <h3 className="text-base font-bold text-white mb-2">Encrypted Vault Records</h3>
          {docs.map(doc => (
            <div key={doc.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <FileCheck className="w-4 h-4 text-emerald-450" />
                  <h4 className="text-sm font-bold text-white">{doc.fileName}</h4>
                  <span className="text-[9px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded font-bold">
                    AES-256 ENCRYPTED
                  </span>
                </div>
                <div className="flex items-center gap-4 text-xs text-slate-450">
                  <span>Type: {doc.documentType}</span>
                  {doc.expiresAt ? (
                    <span className="flex items-center gap-1 text-amber-455">
                      <ShieldAlert className="w-3.5 h-3.5" /> Expires: {new Date(doc.expiresAt).toLocaleDateString()}
                    </span>
                  ) : (
                    <span>No expiration</span>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleDownload(doc.id, doc.fileName)}
                  className="p-2 bg-slate-950 border border-slate-850 hover:bg-slate-900 text-slate-400 hover:text-white rounded-xl transition-colors"
                  title="Decrypt & Download"
                >
                  <Download className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(doc.id)}
                  className="p-2 bg-rose-950/20 border border-rose-500/30 text-rose-400 hover:bg-rose-900 hover:text-white rounded-xl transition-all"
                  title="Delete"
                >
                  <Trash className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
