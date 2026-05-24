export const bookingStyles = {

  page: `min-h-screen bg-[#F5F3FF]`,

  // Header
  header: `
    bg-white border-b border-gray-100
    px-8 py-4 flex items-center
    justify-between shadow-sm
  `,
  headerTitle: `text-2xl font-bold text-gray-800`,
  headerSubtitle: `text-sm text-gray-400 mt-1`,

  // Stats
  statsRow: `grid grid-cols-3 gap-4 px-8 py-6`,
  statCard: `
    bg-white rounded-2xl p-5 shadow-sm
    border border-gray-100
  `,
  statLabel: `
    text-xs text-gray-400 uppercase
    tracking-wide font-medium
  `,
  statValue: `text-3xl font-bold text-gray-800 mt-1`,

  // Filter buttons
  filterRow: `
    px-8 pb-4 flex items-center gap-3
  `,
  filterButton: `
    px-4 py-2 rounded-xl text-sm font-medium
    border border-gray-200 text-gray-500
    hover:border-[#9333EA] hover:text-[#9333EA]
    transition-all
  `,
  filterButtonActive: `
    px-4 py-2 rounded-xl text-sm font-medium
    bg-[#9333EA] text-white border 
    border-[#9333EA] transition-all
  `,

  // Table
  tableContainer: `
    mx-8 bg-white rounded-2xl shadow-sm
    border border-gray-100 overflow-hidden mb-6
  `,
  tableTitle: `
    px-6 py-4 font-bold text-gray-800 text-base
    border-b border-gray-100
  `,
  table: `w-full`,
  tableHead: `bg-gray-50 border-b border-gray-100`,
  tableHeadCell: `
    px-6 py-4 text-left text-xs font-semibold
    text-gray-500 uppercase tracking-wide
  `,
  tableRow: `
    border-b border-gray-50
    hover:bg-purple-50 transition-colors
  `,
  tableCell: `px-6 py-4 text-sm text-gray-700`,

  // Status badges bookings
  badgePending: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-gray-100 text-gray-600
    border border-gray-200
  `,
  badgeActive: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-blue-100 text-blue-700
    border border-blue-200
  `,
  badgeCompleted: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-green-100 text-green-700
    border border-green-200
  `,
  badgeCancelled: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-red-100 text-red-700
    border border-red-200
  `,

  // Status badges payments
  badgeSuccess: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-green-100 text-green-700
    border border-green-200
  `,
  badgeFailed: `
    inline-flex items-center px-3 py-1
    rounded-full text-xs font-medium
    bg-red-100 text-red-700
    border border-red-200
  `,

  // Action button
  actionButton: `
    text-xs px-3 py-1 rounded-lg font-medium
    border transition-all
  `,

  // Empty state
  emptyState: `text-center py-12 text-gray-400`,
  emptyIcon: `text-4xl mb-3`,
  emptyText: `text-sm font-medium`,
};