# s2.validator.js 실제 브라우저 시험

GraalJS·가짜 DOM 시험(`./gradlew check`)으로는 확인할 수 없는 **실제 브라우저 동작**을 Playwright(Chromium)로 검증합니다.
Gradle 빌드와는 분리되어 있으며 Node.js 18+ 가 필요합니다.

| 시나리오 | 확인 내용 |
|---|---|
| 기본 폼 | 빈 제출 차단, 서버 메시지가 `validationMessage` 로 표시, 첫 오류 필드 포커스, `1,000` 거부·앞뒤 공백 무시, 수정 시 오류 해제 |
| 히든 필드 | 히든 입력칸·닫힌 탭·숨은 라디오 그룹에 실제 렌더링되는 앵커 생성, 필드 순서 유지, 재검증 시 제거 |
| 체크박스 | 동의(ASSERT_TRUE)·그룹(REQUIRED) 차단 및 통과 |
| 동적 행 | 행 삭제 후 `reindex` 로 인덱스 재정렬, 새 인덱스로 오류 표시 |
| 렌더러 | `classRenderer` 의 `is-invalid`·메시지 표시(히든 필드 포함), 기본 말풍선 미사용, 입력 시 해제 |

규칙은 `demo/rules.json` 으로, 실제 서버 검증기(`BrowserDemoRules.java`)가 만든 JSON 입니다.

## 실행

```bash
cd s2-validator/browser-test
npm ci
npx playwright install --with-deps chromium   # 최초 1회 (Linux/WSL 은 sudo 필요)
npm test
```

## 언제 실행하나

- **자동**: GitHub Actions `Browser tests (s2.validator.js)` 가 `s2.validator.js`·`browser-test/**` 변경 시에만 실행됩니다.
- **로컬**: `s2.validator.js` 를 수정했다면 푸시 전에 `npm test` 를 실행하십시오. 배포 전에는 변경 여부와 관계없이 한 번 실행합니다.

## 데모 규칙 변경

`BrowserDemoRules.java` 를 수정한 뒤 다음을 실행해 `demo/rules.json` 을 다시 만드십시오. 낡으면 `BrowserDemoRulesTest` 가 실패합니다.

```bash
./gradlew :s2-validator:writeBrowserDemoRules
```

## 눈으로 확인

기본 말풍선의 모양·위치는 자동 시험으로 판단하기 어렵습니다. 다음을 실행한 뒤 브라우저에서 <http://localhost:4173> 을 열어 확인하십시오.

```bash
npm run demo
```
