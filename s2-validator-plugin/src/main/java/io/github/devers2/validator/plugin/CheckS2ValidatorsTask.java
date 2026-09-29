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
package io.github.devers2.validator.plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle Task that performs static analysis on source code to validate {@code S2Validator} field names
 * and chaining completeness.
 * <p>
 * This task uses JavaParser to analyze the AST (Abstract Syntax Tree) of source code.
 * It identifies {@code .field("fieldName")} call patterns and verifies if the specified
 * field actually exists in the target DTO class. Additionally, it detects incomplete
 * validator chains where terminal methods ({@code .validate()} / {@code .build()}) are missing.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 소스 코드를 정적 분석하여 {@code S2Validator}의 필드명 유효성과 체이닝 완결성을 검증하는 Gradle Task입니다.
 * <p>
 * JavaParser를 사용하여 소스 코드의 AST(Abstract Syntax Tree)를 분석하고,
 * {@code .field("fieldName")} 호출 패턴을 찾아 대상 DTO 클래스에 해당 필드가
 * 실제로 존재하는지 확인합니다. 또한 종단 메서드({@code .validate()} / {@code .build()})가
 * 누락된 불완전 체이닝을 감지하여 빌드를 실패시킵니다.
 * </p>
 *
 * <b>Key Features (주요 특징)</b>
 * <ul>
 * <li><b>Target Type Inference:</b> Uses the explicit type argument ({@code S2Validator.<Dto>builder()}) or the declared type of the {@code of(dto)} argument; skips {@code ?}, {@code Object}, JDK types ({@code Map}) and unknown types. | 명시적 타입 인자 또는 {@code of(dto)} 인자의 선언 타입으로 대상 추론. {@code ?}, {@code Object}, JDK 타입({@code Map}), 알 수 없는 타입은 생략</li>
 * <li><b>Incremental &amp; Cacheable:</b> Inputs are the {@code src/main/java} trees of all projects; the task is UP-TO-DATE or restored from the build cache when they are unchanged. | 모든 프로젝트의 {@code src/main/java}가 입력이며, 바뀌지 않으면 UP-TO-DATE 또는 빌드 캐시에서 복원</li>
 * <li><b>Inheritance Support:</b> Includes fields from parent classes in the validation. | 상속받은 부모 클래스의 필드까지 포함하여 검증</li>
 * <li><b>Multi-Project Support:</b> Searches for DTOs across all subprojects within the root project. | 멀티 프로젝트 환경 지원</li>
 * <li><b>Performance Optimization:</b> Caches analyzed DTO field information for faster subsequent checks. | DTO 필드 정보 캐싱을 통한 성능 최적화</li>
 * <li><b>Chaining Completeness Check:</b> Detects incomplete validator chains (e.g. missing terminal {@code .validate()} or {@code .build()}) and fails the build. | 종단 메서드({@code .validate()}/{@code .build()}) 누락 등 불완전 체이닝을 감지하여 빌드 실패 처리</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
@CacheableTask
public abstract class CheckS2ValidatorsTask extends DefaultTask {

    // ANSI 제어 문자를 사용한 로그 색상 정의
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";

    /**
     * 검증기 바인딩 호출({@code S2BindValidator.bind(...)}) 뒤에 이어져도 정상으로 취급하는 메서드 이름들.
     * {@code validate}뿐 아니라 {@code getRulesJson}(클라이언트 공유용 JSON 규칙 조회)도 문서화된 정상
     * 사용 패턴이라 포함한다 — 이걸 빠뜨리면 GET 폼 렌더링에서 흔히 쓰는
     * {@code S2BindValidator.bind(...).getRulesJson()} 패턴이 오탐 처리된다.
     */
    private static final Set<String> TERMINAL_CALL_NAMES = Set.of("validate", "getRulesJson");

    /** Field list cache per analyzed DTO class for performance enhancement | 분석된 DTO 클래스별 필드 목록 캐시 */
    private final Map<String, Set<String>> fieldCache = new LinkedHashMap<>();

    /** List of DTOs for which analysis results have already been logged to prevent log overflow | 로그 오버플로우 방지를 위해 이미 분석 결과를 출력한 DTO 목록 */
    private final Set<String> loggedDTOs = new HashSet<>();

    /** Target classes whose source was not found, logged once each | 소스를 찾지 못해 한 번씩만 기록한 대상 클래스 */
    private final Set<String> skippedDTOs = new HashSet<>();

    /** java.lang types usable without an import; never DTOs in the scanned sources | import 없이 쓰는 java.lang 타입. 스캔 소스의 DTO 가 아님 */
    private static final Set<String> JAVA_LANG_TYPES = Set.of(
            "Object", "String", "CharSequence", "Number", "Integer", "Long", "Short", "Byte", "Double", "Float",
            "Boolean", "Character");

    /**
     * Constructs a new {@code CheckS2ValidatorsTask} and sets the Gradle task group and description.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code CheckS2ValidatorsTask} 객체를 생성하고 Gradle 태스크 그룹 및 설명을 설정합니다.
     */
    public CheckS2ValidatorsTask() {
        setGroup("verification");
        setDescription("소스 코드를 정적 분석하여 S2Validator 필드명 유효성을 검증합니다.");

        // Capture project paths at configuration time; Project must not be accessed in @TaskAction under the configuration cache. | configuration cache 에서는 @TaskAction 안에서 Project 에 접근할 수 없으므로 프로젝트 경로를 설정 시점에 수집
        Project project = getProject();
        getProjectDirectory().convention(project.getLayout().getProjectDirectory());
        getSourceRoots().convention(project.provider(() -> project.getRootProject().getAllprojects().stream()
                .map(p -> p.file("src/main/java"))
                .collect(Collectors.toList())));
        // Every scanned tree is an input: checked files and DTO lookups across projects. | 스캔하는 모든 트리가 입력: 검사 대상 파일과 프로젝트 간 DTO 조회
        getSourceFiles().from(getSourceRoots());
        getResultFile().convention(project.getLayout().getBuildDirectory().file("s2-validator/checkS2Validators.txt"));
    }

