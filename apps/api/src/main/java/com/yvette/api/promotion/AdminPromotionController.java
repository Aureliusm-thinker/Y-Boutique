package com.yvette.api.promotion;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Discount codes and scheduled sales (FR-AD-09). A promotion without a code applies to everyone automatically. */
@RestController
@RequestMapping("/api/v1/admin/promotions")
@Transactional
public class AdminPromotionController {

    public record PromotionRequest(
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "letters, numbers, - and _ only") String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull Promotion.Type type,
            @DecimalMin("0") BigDecimal value,
            @DecimalMin("0") BigDecimal minSubtotal,
            Instant startsAt,
            Instant endsAt,
            @Min(1) Integer usageLimit,
            boolean active) {}

    public record PromotionView(Long id, String code, String name, Promotion.Type type, BigDecimal value,
                                BigDecimal minSubtotal, Instant startsAt, Instant endsAt, Integer usageLimit, int timesUsed,
                                boolean active, String state) {
        static PromotionView of(Promotion p) {
            Instant now = Instant.now();
            String state = !p.isActive() ? "Off"
                    : p.getEndsAt() != null && !now.isBefore(p.getEndsAt()) ? "Ended"
                    : p.getStartsAt() != null && now.isBefore(p.getStartsAt()) ? "Scheduled"
                    : p.getUsageLimit() != null && p.getTimesUsed() >= p.getUsageLimit() ? "Used up"
                    : "Live";
            return new PromotionView(p.getId(), p.getCode(), p.getName(), p.getType(), p.getValue(), p.getMinSubtotal(),
                    p.getStartsAt(), p.getEndsAt(), p.getUsageLimit(), p.getTimesUsed(), p.isActive(), state);
        }
    }

    private final PromotionRepository promotions;

    public AdminPromotionController(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    @GetMapping
    @PreAuthorize(Roles.MARKETING)
    @Transactional(readOnly = true)
    public List<PromotionView> list() {
        return promotions.findAllByOrderByCreatedAtDesc().stream().map(PromotionView::of).toList();
    }

    @PostMapping
    @PreAuthorize(Roles.MARKETING)
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionView create(@Valid @RequestBody PromotionRequest req) {
        validate(req, null);
        Promotion p = new Promotion(req.code(), req.name().strip(), req.type(), value(req));
        apply(p, req);
        return PromotionView.of(promotions.save(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.MARKETING)
    public PromotionView update(@PathVariable Long id, @Valid @RequestBody PromotionRequest req) {
        Promotion p = promotions.findById(id).orElseThrow(() -> new NotFoundException("Promotion not found"));
        validate(req, id);
        apply(p, req);
        return PromotionView.of(p);
    }

    private void validate(PromotionRequest req, Long id) {
        String code = Promotion.normaliseCode(req.code());
        if (code != null && (id == null ? promotions.existsByCode(code) : promotions.existsByCodeAndIdNot(code, id))) {
            throw new ConflictException("The code " + code + " is already used");
        }
        if (req.type() == Promotion.Type.PERCENT && (req.value() == null || req.value().signum() <= 0
                || req.value().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("A percentage discount must be between 1 and 100");
        }
        if (req.type() == Promotion.Type.FIXED && (req.value() == null || req.value().signum() <= 0)) {
            throw new BadRequestException("Enter the amount off in KSh");
        }
        if (req.startsAt() != null && req.endsAt() != null && !req.endsAt().isAfter(req.startsAt())) {
            throw new BadRequestException("The end must be after the start");
        }
    }

    private static BigDecimal value(PromotionRequest req) {
        return req.type() == Promotion.Type.FREE_SHIPPING || req.value() == null ? BigDecimal.ZERO : req.value();
    }

    private static void apply(Promotion p, PromotionRequest req) {
        p.update(req.code(), req.name().strip(), req.type(), value(req), req.minSubtotal(), req.startsAt(), req.endsAt(),
                req.usageLimit(), req.active());
    }
}
