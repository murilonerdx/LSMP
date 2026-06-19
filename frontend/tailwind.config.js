/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        liberthia: {
          900: '#0E0212',
          800: '#120420',
          700: '#1B0830',
          600: '#3B1A5C',
          500: '#6B2A8C',
          400: '#AA40E8',
          300: '#E0A0FF'
        }
      }
    }
  },
  plugins: []
}
