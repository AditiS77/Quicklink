/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: { ink: '#171923', muted: '#777d90', brand: '#635bff', canvas: '#f7f8fc' },
      fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'] },
      boxShadow: { card: '0 12px 38px rgba(28, 34, 66, .07)' },
    },
  },
  plugins: [],
}
