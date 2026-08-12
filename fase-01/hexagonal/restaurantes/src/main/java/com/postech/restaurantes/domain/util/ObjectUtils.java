package com.postech.restaurantes.domain.util;

import java.util.Collection;

/**
 * Verificações simples de presença/existência de objetos.
 * Complementa java.util.Objects com checagens de texto em branco e coleções,
 * centralizando essas guardas em um único ponto do sistema.
 */
public final class ObjectUtils {

    private ObjectUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /** Lança IllegalArgumentException com a mensagem dada se o valor for nulo. */
    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /** Lança IllegalArgumentException com a mensagem dada se o texto estiver em branco. */
    public static String requireNonBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
