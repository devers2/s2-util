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

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;

/**
 * Gradle Task that performs static analysis on source code to validate {@code S2Validator} field names.
 * <p>
 * This task uses JavaParser to analyze the AST (Abstract Syntax Tree) of source code.
 * It identifies {@code .field("fieldName")} call patterns and verifies if the specified
 * field actually exists in the target DTO class.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 소스 코드를 정적 분석하여 {@code S2Validator}의 필드명 유효성을 검증하는 Gradle Task입니다.
 * <p>
 * JavaParser를 사용하여 소스 코드의 AST(Abstract Syntax Tree)를 분석하고,
 * {@code .field("fieldName")} 호출 패턴을 찾아 대상 DTO 클래스에 해당 필드가
 * 실제로 존재하는지 확인합니다.
 * </p>
 *
 * <b>Key Features (주요 특징)</b>
 * <ul>
 * <li><b>Smart Validation Skip:</b> Skips validation if the generic type is {@code ?}, {@code Object}, or omitted. | 제네릭이 {@code ?}, {@code Object}, 또는 생략된 경우 검증 생략</li>
 * <li><b>Inheritance Support:</b> Includes fields from parent classes in the validation. | 상속받은 부모 클래스의 필드까지 포함하여 검증</li>
 * <li><b>Multi-Project Support:</b> Searches for DTOs across all subprojects within the root project. | 멀티 프로젝트 환경 지원</li>
 * <li><b>Performance Optimization:</b> Caches analyzed DTO field information for faster subsequent checks. | DTO 필드 정보 캐싱을 통한 성능 최적화</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
@DisableCachingByDefault(because = "입출력 파일이 선언되지 않은 채 멀티 프로젝트 소스 전체를 직접 스캔하므로 캐시할 수 없다.")
public class CheckS2ValidatorsTask extends DefaultTask {

    // ANSI 제어 문자를 사용한 로그 색상 정의
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";

    /**
     * 검증기 획득 호출(context/getOrRegister/getValidator) 뒤에 이어져도 정상으로 취급하는 메서드 이름들.
     * {@code validate}뿐 아니라 {@code getRulesJson}(클라이언트 공유용 JSON 규칙 조회)도 문서화된 정상
     * 사용 패턴이라 포함한다 — 이걸 빠뜨리면 GET 폼 렌더링에서 흔히 쓰는
     * {@code S2BindValidator.context(...).getRulesJson()} 패턴이 오탐 처리된다.
     */
    private static final Set<String> TERMINAL_CALL_NAMES = Set.of("validate", "getRulesJson");

    /** Field list cache per analyzed DTO class for performance enhancement | 분석된 DTO 클래스별 필드 목록 캐시 */
    private final Map<String, Set<String>> fieldCache = new LinkedHashMap<>();

    /** List of DTOs for which analysis results have already been logged to prevent log overflow | 로그 오버플로우 방지를 위해 이미 분석 결과를 출력한 DTO 목록 */
    private final Set<String> loggedDTOs = new HashSet<>();

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
    }

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
     * @throws IllegalStateException If any invalid field names are found | 유효하지 않은 필드명이 발견된 경우 빌드 실패
     */
    @TaskAction
    public void checkValidators() {
        getLogger().lifecycle("🔍 소스 코드 정적 분석 시작 (JavaParser)...");

        try {
            Project project = getProject();

            // src/main/java 경로
            File srcDir = project.file("src/main/java");
            if (!srcDir.exists()) {
                getLogger().info("ℹ️  src/main/java 디렉토리가 없습니다. 검증 생략.");
                return;
            }

            Map<String, List<ValidationError>> errorsByFile = new LinkedHashMap<>();
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
                    if (!result.bindValidatorWarnings.isEmpty()) {
                        bindWarningsByFile.put(javaFile.toString(), result.bindValidatorWarnings);
                    }
                }
            }

            // 필드명 오류 결과 출력
            if (errorsByFile.isEmpty()) {
                getLogger().lifecycle(ANSI_GREEN + ANSI_BOLD + "✅ [S2Validator Field Check Success] " + ANSI_RESET + "{}개 파일 스캔 완료", totalFiles);
            } else {
                getLogger().error("");
                getLogger().error(ANSI_RED + ANSI_BOLD + "[S2Validator Field Check Error]" + ANSI_RESET);
                getLogger().error(ANSI_RED + "❌ {}개 파일에서 잘못된 필드명이 발견되었습니다." + ANSI_RESET, validatorFiles);

                errorsByFile.forEach((file, errors) -> {
                    Path relativePath = project.getProjectDir().toPath().relativize(Path.of(file));
                    getLogger().error("");
                    getLogger().error("  📄 " + ANSI_BOLD + "{}" + ANSI_RESET, relativePath);
                    errors.forEach(
                            error -> getLogger().error(
                                    "    " + ANSI_YELLOW + "⚠️  Line {}:" + ANSI_RESET + " '{}' (메서드: {}) 필드가 " + ANSI_CYAN + "{}" + ANSI_RESET + "에 없습니다",
                                    error.lineNumber, error.fieldName, error.methodName, error.targetClass
                            )
                    );
                });
                getLogger().error("");
            }

            // S2BindValidator/S2ValidatorFactory로 얻은 검증기가 validate() 없이 버려지는 것으로 의심되는 지점 경고 (빌드는 막지 않음)
            logBindValidatorWarnings(bindWarningsByFile, project);

            if (!errorsByFile.isEmpty()) {
                throw new IllegalStateException(
                        String.format(
                                "%d개 파일에서 총 %d개의 잘못된 필드명이 발견되었습니다.",
                                validatorFiles,
                                errorsByFile.values().stream().mapToInt(list -> list.size()).sum()
                        )
                );
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            getLogger().error("검증 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("S2Validator 필드명 검증 실패", e);
        }
    }

    /**
     * 발견된 {@link BindValidatorWarning}들을 파일별로 정리하여 경고 로그로 출력합니다.
     * 필드명 오류와 달리 이 검사는 위임(다른 메서드로 전달) 여부를 완전히 판별할 수 없는 휴리스틱이라
     * 빌드를 실패시키지 않고 경고만 남깁니다.
     *
     * @param warningsByFile 파일 경로별 경고 목록
     * @param project        경로 상대화를 위한 프로젝트 인스턴스
     */
    private void logBindValidatorWarnings(Map<String, List<BindValidatorWarning>> warningsByFile, Project project) {
        if (warningsByFile.isEmpty()) {
            return;
        }

        int totalWarnings = warningsByFile.values().stream().mapToInt(list -> list.size()).sum();
        getLogger().warn("");
        getLogger().warn(ANSI_YELLOW + ANSI_BOLD + "[S2BindValidator Usage Warning]" + ANSI_RESET);
        getLogger().warn(
                ANSI_YELLOW + "⚠️  {}개 파일에서 validate() 호출이 확인되지 않는 검증기 획득(S2BindValidator.context / S2ValidatorFactory.getOrRegister,getValidator) 사용이 {}건 발견되었습니다 (빌드는 계속 진행됩니다)."
                        + ANSI_RESET,
                warningsByFile.size(), totalWarnings
        );

        warningsByFile.forEach((file, warnings) -> {
            Path relativePath = project.getProjectDir().toPath().relativize(Path.of(file));
            getLogger().warn("");
            getLogger().warn("  📄 " + ANSI_BOLD + "{}" + ANSI_RESET, relativePath);
            warnings.forEach(
                    w -> getLogger().warn(
                            "    " + ANSI_YELLOW + "⚠️  Line {}:" + ANSI_RESET + " context(\"{}\") - {}",
                            w.lineNumber, w.contextKey, w.reason
                    )
            );
        });
        getLogger().warn("");
    }

    /**
     * 개별 Java 파일을 파싱하여 {@code S2Validator} 필드명 유효성과 {@code S2BindValidator} 사용 패턴을 분석합니다.
     *
     * @param javaFile 분석할 Java 소스 파일 경로
     * @return 필드명 오류와 S2BindValidator 경고를 함께 담은 결과 (둘 다 없으면 빈 목록들)
     */
    private FileAnalysisResult analyzeFile(Path javaFile) {
        List<ValidationError> errors = new ArrayList<>();
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
                                && call.getArguments().get(0) instanceof StringLiteralExpr
                );

                for (MethodCallExpr fieldCall : fieldCalls) {
                    String targetClassName = findTargetClassForCall(fieldCall);
                    if (targetClassName == null || "Object".equals(targetClassName)) {
                        continue;
                    }

                    Set<String> validFieldNames = getAllFieldNames(targetClassName);
                    if (validFieldNames == null) {
                        getLogger().lifecycle("⚠️ DTO 소스를 찾을 수 없어 검증을 건너뜁니다: {} (파일: {})", targetClassName, javaFile.getFileName());
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
                                        fieldCall.getBegin().map(pos -> pos.line).orElse(0)
                                )
                        );
                    }
                }
            }

            if (content.contains("S2BindValidator") || content.contains("S2ValidatorFactory")) {
                bindWarnings.addAll(analyzeBindValidatorUsage(cu));
            }

        } catch (Exception e) {
            getLogger().debug("파일 파싱 실패: {}", javaFile.getFileName(), e);
        }

        return new FileAnalysisResult(errors, bindWarnings);
    }

    /**
     * 검증기 획득 호출 지점({@code S2BindValidator.context(...)} 등)을 찾아, 그 결과에서 {@code validate()}가
     * 호출되는지 확인합니다.
     * <p>
     * 검증기를 얻는 두 가지 경로를 모두 다룹니다: {@code S2BindValidator.context(...)}(Spring 연동용)와
     * {@code S2ValidatorFactory.getOrRegister(...)}/{@code getValidator(...)}(팩토리에서 직접 얻어
     * {@code S2Validator}에 바로 {@code validate()}를 호출하는 경로). 어느 쪽이든 판단 로직은 동일하며,
     * 다음 세 가지 경우를 구분합니다:
     * </p>
     * <ol>
     * <li>{@code .context(...).validate(...)}처럼 바로 체이닝됨 → 정상.</li>
     * <li>변수에 담긴 뒤 같은 메서드/생성자/람다 안에서 {@code 변수.validate(...)}가 호출됨 → 정상.</li>
     * <li>그 외(반환값을 그냥 버림, 변수에 담고 다시는 참조하지 않음, 변수에 담았지만 validate() 호출을 못 찾음)
     * → 경고 대상.</li>
     * </ol>
     * 결과가 다른 메서드의 인자로 전달되거나 {@code return}되는 경우는 호출된 곳에서 검증할 수도 있어
     * 판단하지 않고 건너뜁니다(과잉 오탐 방지). {@code getValidator(...)}로 조회만 하고 등록은 다른
     * 파일/메서드에서 이루어지는 경우처럼, 파일 하나를 벗어난 연결까지는 추적하지 않습니다.
     *
     * @param cu 탐색 대상 파일의 CompilationUnit
     * @return 발견된 경고 목록 (없으면 빈 목록)
     */
    private List<BindValidatorWarning> analyzeBindValidatorUsage(CompilationUnit cu) {
        List<BindValidatorWarning> warnings = new ArrayList<>();

        List<MethodCallExpr> acquisitionCalls = cu.findAll(MethodCallExpr.class, this::isValidatorAcquisitionCall);

        for (MethodCallExpr acquisitionCall : acquisitionCalls) {
            String contextKey = extractContextKey(acquisitionCall);
            String calledAs = acquisitionCall.getNameAsString() + "(...)";
            int line = acquisitionCall.getBegin().map(pos -> pos.line).orElse(0);
            Node parent = acquisitionCall.getParentNode().orElse(null);

            if (parent instanceof MethodCallExpr outerCall
                    && outerCall.getScope().isPresent()
                    && outerCall.getScope().get() == acquisitionCall) {
                // 직접 체이닝: .context(...)/.getOrRegister(...)/.getValidator(...) 뒤에 뭔가 더 호출됨
                if (!TERMINAL_CALL_NAMES.contains(outerCall.getNameAsString())) {
                    warnings.add(
                            new BindValidatorWarning(
                                    contextKey, line,
                                    calledAs + " 뒤에 validate()/getRulesJson()이 아닌 " + outerCall.getNameAsString() + "()가 호출되었습니다."
                            )
                    );
                }
                continue;
            }

            String variableName = null;
            if (parent instanceof VariableDeclarator declarator) {
                variableName = declarator.getNameAsString();
            } else if (parent instanceof AssignExpr assignExpr && assignExpr.getTarget() instanceof NameExpr targetName) {
                variableName = targetName.getNameAsString();
            }

            if (variableName != null) {
                checkVariableValidated(variableName, acquisitionCall, contextKey, calledAs, line, warnings);
                continue;
            }

            if (parent instanceof ExpressionStmt) {
                // 체이닝도, 변수 할당도 아닌 단독 구문 -> 반환값이 그냥 버려짐 (항상 의도치 않은 실수)
                warnings.add(
                        new BindValidatorWarning(
                                contextKey, line,
                                calledAs + "의 반환값이 사용되지 않고 버려졌습니다. validate()를 호출해야 실제로 검증이 수행됩니다."
                        )
                );
            }
            // 그 외(다른 메서드의 인자로 전달, return 등)는 호출된 곳에서 검증할 수 있어 판단하지 않고 건너뜀
        }

        return warnings;
    }

    /**
     * 검증기를 "얻는" 지점인지 확인합니다: {@code S2BindValidator.context(...)} 또는
     * {@code S2ValidatorFactory.getOrRegister(...)}/{@code getValidator(...)}.
     *
     * @param call 검사할 메서드 호출 표현식
     * @return 검증기 획득 호출이면 true
     */
    private boolean isValidatorAcquisitionCall(MethodCallExpr call) {
        if (call.getScope().isEmpty()) {
            return false;
        }
        String scope = call.getScope().get().toString();
        String name = call.getNameAsString();
        if ("context".equals(name) && scope.endsWith("S2BindValidator")) {
            return true;
        }
        return ("getOrRegister".equals(name) || "getValidator".equals(name)) && scope.endsWith("S2ValidatorFactory");
    }

    /**
     * 지정된 변수 이름이 속한 메서드/생성자/람다 본문 안에서 {@code 변수.validate(...)} 호출을 찾습니다.
     * 없으면, 그 변수가 아예 다시 참조되지 않는지(확실한 실수) 아니면 다른 방식으로 쓰이는지(위임 가능성 있음)에
     * 따라 다른 문구의 경고를 추가합니다.
     *
     * @param variableName    검증기 획득 호출 결과가 담긴 변수 이름
     * @param acquisitionCall 원본 검증기 획득 호출 (탐색 범위 결정용)
     * @param contextKey      획득 호출의 첫 번째 인자(컨텍스트 키)
     * @param calledAs        원본 호출의 표시용 문자열 (예: {@code "context(...)"})
     * @param line            원본 호출의 소스 라인 번호
     * @param warnings        경고를 누적할 리스트
     */
    private void checkVariableValidated(String variableName, MethodCallExpr acquisitionCall, String contextKey, String calledAs, int line, List<BindValidatorWarning> warnings) {
        Node scopeNode = findEnclosingCallableBody(acquisitionCall);
        if (scopeNode == null) {
            return;
        }

        boolean validated = !scopeNode.findAll(
                MethodCallExpr.class,
                call -> TERMINAL_CALL_NAMES.contains(call.getNameAsString())
                        && call.getScope().isPresent()
                        && call.getScope().get() instanceof NameExpr scopeName
                        && variableName.equals(scopeName.getNameAsString())
        ).isEmpty();
        if (validated) {
            return;
        }

        boolean referencedElsewhere = scopeNode.findAll(NameExpr.class).stream()
                .anyMatch(nameExpr -> variableName.equals(nameExpr.getNameAsString()));

        if (!referencedElsewhere) {
            warnings.add(
                    new BindValidatorWarning(
                            contextKey, line,
                            calledAs + " 결과가 변수 '" + variableName + "'에 저장된 후 어디에서도 사용되지 않았습니다."
                    )
            );
        } else {
            warnings.add(
                    new BindValidatorWarning(
                            contextKey, line,
                            "변수 '" + variableName + "'(" + calledAs + ")에서 validate()/getRulesJson() 호출을 찾지 못했습니다. "
                                    + "(다른 메서드/파일에 위임했다면 무시해도 됩니다)"
                    )
            );
        }
    }

    /** {@code context(...)} 호출의 첫 번째 인자(문자열 리터럴인 contextKey)를 추출합니다. 리터럴이 아니면 "?"를 반환합니다. */
    private String extractContextKey(MethodCallExpr contextCall) {
        if (!contextCall.getArguments().isEmpty() && contextCall.getArguments().get(0) instanceof StringLiteralExpr strExpr) {
            return strExpr.getValue();
        }
        return "?";
    }

    /** 주어진 노드를 감싸는 가장 가까운 메서드/생성자/람다 본문을 찾습니다(변수 사용처 탐색 범위 결정용). */
    private Node findEnclosingCallableBody(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof MethodDeclaration || current instanceof ConstructorDeclaration || current instanceof LambdaExpr) {
                return current;
            }
            current = current.getParentNode().orElse(null);
        }
        return null;
    }

    /**
     * {@code .field()} 호출이 속한 체인을 거슬러 올라가 대상 DTO 클래스명을 추론합니다.
     * 명시적으로 지정된 제네릭 타입 파라미터가 있는 경우에만 유효한 클래스명을 반환합니다.
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
                    Optional<NodeList<Type>> typeArgs = current.getTypeArguments();
                    if (typeArgs.isPresent() && !typeArgs.get().isEmpty()) {
                        Type typeArg = typeArgs.get().get(0);
                        String typeString = typeArg.toString().trim();

                        // ?, Object 또는 와일드카드 타입은 검증 스킵
                        if (typeString.equals("?") || typeString.equals("Object") ||
                                typeString.startsWith("? extends") || typeString.startsWith("? super")) {
                            return null;
                        }

                        return resolveFullClassName(getCU(fieldCall), typeArg.toString());
                    }
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

        // 모든 서브프로젝트 순회
        Set<Project> allProjects = getProject().getRootProject().getAllprojects();

        File sourceFile = null;
        for (Project p : allProjects) {
            File potential = p.file("src/main/java/" + relativePath);
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
                getLogger().lifecycle(ANSI_GREEN + "✅ DTO 분석 완료:" + ANSI_RESET + " {} (필드: {}개)", fullClassName, names.size());
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

    /** 검증기 획득 호출(예: {@code S2BindValidator.context(...)}, {@code S2ValidatorFactory.getOrRegister(...)})이 validate() 없이 버려진 것으로 의심되는 지점의 경고 정보 */
    static class BindValidatorWarning {
        final String contextKey;
        final int lineNumber;
        final String reason;

        BindValidatorWarning(String contextKey, int lineNumber, String reason) {
            this.contextKey = contextKey;
            this.lineNumber = lineNumber;
            this.reason = reason;
        }
    }

    /** 한 파일을 분석한 결과: 필드명 오류 목록과 S2BindValidator 사용 경고 목록을 함께 담는다 */
    private static final class FileAnalysisResult {
        final List<ValidationError> fieldErrors;
        final List<BindValidatorWarning> bindValidatorWarnings;

        FileAnalysisResult(List<ValidationError> fieldErrors, List<BindValidatorWarning> bindValidatorWarnings) {
            this.fieldErrors = fieldErrors;
            this.bindValidatorWarnings = bindValidatorWarnings;
        }
    }
}
