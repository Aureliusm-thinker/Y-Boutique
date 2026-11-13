package com.yvette.api.inventory;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.inventory.InventoryLevelRepository.LowStockRow;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 3;

    private final InventoryLevelRepository levels;
    private final InventoryAdjustmentRepository adjustments;
    private final org.springframework.context.ApplicationEventPublisher events;

    public InventoryService(InventoryLevelRepository levels, InventoryAdjustmentRepository adjustments,
                            org.springframework.context.ApplicationEventPublisher events) {
        this.levels = levels;
        this.adjustments = adjustments;
        this.events = events;
    }

    /** A sold-out variant can be bought again (published after the change commits → back-in-stock emails). */
    public record VariantRestocked(Long variantId) {}

    private void announceIfRestocked(InventoryLevel level, int availableBefore) {
        if (availableBefore <= 0 && level.available() > 0) events.publishEvent(new VariantRestocked(level.getVariantId()));
    }

    /** Available quantity (on hand minus reserved) per variant id; missing variants count as 0. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> availableByVariant(Collection<Long> variantIds) {
        return levels.findAllById(variantIds).stream()
                .collect(Collectors.toMap(InventoryLevel::getVariantId, InventoryLevel::available));
    }

    @Transactional(readOnly = true)
    public Map<Long, InventoryLevel> levelsByVariant(Collection<Long> variantIds) {
        return levels.findAllById(variantIds).stream()
                .collect(Collectors.toMap(InventoryLevel::getVariantId, Function.identity()));
    }

    /** Creates the stock record for a new variant, optionally with opening stock (logged as RECEIVED). */
    public void createLevel(Long variantId, int lowStockThreshold, int openingStock, Long staffId) {
        InventoryLevel level = levels.save(new InventoryLevel(variantId, lowStockThreshold));
        if (openingStock > 0) {
            level.adjust(openingStock);
            adjustments.save(new InventoryAdjustment(variantId, openingStock, AdjustmentReason.RECEIVED,
                    "Opening stock", level.getOnHand(), staffId));
        }
    }

    public void setThreshold(Long variantId, int threshold) {
        levels.findById(variantId).ifPresent(l -> l.setLowStockThreshold(threshold));
    }

    public record AdjustmentResult(Long variantId, int onHand, int available, boolean low) {}

    public AdjustmentResult adjust(Long variantId, int delta, AdjustmentReason reason, String note, Long staffId) {
        if (delta == 0) throw new BadRequestException("Adjustment must change the stock level");
        InventoryLevel level = levels.findForUpdate(variantId)
                .orElseThrow(() -> new NotFoundException("No stock record for variant " + variantId));
        int before = level.available();
        if (!level.adjust(delta)) {
            throw new BadRequestException("Stock cannot go below zero (currently " + level.getOnHand() + " on hand)");
        }
        adjustments.save(new InventoryAdjustment(variantId, delta, reason, note, level.getOnHand(), staffId));
        announceIfRestocked(level, before);
        return new AdjustmentResult(variantId, level.getOnHand(), level.available(), level.isLow());
    }

    // ------------------------------------------------------------------ orders

    /**
     * Locks the stock rows for these variants (in id order, so concurrent checkouts can't deadlock)
     * and returns them. Must be called inside the checkout transaction.
     */
    public Map<Long, InventoryLevel> lockForCheckout(Collection<Long> variantIds) {
        Map<Long, InventoryLevel> locked = new java.util.LinkedHashMap<>();
        variantIds.stream().distinct().sorted().forEach(id ->
                levels.findForUpdate(id).ifPresent(l -> locked.put(id, l)));
        return locked;
    }

    public void releaseReservation(Long variantId, int quantity) {
        levels.findForUpdate(variantId).ifPresent(l -> {
            int before = l.available();
            l.release(quantity);
            announceIfRestocked(l, before);
        });
    }

    /** Paid order line: reserved stock leaves the shelf and is logged as SOLD. */
    public void commitSale(Long variantId, int quantity, Long orderId, String orderNumber) {
        levels.findForUpdate(variantId).ifPresent(l -> {
            l.commitSale(quantity);
            adjustments.save(InventoryAdjustment.forOrder(variantId, -quantity, AdjustmentReason.SOLD,
                    "Order " + orderNumber, l.getOnHand(), orderId, null));
        });
    }

    /** Paid order whose reservation had lapsed: take the stock now if it is still there. */
    public boolean sellWithoutReservation(Long variantId, int quantity, Long orderId, String orderNumber) {
        InventoryLevel l = levels.findForUpdate(variantId).orElse(null);
        if (l == null || !l.reserve(quantity)) return false;
        l.commitSale(quantity);
        adjustments.save(InventoryAdjustment.forOrder(variantId, -quantity, AdjustmentReason.SOLD,
                "Order " + orderNumber, l.getOnHand(), orderId, null));
        return true;
    }

    /** Returned/refunded items put back into stock. */
    public void restock(Long variantId, int quantity, Long orderId, String note, Long staffId) {
        levels.findForUpdate(variantId).ifPresent(l -> {
            int before = l.available();
            l.adjust(quantity);
            announceIfRestocked(l, before);
            adjustments.save(InventoryAdjustment.forOrder(variantId, quantity, AdjustmentReason.RETURNED, note,
                    l.getOnHand(), orderId, staffId));
        });
    }

    @Transactional(readOnly = true)
    public List<LowStockRow> lowStock() {
        return levels.findLowStock();
    }

    public record AdjustmentDto(Long id, int delta, AdjustmentReason reason, String note, int onHandAfter,
                                Long staffUserId, Instant createdAt) {}

    @Transactional(readOnly = true)
    public List<AdjustmentDto> history(Long variantId) {
        return adjustments.findTop50ByVariantIdOrderByCreatedAtDesc(variantId).stream()
                .map(a -> new AdjustmentDto(a.getId(), a.getDelta(), a.getReason(), a.getNote(), a.getOnHandAfter(),
                        a.getStaffUserId(), a.getCreatedAt()))
                .toList();
    }
}
