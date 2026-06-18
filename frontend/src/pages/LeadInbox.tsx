import React, { useState, useEffect } from 'react';
import { bookingService, LeadResponse, LeadNote } from '../services/bookingService';
import { apiClient } from '../services/api';
import { Users, MessageSquare, AlertCircle, RefreshCw, Loader2, TrendingUp } from 'lucide-react';

export default function LeadInbox() {
  const [leads, setLeads] = useState<LeadResponse[]>([]);
  const [selectedLead, setSelectedLead] = useState<LeadResponse | null>(null);
  const [notes, setNotes] = useState<LeadNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingNotes, setLoadingNotes] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Note log state
  const [newNote, setNewNote] = useState('');
  const [loggingNote, setLoggingNote] = useState(false);

  // Assignee list state
  const [admins, setAdmins] = useState<any[]>([]);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    fetchLeads();
    fetchAdmins();
  }, []);

  useEffect(() => {
    if (selectedLead) {
      fetchLeadNotes(selectedLead.id);
    }
  }, [selectedLead]);

  const fetchLeads = async () => {
    setLoading(true);
    setError(null);
    try {
      const leadsData = await bookingService.getLeads();
      setLeads(leadsData.sort((a, b) => b.leadScore - a.leadScore)); // Sort by lead score DESC
      if (leadsData.length > 0) {
        setSelectedLead(leadsData[0]);
      }
    } catch (err: any) {
      console.error('Failed to load CRM leads', err);
      setError('Could not retrieve CRM leads. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchAdmins = async () => {
    try {
      const response = await apiClient.get('/users'); // load all system users or admins
      // Filter out admins (role is ADMIN)
      const systemAdmins = response.data.data.filter((u: any) => u.role === 'ADMIN');
      setAdmins(systemAdmins);
      if (systemAdmins.length > 0) {
        setSelectedAssigneeId(systemAdmins[0].id);
      }
    } catch (err: any) {
      console.error('Failed to load system assignees', err);
    }
  };

  const fetchLeadNotes = async (leadId: string) => {
    setLoadingNotes(true);
    try {
      const notesData = await bookingService.getLeadNotes(leadId);
      setNotes(notesData.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
    } catch (err: any) {
      console.error('Failed to fetch lead notes', err);
    } finally {
      setLoadingNotes(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLead || !newNote.trim()) return;

    setLoggingNote(true);
    try {
      await bookingService.addLeadNote(selectedLead.id, newNote.trim());
      setNewNote('');
      await fetchLeadNotes(selectedLead.id);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add CRM note.');
    } finally {
      setLoggingNote(false);
    }
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLead || !selectedAssigneeId) return;

    setAssigning(true);
    try {
      await bookingService.assignLead(selectedLead.id, selectedAssigneeId);
      alert('Lead successfully assigned!');
      // Refresh lead details
      const refreshedLeads = await bookingService.getLeads();
      setLeads(refreshedLeads.sort((a, b) => b.leadScore - a.leadScore));
      const updated = refreshedLeads.find(l => l.id === selectedLead.id);
      if (updated) {
        setSelectedLead(updated);
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to assign lead.');
    } finally {
      setAssigning(false);
    }
  };

  const formatDate = (isoString: string) => {
    return new Date(isoString).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-success border-success/30 bg-success/5';
    if (score >= 50) return 'text-warning border-warning/30 bg-warning/5';
    return 'text-destructive border-destructive/30 bg-destructive/5';
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 flex justify-center items-center">
        <Loader2 className="h-10 w-10 text-primary animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">CRM Lead Inbox</h1>
          <p className="text-muted-foreground mt-1">Review tenant inquiries, evaluate verified trust profiles, and allocate pipelines.</p>
        </div>
        <button
          onClick={fetchLeads}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh Leads
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {leads.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-border rounded-2xl bg-card">
          <Users className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
          <p className="text-muted-foreground font-semibold">No active leads in inbox</p>
          <p className="text-xs text-muted-foreground mt-1">Leads are automatically qualified when tenants schedule visits or make proposals.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Leads Inbox List */}
          <div className="lg:col-span-1 border border-border bg-card rounded-2xl p-4 shadow-sm h-[650px] overflow-y-auto space-y-3">
            <h3 className="font-bold text-sm text-muted-foreground px-2 uppercase tracking-wider mb-2">Qualified Pipeline ({leads.length})</h3>
            <div className="space-y-2">
              {leads.map(lead => {
                const isSelected = selectedLead?.id === lead.id;
                return (
                  <button
                    key={lead.id}
                    onClick={() => setSelectedLead(lead)}
                    className={`w-full text-left p-4 rounded-xl border transition-all ${
                      isSelected
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-border hover:border-foreground/20'
                    }`}
                    aria-pressed={isSelected}
                  >
                    <div className="flex justify-between items-start gap-2">
                      <div className="font-bold text-sm truncate">Tenant Profile</div>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold border ${getScoreColor(lead.leadScore)}`}>
                        Score: {lead.leadScore}
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-1">Inquiry: {lead.inquiryText || 'No custom notes'}</p>
                    <div className="flex justify-between items-center text-[10px] text-muted-foreground mt-3 pt-2 border-t border-border/40">
                      <span>Status: {lead.status}</span>
                      <span>Qualified: {formatDate(lead.createdAt)}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Selected Lead Details */}
          <div className="lg:col-span-2 space-y-6">
            {selectedLead && (
              <div className="border border-border bg-card rounded-2xl p-6 shadow-sm space-y-6">
                {/* Header info */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-6 border-b border-border">
                  <div>
                    <h2 className="text-xl font-bold tracking-tight">Tenant Verification profile</h2>
                    <p className="text-xs text-muted-foreground mt-0.5">Lead qualified on {formatDate(selectedLead.createdAt)} | status: {selectedLead.status}</p>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className={`p-4 border rounded-2xl text-center min-w-24 ${getScoreColor(selectedLead.leadScore)}`}>
                      <div className="text-[10px] uppercase font-bold tracking-wider">Lead Score</div>
                      <div className="text-3xl font-extrabold mt-1">{selectedLead.leadScore}</div>
                    </div>
                  </div>
                </div>

                {/* Score explanation factors */}
                {selectedLead.leadScoreExplanation && (
                  <div className="bg-muted p-4 rounded-xl space-y-2 border border-border/50">
                    <h4 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                      <TrendingUp className="h-4 w-4 text-primary" />
                      Score Explanation Factors
                    </h4>
                    <p className="text-xs text-muted-foreground leading-relaxed font-mono whitespace-pre-line">
                      {selectedLead.leadScoreExplanation.replace(/ \| /g, '\n')}
                    </p>
                  </div>
                )}

                {/* Contact metadata */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="p-3 border border-border rounded-xl">
                    <span className="font-semibold text-muted-foreground block mb-1">Email Address</span>
                    <span className="font-mono text-foreground">{selectedLead.contactEmail || 'Not provided'}</span>
                  </div>
                  <div className="p-3 border border-border rounded-xl">
                    <span className="font-semibold text-muted-foreground block mb-1">Phone Number</span>
                    <span className="font-mono text-foreground">{selectedLead.contactPhone || 'Not provided'}</span>
                  </div>
                </div>

                {/* Manual allocation */}
                <div className="p-4 border border-border rounded-2xl space-y-3">
                  <h4 className="font-bold text-sm">Assign Lead Pipeline</h4>
                  <form onSubmit={handleAssign} className="flex flex-col sm:flex-row gap-2">
                    <select
                      value={selectedAssigneeId}
                      onChange={(e) => setSelectedAssigneeId(e.target.value)}
                      className="w-full sm:max-w-xs p-2.5 border border-border rounded-xl bg-background text-sm focus:outline-none"
                    >
                      {admins.map(admin => (
                        <option key={admin.id} value={admin.id}>
                          {admin.fullName}
                        </option>
                      ))}
                    </select>
                    <button
                      type="submit"
                      disabled={assigning || admins.length === 0}
                      className="px-5 py-2.5 bg-primary text-primary-foreground font-semibold rounded-xl text-xs hover:bg-opacity-95 disabled:opacity-50 transition-all flex items-center justify-center gap-1.5"
                    >
                      {assigning && <Loader2 className="h-3 w-3 animate-spin" />}
                      Reallocate Lead
                    </button>
                  </form>
                </div>

                {/* Notes Timeline and Audit logs */}
                <div className="space-y-4">
                  <h3 className="font-bold text-sm flex items-center gap-2">
                    <MessageSquare className="h-4 w-4 text-primary" />
                    Auditable CRM Notes Timeline
                  </h3>

                  {/* Add note form */}
                  <form onSubmit={handleAddNote} className="flex gap-2">
                    <input
                      type="text"
                      value={newNote}
                      onChange={(e) => setNewNote(e.target.value)}
                      placeholder="Add an internal audit note..."
                      required
                      className="w-full p-2.5 border border-border rounded-xl bg-background text-xs focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                    <button
                      type="submit"
                      disabled={loggingNote}
                      className="px-4 py-2 bg-primary text-primary-foreground font-semibold rounded-xl text-xs hover:bg-opacity-95 disabled:opacity-50 shrink-0"
                    >
                      Log Note
                    </button>
                  </form>

                  {/* Previous notes list */}
                  {loadingNotes ? (
                    <div className="flex justify-center py-4">
                      <Loader2 className="h-5 w-5 text-primary animate-spin" />
                    </div>
                  ) : notes.length === 0 ? (
                    <p className="text-xs text-muted-foreground text-center py-4">No internal audit notes logged for this lead.</p>
                  ) : (
                    <div className="space-y-3">
                      {notes.map(note => (
                        <div key={note.id} className="p-3 border border-border/60 rounded-xl bg-card space-y-1">
                          <p className="text-xs text-foreground font-medium">{note.content}</p>
                          <div className="flex justify-between text-[10px] text-muted-foreground">
                            <span>Author: {note.authorId}</span>
                            <span>{formatDate(note.createdAt)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
