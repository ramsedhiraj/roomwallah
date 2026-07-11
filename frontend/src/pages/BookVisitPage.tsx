import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { bookingService, VisitSlotResponse } from '../services/bookingService';
import { apiClient } from '../services/api';
import { ChevronLeft, Calendar, Clock, MessageSquare, AlertTriangle, CheckCircle, Loader2 } from 'lucide-react';

export default function BookVisitPage() {
  const { id: propertyId } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [property, setProperty] = useState<any>(null);
  const [slots, setSlots] = useState<VisitSlotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [waitlistMode, setWaitlistMode] = useState(false);

  useEffect(() => {
    fetchData();
  }, [propertyId]);

  const fetchData = async () => {
    if (!propertyId) return;
    setLoading(true);
    setError(null);
    try {
      // Fetch property details
      const propResponse = await apiClient.get(`/properties/${propertyId}`);
      setProperty(propResponse.data.data);

      // Fetch slots
      const slotsData = await bookingService.getPropertySlots(propertyId);
      setSlots(slotsData.filter(s => s.status === 'AVAILABLE'));
    } catch (err: any) {
      logError("Failed to fetch property details or availability slots.", err);
      setError(err.response?.data?.message || 'Failed to load booking information. Please check your network connection.');
    } finally {
      setLoading(false);
    }
  };

  const logError = (msg: string, err: any) => {
    console.error(msg, err);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!propertyId || !selectedSlotId) return;

    setSubmitting(true);
    setError(null);
    setWaitlistMode(false);

    try {
      await bookingService.scheduleVisit({
        propertyId,
        visitSlotId: selectedSlotId,
        notes: notes.trim()
      });
      setSuccess(true);
      setTimeout(() => {
        navigate('/visits/me');
      }, 2000);
    } catch (err: any) {
      logError("Failed to schedule visit", err);
      const msg = err.response?.data?.message || 'An error occurred while scheduling your visit.';
      setError(msg);
      if (msg.toLowerCase().includes('waitlist') || msg.toLowerCase().includes('full')) {
        setWaitlistMode(true);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Group slots by Date (timezone-aware)
  const groupSlotsByDate = () => {
    const groups: { [key: string]: VisitSlotResponse[] } = {};
    slots.forEach(slot => {
      const dateStr = new Date(slot.startTime).toLocaleDateString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
      if (!groups[dateStr]) {
        groups[dateStr] = [];
      }
      groups[dateStr].push(slot);
    });
    return groups;
  };

  const slotGroups = groupSlotsByDate();

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12">
        <div className="h-6 w-24 bg-gray-200 dark:bg-gray-800 rounded animate-pulse mb-6"></div>
        <div className="h-32 w-full bg-gray-200 dark:bg-gray-800 rounded-xl animate-pulse mb-8"></div>
        <div className="space-y-4">
          <div className="h-8 w-48 bg-gray-200 dark:bg-gray-800 rounded animate-pulse"></div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="h-16 bg-gray-200 dark:bg-gray-800 rounded-lg animate-pulse"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center text-sm font-medium text-muted-foreground hover:text-foreground transition-colors mb-6"
        aria-label="Go back to previous page"
      >
        <ChevronLeft className="h-4 w-4 mr-1" />
        Back
      </button>

      {success ? (
        <div className="bg-success/10 border border-success/30 rounded-2xl p-8 text-center max-w-lg mx-auto shadow-lg backdrop-blur-md">
          <CheckCircle className="h-16 w-16 text-success mx-auto mb-4 animate-bounce" />
          <h2 className="text-2xl font-bold mb-2">Visit Scheduled!</h2>
          <p className="text-muted-foreground">Your visit has been successfully scheduled. Redirecting to your visit schedules...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Main Booking Form */}
          <div className="md:col-span-2 space-y-6">
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm">
              <h1 className="text-2xl font-bold tracking-tight mb-2">Schedule a Property Visit</h1>
              <p className="text-sm text-muted-foreground mb-6">
                Choose from the available owner slots to book a physical visit.
              </p>

              {error && (
                <div className="bg-destructive/10 border border-destructive/20 text-destructive rounded-xl p-4 mb-6 flex items-start gap-3">
                  <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
                  <div>
                    <h5 className="font-semibold text-sm">Scheduling Issue</h5>
                    <p className="text-xs mt-0.5">{error}</p>
                    <button 
                      onClick={fetchData} 
                      className="text-xs font-semibold underline mt-2 hover:opacity-90 block"
                    >
                      Retry Loading Availability
                    </button>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Time Slot Selection */}
                <div>
                  <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-primary" />
                    Select an Availability Slot
                  </h3>

                  {slots.length === 0 ? (
                    <div className="text-center py-8 border border-dashed border-border rounded-xl">
                      <Clock className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                      <p className="text-sm text-muted-foreground">No slots currently available for this property.</p>
                      <p className="text-xs text-muted-foreground mt-1">Please ask the property owner to update scheduling slots.</p>
                    </div>
                  ) : (
                    <div className="space-y-6">
                      {Object.entries(slotGroups).map(([dateLabel, groupSlots]) => (
                        <div key={dateLabel} className="space-y-2">
                          <h4 className="text-xs font-medium text-muted-foreground sticky top-0 bg-background py-1">
                            {dateLabel}
                          </h4>
                          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                            {groupSlots.map(slot => {
                              const isSelected = selectedSlotId === slot.id;
                              const timeStr = new Date(slot.startTime).toLocaleTimeString(undefined, {
                                hour: '2-digit',
                                minute: '2-digit'
                              });
                              return (
                                <button
                                  key={slot.id}
                                  type="button"
                                  onClick={() => setSelectedSlotId(slot.id)}
                                  className={`p-3 text-left border rounded-xl transition-all focus:ring-2 focus:ring-primary ${
                                    isSelected
                                      ? 'border-primary bg-primary/5 ring-1 ring-primary'
                                      : 'border-border hover:border-foreground/30'
                                  }`}
                                  aria-pressed={isSelected}
                                >
                                  <div className="font-semibold text-sm">{timeStr}</div>
                                  <div className="text-[10px] text-muted-foreground mt-0.5">
                                    {slot.maxBookings - slot.currentBookings} slots left
                                  </div>
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Optional Inquiry notes */}
                <div className="space-y-2">
                  <label htmlFor="notes" className="text-sm font-semibold flex items-center gap-2">
                    <MessageSquare className="h-4 w-4 text-primary" />
                    Optional Visit Notes
                  </label>
                  <textarea
                    id="notes"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Enter any questions or requirements for the owner..."
                    rows={4}
                    maxLength={1000}
                    className="w-full p-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                  <div className="text-right text-[10px] text-muted-foreground">
                    {notes.length}/1000 characters
                  </div>
                </div>

                {/* Action buttons */}
                <button
                  type="submit"
                  disabled={!selectedSlotId || submitting}
                  className="w-full py-3 bg-primary text-primary-foreground font-semibold rounded-xl hover:bg-opacity-95 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
                >
                  {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  {waitlistMode ? 'Join Slot Waitlist' : 'Confirm Visit Schedule'}
                </button>
              </form>
            </div>
          </div>

          {/* Sidebar Property Preview */}
          <div className="md:col-span-1">
            {property && (
              <div className="border border-border bg-card rounded-2xl overflow-hidden shadow-sm sticky top-6">
                {property.mediaKeys && property.mediaKeys.length > 0 ? (
                  <img
                    src={`${apiClient.defaults.baseURL}/media/files/${property.mediaKeys[0]}`}
                    alt={property.title}
                    className="w-full h-48 object-cover"
                  />
                ) : (
                  <div className="w-full h-48 bg-muted flex items-center justify-center">
                    <Calendar className="h-12 w-12 text-muted-foreground" />
                  </div>
                )}
                <div className="p-4 space-y-3">
                  <span className="text-[10px] uppercase font-bold tracking-widest px-2 py-1 bg-primary/10 text-primary rounded-full">
                    {property.propertyType}
                  </span>
                  <h3 className="font-bold text-lg leading-tight line-clamp-2">{property.title}</h3>
                  <p className="text-xs text-muted-foreground">
                    {property.address?.locality}, {property.address?.city}
                  </p>
                  <hr className="border-border" />
                  <div className="flex justify-between items-center text-sm font-semibold">
                    <span className="text-muted-foreground">Price</span>
                    <span className="text-lg text-primary">
                      ₹{property.price?.amount?.toLocaleString() || 'N/A'} / month
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
