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
      includeAssets: ['favicon.svg', 'icons.svg'],
      manifest: {
        name: 'EduPulse',
        short_name: 'EduPulse',
        description: 'Early Academic Risk Detection & Intervention Intelligence Platform',
        theme_color: '#4f46e5',
        background_color: '#030712',
        display: 'standalone',
        icons: [
          {
            src: 'favicon.svg',
            sizes: '192x192 512x512',
            type: 'image/svg+xml'
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
