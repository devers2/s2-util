package io.github.devers2.s2util.jpa;

import java.util.Collection;

import jakarta.persistence.TypedQuery;

/**
 * Step 3: Final executor with all methods
 */
public interface Executor<T> {
    Executor<T> setParameter(String key, String name, Object value, Object clause, String prefix, String suffix);

    Executor<T> setParameter(String key, String name, Object value, Object clause, String prefix);

    Executor<T> setParameter(String key, String name, Object value, Object clause);

    Executor<T> setParameter(String key, String name, Object value, Object clause, LikeMode likeMode);

    Executor<T> setOrder(String key, String sort);

    // S2Template methods
    Executor<T> bind(String key, Object value, String prefix, String suffix);

    Executor<T> bind(String key, Object value, String prefix);

    Executor<T> bind(String key, Object value);

    Executor<T> bindWhen(String key, boolean condition, Object content, String prefix, String suffix);

    Executor<T> bindWhen(String key, boolean condition, Object content, String prefix);

    Executor<T> bindWhen(String key, boolean condition, Object content);

    Executor<T> bindWhen(String key, Object presence, Object content, String prefix, String suffix);

    Executor<T> bindWhen(String key, Object presence, Object content, String prefix);

    Executor<T> bindWhen(String key, Object presence, Object content);

    Executor<T> bindIn(String key, Collection<?> values, String prefix, String suffix);

    Executor<T> bindIn(String key, Collection<?> values, String prefix);

    Executor<T> bindIn(String key, Collection<?> values);

    Executor<T> bindIn(String key, boolean condition, Collection<?> values, String prefix, String suffix);

    TypedQuery<T> build();
}
