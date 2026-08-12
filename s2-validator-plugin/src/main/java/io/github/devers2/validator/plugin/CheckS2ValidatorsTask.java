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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
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
            int totalFiles = 0;
            int validatorFiles = 0;

            // 모든 Java 파일 스캔
            try (Stream<Path> paths = Files.walk(srcDir.toPath())) {
                List<Path> javaFiles = paths
                        .filter(path -> path.toString().endsWith(".java"))
                        .collect(Collectors.toList());

                totalFiles = javaFiles.size();

                for (Path javaFile : javaFiles) {
                    List<ValidationError> errors = analyzeFile(javaFile);
                    if (!errors.isEmpty()) {
                        validatorFiles++;
                        errorsByFile.put(javaFile.toString(), errors);
                    }
                }
            }

            // 결과 출력
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

                throw new IllegalStateException(
                        String.format(
                                "%d개 파일에서 총 %d개의 잘못된 필드명이 발견되었습니다.",
                                validatorFiles,
                                errorsByFile.values().stream().mapToInt(List::size).sum()
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
     * 개별 Java 파일을 파싱하여 {@code S2Validator} 호출 패턴 및 필드 유효성을 분석합니다.
     *
     * @param javaFile 분석할 Java 소스 파일 경로
     * @return 발견된 유효성 오류 목록 (오류가 없으면 빈 목록)
     */
    private List<ValidationError> analyzeFile(Path javaFile) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            String content = cu.toString();

            if (!content.contains("S2Validator")) {
                return errors;
            }

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

        } catch (Exception e) {
            getLogger().debug("파일 파싱 실패: {}", javaFile.getFileName(), e);
        }

        return errors;
    }

    /**
     * {@code .field()} 호출이 속한 체인을 거슬러 올라가 대상 DTO 클래스명을 추론합니다.
     * 명시적으로 지정된 제네릭 타입 파라미터가 있는 경우에만 유효한 클래스명을 반환합니다.
     *
     * @param fieldCall 분석할 메서드 호출 표현식
     * @return 추론된 클래스의 Full Name (추론 불가 시 null 반환)
     */
    private String findTargetClassForCall(MethodCallExpr fieldCall) {
        MethodCallExpr current = fieldCall;

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

            if (current.getScope().isPresent() && current.getScope().get() instanceof MethodCallExpr) {
                current = (MethodCallExpr) current.getScope().get();
            } else {
                break;
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
}