    /**
     * The source trees read by the check, declared as inputs so the task is skipped when nothing changed.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검사가 읽는 소스 트리입니다. 입력으로 선언되어 바뀐 것이 없으면 태스크를 건너뜁니다.
     *
     * @return The input source files | 입력 소스 파일
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @IgnoreEmptyDirectories
    public abstract ConfigurableFileCollection getSourceFiles();

    /**
     * Summary of the last successful check; the task output that enables up-to-date checks and the build cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 마지막으로 성공한 검사의 요약입니다. 최신 상태 판정과 빌드 캐시를 가능하게 하는 태스크 출력입니다.
     *
     * @return The result file property | 결과 파일 속성
     */
    @OutputFile
    public abstract RegularFileProperty getResultFile();

    /**
     * The directory of the project being checked; its {@code src/main/java} is scanned.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검사 대상 프로젝트 디렉터리이며, 그 아래 {@code src/main/java}를 스캔합니다.
     *
     * @return The project directory property | 프로젝트 디렉터리 속성
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * The {@code src/main/java} directories of all projects in the build, searched to resolve DTO classes.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * DTO 클래스를 찾기 위해 탐색하는 빌드 내 모든 프로젝트의 {@code src/main/java} 디렉터리 목록입니다.
     *
     * @return The source roots property | 소스 루트 목록 속성
     */
    @Internal
    public abstract ListProperty<File> getSourceRoots();

