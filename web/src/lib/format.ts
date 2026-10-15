const pesoFormatter = new Intl.NumberFormat('en-PH', {
  style: 'currency',
  currency: 'PHP',
});

export const peso = (value: number): string => pesoFormatter.format(value);

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleString('en-PH', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
