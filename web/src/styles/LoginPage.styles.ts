export const loginStyles = {

  // Two columns on wide screens: brand panel | form. Stacks on mobile.
  page: `
    min-h-screen bg-gray-50
    grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]
  `,

  // Brand panel (same deep water as the dashboard sidebar)
  brandPanel: `
    relative overflow-hidden
    bg-gradient-to-br from-brand-900 via-brand-700 to-aqua-700
    text-white px-8 py-10 lg:px-14 lg:py-14
    flex flex-col justify-between gap-10
  `,
  brandTop: `
    flex items-center gap-3
  `,
  brandName: `
    text-xl font-bold
  `,
  brandHeadline: `
    text-3xl lg:text-[2.6rem] font-extrabold leading-[1.1]
    tracking-tight max-w-md
  `,
  brandList: `
    mt-8 space-y-3 hidden sm:block
  `,
  brandItem: `
    flex items-center gap-3 text-brand-100
  `,
  brandIcon: `
    text-aqua-400 shrink-0
  `,
  brandFootnote: `
    text-sm text-brand-200 hidden lg:block
  `,

  // Form column
  formColumn: `
    flex flex-col items-center justify-center px-4 py-10
  `,
  card: `
    w-full max-w-md bg-white rounded-3xl
    border border-gray-100 p-8 animate-pop
  `,
  cardTitle: `
    text-2xl font-bold text-gray-900
  `,
  cardSubtitle: `
    text-sm text-gray-500 mt-1 mb-6
  `,

  // Error box
  errorBox: `
    flex items-start gap-2
    bg-red-50 border border-red-200
    text-red-700 text-sm rounded-xl
    px-4 py-3 mb-4 animate-rise
  `,

  // Inputs
  inputLabel: `
    text-sm font-semibold text-gray-700 mb-1.5 block
  `,
  inputWrapper: `
    flex items-center border border-gray-200
    rounded-xl px-4 py-3 bg-white
    transition-colors duration-150
    focus-within:border-brand-500 focus-within:ring-4 focus-within:ring-brand-50
  `,
  inputIcon: `
    text-gray-400 mr-3 shrink-0
  `,
  input: `
    bg-transparent outline-none
    text-sm w-full text-gray-900 placeholder:text-gray-400
  `,
  eyeIcon: `
    text-gray-400 hover:text-gray-600 cursor-pointer ml-2 p-1 rounded-md
  `,

  // Buttons
  loginButton: `
    w-full bg-brand-600 text-white
    font-bold py-3.5 rounded-xl mt-6
    flex items-center justify-center gap-2
    hover:bg-brand-700 transition-[background-color,transform]
    duration-150 active:scale-[0.98]
    disabled:opacity-60
  `,
  dividerWrapper: `
    flex items-center my-5
  `,
  dividerLine: `
    flex-1 border-t border-gray-200
  `,
  dividerText: `
    mx-3 text-xs text-gray-400
  `,
  googleButton: `
    w-full border border-gray-200
    text-gray-800 font-semibold py-3
    rounded-xl flex items-center
    justify-center gap-3
    hover:bg-gray-50 transition-colors
  `,
  googleIcon: `
    w-5 h-5
  `,
  registerText: `
    text-center text-sm text-gray-500 mt-5
  `,

  // Footer
  footer: `
    text-xs text-gray-400 mt-6
  `,
  footerLink: `
    hover:text-brand-600 underline-offset-2 hover:underline
  `,
};
