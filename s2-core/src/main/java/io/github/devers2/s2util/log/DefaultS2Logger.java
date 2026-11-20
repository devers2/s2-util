/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.log;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default console-based logger implementation.
 * <p>
 * This logger acts as a zero-dependency fallback that outputs directly to
 * {@link System#out} and {@link System#err}. It features rich ANSI coloring
 * for better readability in terminals.
 * </p>
 * <p>
 * DEBUG and INFO go to {@link System#out}, WARN and ERROR to {@link System#err}. The minimum level is INFO by
 * default; set system property {@code s2.log.level} ({@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR}) to
 * change it.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 콘솔 기반의 기본 로거 구현체입니다.
 * <p>
 * 별도의 로깅 프레임워크 어댑터가 설정되지 않은 경우 대체제로 사용되며, {@link System#out} 및
 * {@link System#err}를 통해 직접 로그를 출력합니다. 터미널 가독성을 높이기 위한 ANSI 컬러링
 * 기능을 포함합니다.
 * </p>
 * <p>
 * DEBUG·INFO 는 {@link System#out}, WARN·ERROR 는 {@link System#err}로 출력합니다. 최소 레벨은 기본 INFO 이며,
 * 시스템 속성 {@code s2.log.level}({@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR})로 바꿀 수 있습니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class DefaultS2Logger implements S2Logger {

    private final String name;
    private static final AtomicBoolean NOTICE_PRINTED = new AtomicBoolean(false);

    // 출력할 최소 레벨(DEBUG=0, INFO=1, WARN=2, ERROR=3). 기본 INFO — 운영에서 DEBUG 로그가 모두 찍히지 않도록
    private static final int MIN_LEVEL = levelOf(System.getProperty("s2.log.level", "INFO"));

    // 레벨 앞의 ANSI 색은 대화형 터미널에서만 — 파일·로그 수집기(리다이렉트, Docker, CI)에 제어 문자가 남지 않도록.
    // -Ds2.log.color=true/false 로 직접 정할 수 있고, NO_COLOR 환경 변수를 따른다
    private static final boolean COLOR = detectColor();

    private static boolean detectColor() {
        String forced = System.getProperty("s2.log.color");
        if (forced != null) {
            return Boolean.parseBoolean(forced.trim());
        }
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        java.io.Console console = System.console();
        if (console == null) {
            return false; // Java 21 이하는 리다이렉트되면 null
        }
        try {
            // Java 22+ 는 리다이렉트돼도 Console 을 돌려주므로 isTerminal() 로 확인(Java 17 기준이라 리플렉션)
            return (boolean) java.io.Console.class.getMethod("isTerminal").invoke(console);
        } catch (NoSuchMethodException e) {
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static int levelOf(String level) {
        if (level == null) {
            return 1;
        }
        return switch (level.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "DEBUG", "TRACE" -> 0;
            case "WARN" -> 2;
            case "ERROR" -> 3;
            default -> 1;
        };
    }

    /**
     * Constructs a new DefaultS2Logger with the specified name.
     *
     * @param name The name of the logger | 로거 이름
     */
    public DefaultS2Logger(String name) {
        this.name = name;
    }

    @Override
    public void log(String level, String message, Object[] args) {
        int severity = levelOf(level);
        if (severity < MIN_LEVEL) {
            return;
        }
        handleLog(level, severity >= 2 ? System.err : System.out, message, args);
    }

    @Override
    public boolean isDebugEnabled() {
        return MIN_LEVEL <= 0;
    }

    @Override
    public boolean isInfoEnabled() {
        return MIN_LEVEL <= 1;
    }

    @Override
    public boolean isWarnEnabled() {
        return MIN_LEVEL <= 2;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    /**
     * Common internal logging handler.
     *
     * @param level   The log level | 로그 레벨
     * @param stream  The target PrintStream | 출력 스트림
     * @param message The message template | 메시지 템플릿
     * @param args    Arguments for the template | 템플릿 인자
     */
    private void handleLog(String level, PrintStream stream, String message, Object... args) {
        // 외부 어댑터가 없어 DefaultS2Logger로 첫 로그가 출력될 때 딱 1회만 직관적인 가이드를 안내
        if (NOTICE_PRINTED.compareAndSet(false, true)) {
            stream.println("[WARN] [s2-util] No external logger detected. Using fallback console logger.");
            stream.println("  -> Solution 1 (Recommended): Add 'org.slf4j:slf4j-api' to dependencies for automatic binding.");
            stream.println("  -> Solution 2: Call S2LogManager.setLoggerFactory(...) for custom logging. (See Javadoc for examples)");
        }

        Throwable t = null;
        var params = args;

        // 1. 마지막 인자가 Throwable인지 확인 (instanceof 패턴 매칭 적용)
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable foundThrowable) {
            t = foundThrowable;
            params = Arrays.copyOf(args, args.length - 1);
        }

        // 2. 메시지 치환 및 최종 문자열 조립
        var formattedBody = format(message, params);

        // 3. 레벨별 ANSI 색상 코드
        String ansiColor = !COLOR ? "" : switch (level != null ? level.toUpperCase(java.util.Locale.ROOT) : "") {
            case "DEBUG" -> ansiColor = "\u001B[90m"; // 회색
            case "INFO" -> ansiColor = "\u001B[32m"; // 초록색
            case "WARN" -> ansiColor = "\u001B[33m"; // 노란색
            case "ERROR" -> ansiColor = "\u001B[31m"; // 빨간색
            default -> ansiColor = "\u001B[0m"; // 기본값 (Reset)
        };
        var ansiReset = COLOR ? "\u001B[0m" : "";

        var sb = new StringBuilder();
        sb.append(ansiColor).append("[").append(level).append("] ").append(ansiReset);
        if (name != null && !name.isBlank()) {
            sb.append(name).append(" - ");
        }
        sb.append(formattedBody);

        stream.println(sb.toString());

        // 4. 예외 스택 트레이스 출력
        if (t != null) {
            t.printStackTrace(stream);
        }
    }

    /**
     * Formats the message using SLF4J-style placeholders ({}).
     *
     * @param message The message template | 메시지 템플릿
     * @param args    The arguments to substitute | 치환할 인자들
     * @return The formatted message | 포맷팅된 메시지
     */
    private String format(String message, Object... args) {
        if (message == null || args == null || args.length == 0) {
            return message;
        }

        var sb = new StringBuilder();
        var argIndex = 0;
        var lastPos = 0;
        int pos;

        while ((pos = message.indexOf("{}", lastPos)) != -1) {
            sb.append(message, lastPos, pos);
            if (argIndex < args.length) {
                sb.append(args[argIndex++]);
            } else {
                sb.append("{}");
            }
            lastPos = pos + 2;
        }
        sb.append(message.substring(lastPos));
        return sb.toString();
    }

}
