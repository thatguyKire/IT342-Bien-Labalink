// Shared by the Google sign-in hand-off and the Stripe return pages.
export const oauthStyles = {
  page: `
    min-h-screen bg-gray-50 px-4
    flex items-center justify-center
  `,
  card: `
    bg-white rounded-3xl border border-gray-100
    p-10 text-center max-w-sm w-full animate-pop
  `,
  icon: `
    w-16 h-16 mx-auto mb-5 rounded-2xl
    flex items-center justify-center
  `,
  title: `
    text-xl font-bold text-gray-900 mb-2
  `,
  subtitle: `text-sm text-gray-500`,
};
