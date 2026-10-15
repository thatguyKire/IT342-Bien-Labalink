import { useState, useEffect, useCallback, useMemo } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  getAllMachines,
  createMachine,
  updateMachine,
  deleteMachine,
  regenerateQr,
} from '../services/machineService';
import type {
  Machine,
  MachineRequest,
  MachineStatus,
} from '../services/machineService';
import SidebarLayout from '../components/SidebarLayout';
import { useWebSocket } from '../hooks/useWebSocket';
import { errorMessage } from '../lib/api';
import { peso } from '../lib/format';

const defaultForm: MachineRequest = {
  machineName: '',
  machineType: 'WASHER',
  hourlyRate: 25,
  status: 'AVAILABLE',
};

type StatusFilter = 'ALL' | MachineStatus;

const STATUS_BADGE: Record<MachineStatus, { label: string; cls: string }> = {
  AVAILABLE: {
    label: 'Available',
    cls: 'bg-green-100 text-green-700 border-green-200',
  },
  RUNNING: {
    label: 'Running',
    cls: 'bg-[#EDE9FE] text-[#9333EA] border-purple-200',
  },
  OUT_OF_ORDER: {
    label: 'Out of Order',
    cls: 'bg-red-100 text-red-600 border-red-200',
  },
};

const fieldClass = 'w-full border border-gray-200 rounded-xl px-4 py-3 '
  + 'text-sm text-gray-700 outline-none focus:border-[#9333EA] bg-gray-50';
const labelClass = 'text-sm font-medium text-gray-700 mb-1 block';
const overlayClass = 'fixed inset-0 bg-black/50 flex items-center '
  + 'justify-center z-50 px-4';

