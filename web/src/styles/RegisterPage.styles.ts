export const registerStyles = {

  // Page wrapper
  page: `
    min-h-screen bg-[#F5F3FF] 
    flex flex-col items-center 
    justify-center px-4 py-10
  `,

  // Logo section
  logoWrapper: `
    flex flex-col items-center mb-8
  `,
  logoBox: `
    w-16 h-16 bg-[#9333EA] rounded-2xl 
    flex items-center justify-center 
    mb-3 shadow-lg
  `,
  logoIcon: `
    text-white text-2xl
  `,
  logoTitle: `
    text-2xl font-bold text-[#9333EA]
  `,
  logoSubtitle: `
    text-sm text-gray-500 mt-1
  `,

  // Card
  card: `
    bg-white rounded-3xl shadow-md 
    w-full max-w-md p-8
  `,
  cardTitle: `
    text-xl font-bold text-gray-800 
    text-center mb-1
  `,
  cardSubtitle: `
    text-sm text-gray-400 text-center mb-6
  `,

  // Error box
  errorBox: `
    bg-red-50 border border-red-200 
    text-red-600 text-sm rounded-xl 
    px-4 py-3 mb-4
  `,

  // Input group
  inputGroup: `
    mb-4
  `,
  inputLabel: `
    text-sm font-medium text-gray-700 mb-1 block
  `,
  inputWrapper: `
    flex items-center border border-gray-200 
    rounded-xl px-4 py-3 bg-gray-50
  `,
  inputIcon: `
    text-gray-400 mr-3
  `,
  input: `
    bg-transparent outline-none 
    text-sm w-full text-gray-700
  `,
  eyeIcon: `
    text-gray-400 cursor-pointer ml-2
  `,

  // Register button
  registerButton: `
    w-full bg-[#9333EA] text-white 
    font-semibold py-3 rounded-xl mt-6 
    hover:bg-purple-700 transition-all 
    duration-200 active:scale-95 
    disabled:opacity-60
  `,

  // Divider
  dividerWrapper: `
    flex items-center my-5
  `,
  dividerLine: `
    flex-1 border-t border-gray-200
  `,
  dividerText: `
    mx-3 text-xs text-gray-400
  `,

  // Google button
  googleButton: `
    w-full border border-gray-200 
    text-gray-700 font-medium py-3 
    rounded-xl flex items-center 
    justify-center gap-3 
    hover:bg-gray-50 transition-all
  `,
  googleIcon: `
    w-5 h-5
  `,

  // Login link
  loginText: `
    text-center text-sm text-gray-500 mt-5
  `,
  loginLink: `
    text-[#9333EA] font-semibold
  `,

  // Footer
  footer: `
    text-xs text-gray-400 mt-6
  `,
};