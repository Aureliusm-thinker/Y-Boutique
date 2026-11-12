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

    public InventoryService(InventoryLevelRepository levels, InventoryAdjustmentRepository adjustments) {
        this.levels = levels;
        this.adjustments = adjustments;
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
        if (!level.adjust(delta)) {
            throw new BadRequestException("Stock cannot go below zero (currently " + level.getOnHand() + " on hand)");
        }
        adjustments.save(new InventoryAdjustment(variantId, delta, reason, note, level.getOnHand(), staffId));
        return new AdjustmentResult(variantId, level.getOnHand(), level.available(), level.isLow());
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
