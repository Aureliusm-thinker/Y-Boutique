package com.yvette.api.inventory;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.inventory.InventoryLevelRepository.LowStockRow;
import com.yvette.api.inventory.InventoryService.AdjustmentDto;
import com.yvette.api.inventory.InventoryService.AdjustmentResult;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventory;

    public AdminInventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    public record AdjustRequest(@NotNull Long variantId, int delta, @NotNull AdjustmentReason reason,
                                @Size(max = 300) String note) {}

    @PostMapping("/adjustments")
    @PreAuthorize(Roles.INVENTORY_WRITE)
    public AdjustmentResult adjust(@Valid @RequestBody AdjustRequest req, @AuthenticationPrincipal Jwt jwt) {
        return inventory.adjust(req.variantId(), req.delta(), req.reason(), req.note(), CurrentStaff.id(jwt));
    }

    @GetMapping("/low-stock")
    @PreAuthorize(Roles.CATALOG_READ)
    public List<LowStockRow> lowStock() {
        return inventory.lowStock();
    }

    @GetMapping("/variants/{variantId}/adjustments")
    @PreAuthorize(Roles.CATALOG_READ)
    public List<AdjustmentDto> history(@PathVariable Long variantId) {
        return inventory.history(variantId);
    }
}