    /**
     * Entry point for the Gradle Task execution.
     * <p>
     * Scans all Java files in the project to identify {@code S2Validator} configuration errors.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Gradle Task의 실제 실행 진입점입니다.
     * 프로젝트 내의 모든 Java 파일을 스캔하여 {@code S2Validator} 설정 오류를 찾아냅니다.
     *
     * @throws IllegalStateException If any invalid field names or incomplete chaining is found | 유효하지 않은 필드명 또는 불완전한 체이닝이 발견된 경우 빌드 실패
     */
    @TaskAction
    public void checkValidators() {
        getLogger().lifecycle("🔍 소스 코드 정적 분석 시작 (JavaParser)...");
        // Parse with the Java 17 baseline so records and other modern syntax are understood. | record 등 최신 문법을 해석하도록 Java 17 기준으로 파싱
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        try {
            File projectDir = getProjectDirectory().get().getAsFile();

            // src/main/java 경로
            File srcDir = new File(projectDir, "src/main/java");
            if (!srcDir.exists()) {
                getLogger().info("ℹ️  src/main/java 디렉토리가 없습니다. 검증 생략.");
                writeResult(0, Map.of(), projectDir);
                return;
            }

            Map<String, List<ValidationError>> errorsByFile = new LinkedHashMap<>();
            Map<String, List<ChainingError>> chainingErrorsByFile = new LinkedHashMap<>();
            Map<String, List<BindValidatorWarning>> bindWarningsByFile = new LinkedHashMap<>();
            int totalFiles = 0;
            int validatorFiles = 0;

            // 모든 Java 파일 스캔
            try (Stream<Path> paths = Files.walk(srcDir.toPath())) {
                List<Path> javaFiles = paths
                        .filter(path -> path.toString().endsWith(".java"))
                        .collect(Collectors.toList());

                totalFiles = javaFiles.size();

                for (Path javaFile : javaFiles) {
                    FileAnalysisResult result = analyzeFile(javaFile);
                    if (!result.fieldErrors.isEmpty()) {
                        validatorFiles++;
                        errorsByFile.put(javaFile.toString(), result.fieldErrors);
                    }
                    if (!result.chainingErrors.isEmpty()) {
                        chainingErrorsByFile.put(javaFile.toString(), result.chainingErrors);
                    }
                    if (!result.bindValidatorWarnings.isEmpty()) {
                        bindWarningsByFile.put(javaFile.toString(), result.bindValidatorWarnings);
                    }
                }
            }

            // 필드명 오류 결과 출력
            if (errorsByFile.isEmpty()) {
                getLogger().lifecycle(
                        ANSI_GREEN + ANSI_BOLD + "✅ [S2Validator Field Check Success] " + ANSI_RESET + "{}개 파일 스캔 완료",
                        totalFiles);
            } else {
                getLogger().error("");
                getLogger().error(ANSI_RED + ANSI_BOLD + "[S2Validator Field Check Error]" + ANSI_RESET);
                getLogger().error(ANSI_RED + "❌ {}개 파일에서 잘못된 필드명이 발견되었습니다." + ANSI_RESET, validatorFiles);

                errorsByFile.forEach((file, errors) -> {
                    Path relativePath = projectDir.toPath().relativize(Path.of(file));
                    getLogger().error("");
                    getLogger().error("  📄 " + ANSI_BOLD + "{}" + ANSI_RESET, relativePath);
                    errors.forEach(
                            error -> getLogger().error(
                                    "    " + ANSI_YELLOW + "⚠️  Line {}:" + ANSI_RESET + " '{}' (메서드: {}) 필드가 "
                                            + ANSI_CYAN + "{}" + ANSI_RESET + "에 없습니다",
                                    error.lineNumber, error.fieldName, error.methodName, error.targetClass));
                });
                getLogger().error("");
            }

            // 체이닝 완결성 오류 결과 출력
            if (!chainingErrorsByFile.isEmpty()) {
                int totalChainingErrors = chainingErrorsByFile.values().stream().mapToInt(List::size).sum();
                getLogger().error("");
                getLogger().error(ANSI_RED + ANSI_BOLD + "[S2Validator Chaining Error]" + ANSI_RESET);
                getLogger().error(
                        ANSI_RED + "🚫 {}개 파일에서 종단 메서드 누락으로 인한 '죽은 코드(Dead Code)'가 {}건 발견되었습니다." + ANSI_RESET,
                        chainingErrorsByFile.size(), totalChainingErrors);
                getLogger().error(ANSI_RED + "   체이닝이 완결되지 않으면 검증 로직이 실제로 실행되지 않습니다!" + ANSI_RESET);

                chainingErrorsByFile.forEach((file, chainingErrors) -> {
                    Path relativePath = projectDir.toPath().relativize(Path.of(file));
                    getLogger().error("");
                    getLogger().error("  📄 " + ANSI_BOLD + "{}" + ANSI_RESET, relativePath);
                    chainingErrors.forEach(
                            err -> getLogger().error(
                                    "    " + ANSI_RED + "🚫 Line {}:" + ANSI_RESET
                                            + " S2Validator.{}() 체인이 .{}()로 끝나지 않았습니다 (죽은 코드)",
                                    err.lineNumber, err.starterMethod, err.expectedTerminal));
                });
                getLogger().error("");
            } else {
                getLogger().lifecycle(ANSI_GREEN + ANSI_BOLD + "✅ [S2Validator Chaining Check Success] " + ANSI_RESET
                        + "체이닝 완결성 검사 통과");
            }

            // S2BindValidator.bind(...)로 바인딩한 검증기가 validate()/getRulesJson() 없이 버려지는 것으로 의심되는 지점 경고 (빌드는 막지 않음)
            logBindValidatorWarnings(bindWarningsByFile, projectDir);

            boolean hasFatalErrors = !errorsByFile.isEmpty() || !chainingErrorsByFile.isEmpty();
            if (hasFatalErrors) {
                int fieldErrorCount = errorsByFile.values().stream().mapToInt(List::size).sum();
                int chainingErrorCount = chainingErrorsByFile.values().stream().mapToInt(List::size).sum();
                List<String> messages = new ArrayList<>();
                if (fieldErrorCount > 0)
                    messages.add(String.format("%d개의 잘못된 필드명", fieldErrorCount));
                if (chainingErrorCount > 0)
                    messages.add(String.format("%d개의 불완전한 체이닝(Dead Code)", chainingErrorCount));
                throw new IllegalStateException(
                        String.format("S2Validator 정적 분석 실패: %s 발견되었습니다.", String.join(", ", messages)));
            }

            writeResult(totalFiles, bindWarningsByFile, projectDir);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            getLogger().error("검증 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("S2Validator 필드명 검증 실패", e);
        }
    }

    /**
     * Writes the result file (task output). Warnings are kept here because an UP-TO-DATE run does not log them again.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 결과 파일(태스크 출력)을 씁니다. UP-TO-DATE 실행에서는 경고가 다시 출력되지 않으므로 여기에 남깁니다.
     *
     * @param totalFiles     Number of scanned files | 스캔한 파일 수
     * @param warningsByFile Bind usage warnings by file | 파일별 bind 사용 경고
     * @param projectDir     Project directory for relative paths | 상대 경로 기준 프로젝트 디렉터리
     * @throws java.io.IOException If writing fails | 쓰기 실패 시
     */
    private void writeResult(int totalFiles, Map<String, List<BindValidatorWarning>> warningsByFile, File projectDir)
            throws java.io.IOException {
        StringBuilder sb = new StringBuilder("S2Validator check passed: ").append(totalFiles).append(" files\n");
        warningsByFile.forEach((file, warnings) -> warnings.forEach(w -> sb.append("WARN ")
                .append(projectDir.toPath().relativize(Path.of(file)).toString().replace('\\', '/'))
                .append(':').append(w.lineNumber).append(" bind(").append(w.target).append(") - ").append(w.reason)
                .append('\n')));
        Path out = getResultFile().get().getAsFile().toPath();
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 발견된 {@link BindValidatorWarning}들을 파일별로 정리하여 경고 로그로 출력합니다.
     * 필드명 오류와 달리 이 검사는 위임(다른 메서드로 전달) 여부를 완전히 판별할 수 없는 휴리스틱이라
     * 빌드를 실패시키지 않고 경고만 남깁니다.
     *
     * @param warningsByFile 파일 경로별 경고 목록
     * @param projectDir     경로 상대화를 위한 프로젝트 디렉터리
     */
    private void logBindValidatorWarnings(Map<String, List<BindValidatorWarning>> warningsByFile, File projectDir) {
        if (warningsByFile.isEmpty()) {
            return;
        }

        int totalWarnings = warningsByFile.values().stream().mapToInt(list -> list.size()).sum();
        getLogger().warn("");
        getLogger().warn(ANSI_YELLOW + ANSI_BOLD + "[S2BindValidator Usage Warning]" + ANSI_RESET);
        getLogger().warn(
                ANSI_YELLOW
                        + "⚠️  {}개 파일에서 validate()/getRulesJson() 호출이 확인되지 않는 S2BindValidator.bind(...) 사용이 {}건 발견되었습니다 (빌드는 계속 진행됩니다)."
                        + ANSI_RESET,
                warningsByFile.size(), totalWarnings);

        warningsByFile.forEach((file, warnings) -> {
            Path relativePath = projectDir.toPath().relativize(Path.of(file));
            getLogger().warn("");
            getLogger().warn("  📄 " + ANSI_BOLD + "{}" + ANSI_RESET, relativePath);
            warnings.forEach(
                    w -> getLogger().warn(
                            "    " + ANSI_YELLOW + "⚠️  Line {}:" + ANSI_RESET + " bind({}) - {}",
                            w.lineNumber, w.target, w.reason));
        });
        getLogger().warn("");
    }

    /**
     * 개별 Java 파일을 파싱하여 {@code S2Validator} 필드명 유효성, 체이닝 완결성, {@code S2BindValidator} 사용 패턴을 분석합니다.
     *
     * @param javaFile 분석할 Java 소스 파일 경로
     * @return 필드명 오류, 체이닝 완결성 오류, S2BindValidator 경고를 함께 담은 결과 (없으면 빈 목록들)
     */
    private FileAnalysisResult analyzeFile(Path javaFile) {
        List<ValidationError> errors = new ArrayList<>();
        List<ChainingError> chainingErrors = new ArrayList<>();
        List<BindValidatorWarning> bindWarnings = new ArrayList<>();

        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            String content = cu.toString();

            if (content.contains("S2Validator")) {
                // 모든 .field/.when/.and("fieldName") 호출 찾기
                List<MethodCallExpr> fieldCalls = cu.findAll(
                        MethodCallExpr.class, call -> ("field".equals(call.getNameAsString())
                                || "when".equals(call.getNameAsString())
                                || "and".equals(call.getNameAsString()))
                                && !call.getArguments().isEmpty()
                                && call.getArguments().get(0) instanceof StringLiteralExpr);

                for (MethodCallExpr fieldCall : fieldCalls) {
                    String targetClassName = findTargetClassForCall(fieldCall);
                    if (targetClassName == null || "Object".equals(targetClassName)) {
                        continue;
                    }

                    Set<String> validFieldNames = getAllFieldNames(targetClassName);
                    if (validFieldNames == null) {
                        // Log once per class; inferred of(dto) targets make repeats common. | of(dto) 추론으로 반복이 잦으므로 클래스당 한 번만 기록
                        if (skippedDTOs.add(targetClassName)) {
                            getLogger().lifecycle("⚠️ DTO 소스를 찾을 수 없어 검증을 건너뜁니다: {} (파일: {})", targetClassName,
                                    javaFile.getFileName());
                        }
                        continue;
                    }

                    String fieldName = ((StringLiteralExpr) fieldCall.getArguments().get(0)).getValue();
                    String baseName = extractBaseName(fieldName);

                    if (!validFieldNames.contains(baseName)) {
                        errors.add(
                                new ValidationError(
                                        fieldName,
                                        targetClassName,
                                        fieldCall.getNameAsString(),
                                        fieldCall.getBegin().map(pos -> pos.line).orElse(0)));
                    }
                }

                // 체이닝 완결성 검사
                chainingErrors.addAll(analyzeChainingCompleteness(cu));
            }

            if (content.contains("S2BindValidator")) {
                bindWarnings.addAll(analyzeBindValidatorUsage(cu));
            }

        } catch (Exception e) {
            getLogger().debug("파일 파싱 실패: {}", javaFile.getFileName(), e);
        }

        return new FileAnalysisResult(errors, chainingErrors, bindWarnings);
    }

    /**
     * 파일 내의 모든 {@code S2Validator} 체인 시작점({@code of()}, {@code builder()}, {@code check()})을 찾아
     * 종단 메서드({@code validate()} / {@code build()})로 끝나지 않는 불완전 체이닝을 감지합니다.
     *
     * <p>
     * <b>탐지 전략:</b>
     * </p>
     * <ol>
     * <li>파일 전체에서 {@code S2Validator.of(...)}, {@code S2Validator.builder()},
     *     {@code S2Validator.check(...)} 호출을 찾는다.</li>
     * <li>각 시작 호출의 "최상위 체이닝 호출"을 찾는다.
     *     (예: {@code S2Validator.of(x).field("a").validate()} 에서 {@code validate()} 가 최상위)</li>
     * <li>최상위 호출이 이미 종단 메서드인 경우 정상으로 판정한다.</li>
     * <li>최상위 호출이 문장(ExpressionStmt)에 직접 포함되어 있고 종단 메서드가 아니면 불완전 체이닝으로 간주한다.</li>
     * <li>체인이 변수에 저장된 경우, 같은 스코프 안에서 해당 변수에 대해 종단 메서드 호출이 있는지 확인한다.</li>
     * </ol>
     *
     * @param cu 분석 대상 파일의 CompilationUnit
     * @return 발견된 불완전 체이닝 오류 목록 (없으면 빈 목록)
     */
    private List<ChainingError> analyzeChainingCompleteness(CompilationUnit cu) {
        List<ChainingError> errors = new ArrayList<>();

        // S2Validator.of / S2Validator.builder / S2Validator.check 호출 찾기
        List<MethodCallExpr> starterCalls = cu.findAll(MethodCallExpr.class, this::isChainingStarterCall);

        for (MethodCallExpr starterCall : starterCalls) {
            String starterName = starterCall.getNameAsString();
            String expectedTerminal = "builder".equals(starterName) ? "build" : "validate";

            // 이 시작 호출의 최상위(Outermost) 체이닝 호출을 찾는다
            MethodCallExpr outermost = findOutermostChainedCall(starterCall);

            // 최상위 호출이 이미 종단 메서드인 경우 → 정상
            if (expectedTerminal.equals(outermost.getNameAsString())) {
                continue;
            }

            // 최상위 호출이 ExpressionStmt (독립 구문)에 직접 속하는지 확인
            Node parent = outermost.getParentNode().orElse(null);
            if (parent instanceof ExpressionStmt) {
                int line = starterCall.getBegin().map(pos -> pos.line).orElse(0);
                errors.add(new ChainingError(starterName, expectedTerminal, line));
                continue;
            }

            // 체인이 변수에 저장된 경우 (VariableDeclarator 또는 AssignExpr)
            String variableName = null;
            if (parent instanceof VariableDeclarator declarator) {
                variableName = declarator.getNameAsString();
            } else if (parent instanceof AssignExpr assignExpr
                    && assignExpr.getTarget() instanceof NameExpr targetExpr) {
                variableName = targetExpr.getNameAsString();
            }

            if (variableName != null) {
                // 같은 스코프(메서드/생성자/람다) 안에서 변수.validate() 또는 변수.build()가 호출되는지 확인
                Node scopeNode = findEnclosingCallableBody(starterCall);
                if (scopeNode != null && !isVariableTerminalCalled(scopeNode, variableName, expectedTerminal)) {
                    int line = starterCall.getBegin().map(pos -> pos.line).orElse(0);
                    errors.add(new ChainingError(starterName, expectedTerminal, line));
                }
            }
        }

        return errors;
    }

    /**
     * 주어진 메서드 호출이 {@code S2Validator} 체이닝의 시작점인지 확인합니다.
     * ({@code S2Validator.of(...)}, {@code S2Validator.builder()}, {@code S2Validator.check(...)})
     *
     * @param call 검사할 메서드 호출 표현식
     * @return 체이닝 시작점이면 true
     */
    private boolean isChainingStarterCall(MethodCallExpr call) {
        if (call.getScope().isEmpty()) {
            return false;
        }
        String scope = call.getScope().get().toString();
        String name = call.getNameAsString();
        return scope.endsWith("S2Validator") && ("of".equals(name) || "builder".equals(name) || "check".equals(name));
    }

    /**
     * 주어진 체이닝 시작 호출로부터 체인 최상위(최외곽) {@link MethodCallExpr}를 찾습니다.
     *
     * @param startCall 체인 시작 호출
     * @return 최상위 체이닝 호출
     */
    private MethodCallExpr findOutermostChainedCall(MethodCallExpr startCall) {
        MethodCallExpr current = startCall;
        while (true) {
            Node parent = current.getParentNode().orElse(null);
            if (parent instanceof MethodCallExpr parentCall && parentCall.getScope().isPresent()
                    && isAncestorOf(parentCall.getScope().get(), current)) {
                current = parentCall;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * {@code candidate}가 {@code potentialDescendant}와 동일한지 확인합니다.
     */
    private boolean isAncestorOf(Node candidate, Node potentialDescendant) {
        return candidate == potentialDescendant;
    }

    /**
     * 지정된 스코프 노드 안에서 {@code variableName.expectedTerminal(...)}이 호출되는지 확인합니다.
     *
     * @param scopeNode        탐색할 스코프 (메서드/생성자/람다 본문)
     * @param variableName     변수명
     * @param expectedTerminal 기대하는 종단 메서드 이름 (validate 또는 build)
     * @return 종단 메서드 호출이 있으면 true
     */
    private boolean isVariableTerminalCalled(Node scopeNode, String variableName, String expectedTerminal) {
        return !scopeNode.findAll(
                MethodCallExpr.class,
                call -> expectedTerminal.equals(call.getNameAsString())
                        && call.getScope().isPresent()
                        && call.getScope().get() instanceof NameExpr nameExpr
                        && variableName.equals(nameExpr.getNameAsString()))
                .isEmpty();
    }

    /**
     * 검증기 바인딩 호출 지점({@code S2BindValidator.bind(...)})을 찾아, 그 결과에서 {@code validate()}나
     * {@code getRulesJson()}이 호출되는지 확인합니다.
     * <p>
     * 다음 세 가지 경우를 구분합니다:
     * </p>
     * <ol>
     * <li>{@code .bind(...).validate(...)}처럼 바로 체이닝됨 → 정상.</li>
     * <li>변수에 담긴 뒤 같은 메서드/생성자/람다 안에서 {@code 변수.validate(...)}가 호출됨 → 정상.</li>
     * <li>그 외(반환값을 그냥 버림, 변수에 담고 다시는 참조하지 않음, 변수에 담았지만 validate() 호출을 못 찾음)
     * → 경고 대상.</li>
     * </ol>
     * 결과가 다른 메서드의 인자로 전달되거나 {@code return}되는 경우는 호출된 곳에서 검증할 수도 있어
     * 판단하지 않고 건너뜁니다(과잉 오탐 방지). 파일 하나를 벗어난 연결까지는 추적하지 않습니다.
     *
     * @param cu 탐색 대상 파일의 CompilationUnit
     * @return 발견된 경고 목록 (없으면 빈 목록)
     */
    private List<BindValidatorWarning> analyzeBindValidatorUsage(CompilationUnit cu) {
        List<BindValidatorWarning> warnings = new ArrayList<>();

        List<MethodCallExpr> acquisitionCalls = cu.findAll(MethodCallExpr.class, this::isValidatorAcquisitionCall);

        for (MethodCallExpr acquisitionCall : acquisitionCalls) {
            String target = describeBoundValidator(acquisitionCall);
            String calledAs = acquisitionCall.getNameAsString() + "(...)";
            int line = acquisitionCall.getBegin().map(pos -> pos.line).orElse(0);
            Node parent = acquisitionCall.getParentNode().orElse(null);

            if (parent instanceof MethodCallExpr outerCall
                    && outerCall.getScope().isPresent()
                    && outerCall.getScope().get() == acquisitionCall) {
                // 직접 체이닝: .bind(...) 뒤에 뭔가 더 호출됨
                if (!TERMINAL_CALL_NAMES.contains(outerCall.getNameAsString())) {
                    warnings.add(
                            new BindValidatorWarning(
                                    target, line,
                                    calledAs + " 뒤에 validate()/getRulesJson()이 아닌 " + outerCall.getNameAsString()
                                            + "()가 호출되었습니다."));
                }
                continue;
            }

            String variableName = null;
            if (parent instanceof VariableDeclarator declarator) {
                variableName = declarator.getNameAsString();
            } else if (parent instanceof AssignExpr assignExpr
                    && assignExpr.getTarget() instanceof NameExpr targetName) {
                variableName = targetName.getNameAsString();
            }

            if (variableName != null) {
                checkVariableValidated(variableName, acquisitionCall, target, calledAs, line, warnings);
                continue;
            }

            if (parent instanceof ExpressionStmt) {
                // 체이닝도, 변수 할당도 아닌 단독 구문 -> 반환값이 그냥 버려짐 (항상 의도치 않은 실수)
                warnings.add(
                        new BindValidatorWarning(
                                target, line,
                                calledAs + "의 반환값이 사용되지 않고 버려졌습니다. validate()를 호출해야 실제로 검증이 수행됩니다."));
            }
            // 그 외(다른 메서드의 인자로 전달, return 등)는 호출된 곳에서 검증할 수 있어 판단하지 않고 건너뜀
        }

        return warnings;
    }

    /**
     * 검증기를 Spring 환경에 바인딩하는 지점({@code S2BindValidator.bind(...)})인지 확인합니다.
     *
     * @param call 검사할 메서드 호출 표현식
     * @return 검증기 바인딩 호출이면 true
     */
    private boolean isValidatorAcquisitionCall(MethodCallExpr call) {
        if (call.getScope().isEmpty()) {
            return false;
        }
        return "bind".equals(call.getNameAsString()) && call.getScope().get().toString().endsWith("S2BindValidator");
    }

    /**
     * 지정된 변수 이름이 속한 메서드/생성자/람다 본문 안에서 {@code 변수.validate(...)} 호출을 찾습니다.
     * 없으면, 그 변수가 아예 다시 참조되지 않는지(확실한 실수) 아니면 다른 방식으로 쓰이는지(위임 가능성 있음)에
     * 따라 다른 문구의 경고를 추가합니다.
     *
     * @param variableName    검증기 획득 호출 결과가 담긴 변수 이름
     * @param acquisitionCall 원본 검증기 획득 호출 (탐색 범위 결정용)
     * @param target          바인딩한 검증기 표현식(표시용)
     * @param calledAs        원본 호출의 표시용 문자열 (예: {@code "bind(...)"})
     * @param line            원본 호출의 소스 라인 번호
     * @param warnings        경고를 누적할 리스트
     */
    private void checkVariableValidated(String variableName, MethodCallExpr acquisitionCall, String target,
            String calledAs, int line, List<BindValidatorWarning> warnings) {
        Node scopeNode = findEnclosingCallableBody(acquisitionCall);
        if (scopeNode == null) {
            return;
        }

        boolean validated = !scopeNode.findAll(
                MethodCallExpr.class,
                call -> TERMINAL_CALL_NAMES.contains(call.getNameAsString())
                        && call.getScope().isPresent()
                        && call.getScope().get() instanceof NameExpr scopeName
                        && variableName.equals(scopeName.getNameAsString()))
                .isEmpty();
        if (validated) {
            return;
        }

        boolean referencedElsewhere = scopeNode.findAll(NameExpr.class).stream()
                .anyMatch(nameExpr -> variableName.equals(nameExpr.getNameAsString()));

        if (!referencedElsewhere) {
            warnings.add(
                    new BindValidatorWarning(
                            target, line,
                            calledAs + " 결과가 변수 '" + variableName + "'에 저장된 후 어디에서도 사용되지 않았습니다."));
        } else {
            warnings.add(
                    new BindValidatorWarning(
                            target, line,
                            "변수 '" + variableName + "'(" + calledAs + ")에서 validate()/getRulesJson() 호출을 찾지 못했습니다. "
                                    + "(다른 메서드/파일에 위임했다면 무시해도 됩니다)"));
        }
    }

    /** {@code bind(...)} 호출의 첫 번째 인자(바인딩한 검증기 표현식)를 표시용 문자열로 반환합니다. 없으면 "?"를 반환합니다. */
    private String describeBoundValidator(MethodCallExpr bindCall) {
        if (bindCall.getArguments().isEmpty()) {
            return "?";
        }
        String text = bindCall.getArguments().get(0).toString();
        return text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    /** 주어진 노드를 감싸는 가장 가까운 메서드/생성자/람다 본문을 찾습니다(변수 사용처 탐색 범위 결정용). */
    private Node findEnclosingCallableBody(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof MethodDeclaration || current instanceof ConstructorDeclaration
                    || current instanceof LambdaExpr) {
                return current;
            }
            current = current.getParentNode().orElse(null);
        }
        return null;
    }

    /**
     * {@code .field()} 호출이 속한 체인을 거슬러 올라가 대상 DTO 클래스명을 추론합니다.
     * <ul>
     * <li>명시적 타입 인자: {@code S2Validator.<Dto>builder()}, {@code S2Validator.<Dto>of(x)}</li>
     * <li>타입 추론: {@code S2Validator.of(dto)}는 인자의 선언 타입(메서드·람다 파라미터, 지역 변수, 필드,
     * {@code var x = new Dto()}, {@code new Dto()}, 캐스트)에서 DTO 를 찾습니다. {@code builder()}는 Java 가 체인 앞쪽의
     * 타입을 대입 대상에서 추론하지 않으므로 타입 인자가 필수입니다.</li>
     * <li>{@code ?}, {@code Object}, JDK 타입({@code Map} 등), 판별할 수 없는 타입은 검사하지 않습니다.</li>
     * </ul>
     * <p>
     * {@code S2Validator.builder()}가 변수에 담겨 여러 문장으로 나뉘어 사용된 경우
     * (예: {@code var b = S2Validator.<Dto>builder(); b.field(...)}), 체이닝이 변수에서
     * 끊긴 지점을 감지하여 같은 이름의 변수 선언을 찾아 그 초기화식으로 추적을 이어간다.
     * 이 방식이 없으면 체인이 끊기는 순간 검증이 조용히 생략된다.
     *
     * @param fieldCall 분석할 메서드 호출 표현식
     * @return 추론된 클래스의 Full Name (추론 불가 시 null 반환)
     */
    private String findTargetClassForCall(MethodCallExpr fieldCall) {
        MethodCallExpr current = fieldCall;
        // 변수 추적 시 동일 이름을 반복 방문하지 않도록 하여 순환 참조로 인한 무한 루프를 방지함
        Set<String> visitedVariables = new HashSet<>();

        while (current != null) {
            String name = current.getNameAsString();
            if ("builder".equals(name) || "of".equals(name)) {
                if (current.getScope().isPresent() && current.getScope().get().toString().endsWith("S2Validator")) {
                    CompilationUnit cu = getCU(fieldCall);
                    Optional<NodeList<Type>> typeArgs = current.getTypeArguments();
                    if (typeArgs.isPresent() && !typeArgs.get().isEmpty()) {
                        return toCheckableClassName(cu, typeArgs.get().get(0));
                    }
                    // No type argument: of(dto) infers T from its argument. | 타입 인자 없음: of(dto)는 인자에서 T 를 추론
                    if ("of".equals(name) && !current.getArguments().isEmpty()) {
                        return toCheckableClassName(cu, inferExpressionType(current.getArguments().get(0), 0));
                    }
                    return null;
                }
            }

            if (current.getScope().isEmpty()) {
                break;
            }

            var scope = current.getScope().get();
            if (scope instanceof MethodCallExpr scopedCall) {
                current = scopedCall;
            } else if (scope instanceof NameExpr nameExpr && visitedVariables.add(nameExpr.getNameAsString())) {
                current = findInitializerCall(getCU(fieldCall), nameExpr.getNameAsString());
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * Converts a type to the class name to check, or {@code null} when it cannot be a DTO in the scanned sources
     * (wildcards, primitives, arrays, {@code Object}, JDK types). Type arguments are erased ({@code Box<Item>} → Box).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 타입을 검사할 클래스명으로 바꿉니다. 스캔 소스의 DTO 일 수 없는 타입(와일드카드, 기본형, 배열, {@code Object}, JDK 타입)은
     * {@code null}을 반환하며, 타입 인자는 지웁니다({@code Box<Item>} → Box).
     *
     * @param cu   The file that uses the type | 타입을 사용하는 파일
     * @param type The type, or null | 타입 (없으면 null)
     * @return The fully qualified class name, or null | 전체 클래스명, 검사 불가 시 null
     */
    private String toCheckableClassName(CompilationUnit cu, Type type) {
        if (!(type instanceof ClassOrInterfaceType classType)) {
            return null;
        }
        String simpleName = classType.getNameWithScope();
        boolean imported = cu != null && cu.getImports().stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + simpleName));
        if (!imported && JAVA_LANG_TYPES.contains(simpleName)) {
            return null;
        }
        String fullName = resolveFullClassName(cu, simpleName);
        if (fullName == null || fullName.startsWith("java.") || fullName.startsWith("javax.")) {
            return null;
        }
        return fullName;
    }

    /**
     * Returns the declared type of an expression passed to {@code S2Validator.of(...)}, when it can be read from the
     * source alone.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code S2Validator.of(...)}에 전달된 식의 선언 타입을 소스만으로 알 수 있으면 반환합니다.
     *
     * @param expr  The argument expression | 인자 식
     * @param depth Recursion depth for {@code var} initializers | {@code var} 초기화식 추적 깊이
     * @return The type, or null when unknown | 타입, 알 수 없으면 null
     */
    private Type inferExpressionType(Expression expr, int depth) {
        if (depth > 4 || expr == null) {
            return null;
        }
        if (expr instanceof EnclosedExpr enclosed) {
            return inferExpressionType(enclosed.getInner(), depth + 1);
        }
        if (expr instanceof ObjectCreationExpr creation) {
            return creation.getType();
        }
        if (expr instanceof CastExpr cast) {
            return cast.getType();
        }
        if (expr instanceof NameExpr nameExpr) {
            return findDeclaredType(nameExpr.getNameAsString(), expr, depth);
        }
        if (expr instanceof FieldAccessExpr fieldAccess && fieldAccess.getScope() instanceof ThisExpr) {
            return findDeclaredType(fieldAccess.getNameAsString(), expr, depth);
        }
        return null;
    }

    /**
     * Finds the declared type of a variable visible at {@code from}: parameters and local variables of the enclosing
     * lambdas, methods and constructors (nearest first), then fields of the enclosing types.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code from} 위치에서 보이는 변수의 선언 타입을 찾습니다. 감싸는 람다·메서드·생성자의 파라미터와 지역 변수(가까운 순)를 먼저,
     * 그다음 감싸는 타입의 필드를 봅니다.
     *
     * @param name  The variable name | 변수 이름
     * @param from  The node that uses the variable | 변수를 사용하는 노드
     * @param depth Recursion depth for {@code var} initializers | {@code var} 초기화식 추적 깊이
     * @return The declared type, or null when not found or not explicit | 선언 타입, 없거나 명시되지 않았으면 null
     */
    private Type findDeclaredType(String name, Node from, int depth) {
        for (Node node = from.getParentNode().orElse(null); node != null; node = node.getParentNode().orElse(null)) {
            if (node instanceof LambdaExpr || node instanceof MethodDeclaration
                    || node instanceof ConstructorDeclaration) {
                NodeList<Parameter> params = node instanceof LambdaExpr lambda ? lambda.getParameters()
                        : node instanceof MethodDeclaration method ? method.getParameters()
                                : ((ConstructorDeclaration) node).getParameters();
                for (Parameter param : params) {
                    if (name.equals(param.getNameAsString())) {
                        return param.getType() instanceof UnknownType ? null : param.getType();
                    }
                }
                for (VariableDeclarator declarator : node.findAll(VariableDeclarator.class)) {
                    if (name.equals(declarator.getNameAsString())) {
                        return declaredOrInitializerType(declarator, depth);
                    }
                }
            } else if (node instanceof TypeDeclaration<?> typeDecl) {
                for (FieldDeclaration field : typeDecl.getFields()) {
                    for (VariableDeclarator declarator : field.getVariables()) {
                        if (name.equals(declarator.getNameAsString())) {
                            return declaredOrInitializerType(declarator, depth);
                        }
                    }
                }
                if (typeDecl instanceof RecordDeclaration recordDecl) {
                    for (Parameter component : recordDecl.getParameters()) {
                        if (name.equals(component.getNameAsString())) {
                            return component.getType();
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Returns the declared type, or for {@code var} the type of its initializer. | 선언 타입, {@code var}면 초기화식의 타입 */
    private Type declaredOrInitializerType(VariableDeclarator declarator, int depth) {
        if (declarator.getType() instanceof VarType) {
            return declarator.getInitializer().map(init -> inferExpressionType(init, depth + 1)).orElse(null);
        }
        return declarator.getType();
    }

    /**
     * 파일 전체에서 지정된 이름의 변수 선언을 찾아, 그 초기화식이 메서드 호출이면 반환한다.
     * {@link #findTargetClassForCall}이 변수에서 끊긴 빌더 체인을 계속 추적할 수 있도록 돕는다.
     *
     * @param cu           탐색 대상 파일의 CompilationUnit
     * @param variableName 찾을 변수 이름
     * @return 초기화식(메서드 호출), 없거나 메서드 호출이 아니면 null
     */
    private MethodCallExpr findInitializerCall(CompilationUnit cu, String variableName) {
        for (VariableDeclarator declarator : cu.findAll(VariableDeclarator.class)) {
            if (variableName.equals(declarator.getNameAsString())
                    && declarator.getInitializer().isPresent()
                    && declarator.getInitializer().get() instanceof MethodCallExpr initCall) {
                return initCall;
            }
        }
        return null;
    }

    /** 해당 노드가 속한 CompilationUnit(파일 전체 구조)을 획득하는 헬퍼 메서드 */
    private CompilationUnit getCU(com.github.javaparser.ast.Node node) {
        com.github.javaparser.ast.Node current = node;
        while (current != null && !(current instanceof CompilationUnit)) {
            current = current.getParentNode().orElse(null);
        }
        return (CompilationUnit) current;
    }

    /**
     * 지정된 클래스명을 멀티 프로젝트 내의 소스 파일에서 찾아 모든 필드명을 추출합니다.
     * 상속 관계를 분석하여 부모 클래스의 필드까지 재귀적으로 포함합니다.
     *
     * @param fullClassName 분석할 대상 클래스의 전체 이름 (패키지 포함)
     * @return 해당 클래스에서 사용 가능한 유효 필드명 집합
     */
    private Set<String> getAllFieldNames(String fullClassName) {
        if (fieldCache.containsKey(fullClassName)) {
            return fieldCache.get(fullClassName);
        }

        Set<String> names = new HashSet<>();
        String relativePath = fullClassName.replace('.', File.separatorChar) + ".java";

        // 모든 서브프로젝트의 소스 루트 순회
        File sourceFile = null;
        for (File sourceRoot : getSourceRoots().get()) {
            File potential = new File(sourceRoot, relativePath);
            if (potential.exists()) {
                sourceFile = potential;
                break;
            }
        }

        if (sourceFile == null) {
            fieldCache.put(fullClassName, null);
            return null;
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            // 모든 필드 추출
            cu.findAll(com.github.javaparser.ast.body.FieldDeclaration.class).forEach(field -> {
                field.getVariables().forEach(v -> names.add(v.getNameAsString()));
            });

            // Record components are not FieldDeclarations but are accessible fields of the DTO. | 레코드 컴포넌트는 FieldDeclaration 이 아니지만 DTO 의 필드로 접근 가능
            cu.findAll(com.github.javaparser.ast.body.RecordDeclaration.class).forEach(recordDecl -> {
                recordDecl.getParameters().forEach(param -> names.add(param.getNameAsString()));
            });

            // 상속 처리
            cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                clazz.getExtendedTypes().forEach(extendedType -> {
                    String superClassName = resolveFullClassName(cu, extendedType.getNameAsString());
                    if (!"Object".equals(superClassName) && !"java.lang.Object".equals(superClassName)) {
                        Set<String> superFields = getAllFieldNames(superClassName);
                        if (superFields != null)
                            names.addAll(superFields);
                    }
                });
            });

            fieldCache.put(fullClassName, names);

            if (loggedDTOs.add(fullClassName)) {
                getLogger().lifecycle(ANSI_GREEN + "✅ DTO 분석 완료:" + ANSI_RESET + " {} (필드: {}개)", fullClassName,
                        names.size());
            }
        } catch (Exception e) {
            getLogger().debug("DTO 분석 실패: {}", fullClassName);
            fieldCache.put(fullClassName, null);
        }

        return names;
    }

    /** 단순 클래스명을 파일의 Import 섹션이나 패키지 정보를 바탕으로 Full Qualified Name으로 변환합니다. */
    private String resolveFullClassName(CompilationUnit cu, String simpleName) {
        if (simpleName == null || simpleName.contains("."))
            return simpleName;
        if (cu == null)
            return simpleName;

        for (var importDecl : cu.getImports()) {
            String importedName = importDecl.getNameAsString();
            if (importedName.endsWith("." + simpleName))
                return importedName;
        }

        if (cu.getPackageDeclaration().isPresent()) {
            String packageName = cu.getPackageDeclaration().get().getNameAsString();
            return packageName + "." + simpleName;
        }

        return simpleName;
    }

    /** 중첩 필드(Dot)나 리스트 인덱스([])가 포함된 필드 문자열에서 실제 소유 클래스의 필드명을 추출합니다. */
    private String extractBaseName(String fieldName) {
        if (fieldName.contains("."))
            fieldName = fieldName.substring(0, fieldName.indexOf("."));
        fieldName = fieldName.replaceAll("\\[.*?\\]", "");
        return fieldName;
    }

    /** 발견된 유효성 점검 오류 정보를 담는 내부 클래스 */
    static class ValidationError {
        final String fieldName;
        final String targetClass;
        final String methodName;
        final int lineNumber;

        ValidationError(String fieldName, String targetClass, String methodName, int lineNumber) {
            this.fieldName = fieldName;
            this.targetClass = targetClass;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
    }

    /** 검증기 바인딩 호출({@code S2BindValidator.bind(...)})이 validate() 없이 버려진 것으로 의심되는 지점의 경고 정보 */
    static class BindValidatorWarning {
        final String target;
        final int lineNumber;
        final String reason;

        BindValidatorWarning(String target, int lineNumber, String reason) {
            this.target = target;
            this.lineNumber = lineNumber;
            this.reason = reason;
        }
    }

    /**
     * {@code S2Validator} 체이닝이 종단 메서드 없이 중단된 오류 정보를 담는 내부 클래스.
     * <p>
     * 예: {@code S2Validator.of(...).field(...)} — 마지막에 {@code .validate()}가 없는 경우<br>
     * 예: {@code S2Validator.builder().field(...)} — 마지막에 {@code .build()}가 없는 경우
     * </p>
     */
    static class ChainingError {
        /** 체인 시작 메서드 이름 (of / builder / check) */
        final String starterMethod;
        /** 기대되는 종단 메서드 이름 (validate / build) */
        final String expectedTerminal;
        /** 체인 시작 줄 번호 */
        final int lineNumber;

        ChainingError(String starterMethod, String expectedTerminal, int lineNumber) {
            this.starterMethod = starterMethod;
            this.expectedTerminal = expectedTerminal;
            this.lineNumber = lineNumber;
        }
    }

    /** 한 파일을 분석한 결과: 필드명 오류 목록, 체이닝 완결성 오류 목록, S2BindValidator 사용 경고 목록을 함께 담는다 */
    private static final class FileAnalysisResult {
        final List<ValidationError> fieldErrors;
        final List<ChainingError> chainingErrors;
        final List<BindValidatorWarning> bindValidatorWarnings;

        FileAnalysisResult(List<ValidationError> fieldErrors, List<ChainingError> chainingErrors,
                List<BindValidatorWarning> bindValidatorWarnings) {
            this.fieldErrors = fieldErrors;
            this.chainingErrors = chainingErrors;
            this.bindValidatorWarnings = bindValidatorWarnings;
        }
    }
}
