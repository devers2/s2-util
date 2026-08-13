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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin for static analysis and validation of S2Validator field names.
 * <p>
 * This plugin uses JavaParser to perform static analysis on source code. It verifies that
 * field names used in {@code S2Validator.field("fieldName")} calls actually exist
 * within the targeted DTO classes, preventing runtime errors caused by typos or refactoring.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Validator 필드명 검증을 위한 Gradle 플러그인입니다.
 * <p>
 * JavaParser를 사용하여 소스 코드를 정적 분석하고, {@code S2Validator.field("fieldName")} 호출에서
 * 사용된 필드명이 실제 DTO 클래스에 존재하는지 검증하여 오타나 리팩토링으로 인한 런타임 에러를 방지합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2ValidatorPlugin implements Plugin<Project> {

    /**
     * Default constructor for S2ValidatorPlugin.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2ValidatorPlugin의 기본 생성자입니다.
     */
    public S2ValidatorPlugin() {
    }

    /**
     * Applies the plugin to the specified project.
     * <p>
     * Registers the {@code checkS2Validators} task and configures dependencies on
     * standard Gradle tasks like {@code check}, {@code compileJava}, and {@code JavaExec}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 프로젝트에 플러그인을 적용합니다.
     *
     * @param project The Gradle project | Gradle 프로젝트 인스턴스
     */
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("S2 Validator Plugin applied");

        // checkS2Validators Task 등록
        project.getTasks().register("checkS2Validators", CheckS2ValidatorsTask.class);

        // 'check'/'compileJava'는 java 플러그인이 등록하므로, 이 플러그인이 java보다 먼저(또는
        // 없이) 적용되면 named("check")/named("compileJava")가 UnknownTaskException으로 즉시
        // 실패한다. withPlugin으로 java 플러그인이 적용된 시점(이미 적용됐으면 즉시, 나중에
        // 적용되면 그때)에 의존성을 구성하여 build.gradle의 plugins{} 선언 순서와 무관하게
        // 동작하도록 한다 | 'check'/'compileJava' are registered by the java plugin, so calling
        // named(...) eagerly would crash with UnknownTaskException if this plugin is applied
        // before (or without) java. withPlugin defers the wiring until java is present — firing
        // immediately if already applied, or later when it is — making this independent of
        // plugin declaration order in the consumer's build.gradle.
        project.getPluginManager().withPlugin("java", appliedPlugin -> {
            // 'check' 태스크 실행 시 자동으로 검증 수행
            project.getTasks().named("check").configure(task -> task.dependsOn("checkS2Validators"));

            // 'compileJava' 태스크 실행 전 자동으로 검증 수행 (빌드, 실행 등 컴파일이 필요한 모든 경우 포함)
            project.getTasks().named("compileJava").configure(task -> task.dependsOn("checkS2Validators"));
        });

        // 'bootRun' 등 JavaExec 타입의 실행 태스크 실행 전에도 검증 수행 (withType은 지연 조회라 순서 무관하게 안전함)
        project.getTasks().withType(org.gradle.api.tasks.JavaExec.class).configureEach(task -> task.dependsOn("checkS2Validators"));
    }
}
