import React from 'react';
import { Check } from 'lucide-react';

export interface RefundStep {
  label: string;
  status: 'done' | 'active' | 'pending';
}

interface RefundTrackerProps {
  steps: RefundStep[];
}

export default function RefundTracker({ steps }: RefundTrackerProps) {
  return (
    <div className="flex items-center w-full" aria-label="Refund progress tracker" role="list">
      {steps.map((step, idx) => {
        const isDone = step.status === 'done';
        const isActive = step.status === 'active';

        const dotCls = isDone
          ? 'bg-emerald-500 border-emerald-400 text-white'
          : isActive
          ? 'bg-indigo-500 border-indigo-400 text-white animate-pulse'
          : 'bg-slate-800 border-slate-600 text-slate-500';

        const lineCls = isDone ? 'bg-emerald-500' : 'bg-slate-700';

        return (
          <React.Fragment key={step.label}>
            <div className="flex flex-col items-center flex-shrink-0" role="listitem">
              <div
                className={`w-7 h-7 rounded-full border-2 flex items-center justify-center transition-all duration-500 ${dotCls}`}
                aria-label={`${step.label}: ${step.status}`}
              >
                {isDone ? (
                  <Check className="w-3.5 h-3.5" strokeWidth={3} />
                ) : (
                  <span className="text-[10px] font-bold">{idx + 1}</span>
                )}
              </div>
              <span
                className={`text-[10px] font-semibold mt-1 text-center max-w-[60px] leading-tight ${
                  isDone
                    ? 'text-emerald-400'
                    : isActive
                    ? 'text-indigo-300'
                    : 'text-slate-500'
                }`}
              >
                {step.label}
              </span>
            </div>

            {idx < steps.length - 1 && (
              <div
                className={`flex-1 h-0.5 mx-1 rounded-full transition-all duration-700 ${
                  steps[idx].status === 'done' ? lineCls : 'bg-slate-700'
                }`}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}
