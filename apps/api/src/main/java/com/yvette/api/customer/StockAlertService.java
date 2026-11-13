package com.yvette.api.customer;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.inventory.InventoryService;
import com.yvette.api.inventory.InventoryService.VariantRestocked;
import com.yvette.api.notification.Mailer;
import com.yvette.api.notification.Mailer.Block;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Back-in-stock emails: sign up while a size is sold out, get one email when it returns. */
@Service
public class StockAlertService {

    private final JdbcClient jdbc;
    private final InventoryService inventory;
    private final Mailer mailer;
    private final String siteUrl;

    public StockAlertService(JdbcClient jdbc, InventoryService inventory, Mailer mailer,
                             @Value("${yvette.site-url}") String siteUrl) {
        this.jdbc = jdbc;
        this.inventory = inventory;
        this.mailer = mailer;
        this.siteUrl = siteUrl;
    }

    @Transactional
    public void subscribe(Long variantId, String email, Long customerId) {
        int exists = jdbc.sql("SELECT count(*) FROM product_variants WHERE id = :v").param("v", variantId)
                .query(Integer.class).single();
        if (exists == 0) throw new NotFoundException("Item not found");
        if (inventory.availableByVariant(List.of(variantId)).getOrDefault(variantId, 0) > 0) {
            throw new BadRequestException("Good news — this size is in stock now");
        }
        jdbc.sql("""
                INSERT INTO stock_alerts (email, customer_id, variant_id) VALUES (:e, :c, :v)
                ON CONFLICT (variant_id, lower(email)) WHERE notified_at IS NULL DO NOTHING""")
                .param("e", email.strip().toLowerCase(Locale.ROOT)).param("c", customerId).param("v", variantId)
                .update();
    }

    /** When a sold-out variant comes back, email everyone waiting for it (once). */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRestocked(VariantRestocked event) {
        List<Map<String, Object>> waiting = jdbc.sql("""
                        UPDATE stock_alerts a SET notified_at = now()
                        FROM product_variants v JOIN products p ON p.id = v.product_id
                        WHERE a.variant_id = :v AND a.notified_at IS NULL AND v.id = a.variant_id AND p.status = 'ACTIVE'
                        RETURNING a.email, p.title, p.slug, v.size, v.colour""")
                .param("v", event.variantId()).query().listOfRows();
        for (Map<String, Object> row : waiting) {
            String variant = String.join(" / ", java.util.stream.Stream.of(row.get("size"), row.get("colour"))
                    .filter(java.util.Objects::nonNull).map(Object::toString).toList());
            mailer.send((String) row.get("email"), row.get("title") + " is back in stock", List.of(
                    Block.text("Good news: " + row.get("title") + (variant.isEmpty() ? "" : " in " + variant)
                            + " is back. Popular sizes go quickly."),
                    Block.button("Shop it now", siteUrl + "/products/" + row.get("slug"))));
        }
    }
}
