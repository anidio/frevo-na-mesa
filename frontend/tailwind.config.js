/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Tema Claro (Light Mode)
        'tema-fundo': '#F7FAFC',
        'tema-primary': '#005daa',
        'tema-accent': '#d72c2c',
        'tema-text': '#2D3748',
        'tema-text-muted': '#718096',
        'tema-success': '#008355',
        'tema-fundo-dark': '#1A202C',      // Um cinza bem escuro, quase preto
        'tema-surface-dark': '#2D3748',   // Um cinza um pouco mais claro para cards e superfícies
        'tema-text-dark': '#E2E8F0',       // Um branco levemente acinzentado para textos
        'tema-text-muted-dark': '#A0AEC0',// Um cinza mais escuro para textos secundários
        'tema-link-dark': '#38bdf8',
      }
    },
  },
  plugins: [],
};