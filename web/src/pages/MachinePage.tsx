import { useState, useEffect } from 'react';
import {
  getAllMachines,
  createMachine,
  updateMachine,
  deleteMachine,
} from '../services/machineService.ts';
import type {
  Machine,
  MachineRequest,
} from '../services/machineService.ts';
import { machineStyles as s } from
  '../styles/MachinePage.styles.ts';

const defaultForm: MachineRequest = {
  machineName: '',
  machineType: 'WASHER',
  hourlyRate: 25,
  status: 'AVAILABLE',
};

export default function MachinePage() {
  const [machines, setMachines] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingMachine, setEditingMachine] = 
    useState<Machine | null>(null);
  const [form, setForm] = 
    useState<MachineRequest>(defaultForm);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = 
    useState<number | null>(null);

  useEffect(() => {
    fetchMachines();
  }, []);

  const fetchMachines = async () => {
    try {
      setLoading(true);
      const data = await getAllMachines();
      setMachines(data);
    } catch {
      setError('Failed to load machines');
    } finally {
      setLoading(false);
    }
  };

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

  const handleSave = async () => {
    if (!form.machineName.trim()) {
      setError('Machine name is required');
      return;
    }
    if (form.hourlyRate <= 0) {
      setError('Hourly rate must be greater than 0');
      return;
    }

    try {
      setSaving(true);
      setError('');
      if (editingMachine) {
        await updateMachine(editingMachine.id, form);
      } else {
        await createMachine(form);
      }
      await fetchMachines();
      setShowModal(false);
    } catch {
      setError('Failed to save machine. Try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteMachine(id);
      await fetchMachines();
      setDeleteConfirmId(null);
    } catch {
      setError('Failed to delete machine');
    }
  };

  const getStatusBadge = (
      status: Machine['status']) => {
    switch (status) {
      case 'AVAILABLE':
        return <span className={s.badgeAvailable}>
          ● AVAILABLE
        </span>;
      case 'RUNNING':
        return <span className={s.badgeRunning}>
          ● RUNNING
        </span>;
      case 'OUT_OF_ORDER':
        return <span className={s.badgeOutOfOrder}>
          ● OUT OF ORDER
        </span>;
    }
  };

  const totalMachines = machines.length;
  const available = machines.filter(
    m => m.status === 'AVAILABLE').length;
  const running = machines.filter(
    m => m.status === 'RUNNING').length;
  const outOfOrder = machines.filter(
    m => m.status === 'OUT_OF_ORDER').length;

  return (
    <div className={s.page}>

      {/* Header */}
      <div className={s.header}>
        <div className={s.headerLeft}>
          <h1 className={s.headerTitle}>
            Machine Management
          </h1>
          <p className={s.headerSubtitle}>
            Add and manage your laundry machines
          </p>
        </div>
        <div className={s.headerRight}>
          <button
            onClick={handleOpenAdd}
            className={s.addButton}
          >
            + Add New Machine
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className={s.statsRow}>
        <div className={s.statCard}>
          <div className={s.statLabel}>
            Total Machines
          </div>
          <div className={s.statValue}>
            {totalMachines}
          </div>
        </div>
        <div className={s.statCard}>
          <div className={s.statLabel}>Available</div>
          <div className="text-3xl font-bold 
                          text-green-600 mt-1">
            {available}
          </div>
        </div>
        <div className={s.statCard}>
          <div className={s.statLabel}>Running</div>
          <div className="text-3xl font-bold 
                          text-blue-600 mt-1">
            {running}
          </div>
        </div>
        <div className={s.statCard}>
          <div className={s.statLabel}>
            Out of Order
          </div>
          <div className="text-3xl font-bold 
                          text-red-500 mt-1">
            {outOfOrder}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className={s.tableContainer}>
        <table className={s.table}>
          <thead className={s.tableHead}>
            <tr>
              <th className={s.tableHeadCell}>
                Machine ID
              </th>
              <th className={s.tableHeadCell}>
                Machine Name
              </th>
              <th className={s.tableHeadCell}>
                Type
              </th>
              <th className={s.tableHeadCell}>
                Status
              </th>
              <th className={s.tableHeadCell}>
                QR Token
              </th>
              <th className={s.tableHeadCell}>
                Hourly Rate
              </th>
              <th className={s.tableHeadCell}>
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} 
                    className="text-center 
                               py-12 text-gray-400">
                  Loading machines...
                </td>
              </tr>
            ) : machines.length === 0 ? (
              <tr>
                <td colSpan={7}>
                  <div className={s.emptyState}>
                    <div className={s.emptyIcon}>
                      🫧
                    </div>
                    <div className={s.emptyText}>
                      No machines yet
                    </div>
                    <div className={s.emptySubtext}>
                      Click "Add New Machine" 
                      to get started
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              machines.map((machine) => (
                <tr key={machine.id} 
                    className={s.tableRow}>
                  <td className={s.tableCell}>
                    <span className={s.machineId}>
                      MAC-{String(machine.id)
                        .padStart(3, '0')}
                    </span>
                  </td>
                  <td className={s.tableCell}>
                    {machine.machineName}
                  </td>
                  <td className={s.tableCell}>
                    {machine.machineType}
                  </td>
                  <td className={s.tableCell}>
                    {getStatusBadge(machine.status)}
                  </td>
                  <td className={s.tableCell}>
                    <span className={s.qrToken}>
                      {machine.qrToken}
                    </span>
                  </td>
                  <td className={s.tableCell}>
                    ₱{machine.hourlyRate
                      .toFixed(2)}/hr
                  </td>
                  <td className={s.tableCell}>
                    <div className="flex items-center 
                                    gap-1">
                      {/* Edit */}
                      <button
                        onClick={() => 
                          handleOpenEdit(machine)}
                        className={s.editButton}
                        title="Edit"
                      >
                        ✏️
                      </button>
                      {/* Delete */}
                      <button
                        onClick={() => 
                          setDeleteConfirmId(
                            machine.id)}
                        className={s.deleteButton}
                        title="Delete"
                      >
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination info */}
        {machines.length > 0 && (
          <div className={s.pagination}>
            <span className={s.paginationText}>
              Showing 1 to {machines.length} of{' '}
              {machines.length} machines
            </span>
          </div>
        )}
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className={s.modalOverlay}>
          <div className={s.modalCard}>
            <h2 className={s.modalTitle}>
              {editingMachine 
                ? 'Edit Machine' 
                : 'Add New Machine'}
            </h2>
            <p className={s.modalSubtitle}>
              {editingMachine
                ? 'Update machine details below'
                : 'Fill in the details for the new machine'}
            </p>

            {error && (
              <div className={s.errorBox}>
                {error}
              </div>
            )}

            {/* Machine Name */}
            <div className={s.formGroup}>
              <label className={s.formLabel}>
                Machine Name
              </label>
              <input
                type="text"
                placeholder="e.g. Washer 01"
                value={form.machineName}
                onChange={(e) => setForm({
                  ...form, 
                  machineName: e.target.value
                })}
                className={s.formInput}
              />
            </div>

            {/* Machine Type */}
            <div className={s.formGroup}>
              <label className={s.formLabel}>
                Machine Type
              </label>
              <select
                value={form.machineType}
                onChange={(e) => setForm({
                  ...form,
                  machineType: e.target.value as
                    'WASHER' | 'DRYER'
                })}
                className={s.formSelect}
              >
                <option value="WASHER">Washer</option>
                <option value="DRYER">Dryer</option>
              </select>
            </div>

            {/* Hourly Rate */}
            <div className={s.formGroup}>
              <label className={s.formLabel}>
                Hourly Rate (₱)
              </label>
              <input
                type="number"
                placeholder="e.g. 25.00"
                value={form.hourlyRate}
                onChange={(e) => setForm({
                  ...form,
                  hourlyRate: parseFloat(
                    e.target.value) || 0
                })}
                className={s.formInput}
                min="1"
              />
            </div>

            {/* Status */}
            <div className={s.formGroup}>
              <label className={s.formLabel}>
                Status
              </label>
              <select
                value={form.status}
                onChange={(e) => setForm({
                  ...form,
                  status: e.target.value as
                    'AVAILABLE' | 'RUNNING' | 
                    'OUT_OF_ORDER'
                })}
                className={s.formSelect}
              >
                <option value="AVAILABLE">
                  Available
                </option>
                <option value="RUNNING">
                  Running
                </option>
                <option value="OUT_OF_ORDER">
                  Out of Order
                </option>
              </select>
            </div>

            {/* Buttons */}
            <div className={s.modalButtonRow}>
              <button
                onClick={() => setShowModal(false)}
                className={s.cancelButton}
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className={s.saveButton}
              >
                {saving 
                  ? 'Saving...' 
                  : editingMachine 
                    ? 'Save Changes' 
                    : 'Add Machine'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirmId !== null && (
        <div className={s.modalOverlay}>
          <div className={s.modalCard}>
            <h2 className={s.modalTitle}>
              Delete Machine
            </h2>
            <p className="text-sm text-gray-500 mb-6">
              Are you sure you want to delete this 
              machine? This action cannot be undone.
            </p>
            <div className={s.modalButtonRow}>
              <button
                onClick={() => 
                  setDeleteConfirmId(null)}
                className={s.cancelButton}
              >
                Cancel
              </button>
              <button
                onClick={() => 
                  handleDelete(deleteConfirmId)}
                className="flex-1 bg-red-500 
                           text-white py-3 
                           rounded-xl font-semibold 
                           text-sm hover:bg-red-600 
                           transition-all"
              >
                Yes Delete
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}