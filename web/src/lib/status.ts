/** Display tone for a status; the colours live in components/ui.tsx. */
export type Tone = 'brand' | 'aqua' | 'success' | 'info' | 'warning' | 'danger' | 'neutral';

/** Every machine, booking and payment status the API returns. */
const STATUSES: Record<string, { label: string; tone: Tone }> = {
  AVAILABLE: { label: 'Available', tone: 'success' },
  RUNNING: { label: 'Running', tone: 'info' },
  OUT_OF_ORDER: { label: 'Out of order', tone: 'danger' },
  PENDING: { label: 'Pending', tone: 'warning' },
  ACTIVE: { label: 'Active', tone: 'info' },
  COMPLETED: { label: 'Completed', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
  SUCCESS: { label: 'Paid', tone: 'success' },
  FAILED: { label: 'Failed', tone: 'danger' },
};

export function statusLabel(status: string): string {
  return STATUSES[status]?.label ?? status;
}

export function statusTone(status: string): Tone {
  return STATUSES[status]?.tone ?? 'neutral';
}
