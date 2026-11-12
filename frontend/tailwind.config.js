/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        chaos: {
          bg: '#0B0F19',
          card: '#111827',
          border: '#1F2937',
          postgres: '#336791',
          redis: '#DC382D',
          healthy: '#10B981',
          latency: '#F59E0B',
          partition: '#EF4444',
          ap: '#F97316'
        }
      },
      animation: {
        'pulse-fast': 'pulse 1s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      }
    },
  },
  plugins: [],
}
