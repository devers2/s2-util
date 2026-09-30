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
package io.github.devers2.s2util.validation;

/**
 * Comparison operators for {@code when()}/{@code and()} conditions; the server and s2.validator.js judge them the same
 * way.
 * <p>
 * The condition field is looked up in the current row/object first, then outer objects up to the root. A missing
 * value, a blank string and an empty collection all count as <b>empty</b>. For a checkbox group (collection value),
 * {@link #EQ}/{@link #IN} mean "contains".
 * </p>
 * <ul>
 * <li>{@link #EQ}: equal ({@code when(field, value)} without an operator). {@code EQ} with {@code null} means empty.</li>
 * <li>{@link #NE}: not equal (the negation of {@code EQ}).</li>
 * <li>{@link #GT}, {@link #GTE}, {@link #LT}, {@link #LTE}: numeric comparison with the same strict number parsing as
 * {@code MIN_VALUE}; not satisfied when the value is empty, not a plain number ({@code "1,000"}) or a collection.</li>
 * <li>{@link #IN}: equal to one of the given values (a collection or array); {@link #NOT_IN} is its negation.</li>
 * <li>{@link #EMPTY}, {@link #NOT_EMPTY}: the value is empty / present; they take no value.</li>
 * </ul>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@code when()}/{@code and()} 조건의 비교 연산자입니다. 서버와 s2.validator.js 가 같은 방식으로 판정합니다.
 * <p>
 * 조건 필드는 현재 행/객체에서 먼저 찾고, 없으면 바깥 객체를 거쳐 루트까지 찾습니다. 값 없음, 공백 문자열, 빈 컬렉션은 모두
 * <b>빈 값</b>입니다. 체크박스 그룹(컬렉션 값)에서 {@link #EQ}/{@link #IN}은 "포함"을 뜻합니다.
 * </p>
 * <ul>
 * <li>{@link #EQ}: 같음 (연산자 없는 {@code when(field, value)}). {@code null}과의 {@code EQ}는 빈 값을 뜻합니다.</li>
 * <li>{@link #NE}: 같지 않음 ({@code EQ}의 반대).</li>
 * <li>{@link #GT}, {@link #GTE}, {@link #LT}, {@link #LTE}: {@code MIN_VALUE}와 같은 엄격한 숫자 판정으로 크기를 비교합니다. 값이
 * 비었거나, 일반 숫자가 아니거나({@code "1,000"}), 컬렉션이면 충족되지 않습니다.</li>
 * <li>{@link #IN}: 주어진 값(컬렉션 또는 배열) 중 하나와 같음. {@link #NOT_IN}은 그 반대입니다.</li>
 * <li>{@link #EMPTY}, {@link #NOT_EMPTY}: 값이 비었음 / 있음. 비교 값을 받지 않습니다.</li>
 * </ul>
 *
 * @author devers2
 * @since 2.0.0
 */
public enum S2Operator {
    /** Equal | 같음 */
    EQ,
    /** Not equal | 같지 않음 */
    NE,
    /** Greater than (numeric) | 초과 (숫자) */
    GT,
    /** Greater than or equal (numeric) | 이상 (숫자) */
    GTE,
    /** Less than (numeric) | 미만 (숫자) */
    LT,
    /** Less than or equal (numeric) | 이하 (숫자) */
    LTE,
    /** One of the given values | 주어진 값 중 하나 */
    IN,
    /** None of the given values | 주어진 값 중 어느 것도 아님 */
    NOT_IN,
    /** Empty (missing, blank or empty collection) | 빈 값 (없음, 공백, 빈 컬렉션) */
    EMPTY,
    /** Not empty | 값 있음 */
    NOT_EMPTY;

    /** Whether the operator compares numbers | 숫자를 비교하는 연산자인지 */
    boolean isNumeric() {
        return this == GT || this == GTE || this == LT || this == LTE;
    }

    /** Whether the operator takes a list of values | 값 목록을 받는 연산자인지 */
    boolean isList() {
        return this == IN || this == NOT_IN;
    }

    /** Whether the operator takes no value | 비교 값을 받지 않는 연산자인지 */
    boolean isUnary() {
        return this == EMPTY || this == NOT_EMPTY;
    }
}
