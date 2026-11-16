import { defineConfig, devices } from '@playwright/test';

// Start the backend yourself (cd backend && mvn spring-boot:run) or let this config start both.
export default defineConfig({
  testDir: './e2e',
  timeout: 180_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: { baseURL: 'http://localhost:5173', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    { command: 'npm run dev', url: 'http://localhost:5173', reuseExistingServer: true, timeout: 60_000 },
    { command: 'mvn -q spring-boot:run', cwd: '../backend', url: 'http://localhost:8080/actuator/health', reuseExistingServer: true, timeout: 180_000 },
  ],
});
