package com.yvette.api.review;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.review.ReviewService.ProductReviews;
import com.yvette.api.review.ReviewService.ReviewRequest;
import com.yvette.api.review.ReviewService.ReviewView;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewControllers {

    private final ReviewService reviews;

    public ReviewControllers(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/api/v1/catalog/products/{slug}/reviews")
    public ProductReviews forProduct(@PathVariable String slug) {
        return reviews.forProduct(slug);
    }

    @PostMapping("/api/v1/orders/{number}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView submit(@PathVariable String number, @Valid @RequestBody ReviewRequest req) {
        return reviews.submit(number, req);
    }

    @GetMapping("/api/v1/orders/{number}/reviews")
    public List<Long> reviewed(@PathVariable String number, @RequestParam UUID token) {
        return reviews.reviewedProductIds(number, token);
    }

    @GetMapping("/api/v1/admin/reviews")
    @PreAuthorize(Roles.REVIEWS)
    public List<ReviewView> queue(@RequestParam(defaultValue = "PENDING") String status) {
        return reviews.moderationQueue(status);
    }

    @PostMapping("/api/v1/admin/reviews/{id}/approve")
    @PreAuthorize(Roles.REVIEWS)
    public ReviewView approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return reviews.moderate(id, true, CurrentStaff.id(jwt));
    }

    @PostMapping("/api/v1/admin/reviews/{id}/reject")
    @PreAuthorize(Roles.REVIEWS)
    public ReviewView reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return reviews.moderate(id, false, CurrentStaff.id(jwt));
    }
}
