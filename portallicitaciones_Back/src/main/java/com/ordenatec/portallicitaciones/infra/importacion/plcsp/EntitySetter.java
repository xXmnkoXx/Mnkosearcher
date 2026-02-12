package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Setter por reflexión:
 * - Llama a setters si existen (setXxx)
 * - Convierte tipos básicos cuando puede
 * - Si el setter no existe o no encaja el tipo: NO rompe (devuelve false)
 *
 * Objetivo: adaptar fuente nueva a TU entity/tabla sin tocar BD.
 */
public final class EntitySetter {

    private EntitySetter() {}

    // Cache por clase para no buscar methods en cada fila
    private static final Map<Class<?>, Map<String, Method[]>> CACHE = new HashMap<>();

    public static boolean set(Object target, String setterName, Object value) {
        if (target == null || setterName == null || setterName.isBlank()) return false;
        if (value == null) return false;

        Class<?> cls = target.getClass();
        Method[] candidates = getCandidates(cls, setterName);
        if (candidates.length == 0) return false;

        for (Method m : candidates) {
            Class<?> param = m.getParameterTypes()[0];
            Object coerced = coerce(value, param);
            if (coerced == Coerce.NO_MATCH) continue;

            try {
                m.invoke(target, coerced);
                return true;
            } catch (Exception ignored) {
                // seguimos probando otros overloads si existen
            }
        }

        return false;
    }

    private static Method[] getCandidates(Class<?> cls, String setterName) {
        Map<String, Method[]> byName = CACHE.computeIfAbsent(cls, EntitySetter::scanSetters);
        return byName.getOrDefault(setterName, new Method[0]);
    }

    private static Map<String, Method[]> scanSetters(Class<?> cls) {
        Map<String, java.util.List<Method>> tmp = new HashMap<>();
        for (Method m : cls.getMethods()) {
            if (!m.getName().startsWith("set")) continue;
            if (m.getParameterCount() != 1) continue;

            tmp.computeIfAbsent(m.getName(), k -> new java.util.ArrayList<>()).add(m);
        }

        Map<String, Method[]> out = new HashMap<>();
        for (var e : tmp.entrySet()) {
            out.put(e.getKey(), e.getValue().toArray(new Method[0]));
        }
        return out;
    }

    private static final class Coerce {
        static final Object NO_MATCH = new Object();
    }

    /**
     * Convierte value al tipo del parámetro si es posible.
     * Tipos soportados:
     * - String
     * - BigDecimal
     * - LocalDate
     * - Instant
     * - Long/Integer/Double/Boolean (básicos)
     */
    private static Object coerce(Object value, Class<?> targetType) {
        if (value == null) return Coerce.NO_MATCH;

        // ya encaja
        if (targetType.isInstance(value)) return value;

        // String
        if (targetType == String.class) {
            String s = String.valueOf(value).trim();
            return s.isBlank() ? Coerce.NO_MATCH : s;
        }

        // BigDecimal
        if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal bd) return bd;
            if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            String s = String.valueOf(value).trim();
            if (s.isBlank()) return Coerce.NO_MATCH;
            try {
                return new BigDecimal(s);
            } catch (Exception ignored) {
                return Coerce.NO_MATCH;
            }
        }

        // LocalDate
        if (targetType == LocalDate.class) {
            if (value instanceof LocalDate d) return d;
            return Coerce.NO_MATCH;
        }

        // Instant
        if (targetType == Instant.class) {
            if (value instanceof Instant i) return i;
            return Coerce.NO_MATCH;
        }

        // Primitivos / wrappers
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number n) return n.intValue();
            try { return Integer.parseInt(String.valueOf(value).trim()); } catch (Exception ignored) { return Coerce.NO_MATCH; }
        }
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number n) return n.longValue();
            try { return Long.parseLong(String.valueOf(value).trim()); } catch (Exception ignored) { return Coerce.NO_MATCH; }
        }
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(String.valueOf(value).trim()); } catch (Exception ignored) { return Coerce.NO_MATCH; }
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean b) return b;
            String s = String.valueOf(value).trim().toLowerCase();
            if (s.isBlank()) return Coerce.NO_MATCH;
            if (s.equals("true") || s.equals("sí") || s.equals("si") || s.equals("1")) return true;
            if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
            return Coerce.NO_MATCH;
        }

        return Coerce.NO_MATCH;
    }
}
