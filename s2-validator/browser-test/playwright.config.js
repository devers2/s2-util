// Playwright configuration for the real-browser tests of s2.validator.js. | s2.validator.js 실제 브라우저 시험 설정
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  reporter: process.env.CI ? [['github'], ['list']] : 'list',
  use: { baseURL: 'http://localhost:4173', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: { command: 'node server.js', url: 'http://localhost:4173/', reuseExistingServer: !process.env.CI }
});
