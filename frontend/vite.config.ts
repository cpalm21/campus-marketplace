import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Proxy API calls to the Spring Boot backend so the browser sees one
    // origin. Without this the cross-origin request is blocked, since the
    // backend sends no CORS headers.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
