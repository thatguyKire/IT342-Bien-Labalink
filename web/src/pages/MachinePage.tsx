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
// styles are inline via Tailwind; removed unused import

  import SidebarLayout from '../components/SidebarLayout';

  import { useWebSocket } from '../hooks/useWebSocket';

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
  const [search, setSearch] = useState('');

  useWebSocket({
    onMachineUpdate: () => fetchMachines(),
  });

  useEffect(() => {
    fetchMachines();
  }, []);

  const fetchMachines = async () => {
    try {
      setLoading(true);
      const data = await getAllMachines();
      setMachines(data);
    } catch (err) {
      console.error('Failed to load machines', err);
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
      console.error('Failed to delete');
    }
  };

  const filteredMachines = machines.filter(m =>
    m.machineName.toLowerCase()
      .includes(search.toLowerCase()) ||
    m.qrToken.toLowerCase()
      .includes(search.toLowerCase())
  );

  const total = machines.length;
  const available = machines.filter(
    m => m.status === 'AVAILABLE').length;
  const running = machines.filter(
    m => m.status === 'RUNNING').length;
  const outOfOrder = machines.filter(
    m => m.status === 'OUT_OF_ORDER').length;

  const getStatusBadge = (
      status: Machine['status']) => {
    switch (status) {
      case 'AVAILABLE':
        return (
          <span className="inline-flex items-center 
                           px-3 py-1 rounded-full 
                           text-xs font-medium 
                           bg-green-100 text-green-700 
                           border border-green-200">
            ● Available
          </span>
        );
      case 'RUNNING':
        return (
          <span className="inline-flex items-center 
                           px-3 py-1 rounded-full 
                           text-xs font-medium 
                           bg-[#EDE9FE] text-[#9333EA] 
                           border border-purple-200">
            ● Running
          </span>
        );
      case 'OUT_OF_ORDER':
        return (
          <span className="inline-flex items-center 
                           px-3 py-1 rounded-full 
                           text-xs font-medium 
                           bg-red-100 text-red-600 
                           border border-red-200">
            ● Out of Order
          </span>
        );
    }
  };

  const getMachineTypeIcon = (
      type: Machine['machineType']) => {
    return type === 'WASHER' ? '🫧' : '💨';
  };

  return (
    <SidebarLayout>
      <div className="min-h-screen bg-[#F5F3FF]">

        {/* Header */}
        <div className="bg-white border-b 
                        border-gray-100 px-8 
                        py-5 flex items-start 
                        justify-between shadow-sm">
          <div>
            <h1 className="text-2xl font-bold 
                           text-gray-800">
              Machine Management
            </h1>
            <p className="text-sm text-gray-400 mt-1">
              Add and manage your laundry machines 
              and their operational status.
            </p>
          </div>
          <button
            onClick={handleOpenAdd}
            className="bg-[#9333EA] text-white 
                       px-5 py-2.5 rounded-xl 
                       font-semibold text-sm 
                       hover:bg-purple-700 
                       transition-all active:scale-95 
                       flex items-center gap-2 
                       shadow-md shadow-purple-200"
          >
            + Add New Machine
          </button>
        </div>

        <div className="px-8 py-6">

          {/* Search and Filter */}
          <div className="flex items-center 
                          gap-3 mb-6">
            <div className="flex-1 relative">
              <span className="absolute left-4 
                               top-1/2 -translate-y-1/2 
                               text-gray-400 text-sm">
                🔍
              </span>
              <input
                type="text"
                placeholder="Search by Machine ID or Token..."
                value={search}
                onChange={(e) => 
                  setSearch(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 
                           border border-gray-200 
                           rounded-xl text-sm 
                           text-gray-700 bg-white 
                           outline-none 
                           focus:border-[#9333EA] 
                           focus:ring-2 
                           focus:ring-purple-100"
              />
            </div>
            <button className="border border-gray-200 
                               bg-white text-gray-600 
                               px-4 py-2.5 rounded-xl 
                               text-sm font-medium 
                               hover:bg-gray-50 
                               transition-all">
              Filter
            </button>
          </div>

          {/* Stat Cards */}
          <div className="grid grid-cols-4 
                          gap-4 mb-6">

            {/* Total */}
            <div className="bg-white rounded-2xl 
                            p-5 shadow-sm 
                            border border-gray-100 
                            flex items-center 
                            justify-between">
              <div>
                <p className="text-xs text-gray-400 
                              uppercase tracking-wide 
                              font-medium">
                  Total Machines
                </p>
                <p className="text-3xl font-bold 
                              text-gray-800 mt-1">
                  {total}
                </p>
              </div>
              <div className="w-12 h-12 rounded-full 
                              bg-[#EDE9FE] flex 
                              items-center 
                              justify-center text-xl">
                🫧
              </div>
            </div>

            {/* Available */}
            <div className="bg-white rounded-2xl 
                            p-5 shadow-sm 
                            border border-gray-100 
                            flex items-center 
                            justify-between">
              <div>
                <p className="text-xs text-gray-400 
                              uppercase tracking-wide 
                              font-medium">
                  Available
                </p>
                <p className="text-3xl font-bold 
                              text-green-600 mt-1">
                  {available}
                </p>
              </div>
              <div className="w-12 h-12 rounded-full 
                              bg-green-100 flex 
                              items-center 
                              justify-center text-xl">
                ✅
              </div>
            </div>

            {/* Running */}
            <div className="bg-white rounded-2xl 
                            p-5 shadow-sm 
                            border border-gray-100 
                            flex items-center 
                            justify-between">
              <div>
                <p className="text-xs text-gray-400 
                              uppercase tracking-wide 
                              font-medium">
                  Running
                </p>
                <p className="text-3xl font-bold 
                              text-blue-600 mt-1">
                  {running}
                </p>
              </div>
              <div className="w-12 h-12 rounded-full 
                              bg-blue-100 flex 
                              items-center 
                              justify-center text-xl">
                ▶️
              </div>
            </div>

            {/* Out of Order */}
            <div className="bg-white rounded-2xl 
                            p-5 shadow-sm 
                            border border-gray-100 
                            flex items-center 
                            justify-between">
              <div>
                <p className="text-xs text-gray-400 
                              uppercase tracking-wide 
                              font-medium">
                  Out of Order
                </p>
                <p className="text-3xl font-bold 
                              text-red-500 mt-1">
                  {outOfOrder}
                </p>
              </div>
              <div className="w-12 h-12 rounded-full 
                              bg-red-100 flex 
                              items-center 
                              justify-center text-xl">
                ⚠️
              </div>
            </div>

          </div>

          {/* Table */}
          <div className="bg-white rounded-2xl 
                          shadow-sm border 
                          border-gray-100 
                          overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 
                                border-b 
                                border-gray-100">
                <tr>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    Machine ID
                  </th>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    Machine Type
                  </th>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    Status
                  </th>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    QR Token
                  </th>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    Hourly Rate
                  </th>
                  <th className="px-6 py-4 text-left 
                                 text-xs font-semibold 
                                 text-gray-500 
                                 uppercase tracking-wide">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6}
                        className="text-center 
                                   py-12 text-gray-400">
                      Loading machines...
                    </td>
                  </tr>
                ) : filteredMachines.length === 0 ? (
                  <tr>
                    <td colSpan={6}>
                      <div className="text-center 
                                      py-16 
                                      text-gray-400">
                        <div className="text-5xl mb-4">
                          🫧
                        </div>
                        <div className="text-base 
                                        font-medium">
                          No machines found
                        </div>
                        <div className="text-sm mt-1">
                          Click "+ Add New Machine" 
                          to get started
                        </div>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredMachines.map((machine) => (
                    <tr key={machine.id}
                        className="border-b 
                                   border-gray-50 
                                   hover:bg-purple-50 
                                   transition-colors">
                      <td className="px-6 py-4">
                        <span className="text-[#9333EA] 
                                         font-semibold 
                                         text-sm">
                          MAC-{String(machine.id)
                            .padStart(3, '0')}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex 
                                        items-center 
                                        gap-2 text-sm 
                                        text-gray-700">
                          <span>
                            {getMachineTypeIcon(
                              machine.machineType)}
                          </span>
                          {machine.machineName}
                          <span className="text-xs 
                                           text-gray-400">
                            ({machine.machineType})
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {getStatusBadge(
                          machine.status)}
                      </td>
                      <td className="px-6 py-4">
                        <span className="font-mono 
                                         text-xs 
                                         text-gray-500 
                                         bg-gray-100 
                                         px-2 py-1 
                                         rounded-lg">
                          {machine.qrToken}
                        </span>
                      </td>
                      <td className="px-6 py-4 
                                     text-sm 
                                     text-gray-700">
                        ₱{machine.hourlyRate
                          .toFixed(2)}/hr
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex 
                                        items-center 
                                        gap-1">
                          {/* Edit */}
                          <button
                            onClick={() =>
                              handleOpenEdit(machine)}
                            className="text-gray-400 
                                       hover:text-[#9333EA] 
                                       transition-colors 
                                       p-1.5 rounded-lg 
                                       hover:bg-purple-50"
                            title="Edit"
                          >
                            ✏️
                          </button>
                          {/* Delete */}
                          <button
                            onClick={() =>
                              setDeleteConfirmId(
                                machine.id)}
                            className="text-gray-400 
                                       hover:text-red-500 
                                       transition-colors 
                                       p-1.5 rounded-lg 
                                       hover:bg-red-50"
                            title="Delete"
                          >
                            🗑️
                          </button>
                          {/* More */}
                          <button
                            className="text-gray-400 
                                       hover:text-gray-600 
                                       transition-colors 
                                       p-1.5 rounded-lg 
                                       hover:bg-gray-100"
                            title="More"
                          >
                            ···
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            {/* Pagination */}
            {filteredMachines.length > 0 && (
              <div className="px-6 py-4 flex 
                              items-center 
                              justify-between 
                              border-t border-gray-100">
                <span className="text-sm 
                                 text-gray-500">
                  Showing 1 to{' '}
                  {filteredMachines.length} of{' '}
                  {filteredMachines.length} machines
                </span>
                <div className="flex items-center 
                                gap-2">
                  <button className="px-3 py-1.5 
                                     border 
                                     border-gray-200 
                                     rounded-lg 
                                     text-sm 
                                     text-gray-500 
                                     hover:bg-gray-50">
                    Previous
                  </button>
                  <button className="px-3 py-1.5 
                                     bg-[#9333EA] 
                                     text-white 
                                     rounded-lg 
                                     text-sm">
                    1
                  </button>
                  <button className="px-3 py-1.5 
                                     border 
                                     border-gray-200 
                                     rounded-lg 
                                     text-sm 
                                     text-gray-500 
                                     hover:bg-gray-50">
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>

        </div>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 
                        bg-black bg-opacity-50 
                        flex items-center 
                        justify-center z-50 px-4">
          <div className="bg-white rounded-3xl 
                          shadow-2xl w-full 
                          max-w-md p-8">
            <h2 className="text-xl font-bold 
                           text-gray-800 mb-1">
              {editingMachine
                ? 'Edit Machine'
                : 'Add New Machine'}
            </h2>
            <p className="text-sm text-gray-400 mb-6">
              {editingMachine
                ? 'Update machine details below'
                : 'Fill in the details for the new machine'}
            </p>

            {error && (
              <div className="bg-red-50 
                              border border-red-200 
                              text-red-600 text-sm 
                              rounded-xl px-4 
                              py-3 mb-4">
                {error}
              </div>
            )}

            <div className="mb-4">
              <label className="text-sm font-medium 
                                text-gray-700 
                                mb-1 block">
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
                className="w-full border 
                           border-gray-200 
                           rounded-xl px-4 py-3 
                           text-sm text-gray-700 
                           outline-none 
                           focus:border-[#9333EA] 
                           bg-gray-50"
              />
            </div>

            <div className="mb-4">
              <label className="text-sm font-medium 
                                text-gray-700 
                                mb-1 block">
                Machine Type
              </label>
              <select
                value={form.machineType}
                onChange={(e) => setForm({
                  ...form,
                  machineType: e.target.value as
                    'WASHER' | 'DRYER'
                })}
                className="w-full border 
                           border-gray-200 
                           rounded-xl px-4 py-3 
                           text-sm text-gray-700 
                           outline-none 
                           focus:border-[#9333EA] 
                           bg-gray-50"
              >
                <option value="WASHER">Washer</option>
                <option value="DRYER">Dryer</option>
              </select>
            </div>

            <div className="mb-4">
              <label className="text-sm font-medium 
                                text-gray-700 
                                mb-1 block">
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
                className="w-full border 
                           border-gray-200 
                           rounded-xl px-4 py-3 
                           text-sm text-gray-700 
                           outline-none 
                           focus:border-[#9333EA] 
                           bg-gray-50"
                min="1"
              />
            </div>

            <div className="mb-4">
              <label className="text-sm font-medium 
                                text-gray-700 
                                mb-1 block">
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
                className="w-full border 
                           border-gray-200 
                           rounded-xl px-4 py-3 
                           text-sm text-gray-700 
                           outline-none 
                           focus:border-[#9333EA] 
                           bg-gray-50"
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

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowModal(false)}
                className="flex-1 border 
                           border-gray-200 
                           text-gray-600 py-3 
                           rounded-xl font-semibold 
                           text-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 bg-[#9333EA] 
                           text-white py-3 
                           rounded-xl font-semibold 
                           text-sm hover:bg-purple-700 
                           transition-all 
                           active:scale-95 
                           disabled:opacity-60"
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
        <div className="fixed inset-0 
                        bg-black bg-opacity-50 
                        flex items-center 
                        justify-center z-50 px-4">
          <div className="bg-white rounded-3xl 
                          shadow-2xl w-full 
                          max-w-md p-8">
            <h2 className="text-xl font-bold 
                           text-gray-800 mb-2">
              Delete Machine
            </h2>
            <p className="text-sm text-gray-500 mb-6">
              Are you sure you want to delete this 
              machine? This action cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() =>
                  setDeleteConfirmId(null)}
                className="flex-1 border 
                           border-gray-200 
                           text-gray-600 py-3 
                           rounded-xl font-semibold 
                           text-sm hover:bg-gray-50"
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

    </SidebarLayout>
  );
}