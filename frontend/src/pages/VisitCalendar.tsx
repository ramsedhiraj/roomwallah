import React, { useState, useEffect } from 'react';
import { bookingService, VisitSlotResponse } from '../services/bookingService';
import { apiClient } from '../services/api';
import { Calendar, Clock, Sparkles, PlusCircle, AlertCircle, RefreshCw, Loader2, ArrowRight } from 'lucide-react';

interface RecurrenceRule {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  isAvailable: boolean;
}

const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export default function VisitCalendar() {
  const [properties, setProperties] = useState<any[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [slots, setSlots] = useState<VisitSlotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Recurrence Rules State
  const [rules, setRules] = useState<RecurrenceRule[]>(
    DAYS_OF_WEEK.map(day => ({ dayOfWeek: day, startTime: '09:00', endTime: '17:00', isAvailable: false }))
  );

  // Blackouts & Vacation State
  const [blackoutDatesInput, setBlackoutDatesInput] = useState('');
  const [vacationStart, setVacationStart] = useState('');
  const [vacationEnd, setVacationEnd] = useState('');

  // Slot Batch Generation State
  const [genStart, setGenStart] = useState('');
  const [genEnd, setGenEnd] = useState('');
  const [generating, setGenerating] = useState(false);
  const [rulesSaving, setRulesSaving] = useState(false);

  useEffect(() => {
    fetchProperties();
  }, []);

  useEffect(() => {
    if (selectedPropertyId) {
      fetchSlots();
    }
  }, [selectedPropertyId]);

  const fetchProperties = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get('/properties/me'); // load owner properties
      const props = response.data.data;
      setProperties(props);
      if (props.length > 0) {
        setSelectedPropertyId(props[0].id);
      }
      
      // Load current calendar rules
      const rulesData = await bookingService.getCalendarRules();
      if (rulesData) {
        if (rulesData.recurrenceRulesJson) {
          const parsedRules = JSON.parse(rulesData.recurrenceRulesJson);
          const merged = rules.map(r => {
            const matched = parsedRules.find((pr: any) => pr.dayOfWeek === r.dayOfWeek);
            return matched ? { ...r, ...matched } : r;
          });
          setRules(merged);
        }
        if (rulesData.blackoutDatesJson) {
          const parsedBlackouts = JSON.parse(rulesData.blackoutDatesJson);
          setBlackoutDatesInput(parsedBlackouts.join(', '));
        }
        if (rulesData.vacationStart) {
          setVacationStart(new Date(rulesData.vacationStart).toISOString().slice(0, 16));
        }
        if (rulesData.vacationEnd) {
          setVacationEnd(new Date(rulesData.vacationEnd).toISOString().slice(0, 16));
        }
      }
    } catch (err: any) {
      console.error('Failed to load owner properties or rules', err);
      setError('Could not retrieve property profiles.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSlots = async () => {
    if (!selectedPropertyId) return;
    setLoadingSlots(true);
    try {
      const slotsData = await bookingService.getPropertySlots(selectedPropertyId);
      setSlots(slotsData.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()));
    } catch (err: any) {
      console.error('Failed to load property slots', err);
    } finally {
      setLoadingSlots(false);
    }
  };

  const handleRuleToggle = (day: string) => {
    setRules(rules.map(r => r.dayOfWeek === day ? { ...r, isAvailable: !r.isAvailable } : r));
  };

  const handleTimeChange = (day: string, field: 'startTime' | 'endTime', value: string) => {
    setRules(rules.map(r => r.dayOfWeek === day ? { ...r, [field]: value } : r));
  };

  const handleSaveCalendar = async (e: React.FormEvent) => {
    e.preventDefault();
    setRulesSaving(true);
    setError(null);
    try {
      const blackoutsArray = blackoutDatesInput.split(',')
        .map(d => d.trim())
        .filter(d => d.length > 0);

      await bookingService.saveCalendarRules({
        recurrenceRulesJson: JSON.stringify(rules),
        blackoutDatesJson: blackoutsArray.length > 0 ? JSON.stringify(blackoutsArray) : undefined,
        vacationStart: vacationStart ? new Date(vacationStart).toISOString() : undefined,
        vacationEnd: vacationEnd ? new Date(vacationEnd).toISOString() : undefined
      });
      alert('Calendar rules saved successfully!');
    } catch (err: any) {
      console.error('Failed to save rules', err);
      setError('Could not update calendar configurations.');
    } finally {
      setRulesSaving(false);
    }
  };

  const handleGenerateSlots = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPropertyId || !genStart || !genEnd) return;

    setGenerating(true);
    setError(null);
    try {
      await bookingService.generateSlots(
        selectedPropertyId,
        new Date(genStart).toISOString(),
        new Date(genEnd).toISOString()
      );
      alert('Availability slots generated successfully!');
      await fetchSlots();
    } catch (err: any) {
      console.error('Failed to generate slots', err);
      setError(err.response?.data?.message || 'Failed to generate visit slots.');
    } finally {
      setGenerating(false);
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

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-12 flex justify-center items-center">
        <Loader2 className="h-10 w-10 text-primary animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Visit Slots Calendar Manager</h1>
        <p className="text-muted-foreground mt-1">Configure your weekly availability rules and generate booking slots for visits.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive rounded-xl p-4 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {properties.length === 0 ? (
        <div className="text-center py-12 border border-dashed border-border rounded-2xl bg-card">
          <Calendar className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
          <p className="text-muted-foreground font-semibold">No properties registered</p>
          <p className="text-xs text-muted-foreground mt-1">You must create a listing first to manage its availability slots.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Calendar Rules Form */}
          <div className="lg:col-span-2 space-y-6">
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold tracking-tight mb-4 flex items-center gap-2">
                <Sparkles className="h-5 w-5 text-primary" />
                Weekly Availability Recurrence Rules
              </h2>

              <form onSubmit={handleSaveCalendar} className="space-y-6">
                <div className="space-y-3">
                  {rules.map((rule, idx) => (
                    <div
                      key={rule.dayOfWeek}
                      className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-3 border border-border rounded-xl gap-3"
                    >
                      <label className="flex items-center gap-2 cursor-pointer font-medium text-sm">
                        <input
                          type="checkbox"
                          checked={rule.isAvailable}
                          onChange={() => handleRuleToggle(rule.dayOfWeek)}
                          className="rounded text-primary focus:ring-primary h-4 w-4"
                          aria-label={`Enable availability for ${rule.dayOfWeek}`}
                        />
                        {rule.dayOfWeek}
                      </label>

                      {rule.isAvailable && (
                        <div className="flex items-center gap-2 w-full sm:w-auto">
                          <input
                            type="time"
                            value={rule.startTime}
                            onChange={(e) => handleTimeChange(rule.dayOfWeek, 'startTime', e.target.value)}
                            className="p-1.5 border border-border rounded bg-background text-xs"
                            aria-label={`Start time for ${rule.dayOfWeek}`}
                          />
                          <span className="text-xs text-muted-foreground">to</span>
                          <input
                            type="time"
                            value={rule.endTime}
                            onChange={(e) => handleTimeChange(rule.dayOfWeek, 'endTime', e.target.value)}
                            className="p-1.5 border border-border rounded bg-background text-xs"
                            aria-label={`End time for ${rule.dayOfWeek}`}
                          />
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                <hr className="border-border" />

                {/* Blackouts and Vacation ranges */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-muted-foreground block">Blackout Dates (comma-separated YYYY-MM-DD)</label>
                    <input
                      type="text"
                      value={blackoutDatesInput}
                      onChange={(e) => setBlackoutDatesInput(e.target.value)}
                      placeholder="e.g. 2026-06-25, 2026-07-04"
                      className="w-full p-3 rounded-xl border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs font-semibold text-muted-foreground block">Vacation range (Start - End)</label>
                    <div className="flex items-center gap-2">
                      <input
                        type="datetime-local"
                        value={vacationStart}
                        onChange={(e) => setVacationStart(e.target.value)}
                        className="p-2 border border-border rounded-lg bg-background text-xs w-full"
                        aria-label="Vacation start date-time"
                      />
                      <ArrowRight className="h-4 w-4 text-muted-foreground shrink-0" />
                      <input
                        type="datetime-local"
                        value={vacationEnd}
                        onChange={(e) => setVacationEnd(e.target.value)}
                        className="p-2 border border-border rounded-lg bg-background text-xs w-full"
                        aria-label="Vacation end date-time"
                      />
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={rulesSaving}
                  className="w-full py-3 bg-primary text-primary-foreground font-semibold rounded-xl hover:bg-opacity-95 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
                >
                  {rulesSaving && <Loader2 className="h-4 w-4 animate-spin" />}
                  Save Calendar Configurations
                </button>
              </form>
            </div>

            {/* Generated Availability Timeline */}
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold tracking-tight">Active Availability Timeline</h3>
                <button
                  onClick={fetchSlots}
                  className="p-2 border border-border rounded-xl hover:bg-card transition-all"
                  aria-label="Refresh generated slots"
                >
                  <RefreshCw className="h-4 w-4" />
                </button>
              </div>

              {loadingSlots ? (
                <div className="flex justify-center py-6">
                  <Loader2 className="h-6 w-6 text-primary animate-spin" />
                </div>
              ) : slots.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-8">No visit slots generated for the selected property yet.</p>
              ) : (
                <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
                  {slots.map(slot => (
                    <div
                      key={slot.id}
                      className="flex items-center justify-between p-3 border border-border rounded-xl text-xs"
                    >
                      <div className="space-y-0.5">
                        <div className="font-semibold">{formatDate(slot.startTime)}</div>
                        <div className="text-muted-foreground flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                        </div>
                      </div>
                      <span className={`px-2 py-0.5 font-bold rounded-full ${
                        slot.status === 'AVAILABLE' ? 'bg-success/15 text-success' : 'bg-muted text-muted-foreground'
                      }`}>
                        {slot.status} ({slot.currentBookings}/{slot.maxBookings} booked)
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Slots Generator Settings */}
          <div className="space-y-6">
            <div className="border border-border bg-card rounded-2xl p-6 shadow-sm space-y-4">
              <h3 className="font-bold text-lg flex items-center gap-2">
                <PlusCircle className="h-5 w-5 text-primary" />
                Batch Slot Generator
              </h3>

              <div className="space-y-2">
                <label className="text-xs font-semibold text-muted-foreground">Select Property</label>
                <select
                  value={selectedPropertyId}
                  onChange={(e) => setSelectedPropertyId(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-border bg-background text-sm focus:outline-none"
                >
                  {properties.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.title}
                    </option>
                  ))}
                </select>
              </div>

              <form onSubmit={handleGenerateSlots} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-muted-foreground">Start Date-Time</label>
                  <input
                    type="datetime-local"
                    value={genStart}
                    onChange={(e) => setGenStart(e.target.value)}
                    required
                    className="w-full p-2.5 border border-border rounded-xl bg-background text-sm"
                    aria-label="Generation range start"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-muted-foreground">End Date-Time</label>
                  <input
                    type="datetime-local"
                    value={genEnd}
                    onChange={(e) => setGenEnd(e.target.value)}
                    required
                    className="w-full p-2.5 border border-border rounded-xl bg-background text-sm"
                    aria-label="Generation range end"
                  />
                </div>

                <button
                  type="submit"
                  disabled={generating || !selectedPropertyId}
                  className="w-full py-2.5 bg-primary text-primary-foreground font-semibold rounded-xl hover:bg-opacity-95 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
                >
                  {generating && <Loader2 className="h-4 w-4 animate-spin" />}
                  Generate Slots Batch
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
