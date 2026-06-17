import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { bookingService, BookingResponse, PropertyVisitResponse } from '../services/bookingService';
import { Calendar, Clock, Download, XCircle, AlertCircle, RefreshCw, Layers } from 'lucide-react';

export default function MyBookingsPage() {
  const { pathname } = useLocation();
  const [activeTab, setActiveTab] = useState<'bookings' | 'visits'>(
    pathname.includes('visits') ? 'visits' : 'bookings'
  );
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [visits, setVisits] = useState<PropertyVisitResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  useEffect(() => {
    fetchData();

    // SSE connection for real-time updates
    const streamUrl = bookingService.getTenantStreamUrl();
    logStreamUrl(streamUrl);
    const eventSource = new EventSource(streamUrl, { withCredentials: true });

    eventSource.addEventListener('BOOKING_CREATED', (e) => {
      logEvent('BOOKING_CREATED', e);
      fetchData();
    });

    eventSource.addEventListener('BOOKING_CANCELLED', (e) => {
      logEvent('BOOKING_CANCELLED', e);
      fetchData();
    });

    eventSource.addEventListener('VISIT_SCHEDULED', (e) => {
      logEvent('VISIT_SCHEDULED', e);
      fetchData();
    });

    eventSource.addEventListener('VISIT_CANCELLED', (e) => {
      logEvent('VISIT_CANCELLED', e);
      fetchData();
    });

    eventSource.onerror = (err) => {
      logStreamError("SSE Connection closed or error occurred.", err);
    };

    return () => {
      eventSource.close();
    };
  }, []);

  const logStreamUrl = (url: string) => {
    console.log("Connecting to tenant updates stream:", url);
  };

  const logEvent = (name: string, e: any) => {
    console.log(`SSE Update: ${name}`, e.data);
  };

  const logStreamError = (msg: string, err: any) => {
    console.error(msg, err);
  };

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [bookingsData, visitsData] = await Promise.all([
        bookingService.getMyBookings(),
        bookingService.getMyVisits()
      ]);
      setBookings(bookingsData.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
      setVisits(visitsData.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()));
    } catch (err: any) {
      logStreamError("Failed to fetch tenant bookings or visits", err);
      setError('Could not retrieve your bookings. Please check your network connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this booking request?')) return;
    setCancellingId(id);
    try {
      await bookingService.cancelBooking(id);
      await fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel booking.');
    } finally {
      setCancellingId(null);
    }
  };

  const handleCancelVisit = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this scheduled property visit?')) return;
    setCancellingId(id);
    try {
      await bookingService.cancelVisit(id);
      await fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel visit.');
    } finally {
      setCancellingId(null);
    }
  };

  const formatDate = (isoString: string) => {
    return new Date(isoString).toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const formatTime = (isoString: string) => {
    return new Date(isoString).toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
      case 'COMPLETED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-success/10 text-success border border-success/20">Active</span>;
      case 'PENDING':
      case 'PENDING_CONFIRMATION':
      case 'SCHEDULED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-warning/10 text-warning border border-warning/20">Pending</span>;
      case 'REJECTED':
      case 'CANCELLED':
      case 'NO_SHOW':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-destructive/10 text-destructive border border-destructive/20">Cancelled</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-muted text-muted-foreground">{status}</span>;
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">My Bookings & Visits</h1>
          <p className="text-muted-foreground mt-1">Track status and manage schedules of your property deals.</p>
        </div>
        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm font-semibold hover:bg-card transition-all"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh
        </button>
      </div>

      {/* Tabs list */}
      <div className="flex border-b border-border mb-6">
        <button
          onClick={() => setActiveTab('bookings')}
          className={`px-5 py-3 text-sm font-semibold border-b-2 transition-all ${
            activeTab === 'bookings'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          }`}
        >
          Booking Deals ({bookings.length})
        </button>
        <button
          onClick={() => setActiveTab('visits')}
          className={`px-5 py-3 text-sm font-semibold border-b-2 transition-all ${
            activeTab === 'visits'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          }`}
        >
          Schedules & Visits ({visits.length})
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive rounded-xl p-4 mb-6 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {loading && bookings.length === 0 && visits.length === 0 ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-24 bg-card border border-border rounded-xl animate-pulse"></div>
          ))}
        </div>
      ) : (
        <div>
          {/* Tab 1: Bookings list */}
          {activeTab === 'bookings' && (
            <div className="space-y-4">
              {bookings.length === 0 ? (
                <div className="text-center py-12 border border-dashed border-border rounded-2xl bg-card">
                  <Layers className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
                  <p className="text-muted-foreground font-semibold">No booking requests found</p>
                  <p className="text-xs text-muted-foreground mt-1">Explore active properties and make a proposal to get started.</p>
                </div>
              ) : (
                bookings.map(booking => (
                  <div
                    key={booking.id}
                    className="border border-border bg-card rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col md:flex-row md:items-center justify-between gap-4"
                  >
                    <div className="space-y-2">
                      <div className="flex items-center gap-3">
                        <span className="font-semibold text-lg text-primary">Proposal ID: {booking.id.substring(0, 8)}</span>
                        {getStatusBadge(booking.status)}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        Rent Amount: <span className="font-semibold text-foreground">₹{booking.priceAmount.toLocaleString()} {booking.priceCurrency}</span>
                      </p>
                      {booking.notes && (
                        <p className="text-xs text-muted-foreground bg-muted p-2.5 rounded-lg border border-border inline-block max-w-lg italic">
                          "{booking.notes}"
                        </p>
                      )}
                      <p className="text-[10px] text-muted-foreground">Created on: {formatDate(booking.createdAt)}</p>
                    </div>

                    {(booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
                      <button
                        onClick={() => handleCancelBooking(booking.id)}
                        disabled={cancellingId === booking.id}
                        className="flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-destructive hover:bg-destructive/10 border border-destructive/20 rounded-xl transition-all"
                      >
                        <XCircle className="h-4 w-4" />
                        Cancel Request
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>
          )}

          {/* Tab 2: Visits list */}
          {activeTab === 'visits' && (
            <div className="space-y-4">
              {visits.length === 0 ? (
                <div className="text-center py-12 border border-dashed border-border rounded-2xl bg-card">
                  <Calendar className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
                  <p className="text-muted-foreground font-semibold">No scheduled visits found</p>
                  <p className="text-xs text-muted-foreground mt-1">Schedule a visit to view listings in person.</p>
                </div>
              ) : (
                visits.map(visit => (
                  <div
                    key={visit.id}
                    className="border border-border bg-card rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col md:flex-row md:items-center justify-between gap-4"
                  >
                    <div className="space-y-2">
                      <div className="flex items-center gap-3">
                        <span className="font-semibold text-lg text-primary">Visit ID: {visit.id.substring(0, 8)}</span>
                        {getStatusBadge(visit.status)}
                      </div>
                      <div className="flex items-center gap-4 text-sm text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-4 w-4" />
                          {formatDate(visit.startTime)}
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="h-4 w-4" />
                          {formatTime(visit.startTime)} - {formatTime(visit.endTime)}
                        </span>
                      </div>
                      {visit.notes && (
                        <p className="text-xs text-muted-foreground bg-muted p-2.5 rounded-lg border border-border inline-block max-w-lg italic">
                          "{visit.notes}"
                        </p>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <a
                        href={bookingService.downloadIcsUrl(visit.id)}
                        className="flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold border border-border rounded-xl hover:bg-card transition-all"
                        title="Download to Google Calendar / Outlook"
                      >
                        <Download className="h-4 w-4" />
                        Calendar .ICS
                      </a>

                      {visit.status === 'SCHEDULED' && (
                        <button
                          onClick={() => handleCancelVisit(visit.id)}
                          disabled={cancellingId === visit.id}
                          className="flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-destructive hover:bg-destructive/10 border border-destructive/20 rounded-xl transition-all"
                        >
                          <XCircle className="h-4 w-4" />
                          Cancel Visit
                        </button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
