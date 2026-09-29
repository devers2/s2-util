// Playwright configuration for the real-browser tests of s2.validator.js. | s2.validator.js 실제 브라우저 시험 설정
const { defineConfig, devices } = require('@playwright/test');

// WSL mirrored networking lets connects to a closed 127.0.0.1 port hang (~2 min) instead of being refused, which stalls
// Playwright's "server already running?" probe; ::1 is refused at once. | WSL mirrored 네트워크는 닫힌 127.0.0.1 포트 연결이
// 거절되지 않고 약 2분 대기하여 Playwright 의 "서버 실행 중?" 확인이 멈춤. ::1 은 즉시 거절됨
const host = process.env.WSL_DISTRO_NAME ? '[::1]' : 'localhost';
const baseURL = `http://${host}:4173`;

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  reporter: process.env.CI ? [['github'], ['list']] : 'list',
  use: { baseURL, trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: { command: 'node server.js', url: `${baseURL}/`, reuseExistingServer: !process.env.CI }
});
