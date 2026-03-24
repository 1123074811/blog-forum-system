/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        paper: 'var(--paper)',
        ink: 'var(--ink)',
        accent: 'var(--accent)',
        subtleBlue: 'var(--subtle-blue)',
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        }
      },
      backdropBlur: {
        xs: '2px',
      },
      fontFamily: {
        serif: ['Noto Serif SC', 'serif'],
        cursive: ['Ma Shan Zheng', 'cursive'],
      },
      borderRadius: {
        '2xl': '16px',
        '3xl': '24px',
        'sketch': '255px 15px 225px 15px / 15px 225px 15px 255px',
      },
      boxShadow: {
        'glass': '0 8px 32px rgba(0, 0, 0, 0.08)',
        'glass-lg': '0 20px 40px rgba(0, 0, 0, 0.12)',
        'retro': '15px 15px 0px var(--subtle-blue)',
      }
    },
  },
  plugins: [],
}
