import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(), 
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'masked-icon.svg'],
      manifest: {
        name: 'EduPulse',
        short_name: 'EduPulse',
        description: 'Early Academic Risk Detection & Intervention Intelligence Platform',
        theme_color: '#4f46e5',
        background_color: '#030712',
        display: 'standalone',
        icons: [
          {
            src: 'pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: 'pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      },
      workbox: {
        runtimeCaching: [
          {
            urlPattern: /\/api\/.*/,
            handler: 'NetworkOnly', // NEVER CACHE API CALLS for privacy
            options: {
              cacheableResponse: {
                statuses: [0] // Dummy, network only ignores cache
              }
            }
          }
        ]
      }
    })
  ],
})
