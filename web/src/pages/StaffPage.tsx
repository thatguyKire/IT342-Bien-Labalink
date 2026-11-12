import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, CircleCheck, UserPlus } from 'lucide-react';
import SidebarLayout from '../components/SidebarLayout';
import { PageHeader, TableHead, alertClass } from '../components/ui';
import { errorMessage } from '../lib/api';
import { formatDateTime } from '../lib/format';
import { createAttendant, getAttendants } from '../services/userService';
import type { UserSummary } from '../services/userService';

const PASSWORD_RULE = /^(?=.*\p{L})(?=.*\p{N}).{8,72}$/u;

/**
 * Attendant accounts can only be created here (or via bootstrap config).
 * Public sign-up always creates customers, so nobody can make themselves
 * an attendant.
 */
export default function StaffPage() {
  const [staff, setStaff] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setStaff(await getAttendants());
    } catch (err) {
      setError(errorMessage(err, 'Failed to load staff'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (form.username.trim().length < 3) {
      setError('Name must be at least 3 characters');
      return;
    }
    if (!PASSWORD_RULE.test(form.password)) {
      setError('Password must be 8-72 characters with a letter and a number');
      return;
    }
    try {
      setSaving(true);
      const created = await createAttendant({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      setSuccess(`${created.username} can now sign in to the dashboard.`);
      setForm({ username: '', email: '', password: '' });
      await load();
    } catch (err) {
      setError(errorMessage(err, 'Failed to create attendant'));
    } finally {
      setSaving(false);
    }
  };

  const input = 'w-full border border-gray-200 rounded-xl px-4 py-3 text-sm '
    + 'text-gray-900 outline-none bg-white transition-colors '
    + 'focus:border-brand-500 focus:ring-4 focus:ring-brand-50';

  return (
    <SidebarLayout>
      <PageHeader
        title="Staff"
        description="Shop attendants who can sign in to this dashboard."
      />

      <div className="px-8 py-6 grid grid-cols-3 gap-6 items-start">
        <div className="col-span-2 bg-white rounded-2xl border
                        border-gray-100 overflow-hidden">
          <table className="w-full">
            <TableHead columns={['Name', 'Email', 'Sign-in', 'Since']} />
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={4} className="text-center py-12 text-gray-400">
                    Loading staff…
                  </td>
                </tr>
              ) : staff.map((u, i) => (
                <tr key={u.id} style={{ '--i': i } as React.CSSProperties}
                    className="stagger border-b border-gray-50">
                  <td className="px-6 py-4 text-sm font-semibold text-gray-900">
                    {u.username}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {u.email}
                  </td>
                  <td className="px-6 py-4 text-xs text-gray-500">
                    {u.authProvider === 'GOOGLE' ? 'Google' : 'Password'}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {formatDateTime(u.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <form onSubmit={handleSubmit} noValidate
              className="bg-white rounded-2xl border
                         border-gray-100 p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-1 flex items-center gap-2">
            <UserPlus size={20} className="text-brand-600" aria-hidden="true" />
            Add an attendant
          </h2>
          <p className="text-sm text-gray-500 mb-5">
            Share the password with them privately.
          </p>

          {error && (
            <div role="alert" className={`${alertClass} mb-4`}>
              <CircleAlert size={18} className="shrink-0 mt-px" aria-hidden="true" />
              {error}
            </div>
          )}
          {success && (
            <div role="status" className="flex items-start gap-2 bg-green-50 border
                                          border-green-200 text-green-700
                                          text-sm rounded-xl px-4 py-3 mb-4 animate-rise">
              <CircleCheck size={18} className="shrink-0 mt-px" aria-hidden="true" />
              {success}
            </div>
          )}

          <label className="text-sm font-medium text-gray-700 mb-1 block"
                 htmlFor="staff-name">
            Name
          </label>
          <input id="staff-name" className={`${input} mb-4`} maxLength={50}
                 value={form.username}
                 onChange={(e) => setForm({ ...form, username: e.target.value })} />

          <label className="text-sm font-medium text-gray-700 mb-1 block"
                 htmlFor="staff-email">
            Email
          </label>
          <input id="staff-email" type="email" className={`${input} mb-4`}
                 maxLength={254} autoComplete="off"
                 value={form.email}
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />

          <label className="text-sm font-medium text-gray-700 mb-1 block"
                 htmlFor="staff-password">
            Temporary password
          </label>
          <input id="staff-password" type="password" className={`${input} mb-1`}
                 maxLength={72} autoComplete="new-password"
                 value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <p className="text-xs text-gray-400 mb-5">
            8+ characters with at least one letter and one number.
          </p>

          <button type="submit" disabled={saving}
                  className="w-full bg-brand-600 text-white py-3 rounded-xl
                             font-bold text-sm hover:bg-brand-700
                             active:scale-[0.98] transition-[background-color,transform]
                             disabled:opacity-60">
            {saving ? 'Creating…' : 'Create attendant'}
          </button>
        </form>
      </div>
    </SidebarLayout>
  );
}
