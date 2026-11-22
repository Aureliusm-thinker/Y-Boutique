package com.yvette.api.recommend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds "customers also viewed / bought together" (product_co_occurrence) from the last 90 days:
 * <ul>
 *   <li>two products viewed by the same visitor count once per visitor, less for visitors who browse a lot
 *       (1 / ln(2 + products viewed)), and visitors who viewed more than 60 products (likely bots) are ignored;</li>
 *   <li>two products bought in the same paid order count 3.</li>
 * </ul>
 * A pair needs support from at least {@code yvette.recommendations.min-support} visitors or orders, so one
 * person's browsing never shows up as "customers also viewed".
 */
@Component
public class RecommendationJob {

    private static final Logger log = LoggerFactory.getLogger(RecommendationJob.class);

    private final JdbcClient jdbc;
    private final int minSupport;

    public RecommendationJob(JdbcClient jdbc, @Value("${yvette.recommendations.min-support:2}") int minSupport) {
        this.jdbc = jdbc;
        this.minSupport = minSupport;
    }

    @Scheduled(fixedDelayString = "${yvette.recommendations.rebuild-every:PT6H}", initialDelayString = "PT1M")
    public void scheduledRebuild() {
        try {
            int pairs = rebuild();
            log.info("Recommendations: rebuilt {} product pairs", pairs);
        } catch (RuntimeException e) {
            log.warn("Recommendations: rebuild failed, keeping the previous data: {}", e.getMessage());
        }
    }

    /** Replaces the table in one transaction, so readers see either the old or the new pairs. */
    @Transactional
    public int rebuild() {
        jdbc.sql("DELETE FROM product_co_occurrence").update();
        return jdbc.sql("""
                        WITH viewed AS (
                            SELECT DISTINCT e.anonymous_id AS visitor, (e.properties ->> 'productId')::bigint AS pid
                            FROM events e
                            WHERE e.name = 'product_view'
                              AND e.occurred_at > now() - interval '90 days'
                              AND e.properties ->> 'productId' ~ '^[0-9]{1,18}$'
                        ),
                        visitors AS (
                            SELECT visitor, pid, count(*) OVER (PARTITION BY visitor) AS n FROM viewed
                        ),
                        pairs AS (
                            SELECT a.pid AS product_id, b.pid AS other_id, 1.0 / ln(2 + a.n) AS w
                            FROM visitors a JOIN visitors b ON a.visitor = b.visitor AND a.pid <> b.pid
                            WHERE a.n <= 60
                            UNION ALL
                            SELECT DISTINCT ON (a.order_id, a.product_id, b.product_id) a.product_id, b.product_id, 3.0
                            FROM order_lines a
                            JOIN order_lines b ON a.order_id = b.order_id AND a.product_id <> b.product_id
                            JOIN orders o ON o.id = a.order_id
                            WHERE o.paid_at > now() - interval '90 days' AND o.status <> 'CANCELLED'
                        )
                        INSERT INTO product_co_occurrence (product_id, other_id, score)
                        SELECT pairs.product_id, pairs.other_id, sum(pairs.w)
                        FROM pairs
                        JOIN products pa ON pa.id = pairs.product_id
                        JOIN products pb ON pb.id = pairs.other_id
                        GROUP BY pairs.product_id, pairs.other_id
                        HAVING count(*) >= :minSupport""")
                .param("minSupport", minSupport)
                .update();
    }
}
