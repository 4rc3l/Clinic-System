package com.clinic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The usual HMO coverage: how much of the service fee the HMO card takes off
 * the bill, depending on which HMO the patient's card is from.
 *
 * To change a rate, or to add another HMO, edit the table below. A provider that
 * isn't in the table gets DEFAULT_PERCENT.
 */
public final class HmoProvider {

    /** Reduction (in %) for an HMO that isn't listed below. */
    public static final int DEFAULT_PERCENT = 50;

    private static final Map<String, Integer> PERCENT = new LinkedHashMap<>();
    static {
        PERCENT.put("Maxicare", 80);
        PERCENT.put("Medicard", 75);
        PERCENT.put("Intellicare", 70);
        PERCENT.put("PhilCare", 70);
        PERCENT.put("Cocolife", 65);
        PERCENT.put("Pacific Cross", 65);
        PERCENT.put("Avega", 60);
    }

    private HmoProvider() { }

    /** The listed HMO names, for hints. */
    public static Iterable<String> names() { return PERCENT.keySet(); }

    /** True when the name matches a listed HMO. */
    public static boolean isKnown(String provider) { return lookup(provider) != null; }

    /** Reduction in percent for this HMO card (DEFAULT_PERCENT when it isn't listed). */
    public static int percentFor(String provider) {
        Integer p = lookup(provider);
        return p == null ? DEFAULT_PERCENT : p;
    }

    /** The peso amount the HMO takes off a price, rounded to centavos. Zero when no provider is given. */
    public static BigDecimal discountFor(String provider, BigDecimal price) {
        if (provider == null || provider.trim().isEmpty() || price == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(percentFor(provider)))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static Integer lookup(String provider) {
        if (provider == null) return null;
        String key = provider.trim().replaceAll("\\s+", " ");
        for (Map.Entry<String, Integer> e : PERCENT.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }
}