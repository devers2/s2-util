# S2Util Library - Core Module (s2-core)

[English](README.md) | [한국어](README.ko.md)

---

## 📖 개요 (Overview)

**s2-core** 모듈은 S2Util 프로젝트의 기반이 되는 라이브러리로, 자바 개발에 필요한 고성능 핵심 유틸리티 클래스와 필수 공통 로직을 제공합니다. Method Handle을 활용한 효율적인 리플렉션, **외부 라이브러리 없이 동작하는 자체 고성능 경량 캐시**, 자바 버전별 적응형 스레드 팩토리 등 첨단 기술을 활용합니다. 최적화된 데이터 접근/조작 메서드(`getValue()`, `setValue()`), 문자열 유틸리티, 날짜/시간 처리, 타입 변환 등의 핵심 기능을 제공합니다.

---

## ✨ 주요 기능 (Key Features)

1. **Method Handle을 활용한 고성능 리플렉션**
   - Java 표준 리플렉션(`java.lang.reflect`)의 성능 병목 제거
   - JIT 컴파일러가 MethodHandle 호출을 네이티브에 가까운 수준으로 최적화
   - ConcurrentHashMap에서 MethodHandle을 전략적으로 캐싱하여 반복 접근 시 오버헤드 제거

2. **지능형 캐싱 (듀얼 모드 지원)**
   - **기본**: 외부 의존성 없는 자체 구현 `S2OptimisticCache` 탑재
     - Lock-free 조회 및 낙관적/원자적 생성으로 최고의 성능 보장
     - Sequence 기반 LRU 축출 정책으로 메모리 효율 극대화
   - **선택 사항**: 엔터프라이즈급 부하 처리를 위한 **Caffeine Cache** 완벽 연동
     - 클래스패스에 Caffeine 라이브러리 존재 시 자동 감지 및 활성화
     - W-TinyLFU 알고리즘을 통한 극한의 캐시 적중률 제공
     - **확인 방법**: `S2Cache.isCaffeineEnabled()` 메서드로 Caffeine 활성화 여부를 확인할 수 있습니다.

3. **자바 버전별 적응형 스레드 팩토리**
   - Java 21 이상 환경에서 가상 스레드(Virtual Thread) 지원
   - 이전 버전 환경에서는 최적화된 플랫폼 스레드 풀로 폴백
   - 버전 간 호환성을 위한 통일된 API

4. **최적화된 데이터 접근 및 조작**
   - `getValue()`: 점 표기법(`user.address.street`)을 사용한 중첩 객체값 추출
   - `setValue()`: 배열/컬렉션 지원을 통한 중첩 구조값 설정
   - Map, List, Array, Record, DTO/VO, JPA Hibernate 프록시 지원
   - 대괄호 표기법 지원(`users[0].name`, `matrix[1][2]`)

5. **종합적인 유틸리티 모듈**
   - **S2Cache**: 패턴 기반 축출 정책의 고급 캐싱
   - **S2ThreadUtil**: 버전 인식형 최적화를 포함한 스레드 및 실행기 관리
   - **S2StringUtil**: 문자 치환, 검증, 인코딩 등의 문자열 조작; 정규식 캐싱으로 컴파일 오버헤드 감소
   - **S2DateUtil**: 날짜/시간 파싱, 포매팅, 타임존 처리

6. **다층 접근 모드**
   - **Public 모드**: Public 계약(Getter/Setter) 준수로 최대 성능 달성
   - **Private 모드**: 명시적으로 필요한 경우 private 멤버 접근 활성화

---

## ⚙️ 요구사항 (Requirements)

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📦 의존성 (Dependencies)

본 모듈은 **필수 런타임 의존성이 전혀 없습니다 (ZERO dependencies)**.

- **Caffeine Cache**: 선택 사항. 대규모 동시성 환경을 위한 고급 캐싱 기능이 필요한 경우에만 추가합니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.
