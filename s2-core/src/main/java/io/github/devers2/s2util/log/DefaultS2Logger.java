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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Default console-based logger implementation.
 * <p>
 * This logger acts as a zero-dependency fallback that outputs directly to
 * {@link System#out} and {@link System#err}. It features rich ANSI coloring
 * for better readability in terminals and a diagnostic setup guide.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 콘솔 기반의 기본 로거 구현체입니다.
 * <p>
 * 별도의 로깅 프레임워크 어댑터가 설정되지 않은 경우 대체제로 사용되며, {@link System#out} 및
 * {@link System#err}를 통해 직접 로그를 출력합니다. 터미널 가독성을 높이기 위한 ANSI 컬러링
 * 기능과 어댑터 설정 누락 시 가이드를 제공하는 경고 시스템을 포함합니다.
 * </p>
 *
 * <p>
 * <b>[WARNING]</b>
 * </p>
 * If you see the "S2Logger NOT CONFIGURED" banner, it means the library is using
 * synchronous system output which might impact performance. It is highly
 * recommended to configure a proper logging adapter (e.g., SLF4J).
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class DefaultS2Logger implements S2Logger {

    private final String name;
    private static boolean adapterConfigured = false;
    private static boolean warningShown = false;

    static {
        try {
            /**
             * 클래스 로딩 시점에 시스템 인코딩 설정 적용
             * vmArgs의 -Dfile.encoding 값을 읽어 표준 출력(out/err) 스트림을 재설정한다.
             */
            String encoding = System.getProperty("file.encoding", "UTF-8");

            // FileDescriptor를 사용하여 OS 표준 출력에 새 스트림을 연결함
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, encoding));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, encoding));
        } catch (UnsupportedEncodingException e) {
            // 인코딩 설정 실패 시 기본 스트림 유지
        }
    }

    /**
     * Marks that an external logger adapter has been configured.
     * <p>
     * Once this method is called, the warning banner will no longer be displayed.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 외부 로거 어댑터가 설정되었음을 표시합니다.
     * 이 메서드가 호출되면 이후에는 어떤 경로로든 경고 배너가 출력되지 않습니다.
     */
    public synchronized static void markAdapterConfigured() {
        adapterConfigured = true;
    }

    /**
     * Constructs a new DefaultS2Logger with the specified name.
     *
     * @param name The name of the logger | 로거 이름
     */
    public DefaultS2Logger(String name) {
        this.name = name;
        printWarningBannerOnce();
    }

    /**
     * Prints the configuration missing warning banner exactly once.
     * <p>
     * The banner is not shown if an adapter has already been configured.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정 누락 경고 배너를 단 한 번만 출력합니다.
     * 단, 어댑터가 이미 설정된 경우에는 출력하지 않습니다.
     */
    public synchronized static void printWarningBannerOnce() {
        if (adapterConfigured || warningShown) {
            return;
        }
        warningShown = true;

        // 색상 상수 정의
        var RS = "\u001B[0m"; // Reset
        var BD = "\u001B[1m"; // Bold
        var BN = "\u001B[38;5;167m"; // 차분한 빨강 (배너)
        var CM = "\u001B[38;5;65m"; // 차분한 녹색 (주석)
        var AN = "\u001B[38;5;79m"; // 청록색 (어노테이션)
        var KW = "\u001B[38;5;75m"; // 파란색 (예약어/접근제한자/기본타입)
        var TP = "\u001B[38;5;79m"; // 청록색 (클래스/인터페이스 타입)
        var FN = "\u001B[38;5;221m"; // 노란색 (메서드명)
        var VR = "\u001B[38;5;153m"; // 연파랑 (변수명)
        var ST = "\u001B[38;5;173m"; // 황토색 (문자열)

        // Java 17 Text Block을 사용한 템플릿 (가독성을 위해 색상 위치에 플레이스홀더 사용)
        var template = """
                       {RS}{BN}{BD}***********************************************************************************
                       {BN}*                                                                                 *
                       {BN}*                     !!! S2Logger NOT CONFIGURED WARNING !!!                     *
                       {BN}*                                                                                 *
                       {BN}*    S2LoggerAdapter is missing. Falling back to DefaultS2Logger (System.out).    *
                       {BN}*                                                                                 *
                       {BN}***********************************************************************************
                       {RS}{CM}/**
                       {CM} * Quick Setup Guide
                       {CM} * Spring Configuration Example
                       {CM} */
                       {RS}{KW}import{RS} {TP}org.springframework.context.annotation.Configuration{RS};
                       {KW}import{RS} {TP}javax.annotation.PostConstruct{RS};
                       {KW}import{RS} {TP}io.github.devers2.s2util.log.S2LogManager{RS};
                       {KW}import{RS} {TP}io.github.devers2.s2util.log.S2Logger{RS};
                       {KW}import{RS} {TP}io.github.devers2.s2util.log.S2LoggerFactory{RS};

                       {AN}@Configuration{RS}
                       {KW}public{RS} {KW}class{RS} {TP}S2LogConfig{RS} {
                           {AN}@PostConstruct{RS}
                           {KW}public{RS} {KW}void{RS} {FN}init{RS}() {
                               {TP}S2LogManager{RS}.{FN}setLoggerFactory{RS}({KW}new{RS} {TP}S2LoggerFactory{RS}() {
                                   {AN}@Override{RS}
                                   {KW}public{RS} {TP}S2Logger{RS} {FN}getLogger{RS}({TP}String{RS} {VR}name{RS}) {
                                       {KW}final{RS} {TP}org.slf4j.Logger{RS} {VR}logger{RS} = {TP}org.slf4j.LoggerFactory{RS}.{FN}getLogger{RS}({VR}name{RS});

                                       {KW}return{RS} {KW}new{RS} {TP}S2Logger{RS}() {
                                           {AN}@Override{RS}
                                           {KW}public{RS} {KW}void{RS} {FN}log{RS}({TP}String{RS} {VR}level{RS}, {TP}String{RS} {VR}message{RS}, {TP}Object{RS}[] {VR}args{RS}) {
                                               {KW}if{RS} ({ST}"DEBUG"{RS}.{FN}equals{RS}({VR}level{RS})) {VR}logger{RS}.{FN}debug{RS}({VR}message{RS}, {VR}args{RS});
                                               {KW}else if{RS} ({ST}"INFO"{RS}.{FN}equals{RS}({VR}level{RS})) {VR}logger{RS}.{FN}info{RS}({VR}message{RS}, {VR}args{RS});
                                               {KW}else if{RS} ({ST}"WARN"{RS}.{FN}equals{RS}({VR}level{RS})) {VR}logger{RS}.{FN}warn{RS}({VR}message{RS}, {VR}args{RS});
                                               {KW}else if{RS} ({ST}"ERROR"{RS}.{FN}equals{RS}({VR}level{RS})) {VR}logger{RS}.{FN}error{RS}({VR}message{RS}, {VR}args{RS});
                                           }

                                           {AN}@Override{RS}
                                           {KW}public{RS} {KW}boolean{RS} {FN}isDebugEnabled{RS}() { {KW}return{RS} {VR}logger{RS}.{FN}isDebugEnabled{RS}(); }
                                           {AN}@Override{RS}
                                           {KW}public{RS} {KW}boolean{RS} {FN}isInfoEnabled{RS}()  { {KW}return{RS} {VR}logger{RS}.{FN}isInfoEnabled{RS}(); }
                                           {AN}@Override{RS}
                                           {KW}public{RS} {KW}boolean{RS} {FN}isWarnEnabled{RS}()  { {KW}return{RS} {VR}logger{RS}.{FN}isWarnEnabled{RS}(); }
                                           {AN}@Override{RS}
                                           {KW}public{RS} {KW}boolean{RS} {FN}isErrorEnabled{RS}()  { {KW}return{RS} {VR}logger{RS}.{FN}isErrorEnabled{RS}(); }
                                       };
                                   }

                                   {AN}@Override{RS}
                                   {KW}public{RS} <T> {TP}S2Logger{RS} {FN}getLogger{RS}({TP}Class{RS}<T> {VR}clazz{RS}) {
                                       {KW}return{RS} {FN}getLogger{RS}({VR}clazz{RS} == {KW}null{RS} ? {ST}"unknown"{RS} : {VR}clazz{RS}.{FN}getName{RS}());
                                   }
                               });
                           }
                       }
                       """;

        // 플레이스홀더를 실제 ANSI 코드로 치환
        System.out.println(
                template
                        .replace("{RS}", RS).replace("{BD}", BD)
                        .replace("{BN}", BN).replace("{CM}", CM)
                        .replace("{AN}", AN).replace("{KW}", KW)
                        .replace("{TP}", TP).replace("{FN}", FN)
                        .replace("{VR}", VR).replace("{ST}", ST)
        );
    }

    @Override
    public void log(String level, String message, Object[] args) {
        handleLog(level, System.out, message, args);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
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
        String ansiColor = switch (level != null ? level.toUpperCase() : "") {
            case "DEBUG" -> ansiColor = "\u001B[90m"; // 회색
            case "INFO" -> ansiColor = "\u001B[32m"; // 초록색
            case "WARN" -> ansiColor = "\u001B[33m"; // 노란색
            case "ERROR" -> ansiColor = "\u001B[31m"; // 빨간색
            default -> ansiColor = "\u001B[0m"; // 기본값 (Reset)
        };
        var ansiReset = "\u001B[0m";

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
