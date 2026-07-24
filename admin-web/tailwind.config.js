/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        pickle: {
          50: "#e8f5e9",
          500: "#2e7d32",
          700: "#1b5e20",
        },
      },
    },
  },
  plugins: [],
};
