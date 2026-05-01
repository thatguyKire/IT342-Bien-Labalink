export const walletStyles = {

  page: `min-h-screen bg-[#F5F3FF]`,

  header: `
    bg-white border-b border-gray-100
    px-8 py-4 shadow-sm flex items-start
    justify-between gap-4
  `,
  headerTitle: `text-2xl font-bold text-gray-800`,
  headerSubtitle: `text-sm text-gray-400 mt-1`,

  content: `px-8 py-6 max-w-2xl mx-auto`,

  // Balance card
  balanceCard: `
    bg-gradient-to-r from-[#9333EA] to-[#7C3AED]
    rounded-2xl p-6 text-white mb-6 shadow-lg
  `,
  balanceLabel: `
    text-purple-200 text-sm font-medium mb-1
  `,
  balanceAmount: `
    text-4xl font-bold tracking-tight
  `,
  balanceSubtext: `
    text-purple-200 text-xs mt-2
  `,

  // Top up section
  topUpCard: `
    bg-white rounded-2xl p-6 shadow-sm
    border border-gray-100 mb-6
  `,
  topUpTitle: `
    text-base font-bold text-gray-800 mb-4
  `,

  // Quick amounts
  quickAmounts: `
    grid grid-cols-4 gap-3 mb-4
  `,
  quickAmountBtn: `
    border border-gray-200 rounded-xl
    py-2 text-sm font-medium text-gray-600
    hover:border-[#9333EA] hover:text-[#9333EA]
    transition-all text-center cursor-pointer
  `,
  quickAmountBtnActive: `
    border border-[#9333EA] rounded-xl
    py-2 text-sm font-medium text-[#9333EA]
    bg-purple-50 transition-all 
    text-center cursor-pointer
  `,

  // Input
  inputLabel: `
    text-sm font-medium text-gray-700 mb-1 block
  `,
  input: `
    w-full border border-gray-200 rounded-xl
    px-4 py-3 text-sm text-gray-700
    outline-none focus:border-[#9333EA]
    focus:ring-2 focus:ring-purple-100
    bg-gray-50 transition-all mb-4
  `,

  // Pay button
  payButton: `
    w-full bg-[#9333EA] text-white py-3
    rounded-xl font-semibold text-sm
    hover:bg-purple-700 transition-all
    active:scale-95 disabled:opacity-60
  `,

  poweredBy: `
    text-center text-xs text-gray-400 mt-2
  `,

  // Error and success
  errorBox: `
    bg-red-50 border border-red-200
    text-red-600 text-sm rounded-xl
    px-4 py-3 mb-4
  `,
  successBox: `
    bg-green-50 border border-green-200
    text-green-600 text-sm rounded-xl
    px-4 py-3 mb-4
  `,

  // Transaction history
  historyCard: `
    bg-white rounded-2xl shadow-sm
    border border-gray-100 overflow-hidden
  `,
  historyTitle: `
    px-6 py-4 font-bold text-gray-800
    border-b border-gray-100
  `,
  historyItem: `
    px-6 py-4 flex items-center
    justify-between border-b border-gray-50
    hover:bg-gray-50 transition-colors
  `,
  historyAmount: `font-semibold text-gray-800`,
  historyRef: `text-xs text-gray-400 mt-0.5`,
  historyDate: `text-xs text-gray-400`,
  badgeSuccess: `
    inline-flex items-center px-2 py-1
    rounded-full text-xs font-medium
    bg-green-100 text-green-700
  `,
  badgeFailed: `
    inline-flex items-center px-2 py-1
    rounded-full text-xs font-medium
    bg-red-100 text-red-700
  `,
  badgePending: `
    inline-flex items-center px-2 py-1
    rounded-full text-xs font-medium
    bg-gray-100 text-gray-600
  `,
};