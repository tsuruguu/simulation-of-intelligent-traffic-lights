/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./App.tsx",                       // Skanuj App.tsx w głównym folderze
    "./src/**/*.{js,ts,jsx,tsx}",      // Skanuj folder src
    "./components/**/*.{js,ts,jsx,tsx}", // Skanuj folder components (tam jest Dashboard)[cite: 11]
    "./types/**/*.{js,ts,jsx,tsx}",    // Skanuj folder types[cite: 14]
  ],
  theme: {
    extend: {
      colors: {
        'traffic-gray': '#2d2d2d', // To pozwoli na działanie klasy bg-traffic-gray
      },
    },
  },
  plugins: [],
}