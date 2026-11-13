package com.yvette.api.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Storefront product filtering, sorting and typo-tolerant search (FR-SF-02/03/04) in one SQL query.
 *
 * <p>Variant-level filters (size, colour, in stock) apply to the same variant, so "M + Black + in stock"
 * only matches products that have a black M in stock. Each search word must match the product's text
 * (title, category, material, occasion, style tags, colours) either as a substring ("dres" → "Dress")
 * or by trigram similarity ("dreses" → "Dress").
 */
@Component
public class CatalogSearch {

    public enum Sort { NEWEST, PRICE_ASC, PRICE_DESC, RELEVANCE }

    public record Filters(String category, String query, List<String> sizes, List<String> colours,
                          BigDecimal minPrice, BigDecimal maxPrice, boolean inStockOnly, Sort sort) {

        boolean hasQuery() {
            return query != null && !words(query).isEmpty();
        }
    }

    public record Result(List<Long> productIds, long total) {}

    /** How close a misspelt word must be to a word in the product text (0–1). */
    private static final double WORD_SIMILARITY = 0.45;
    private static final int MAX_WORDS = 6;

    private static final String SEARCH_TEXT = """
            lower(p.title || ' ' || coalesce(c.name, '') || ' ' || coalesce(p.material, '') || ' '
                  || coalesce(p.occasion, '') || ' ' || coalesce(p.fit, '') || ' '
                  || array_to_string(p.style_tags, ' ') || ' ' || coalesce(v.colour, ''))""";

    private final JdbcClient jdbc;

    public CatalogSearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Result search(Filters f, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String matching = matchingProductsSql(f, params);

        long total = jdbc.sql("SELECT count(*) FROM (" + matching + ") m").params(params)
                .query(Long.class).single();

        String order = switch (effectiveSort(f)) {
            case PRICE_ASC -> "m.price_from ASC, m.id DESC";
            case PRICE_DESC -> "m.price_from DESC, m.id DESC";
            case RELEVANCE -> "m.relevance DESC, m.created_at DESC, m.id DESC";
            case NEWEST -> "m.created_at DESC, m.id DESC";
        };
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<Long> ids = jdbc.sql("SELECT m.id FROM (" + matching + ") m ORDER BY " + order
                        + " LIMIT :limit OFFSET :offset")
                .params(params).query(Long.class).list();
        return new Result(ids, total);
    }

    /** Sizes, colours and price range available among live products (optionally in one category). */
    public record Facets(List<String> sizes, List<String> colours, BigDecimal minPrice, BigDecimal maxPrice) {}

    public Facets facets(String category) {
        String where = "p.status = 'ACTIVE'" + (category == null || category.isBlank() ? "" : " AND c.slug = :category");
        String from = " FROM product_variants v JOIN products p ON p.id = v.product_id"
                + " LEFT JOIN categories c ON c.id = p.category_id WHERE " + where;
        Map<String, Object> params = category == null || category.isBlank() ? Map.of() : Map.of("category", category);
        List<String> sizes = jdbc.sql("SELECT v.size, min(v.position) AS pos" + from
                        + " AND v.size IS NOT NULL GROUP BY v.size ORDER BY " + SIZE_ORDER + ", v.size")
                .params(params).query((rs, n) -> rs.getString(1)).list();
        List<String> colours = jdbc.sql("SELECT DISTINCT initcap(lower(v.colour))" + from
                        + " AND v.colour IS NOT NULL ORDER BY 1")
                .params(params).query(String.class).list();
        record Range(BigDecimal min, BigDecimal max) {}
        Range range = jdbc.sql("SELECT min(v.price), max(v.price)" + from).params(params)
                .query((rs, n) -> new Range(rs.getBigDecimal(1), rs.getBigDecimal(2))).single();
        return new Facets(sizes, colours, range.min(), range.max());
    }

    /** Clothing sizes in their natural order, anything else (e.g. "One size", "38") after. */
    private static final String SIZE_ORDER = """
            CASE upper(v.size) WHEN 'XXS' THEN 1 WHEN 'XS' THEN 2 WHEN 'S' THEN 3 WHEN 'M' THEN 4
              WHEN 'L' THEN 5 WHEN 'XL' THEN 6 WHEN 'XXL' THEN 7 ELSE 100 + min(v.position) END""";

    private static Sort effectiveSort(Filters f) {
        if (f.sort() != null && (f.sort() != Sort.RELEVANCE || f.hasQuery())) return f.sort();
        return f.hasQuery() ? Sort.RELEVANCE : Sort.NEWEST;
    }

    private static String matchingProductsSql(Filters f, Map<String, Object> params) {
        List<String> where = new ArrayList<>();
        where.add("p.status = 'ACTIVE'");
        if (f.category() != null && !f.category().isBlank()) {
            where.add("c.slug = :category");
            params.put("category", f.category());
        }
        if (f.sizes() != null && !f.sizes().isEmpty()) {
            where.add("upper(v.size) IN (:sizes)");
            params.put("sizes", f.sizes().stream().map(s -> s.toUpperCase(Locale.ROOT)).toList());
        }
        if (f.colours() != null && !f.colours().isEmpty()) {
            where.add("lower(v.colour) IN (:colours)");
            params.put("colours", f.colours().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList());
        }
        if (f.inStockOnly()) {
            where.add("coalesce(i.on_hand - i.reserved, 0) > 0");
        }

        String relevance = "0";
        if (f.hasQuery()) {
            List<String> words = words(f.query());
            List<String> scores = new ArrayList<>();
            for (int n = 0; n < words.size(); n++) {
                String w = "w" + n;
                params.put(w, words.get(n));
                params.put(w + "like", "%" + escapeLike(words.get(n)) + "%");
                where.add("(" + SEARCH_TEXT + " LIKE :" + w + "like OR word_similarity(:" + w + ", " + SEARCH_TEXT
                        + ") >= " + WORD_SIMILARITY + ")");
                scores.add("word_similarity(:" + w + ", lower(p.title))");
            }
            relevance = "max(" + String.join(" + ", scores) + ")";
        }

        List<String> having = new ArrayList<>();
        if (f.minPrice() != null) {
            having.add("min(v.price) >= :minPrice");
            params.put("minPrice", f.minPrice());
        }
        if (f.maxPrice() != null) {
            having.add("min(v.price) <= :maxPrice");
            params.put("maxPrice", f.maxPrice());
        }

        return "SELECT p.id, p.created_at, min(v.price) AS price_from, " + relevance + " AS relevance"
                + " FROM products p"
                + " JOIN product_variants v ON v.product_id = p.id"
                + " LEFT JOIN categories c ON c.id = p.category_id"
                + " LEFT JOIN inventory_levels i ON i.variant_id = v.id"
                + " WHERE " + String.join(" AND ", where)
                + " GROUP BY p.id, p.created_at"
                + (having.isEmpty() ? "" : " HAVING " + String.join(" AND ", having));
    }

    static List<String> words(String query) {
        return java.util.Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isBlank())
                .distinct()
                .limit(MAX_WORDS)
                .toList();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
