import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // All /api calls go to Spring Boot — includes /api/auth/login, /api/auth/logout, etc.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // Pass Set-Cookie headers through so the session cookie reaches the browser
          proxy.on('proxyRes', (proxyRes) => {
            const setCookie = proxyRes.headers['set-cookie']
            if (setCookie) {
              proxyRes.headers['set-cookie'] = setCookie.map(cookie =>
                cookie.replace(/; SameSite=\w+/i, '').replace(/; Secure/i, '')
              )
            }
          })
        }
      }
    }
  }
})