export default function MachinePage() {
  const [machines, setMachines] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const [form, setForm] = useState<MachineRequest>(defaultForm);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Machine | null>(null);
  const [deleteError, setDeleteError] = useState('');
  const [qrMachine, setQrMachine] = useState<Machine | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');

  const fetchMachines = useCallback(async () => {
    try {
      setMachines(await getAllMachines());
      setPageError('');
    } catch (err) {
      setPageError(errorMessage(err, 'Failed to load machines'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMachines();
  }, [fetchMachines]);

  useWebSocket({ onMachineUpdate: () => fetchMachines() });

  const handleOpenAdd = () => {
    setEditingMachine(null);
    setForm(defaultForm);
    setError('');
    setShowModal(true);
  };

  const handleOpenEdit = (machine: Machine) => {
    setEditingMachine(machine);
    setForm({
      machineName: machine.machineName,
      machineType: machine.machineType,
      hourlyRate: machine.hourlyRate,
      status: machine.status,
    });
    setError('');
    setShowModal(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.machineName.trim()) {
      setError('Machine name is required');
      return;
    }
    if (!(form.hourlyRate >= 1)) {
      setError('Hourly rate must be at least ₱1.00');
      return;
    }
    try {
      setSaving(true);
      setError('');
      const payload = {
        ...form,
        machineName: form.machineName.trim(),
        hourlyRate: Math.round(form.hourlyRate * 100) / 100,
      };
      if (editingMachine) {
        await updateMachine(editingMachine.id, payload);
      } else {
        const created = await createMachine(payload);
        setQrMachine(created); // show the sticker right away
      }
      await fetchMachines();
      setShowModal(false);
    } catch (err) {
      setError(errorMessage(err, 'Failed to save machine. Try again.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteMachine(deleteTarget.id);
      await fetchMachines();
      setDeleteTarget(null);
    } catch (err) {
      setDeleteError(errorMessage(err, 'Failed to delete machine'));
    }
  };

  const handleRegenerate = async () => {
    if (!qrMachine) return;
    if (!window.confirm(
      'Generate a new QR code? The old sticker will stop working and '
      + 'must be replaced.')) {
      return;
    }
    try {
      setQrMachine(await regenerateQr(qrMachine.id));
      await fetchMachines();
    } catch (err) {
      setPageError(errorMessage(err, 'Failed to regenerate QR code'));
    }
  };

  const filteredMachines = useMemo(() => {
    const q = search.trim().toLowerCase();
    return machines.filter((m) =>
      (statusFilter === 'ALL' || m.status === statusFilter)
      && (!q
        || m.machineName.toLowerCase().includes(q)
        || `mac-${String(m.id).padStart(3, '0')}`.includes(q)));
  }, [machines, search, statusFilter]);

  const count = (status: MachineStatus) =>
    machines.filter((m) => m.status === status).length;

  return (
    <SidebarLayout>
      <div className="min-h-screen bg-[#F5F3FF]">

        {/* Header */}
        <div className="bg-white border-b border-gray-100 px-8 py-5 flex
                        items-start justify-between shadow-sm">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">
              Machine Management
            </h1>
            <p className="text-sm text-gray-400 mt-1">
              Add and manage your laundry machines and their operational
              status.
            </p>
          </div>
          <button
            onClick={handleOpenAdd}
            className="bg-[#9333EA] text-white px-5 py-2.5 rounded-xl
                       font-semibold text-sm hover:bg-purple-700
                       transition-all active:scale-95 flex items-center
                       gap-2 shadow-md shadow-purple-200"
          >
            + Add New Machine
          </button>
        </div>

        <div className="px-8 py-6">

          {pageError && (
            <div role="alert" className="bg-red-50 border border-red-200
                                         text-red-600 text-sm rounded-xl
                                         px-4 py-3 mb-4">
              {pageError}
            </div>
          )}

          {/* Search and Filter */}
          <div className="flex items-center gap-3 mb-6">
            <div className="flex-1 relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2
                               text-gray-400 text-sm" aria-hidden="true">
                🔍
              </span>
              <input
                type="search"
                placeholder="Search by machine name or ID (MAC-001)..."
                aria-label="Search machines"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 border border-gray-200
                           rounded-xl text-sm text-gray-700 bg-white
                           outline-none focus:border-[#9333EA] focus:ring-2
                           focus:ring-purple-100"
              />
            </div>
            <select
              aria-label="Filter by status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
              className="border border-gray-200 bg-white text-gray-600 px-4
                         py-2.5 rounded-xl text-sm font-medium outline-none
                         focus:border-[#9333EA]"
            >
              <option value="ALL">All statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="RUNNING">Running</option>
              <option value="OUT_OF_ORDER">Out of Order</option>
            </select>
          </div>

          {/* Stat Cards */}
          <div className="grid grid-cols-4 gap-4 mb-6">
            <StatCard label="Total Machines" value={machines.length}
                      icon="🫧" tone="text-gray-800" bg="bg-[#EDE9FE]" />
            <StatCard label="Available" value={count('AVAILABLE')}
                      icon="✅" tone="text-green-600" bg="bg-green-100" />
            <StatCard label="Running" value={count('RUNNING')}
                      icon="▶️" tone="text-blue-600" bg="bg-blue-100" />
            <StatCard label="Out of Order" value={count('OUT_OF_ORDER')}
                      icon="⚠️" tone="text-red-500" bg="bg-red-100" />
          </div>

          {/* Table */}
          <div className="bg-white rounded-2xl shadow-sm border
                          border-gray-100 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  {['Machine ID', 'Machine', 'Status', 'QR Sticker',
                    'Hourly Rate', 'Actions'].map((h) => (
                    <th key={h} className="px-6 py-4 text-left text-xs
                                           font-semibold text-gray-500
                                           uppercase tracking-wide">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-12
                                               text-gray-400">
                      Loading machines...
                    </td>
                  </tr>
                ) : filteredMachines.length === 0 ? (
                  <tr>
                    <td colSpan={6}>
                      <div className="text-center py-16 text-gray-400">
                        <div className="text-5xl mb-4">🫧</div>
                        <div className="text-base font-medium">
                          No machines found
                        </div>
                        <div className="text-sm mt-1">
                          Click "+ Add New Machine" to get started
                        </div>
                      </div>
                    </td>
                  </tr>
                ) : filteredMachines.map((machine) => (
                  <tr key={machine.id} className="border-b border-gray-50
                                                  hover:bg-purple-50
                                                  transition-colors">
                    <td className="px-6 py-4">
                      <span className="text-[#9333EA] font-semibold text-sm">
                        MAC-{String(machine.id).padStart(3, '0')}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2 text-sm
                                      text-gray-700">
                        <span aria-hidden="true">
                          {machine.machineType === 'WASHER' ? '🫧' : '💨'}
                        </span>
                        {machine.machineName}
                        <span className="text-xs text-gray-400">
                          ({machine.machineType})
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-3 py-1
                                        rounded-full text-xs font-medium
                                        border
                                        ${STATUS_BADGE[machine.status].cls}`}>
                        ● {STATUS_BADGE[machine.status].label}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => setQrMachine(machine)}
                        className="text-xs font-medium text-[#9333EA]
                                   border border-purple-200 rounded-lg
                                   px-3 py-1 hover:bg-purple-50"
                      >
                        ▦ View / Print
                      </button>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700">
                      {peso(machine.hourlyRate)}/hr
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleOpenEdit(machine)}
                          className="text-gray-400 hover:text-[#9333EA]
                                     transition-colors p-1.5 rounded-lg
                                     hover:bg-purple-50"
                          title="Edit"
                          aria-label={`Edit ${machine.machineName}`}
                        >
                          ✏️
                        </button>
                        <button
                          onClick={() => {
                            setDeleteError('');
                            setDeleteTarget(machine);
                          }}
                          className="text-gray-400 hover:text-red-500
                                     transition-colors p-1.5 rounded-lg
                                     hover:bg-red-50"
                          title="Delete"
                          aria-label={`Delete ${machine.machineName}`}
                        >
                          🗑️
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {filteredMachines.length > 0 && (
              <div className="px-6 py-4 border-t border-gray-100 text-sm
                              text-gray-500">
                Showing {filteredMachines.length} of {machines.length}{' '}
                machines
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className={overlayClass} role="dialog" aria-modal="true"
             aria-labelledby="machine-modal-title">
          <form onSubmit={handleSave} noValidate
                className="bg-white rounded-3xl shadow-2xl w-full max-w-md
                           p-8">
            <h2 id="machine-modal-title"
                className="text-xl font-bold text-gray-800 mb-1">
              {editingMachine ? 'Edit Machine' : 'Add New Machine'}
            </h2>
            <p className="text-sm text-gray-400 mb-6">
              {editingMachine
                ? 'Update machine details below'
                : 'A unique QR sticker is generated automatically'}
            </p>

            {error && (
              <div role="alert" className="bg-red-50 border border-red-200
                                           text-red-600 text-sm rounded-xl
                                           px-4 py-3 mb-4">
                {error}
              </div>
            )}

            <div className="mb-4">
              <label className={labelClass} htmlFor="machine-name">
                Machine Name
              </label>
              <input
                id="machine-name"
                type="text"
                placeholder="e.g. Washer 01"
                maxLength={100}
                value={form.machineName}
                onChange={(e) =>
                  setForm({ ...form, machineName: e.target.value })}
                className={fieldClass}
              />
            </div>

            <div className="mb-4">
              <label className={labelClass} htmlFor="machine-type">
                Machine Type
              </label>
              <select
                id="machine-type"
                value={form.machineType}
                onChange={(e) => setForm({
                  ...form,
                  machineType: e.target.value as MachineRequest['machineType'],
                })}
                className={fieldClass}
              >
                <option value="WASHER">Washer</option>
                <option value="DRYER">Dryer</option>
              </select>
            </div>

            <div className="mb-4">
              <label className={labelClass} htmlFor="machine-rate">
                Hourly Rate (₱)
              </label>
              <input
                id="machine-rate"
                type="number"
                placeholder="e.g. 25.00"
                min="1"
                max="10000"
                step="0.01"
                value={Number.isNaN(form.hourlyRate) ? '' : form.hourlyRate}
                onChange={(e) => setForm({
                  ...form,
                  hourlyRate: parseFloat(e.target.value),
                })}
                className={fieldClass}
              />
            </div>

            <div className="mb-4">
              <label className={labelClass} htmlFor="machine-status">
                Status
              </label>
              <select
                id="machine-status"
                value={form.status}
                onChange={(e) => setForm({
                  ...form,
                  status: e.target.value as MachineStatus,
                })}
                className={fieldClass}
              >
                <option value="AVAILABLE">Available</option>
                <option value="RUNNING">Running</option>
                <option value="OUT_OF_ORDER">Out of Order</option>
              </select>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="flex-1 border border-gray-200 text-gray-600 py-3
                           rounded-xl font-semibold text-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex-1 bg-[#9333EA] text-white py-3 rounded-xl
                           font-semibold text-sm hover:bg-purple-700
                           transition-all active:scale-95
                           disabled:opacity-60"
              >
                {saving
                  ? 'Saving...'
                  : editingMachine ? 'Save Changes' : 'Add Machine'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* QR Sticker Modal */}
      {qrMachine?.qrToken && (
        <div className={overlayClass} role="dialog" aria-modal="true"
             aria-labelledby="qr-title">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm
                          p-8 text-center">
            <div id="qr-print-area">
              <p className="text-xs uppercase tracking-wider text-gray-400">
                Scan with the LabaLink app
              </p>
              <h2 id="qr-title" className="text-xl font-bold text-gray-800
                                           mt-1 mb-4">
                {qrMachine.machineName}
              </h2>
              <div className="inline-block p-4 bg-white border
                              border-gray-100 rounded-2xl">
                <QRCodeSVG value={qrMachine.qrToken} size={208} level="M"
                           marginSize={0} />
              </div>
              <p className="text-sm font-semibold text-[#9333EA] mt-4">
                MAC-{String(qrMachine.id).padStart(3, '0')} ·{' '}
                {peso(qrMachine.hourlyRate)}/hr
              </p>
            </div>
            <p className="text-xs text-gray-400 mt-4">
              Treat this code like a key: only someone standing at the
              machine should be able to scan it.
            </p>
            <div className="flex gap-2 mt-6">
              <button
                onClick={() => setQrMachine(null)}
                className="flex-1 border border-gray-200 text-gray-600 py-2.5
                           rounded-xl font-semibold text-sm hover:bg-gray-50"
              >
                Close
              </button>
              <button
                onClick={handleRegenerate}
                className="flex-1 border border-red-200 text-red-500 py-2.5
                           rounded-xl font-semibold text-sm hover:bg-red-50"
              >
                New code
              </button>
              <button
                onClick={() => window.print()}
                className="flex-1 bg-[#9333EA] text-white py-2.5 rounded-xl
                           font-semibold text-sm hover:bg-purple-700"
              >
                Print
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className={overlayClass} role="dialog" aria-modal="true"
             aria-labelledby="delete-title">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md p-8">
            <h2 id="delete-title" className="text-xl font-bold text-gray-800
                                             mb-2">
              Delete {deleteTarget.machineName}?
            </h2>
            <p className="text-sm text-gray-500 mb-4">
              This cannot be undone. Machines with booking history can't be
              deleted; mark them Out of Order instead.
            </p>
            {deleteError && (
              <div role="alert" className="bg-red-50 border border-red-200
                                           text-red-600 text-sm rounded-xl
                                           px-4 py-3 mb-4">
                {deleteError}
              </div>
            )}
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="flex-1 border border-gray-200 text-gray-600 py-3
                           rounded-xl font-semibold text-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 bg-red-500 text-white py-3 rounded-xl
                           font-semibold text-sm hover:bg-red-600
                           transition-all"
              >
                Yes, delete
              </button>
            </div>
          </div>
        </div>
      )}
    </SidebarLayout>
  );
}

function StatCard({
  label,
  value,
  icon,
  tone,
  bg,
}: {
  label: string;
  value: number;
  icon: string;
  tone: string;
  bg: string;
}) {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100
                    flex items-center justify-between">
      <div>
        <p className="text-xs text-gray-400 uppercase tracking-wide
                      font-medium">
          {label}
        </p>
        <p className={`text-3xl font-bold mt-1 ${tone}`}>{value}</p>
      </div>
      <div className={`w-12 h-12 rounded-full ${bg} flex items-center
                       justify-center text-xl`} aria-hidden="true">
        {icon}
      </div>
    </div>
  );
}
