/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
        dropShadow: {
        'glow-orange': '0 2px 8px rgba(2, 132, 199, 0.6)',
      }
    },
  },
  plugins: [],
};
