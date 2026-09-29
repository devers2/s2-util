#!/usr/bin/env bash
# Runs the real-browser tests, preparing only what is missing: npm packages, Chromium, and (Linux) its system libraries.
# | 실제 브라우저 시험 실행. 없는 것만 준비: npm 패키지, Chromium, (Linux) 시스템 라이브러리
# Usage | 사용법: ./run.sh [playwright test options]   e.g. ./run.sh --headed, ./run.sh -g "hidden"
set -euo pipefail
cd "$(dirname "$0")"

# Prints a step header so each phase shows it is running. | 각 단계가 진행 중임을 보이도록 단계 머리글 출력
# Colored only on a terminal so logs stay free of escape codes. | 로그에 제어 문자가 남지 않도록 터미널일 때만 색 사용
if [ -t 1 ]; then c_on=$'\033[1;36m'; c_off=$'\033[0m'; else c_on=''; c_off=''; fi
step() { printf '\n%s▶ [%s/4] %s%s\n' "$c_on" "$1" "$2" "$c_off"; }

command -v node >/dev/null || { echo "Node.js 18+ is required. | Node.js 18+ 가 필요합니다." >&2; exit 1; }

step 1 "npm packages | npm 패키지 확인"
# Install packages when missing or when the lock file changed. | 패키지가 없거나 잠금 파일이 바뀌었으면 설치
if [ ! -d node_modules/@playwright/test ] || [ package-lock.json -nt node_modules/.package-lock.json ]; then
  npm ci
else
  echo "up to date | 최신 상태"
fi

step 2 "Chromium | Chromium 확인 (최초 1회 다운로드)"
# Download Chromium for this Playwright version; a no-op when already present. | 이 Playwright 버전용 Chromium 다운로드. 이미 있으면 건너뜀
npx playwright install chromium
echo "ok | 정상"

# Lists shared libraries the downloaded Chromium binaries cannot find. | 받은 Chromium 실행 파일이 찾지 못하는 공유 라이브러리 목록
missing_libs() {
  find "${PLAYWRIGHT_BROWSERS_PATH:-$HOME/.cache/ms-playwright}" -maxdepth 3 -type f \
    \( -name chrome-headless-shell -o -name chrome \) -exec ldd {} \; 2>/dev/null | grep "not found" | sort -u || true
}

step 3 "System libraries | 시스템 라이브러리 확인"
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
  echo "ok | 정상"
else
  echo "skipped (not Linux) | 건너뜀 (Linux 아님)"
fi

step 4 "Browser tests | 브라우저 시험 실행"
npx playwright test "$@"
echo "Done in ${SECONDS}s | ${SECONDS}초 만에 완료"
