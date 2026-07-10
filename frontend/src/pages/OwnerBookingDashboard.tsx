import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookingService, BookingResponse } from '../services/bookingService';
import { Check, X, Calendar, Users, Layers, AlertCircle, RefreshCw, Loader2, MessageSquare } from 'lucide-react';

export default function OwnerBookingDashboard() {
  const navigate = useNavigate();

  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Rejection Dialog State
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [submittingReject, setSubmittingReject] = useState(false);
  const [actioningId, setActioningId] = useState<string | null>(null);

  useEffect(() => {
    fetchData();

    // SSE connection for owner updates
    const streamUrl = bookingService.getAdminStreamUrl();
    const eventSource = new EventSource(streamUrl, { withCredentials: true });

    eventSource.addEventListener('BOOKING_APPROVED', () => {
      fetchData();
    });

    eventSource.addEventListener('BOOKING_REJECTED', () => {
      fetchData();
    });

    eventSource.onerror = (err) => {
      console.error("SSE Connection error", err);
    };

    return () => {
      eventSource.close();
    };
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const bookingsData = await bookingService.getOwnerBookings();
      setBookings(bookingsData.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
    } catch (err: any) {
      console.error("Failed to load owner bookings", err);
      setError('Could not load booking requests. Please check your network connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id: string) => {
    if (!window.confirm('Are you sure you want to approve this booking request?')) return;
    setActioningId(id);
    try {
      await bookingService.approveBooking(id);
      await fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to approve booking request.');
    } finally {
      setActioningId(null);
    }
  };

  const handleComplete = async (id: string) => {
    if (!window.confirm('Are you sure you want to mark this booking as completed?')) return;
    setActioningId(id);
    try {
      await bookingService.completeBooking(id);
      await fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to complete booking.');
    } finally {
      setActioningId(null);
    }
  };

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingId || !rejectReason.trim()) return;

    setSubmittingReject(true);
    try {
      await bookingService.rejectBooking(rejectingId, rejectReason.trim());
      setRejectingId(null);
      setRejectReason('');
      await fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reject booking request.');
    } finally {
      setSubmittingReject(false);
    }
  };

  const getPendingBookings = () => bookings.filter(b => b.status === 'PENDING');
  const getConfirmedBookings = () => bookings.filter(b => b.status === 'CONFIRMED');

  const formatDate = (isoString: string) => {
    return new Date(isoString).toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Owner Bookings Dashboard</h1>
          <p className="text-muted-foreground mt-1">Review tenant proposals, approve contracts, and schedule calendar slots.</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/listings/calendar')}
            className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
          >
            <Calendar className="h-4 w-4" />
            Visit Calendar
          </button>
          <button
            onClick={() => navigate('/listings/leads')}
            className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
          >
            <Users className="h-4 w-4" />
            CRM Lead Inbox
          </button>
          <button
            onClick={fetchData}
            className="flex items-center justify-center p-2.5 border border-border rounded-xl hover:bg-card transition-all"
            aria-label="Refresh bookings data"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-card border border-border rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-semibold text-muted-foreground">Pending Proposals</h3>
          <p className="text-3xl font-extrabold mt-2 text-warning">{getPendingBookings().length}</p>
        </div>
        <div className="bg-card border border-border rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-semibold text-muted-foreground">Confirmed Deals</h3>
          <p className="text-3xl font-extrabold mt-2 text-success">{getConfirmedBookings().length}</p>
        </div>
        <div className="bg-card border border-border rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-semibold text-muted-foreground">Total Proposals</h3>
          <p className="text-3xl font-extrabold mt-2 text-primary">{bookings.length}</p>
        </div>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          {[1, 2].map(i => (
            <div key={i} className="h-28 bg-card border border-border rounded-xl animate-pulse"></div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Bookings Proposals List */}
          <div className="lg:col-span-2 space-y-6">
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold tracking-tight mb-4 flex items-center gap-2">
                <Layers className="h-5 w-5 text-primary" />
                Pending Booking Requests
              </h2>

              {getPendingBookings().length === 0 ? (
                <div className="text-center py-12 border border-dashed border-border rounded-xl">
                  <p className="text-muted-foreground">No pending booking requests</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {getPendingBookings().map(booking => (
                    <div
                      key={booking.id}
                      className="border border-border rounded-xl p-5 hover:border-foreground/20 transition-all flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4"
                    >
                      <div className="space-y-1">
                        <div className="font-semibold text-sm">Proposal ID: {booking.id.substring(0, 8)}</div>
                        <p className="text-xs text-muted-foreground">Tenant: {booking.tenantId}</p>
                        <p className="text-sm font-bold text-primary">₹{booking.priceAmount.toLocaleString()} / month</p>
                        {booking.notes && <p className="text-xs text-muted-foreground bg-muted p-2 rounded-lg mt-2 font-mono">"{booking.notes}"</p>}
                        <p className="text-[10px] text-muted-foreground">Submitted: {formatDate(booking.createdAt)}</p>
                      </div>

                      <div className="flex items-center gap-2 w-full sm:w-auto">
                        <button
                          onClick={() => navigate('/chat/' + booking.id)}
                          className="flex items-center justify-center p-2.5 border border-border rounded-xl hover:bg-card transition-all"
                          title="Chat with tenant"
                        >
                          <MessageSquare className="h-4 w-4 text-slate-400" />
                        </button>
                        <button
                          onClick={() => handleApprove(booking.id)}
                          disabled={actioningId === booking.id}
                          className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-4 py-2 bg-success text-success-foreground text-xs font-bold rounded-xl hover:bg-success/90 transition-all"
                        >
                          {actioningId === booking.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
                          Approve
                        </button>
                        <button
                          onClick={() => setRejectingId(booking.id)}
                          className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-4 py-2 bg-destructive text-destructive-foreground text-xs font-bold rounded-xl hover:bg-destructive/90 transition-all"
                        >
                          <X className="h-3.5 w-3.5" />
                          Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Confirmed / Historical Bookings List */}
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold tracking-tight mb-4">Confirmed Deals & History</h2>
              {bookings.filter(b => b.status !== 'PENDING').length === 0 ? (
                <p className="text-muted-foreground text-sm">No historical proposals available.</p>
              ) : (
                <div className="divide-y divide-border">
                  {bookings.filter(b => b.status !== 'PENDING').map(booking => (
                    <div key={booking.id} className="py-4 flex justify-between items-center">
                      <div>
                        <div className="font-semibold text-sm">Proposal: {booking.id.substring(0, 8)}</div>
                        <p className="text-xs text-muted-foreground">Rent: ₹{booking.priceAmount.toLocaleString()} | Date: {formatDate(booking.createdAt)}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <button
                          onClick={() => navigate('/chat/' + booking.id)}
                          className="p-2 border border-border rounded-xl hover:bg-card transition-all"
                          title="Chat with tenant"
                        >
                          <MessageSquare className="h-3.5 w-3.5 text-slate-400" />
                        </button>
                        {booking.status === 'CONFIRMED' && (
                          <button
                            onClick={() => handleComplete(booking.id)}
                            disabled={actioningId === booking.id}
                            className="px-3 py-1 bg-success text-success-foreground text-xs font-bold rounded-xl hover:bg-success/90 transition-all flex items-center gap-1"
                          >
                            {actioningId === booking.id ? <Loader2 className="h-3 w-3 animate-spin" /> : <Check className="h-3 w-3" />}
                            Complete
                          </button>
                        )}
                        <span className={`px-2 py-0.5 text-[10px] font-bold rounded-full ${
                          booking.status === 'CONFIRMED' ? 'bg-success/15 text-success' :
                          booking.status === 'COMPLETED' ? 'bg-indigo-500/15 text-indigo-400' : 'bg-muted text-muted-foreground'
                        }`}>
                          {booking.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Sidebar Guidelines */}
          <div className="space-y-6">
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm space-y-4">
              <h3 className="font-bold text-lg">Booking Moderation</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                As a RoomWallah owner, you must respond to booking proposals within 24 hours. Unconfirmed proposals are automatically expired and marked as rejected.
              </p>
              <div className="bg-muted p-4 rounded-xl space-y-2">
                <h5 className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 text-primary" />
                  CRM Rules
                </h5>
                <p className="text-[11px] text-muted-foreground">
                  Confirming scheduling slots helps raise your overall verification trust score and generates qualified leads.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Rejection Dialog Modal */}
      {rejectingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <div className="bg-card border border-border rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4 animate-in fade-in-50 zoom-in-95">
            <h3 className="text-lg font-bold">Reject Booking Proposal</h3>
            <p className="text-xs text-muted-foreground">Please provide a constructive reason for rejecting this proposal so the tenant can modify their details.</p>

            <form onSubmit={handleRejectSubmit} className="space-y-4">
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Reason (e.g. price is too low, lease start date is not suitable...)"
                rows={3}
                required
                className="w-full p-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => { setRejectingId(null); setRejectReason(''); }}
                  className="px-4 py-2 border border-border rounded-xl text-xs font-semibold hover:bg-card transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingReject}
                  className="px-4 py-2 bg-destructive text-destructive-foreground text-xs font-semibold rounded-xl hover:bg-destructive/95 transition-all flex items-center gap-1.5"
                >
                  {submittingReject && <Loader2 className="h-3 w-3 animate-spin" />}
                  Reject Proposal
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
