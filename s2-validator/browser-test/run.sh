#!/usr/bin/env bash
# Runs the real-browser tests, preparing only what is missing: npm packages, Chromium, and (Linux) its system libraries.
# | 실제 브라우저 시험 실행. 없는 것만 준비: npm 패키지, Chromium, (Linux) 시스템 라이브러리
# Usage | 사용법: ./run.sh [playwright test options]   e.g. ./run.sh --headed, ./run.sh -g "hidden"
set -euo pipefail
cd "$(dirname "$0")"

command -v node >/dev/null || { echo "Node.js 18+ is required. | Node.js 18+ 가 필요합니다." >&2; exit 1; }

# Install packages when missing or when the lock file changed. | 패키지가 없거나 잠금 파일이 바뀌었으면 설치
if [ ! -d node_modules/@playwright/test ] || [ package-lock.json -nt node_modules/.package-lock.json ]; then
  npm ci
fi

# Download Chromium for this Playwright version; a no-op when already present. | 이 Playwright 버전용 Chromium 다운로드. 이미 있으면 건너뜀
npx playwright install chromium

# Lists shared libraries the downloaded Chromium binaries cannot find. | 받은 Chromium 실행 파일이 찾지 못하는 공유 라이브러리 목록
missing_libs() {
  find "${PLAYWRIGHT_BROWSERS_PATH:-$HOME/.cache/ms-playwright}" -maxdepth 3 -type f \
    \( -name chrome-headless-shell -o -name chrome \) -exec ldd {} \; 2>/dev/null | grep "not found" | sort -u || true
}

if [ "$(uname -s)" = "Linux" ]; then
  missing="$(missing_libs)"
  if [ -n "$missing" ]; then
    echo "Chromium system libraries are missing (one-time sudo install): | Chromium 시스템 라이브러리 없음 (최초 1회 sudo 설치):"
    echo "$missing"
    # Authenticate once up front so a wrong password stops here instead of prompting again for the fallback. | 암호를 먼저 한 번 확인해 틀리면 대체 설치에서 다시 묻지 않고 여기서 멈춤
    if ! sudo -v; then
      echo "sudo authentication failed. On WSL this is the Linux user password, not the Windows one. | sudo 인증 실패. WSL 은 Windows 암호가 아닌 리눅스 사용자 암호입니다." >&2
      echo "To reset it, in Windows PowerShell: wsl -u root, then passwd $(whoami) | 재설정: Windows PowerShell 에서 wsl -u root 후 passwd $(whoami)" >&2
      exit 1
    fi
    # libasound2t64 is the Ubuntu 24.04+ name; older releases call it libasound2. | libasound2t64 는 Ubuntu 24.04+ 이름, 이전 버전은 libasound2
    sudo apt-get install -y libnss3 libnspr4 libasound2t64 || sudo apt-get install -y libnss3 libnspr4 libasound2
    missing="$(missing_libs)"
    if [ -n "$missing" ]; then
      echo "Still missing; try: sudo npx playwright install-deps chromium | 여전히 없음. 다음을 시도: sudo npx playwright install-deps chromium" >&2
      echo "$missing" >&2
      exit 1
    fi
  fi
fi

npx playwright test "$@"
