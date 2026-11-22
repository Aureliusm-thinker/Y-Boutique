package com.yvette.api.recommend;

import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.catalog.CatalogSearch;
import com.yvette.api.catalog.CatalogSearch.Filters;
import com.yvette.api.catalog.CatalogService;
import com.yvette.api.common.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recommendations without machine learning (docs/04 AI-1, step 1).
 *
 * <p><b>For you</b> — built from one visitor's last 30 days of consented events:
 * <ul>
 *   <li>their recent <b>searches</b> are re-run, and the best matches score highest;</li>
 *   <li>products they <b>viewed, wishlisted or added to the bag</b> pull in similar products (same category,
 *       shared style tags, same occasion, similar price) and products other shoppers looked at alongside them;</li>
 *   <li>every signal fades with age (half its weight every 7 days), so today's search beats last month's.</li>
 * </ul>
 * Products the visitor already viewed, wishlisted or bagged are left out (the "Recently viewed" row and the
 * bag already show them), as are sold-out products. Anything left over is filled with popular products, which is
 * also all a brand-new visitor, or one who declined analytics, sees.
 *
 * <p><b>Also viewed</b> — for a product page: products other shoppers viewed alongside it or bought in the same
 * order (see {@link RecommendationJob}), topped up with similar products while there is little data.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    public record ForYou(boolean personalised, List<String> basedOnSearches, List<ProductSummaryDto> items) {}

    /** {@code basedOn} is "customers" when shopper behaviour drove the list, "similar" when only attributes did. */
    public record AlsoViewed(String basedOn, List<ProductSummaryDto> items) {}

    static final int LOOKBACK_DAYS = 30;
    static final double HALF_LIFE_DAYS = 7;
    private static final int MAX_EVENTS = 200;
    private static final int MAX_SEARCHES = 5;
    private static final int MAX_SEEDS = 10;
    private static final int RESULTS_PER_SEARCH = 12;

    // How strongly each kind of signal says "show me more like this".
    private static final double SEARCH_WEIGHT = 3.0;
    private static final double CART_WEIGHT = 2.5;
    private static final double WISHLIST_WEIGHT = 2.0;
    private static final double VIEW_WEIGHT = 1.0;

    /** Below this attribute similarity a product isn't "like" another. */
    private static final double MIN_SIMILARITY = 0.35;

    private final JdbcClient jdbc;
    private final CatalogSearch search;
    private final CatalogService catalog;

    public RecommendationService(JdbcClient jdbc, CatalogSearch search, CatalogService catalog) {
        this.jdbc = jdbc;
        this.search = search;
        this.catalog = catalog;
    }

    // ---------------------------------------------------------------- For you

    private record Signal(String name, String query, Long productId, double ageDays) {
        double decay() {
            return Math.pow(0.5, Math.max(0, ageDays) / HALF_LIFE_DAYS);
        }
    }

    public ForYou forYou(String visitor, Long excludeProductId, int limit) {
        Set<Long> exclude = new HashSet<>();
        if (excludeProductId != null) exclude.add(excludeProductId);
        List<Signal> signals = visitor == null || visitor.isBlank() ? List.of() : recentSignals(visitor);

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, String> searchReason = new HashMap<>();   // product → the search that scored it highest
        Map<Long, Double> searchReasonScore = new HashMap<>();

        // 1. Recent searches, newest first, one entry per distinct query.
        Map<String, Signal> searches = new LinkedHashMap<>();
        for (Signal s : signals) {
            if (!"search".equals(s.name()) || s.query() == null || s.query().isBlank()) continue;
            String key = String.join(" ", CatalogSearch.words(s.query()));
            if (!key.isEmpty() && searches.size() < MAX_SEARCHES) searches.putIfAbsent(key, s);
        }
        for (Signal s : searches.values()) {
            String query = s.query().strip();
            if (query.length() > 100) query = query.substring(0, 100);
            List<Long> hits = search.search(new Filters(null, query, List.of(), List.of(), null, null, true,
                    CatalogSearch.Sort.RELEVANCE), 0, RESULTS_PER_SEARCH).productIds();
            for (int rank = 0; rank < hits.size(); rank++) {
                Long id = hits.get(rank);
                double add = SEARCH_WEIGHT * s.decay() / (1 + 0.25 * rank);
                scores.merge(id, add, Double::sum);
                if (add > searchReasonScore.getOrDefault(id, 0.0)) {
                    searchReasonScore.put(id, add);
                    searchReason.put(id, query);
                }
            }
        }

        // 2. Products they engaged with become seeds for "more like this".
        Map<Long, Double> seeds = new HashMap<>();
        for (Signal s : signals) {
            if (s.productId() == null) continue;
            double weight = switch (s.name()) {
                case "add_to_cart" -> CART_WEIGHT;
                case "wishlist_add" -> WISHLIST_WEIGHT;
                case "product_view" -> VIEW_WEIGHT;
                default -> 0;
            };
            if (weight > 0) {
                seeds.merge(s.productId(), weight * s.decay(), Double::sum);
                exclude.add(s.productId());
            }
        }
        List<Long> topSeeds = seeds.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(MAX_SEEDS).map(Map.Entry::getKey).toList();
        Map<Long, Map<Long, Double>> neighbours = neighbours(topSeeds);
        for (Long seed : topSeeds) {
            double seedWeight = seeds.get(seed);
            neighbours.getOrDefault(seed, Map.of())
                    .forEach((other, sim) -> scores.merge(other, seedWeight * sim, Double::sum));
        }

        // 3. Rank, drop what they've already seen and anything sold out, fill up with popular products.
        List<Long> ranked = scores.entrySet().stream()
                .filter(e -> !exclude.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .limit(limit * 3L)
                .toList();
        List<ProductSummaryDto> items = new ArrayList<>(inStock(ranked, limit));
        boolean personalised = !items.isEmpty();

        Set<String> basedOn = new LinkedHashSet<>();
        for (ProductSummaryDto p : items) {
            String q = searchReason.get(p.id());
            if (q != null && basedOn.size() < 3) basedOn.add(q);
        }

        if (items.size() < limit) {
            items.forEach(p -> exclude.add(p.id()));
            items.addAll(popular(limit - items.size(), exclude));
        }
        return new ForYou(personalised, List.copyOf(basedOn), items);
    }

    private List<Signal> recentSignals(String visitor) {
        // The browser sends productId (views, wishlist) or variantId (bag); both are checked to be numbers
        // before casting, since event properties are free-form JSON.
        return jdbc.sql("""
                        SELECT e.name,
                               e.properties ->> 'query' AS query,
                               coalesce(CASE WHEN e.properties ->> 'productId' ~ '^[0-9]{1,18}$'
                                             THEN (e.properties ->> 'productId')::bigint END,
                                        v.product_id) AS product_id,
                               extract(epoch FROM (now() - e.occurred_at)) / 86400.0 AS age_days
                        FROM events e
                        LEFT JOIN product_variants v
                               ON v.id = CASE WHEN e.name = 'add_to_cart'
                                               AND e.properties ->> 'variantId' ~ '^[0-9]{1,18}$'
                                              THEN (e.properties ->> 'variantId')::bigint END
                        WHERE e.anonymous_id = :visitor
                          AND e.occurred_at > now() - make_interval(days => :days)
                          AND e.name IN ('search', 'product_view', 'add_to_cart', 'wishlist_add')
                        ORDER BY e.occurred_at DESC
                        LIMIT :max""")
                .param("visitor", visitor)
                .param("days", LOOKBACK_DAYS)
                .param("max", MAX_EVENTS)
                .query((rs, n) -> new Signal(rs.getString("name"), rs.getString("query"),
                        rs.getObject("product_id", Long.class), rs.getDouble("age_days")))
                .list();
    }

    // ---------------------------------------------------------------- Also viewed

    public AlsoViewed alsoViewed(String slug, int limit) {
        Long id = jdbc.sql("SELECT id FROM products WHERE slug = :slug AND status = 'ACTIVE'")
                .param("slug", slug).query(Long.class).optional()
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        // Hand-picked "Complete the look" products are already on the page.
        Set<Long> exclude = new HashSet<>(jdbc.sql(
                        "SELECT related_product_id FROM product_relations WHERE product_id = :id")
                .param("id", id).query(Long.class).list());
        exclude.add(id);

        Map<Long, Double> together = coOccurrence(List.of(id)).getOrDefault(id, Map.of());
        Map<Long, Double> similar = similarity(List.of(id)).getOrDefault(id, Map.of());

        // Shopper behaviour first; attribute similarity breaks ties and fills the gaps.
        Map<Long, Double> scores = new HashMap<>();
        together.forEach((other, s) -> scores.merge(other, 1 + s, Double::sum));
        similar.forEach((other, s) -> scores.merge(other, 0.5 * s, Double::sum));

        List<Long> ranked = scores.entrySet().stream()
                .filter(e -> !exclude.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .limit(limit * 3L)
                .toList();
        List<ProductSummaryDto> items = inStock(ranked, limit);
        boolean fromCustomers = items.stream().anyMatch(p -> together.containsKey(p.id()));
        return new AlsoViewed(fromCustomers ? "customers" : "similar", items);
    }

    // ---------------------------------------------------------------- Building blocks

    /** Neighbours of each seed: attribute similarity plus (half-weighted) co-occurrence, capped at 1. */
    private Map<Long, Map<Long, Double>> neighbours(List<Long> seeds) {
        Map<Long, Map<Long, Double>> out = similarity(seeds);
        coOccurrence(seeds).forEach((seed, others) -> others.forEach((other, s) ->
                out.computeIfAbsent(seed, k -> new HashMap<>()).merge(other, 0.5 * s, (a, b) -> Math.min(1, a + b))));
        return out;
    }

    /**
     * How alike two products are, 0–1: same category 0.4, shared style tags up to 0.3, same occasion 0.15,
     * price within ×1.5 either way 0.1, same material 0.05.
     */
    Map<Long, Map<Long, Double>> similarity(Collection<Long> seeds) {
        Map<Long, Map<Long, Double>> out = new HashMap<>();
        if (seeds.isEmpty()) return out;
        jdbc.sql("""
                        WITH price AS (SELECT product_id, min(price) AS p FROM product_variants GROUP BY product_id)
                        SELECT s.id AS seed, o.id AS other,
                               (CASE WHEN o.category_id = s.category_id THEN 0.4 ELSE 0 END
                                + 0.15 * least(2, cardinality(ARRAY(SELECT unnest(o.style_tags)
                                                                   INTERSECT SELECT unnest(s.style_tags))))
                                + CASE WHEN lower(o.occasion) = lower(s.occasion) THEN 0.15 ELSE 0 END
                                + CASE WHEN po.p BETWEEN ps.p / 1.5 AND ps.p * 1.5 THEN 0.1 ELSE 0 END
                                + CASE WHEN lower(o.material) = lower(s.material) THEN 0.05 ELSE 0 END) AS sim
                        FROM products s
                        JOIN products o ON o.id <> s.id AND o.status = 'ACTIVE'
                        LEFT JOIN price ps ON ps.product_id = s.id
                        LEFT JOIN price po ON po.product_id = o.id
                        WHERE s.id IN (:seeds)""")
                .param("seeds", seeds)
                .query((rs, n) -> {
                    double sim = rs.getDouble("sim");
                    if (sim >= MIN_SIMILARITY) {
                        out.computeIfAbsent(rs.getLong("seed"), k -> new HashMap<>()).put(rs.getLong("other"), sim);
                    }
                    return null;
                })
                .list();
        return out;
    }

    /** Co-occurrence scores per seed, scaled so each seed's strongest partner is 1. */
    Map<Long, Map<Long, Double>> coOccurrence(Collection<Long> seeds) {
        Map<Long, Map<Long, Double>> out = new HashMap<>();
        if (seeds.isEmpty()) return out;
        jdbc.sql("""
                        SELECT c.product_id, c.other_id,
                               c.score / max(c.score) OVER (PARTITION BY c.product_id) AS s
                        FROM product_co_occurrence c
                        JOIN products o ON o.id = c.other_id AND o.status = 'ACTIVE'
                        WHERE c.product_id IN (:seeds)""")
                .param("seeds", seeds)
                .query((rs, n) -> out.computeIfAbsent(rs.getLong(1), k -> new HashMap<>()).put(rs.getLong(2), rs.getDouble(3)))
                .list();
        return out;
    }

    /**
     * Best sellers (paid orders, last 90 days, ×5) plus distinct viewers (last 30 days); newest first on ties,
     * so a shop with no data yet shows its new arrivals.
     */
    public List<ProductSummaryDto> popular(int limit, Set<Long> exclude) {
        if (limit <= 0) return List.of();
        List<Long> ids = jdbc.sql("""
                        SELECT p.id
                        FROM products p
                        LEFT JOIN (SELECT ol.product_id, sum(ol.quantity - ol.returned_quantity) AS units
                                   FROM order_lines ol JOIN orders o ON o.id = ol.order_id
                                   WHERE o.paid_at > now() - interval '90 days' AND o.status <> 'CANCELLED'
                                   GROUP BY ol.product_id) sold ON sold.product_id = p.id
                        LEFT JOIN (SELECT properties ->> 'productId' AS pid, count(DISTINCT anonymous_id) AS viewers
                                   FROM events
                                   WHERE name = 'product_view' AND occurred_at > now() - interval '30 days'
                                   GROUP BY 1) seen ON seen.pid = p.id::text
                        WHERE p.status = 'ACTIVE' AND p.id NOT IN (:exclude)
                        ORDER BY 5 * coalesce(sold.units, 0) + coalesce(seen.viewers, 0) DESC, p.created_at DESC, p.id DESC
                        LIMIT :n""")
                .param("exclude", exclude.isEmpty() ? List.of(-1L) : exclude)
                .param("n", limit * 3)
                .query(Long.class).list();
        return inStock(ids, limit);
    }

    private List<ProductSummaryDto> inStock(List<Long> ids, int limit) {
        return catalog.summaries(ids).stream().filter(p -> !p.soldOut()).limit(limit).toList();
    }
}
