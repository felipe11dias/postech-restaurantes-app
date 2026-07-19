package com.postech.restaurantes.util;

import java.text.Normalizer;

/**
 * Funções utilitárias para normalização de texto, usadas de forma transversal
 * pelo sistema (VOs, services). Classe final, não instanciável.
 */
public final class TextUtils {

    private TextUtils() {
    }

    /** Remove espaços nas pontas e colapsa espaços internos repetidos. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    /** Normaliza e converte para minúsculas (útil para e-mails, logins). */
    public static String toLowerNormalized(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    /** Remove acentos e diacríticos (ex.: "São Paulo" -> "Sao Paulo"). */
    public static String removeAccents(String value) {
        if (value == null) {
            return null;
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /** Mantém apenas dígitos (útil para CEP, telefone, documentos). */
    public static String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
