/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#5b5bd6',
          dark: '#4646b8',
        },
      },
    },
  },
  plugins: [],
};
