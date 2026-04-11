export const machineStyles = {

  // Page wrapper
  page: `
    min-h-screen bg-[#F5F3FF]
  `,

  // Top header bar
  header: `
    bg-white border-b border-gray-100 
    px-8 py-4 flex items-center 
    justify-between shadow-sm
  `,
  headerLeft: `
    flex flex-col
  `,
  headerTitle: `
    text-2xl font-bold text-gray-800
  `,
  headerSubtitle: `
    text-sm text-gray-400 mt-1
  `,
  headerRight: `
    flex items-center gap-3
  `,

  // Add button
  addButton: `
    bg-[#9333EA] text-white px-5 py-2.5 
    rounded-xl font-semibold text-sm
    hover:bg-purple-700 transition-all 
    active:scale-95 flex items-center gap-2
  `,

  // Stats row
  statsRow: `
    grid grid-cols-4 gap-4 px-8 py-6
  `,
  statCard: `
    bg-white rounded-2xl p-5 shadow-sm 
    border border-gray-100
  `,
  statLabel: `
    text-xs text-gray-400 uppercase 
    tracking-wide font-medium
  `,
  statValue: `
    text-3xl font-bold text-gray-800 mt-1
  `,

  // Table container
  tableContainer: `
    mx-8 bg-white rounded-2xl shadow-sm 
    border border-gray-100 overflow-hidden
  `,
  table: `
    w-full
  `,
  tableHead: `
    bg-gray-50 border-b border-gray-100
  `,
  tableHeadCell: `
    px-6 py-4 text-left text-xs font-semibold 
    text-gray-500 uppercase tracking-wide
  `,
  tableRow: `
    border-b border-gray-50 
    hover:bg-purple-50 transition-colors
  `,
  tableCell: `
    px-6 py-4 text-sm text-gray-700
  `,
  machineId: `
    text-[#9333EA] font-semibold text-sm
  `,
  qrToken: `
    font-mono text-xs text-gray-500 
    bg-gray-100 px-2 py-1 rounded-lg
  `,

  // Status badges
  badgeAvailable: `
    inline-flex items-center px-3 py-1 
    rounded-full text-xs font-medium 
    bg-green-100 text-green-700 
    border border-green-200
  `,
  badgeRunning: `
    inline-flex items-center px-3 py-1 
    rounded-full text-xs font-medium 
    bg-blue-100 text-blue-700 
    border border-blue-200
  `,
  badgeOutOfOrder: `
    inline-flex items-center px-3 py-1 
    rounded-full text-xs font-medium 
    bg-red-100 text-red-700 
    border border-red-200
  `,

  // Action buttons
  editButton: `
    text-gray-400 hover:text-[#9333EA] 
    transition-colors p-1.5 rounded-lg 
    hover:bg-purple-50
  `,
  deleteButton: `
    text-gray-400 hover:text-red-500 
    transition-colors p-1.5 rounded-lg 
    hover:bg-red-50
  `,

  // Pagination
  pagination: `
    px-6 py-4 flex items-center 
    justify-between border-t border-gray-100
  `,
  paginationText: `
    text-sm text-gray-500
  `,

  // Modal overlay
  modalOverlay: `
    fixed inset-0 bg-black bg-opacity-50 
    flex items-center justify-center z-50 px-4
  `,
  modalCard: `
    bg-white rounded-3xl shadow-2xl 
    w-full max-w-md p-8
  `,
  modalTitle: `
    text-xl font-bold text-gray-800 mb-1
  `,
  modalSubtitle: `
    text-sm text-gray-400 mb-6
  `,

  // Form elements
  formGroup: `
    mb-4
  `,
  formLabel: `
    text-sm font-medium text-gray-700 
    mb-1 block
  `,
  formInput: `
    w-full border border-gray-200 rounded-xl 
    px-4 py-3 text-sm text-gray-700 
    outline-none focus:border-[#9333EA] 
    focus:ring-2 focus:ring-purple-100 
    bg-gray-50 transition-all
  `,
  formSelect: `
    w-full border border-gray-200 rounded-xl 
    px-4 py-3 text-sm text-gray-700 
    outline-none focus:border-[#9333EA] 
    focus:ring-2 focus:ring-purple-100 
    bg-gray-50 transition-all
  `,

  // Modal buttons
  modalButtonRow: `
    flex gap-3 mt-6
  `,
  cancelButton: `
    flex-1 border border-gray-200 
    text-gray-600 py-3 rounded-xl 
    font-semibold text-sm 
    hover:bg-gray-50 transition-all
  `,
  saveButton: `
    flex-1 bg-[#9333EA] text-white 
    py-3 rounded-xl font-semibold text-sm 
    hover:bg-purple-700 transition-all 
    active:scale-95 disabled:opacity-60
  `,

  // Error box
  errorBox: `
    bg-red-50 border border-red-200 
    text-red-600 text-sm rounded-xl 
    px-4 py-3 mb-4
  `,

  // Empty state
  emptyState: `
    text-center py-16 text-gray-400
  `,
  emptyIcon: `
    text-5xl mb-4
  `,
  emptyText: `
    text-base font-medium
  `,
  emptySubtext: `
    text-sm mt-1
  `,
};