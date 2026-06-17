import React from 'react';

interface TimelineStep {
  label: string;
  date?: string;
  completed: boolean;
  active?: boolean;
}

interface EscrowTimelineProps {
  heldAt?: string;
  expectedReleaseAt?: string;
  releasedAt?: string;
  status: 'HELD' | 'RELEASED' | 'REFUNDED';
}

export default function EscrowTimeline({ heldAt, expectedReleaseAt, releasedAt, status }: EscrowTimelineProps) {
  const formatDate = (iso?: string) =>
    iso ? new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—';

  const steps: TimelineStep[] = [
    {
      label: 'Funds Held',
      date: formatDate(heldAt),
      completed: true,
      active: status === 'HELD',
    },
    {
      label: status === 'REFUNDED' ? 'Refunded' : 'Expected Release',
      date: status === 'REFUNDED' ? formatDate(releasedAt) : formatDate(expectedReleaseAt),
      completed: status !== 'HELD',
      active: status === 'HELD',
    },
    {
      label: 'Released',
      date: formatDate(releasedAt),
      completed: status === 'RELEASED',
      active: false,
    },
  ];

  const dotColor = (step: TimelineStep, idx: number) => {
    if (status === 'REFUNDED' && idx === 1) return 'bg-blue-500 border-blue-400';
    if (step.completed) return 'bg-emerald-500 border-emerald-400';
    if (step.active) return 'bg-amber-400 border-amber-300 animate-pulse';
    return 'bg-slate-700 border-slate-600';
  };

  const lineColor = (idx: number) => {
    if (idx === 0 && status !== 'HELD') return 'bg-emerald-500';
    if (idx === 1 && status === 'RELEASED') return 'bg-emerald-500';
    return 'bg-slate-700';
  };

  return (
    <div className="flex items-center w-full py-3" role="list" aria-label="Escrow timeline">
      {steps.map((step, idx) => (
        <React.Fragment key={step.label}>
          {/* Step dot + label */}
          <div className="flex flex-col items-center flex-shrink-0" role="listitem">
            <div
              className={`w-4 h-4 rounded-full border-2 transition-all duration-500 ${dotColor(step, idx)}`}
              aria-label={step.label}
            />
            <span className="text-[10px] font-semibold text-muted-foreground mt-1 text-center max-w-[72px] leading-tight">
              {step.label}
            </span>
            {step.date && step.date !== '—' && (
              <span className="text-[9px] text-slate-500 mt-0.5 text-center">{step.date}</span>
            )}
          </div>

          {/* Connector line */}
          {idx < steps.length - 1 && (
            <div className={`flex-1 h-0.5 mx-1 rounded-full transition-all duration-700 ${lineColor(idx)}`} />
          )}
        </React.Fragment>
      ))}
    </div>
  );
}
