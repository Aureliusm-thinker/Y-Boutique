package com.yvette.api.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

final class Slugs {

    private Slugs() {}

    /** "Linen Wrap Dress — Sand" → "linen-wrap-dress-sand". */
    static String fromTitle(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() > 150) slug = slug.substring(0, 150).replaceAll("-$", "");
        return slug.isEmpty() ? "item" : slug;
    }

    /** Appends -2, -3, … until {@code taken} says the slug is free. */
    static String unique(String base, Predicate<String> taken) {
        String candidate = base;
        for (int i = 2; taken.test(candidate); i++) {
            candidate = base + "-" + i;
        }
        return candidate;
    }
}
