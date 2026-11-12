import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { statusLabel, statusTone } from '../lib/status';
import type { Tone } from '../lib/status';

/*
 * Shared dashboard building blocks, so every page uses the same header,
 * stats, badges and states. Colours come from the tokens in index.css.
 */

const TONE_TEXT: Record<Tone, string> = {
  brand: 'text-brand-600',
  aqua: 'text-aqua-700',
  success: 'text-green-700',
  info: 'text-blue-700',
  warning: 'text-amber-700',
  danger: 'text-red-600',
  neutral: 'text-gray-600',
};

const TONE_SOFT: Record<Tone, string> = {
  brand: 'bg-brand-50 text-brand-600',
  aqua: 'bg-aqua-50 text-aqua-700',
  success: 'bg-green-50 text-green-700',
  info: 'bg-blue-50 text-blue-700',
  warning: 'bg-amber-50 text-amber-700',
  danger: 'bg-red-50 text-red-600',
  neutral: 'bg-gray-100 text-gray-600',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-full
                      text-xs font-semibold ${TONE_SOFT[statusTone(status)]}`}>
      {statusLabel(status)}
    </span>
  );
}

export function PageHeader({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <header className="bg-white border-b border-gray-100 px-8 py-6
                       flex items-start justify-between gap-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">{title}</h1>
        <p className="text-sm text-gray-500 mt-1">{description}</p>
      </div>
      {children && <div className="flex items-center gap-3 shrink-0">{children}</div>}
    </header>
  );
}

export function StatCard({
  label,
  value,
  icon: Icon,
  tone = 'brand',
}: {
  label: string;
  value: ReactNode;
  icon: LucideIcon;
  tone?: Tone;
}) {
  return (
    <div className="bg-white rounded-2xl p-5 border border-gray-100
                    flex items-center justify-between gap-4">
      <div className="min-w-0">
        <p className="text-sm text-gray-500 font-medium">{label}</p>
        <p className={`text-3xl font-extrabold mt-1 tabular tracking-tight
                       ${TONE_TEXT[tone]}`}>
          {value}
        </p>
      </div>
      <div className={`w-11 h-11 rounded-xl flex items-center justify-center
                       shrink-0 ${TONE_SOFT[tone]}`} aria-hidden="true">
        <Icon size={22} />
      </div>
    </div>
  );
}

/** Pulsing dot while the WebSocket feed is connected. */
export function LiveIndicator({ live }: { live: boolean }) {
  return (
    <span className="inline-flex items-center gap-2 text-sm font-semibold"
          aria-live="polite">
      <span className="relative flex w-2.5 h-2.5" aria-hidden="true">
        {live && (
          <span className="absolute inset-0 rounded-full bg-aqua-400 animate-halo" />
        )}
        <span className={`relative w-2.5 h-2.5 rounded-full
                          ${live ? 'bg-aqua-400' : 'bg-gray-300'}`} />
      </span>
      <span className={live ? 'text-aqua-700' : 'text-gray-400'}>
        {live ? 'Live' : 'Connecting…'}
      </span>
    </span>
  );
}

export function EmptyState({
  icon: Icon,
  title,
  hint,
}: {
  icon: LucideIcon;
  title: string;
  hint?: string;
}) {
  return (
    <div className="text-center py-16 animate-rise">
      <div className="w-14 h-14 mx-auto rounded-2xl bg-brand-50 text-brand-600
                      flex items-center justify-center mb-4" aria-hidden="true">
        <Icon size={26} />
      </div>
      <p className="text-base font-semibold text-gray-800">{title}</p>
      {hint && <p className="text-sm text-gray-500 mt-1">{hint}</p>}
    </div>
  );
}

export function TableHead({ columns }: { columns: string[] }) {
  return (
    <thead className="bg-gray-50 border-b border-gray-100">
      <tr>
        {columns.map((c) => (
          <th key={c} scope="col"
              className="px-6 py-3.5 text-left text-xs font-semibold text-gray-500">
            {c}
          </th>
        ))}
      </tr>
    </thead>
  );
}

/** Single-choice filter row (statuses). */
export function FilterChips<T extends string>({
  options,
  value,
  onChange,
  labelFor,
}: {
  options: T[];
  value: T;
  onChange: (next: T) => void;
  labelFor: (option: T) => string;
}) {
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label="Filter">
      {options.map((option) => {
        const active = option === value;
        return (
          <button
            key={option}
            onClick={() => onChange(option)}
            aria-pressed={active}
            className={`px-3.5 py-2 rounded-xl text-sm font-semibold
                        transition-colors duration-150 border
                        ${active
                          ? 'bg-brand-600 border-brand-600 text-white'
                          : 'bg-white border-gray-200 text-gray-600 hover:border-brand-300 hover:text-brand-600'}`}
          >
            {labelFor(option)}
          </button>
        );
      })}
    </div>
  );
}

export const alertClass =
  'flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 '
  + 'text-sm rounded-xl px-4 py-3 animate-rise';

export const overlayClass =
  'fixed inset-0 bg-brand-900/50 backdrop-blur-sm animate-fade '
  + 'flex items-center justify-center z-50 px-4';

export const dialogClass =
  'bg-white rounded-3xl shadow-2xl animate-pop w-full';
