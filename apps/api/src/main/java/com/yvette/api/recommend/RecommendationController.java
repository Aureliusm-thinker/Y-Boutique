package com.yvette.api.recommend;

import com.yvette.api.recommend.RecommendationService.AlsoViewed;
import com.yvette.api.recommend.RecommendationService.ForYou;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront recommendations (under /catalog, so they are open like the rest of the catalogue). */
@RestController
@RequestMapping("/api/v1/catalog")
public class RecommendationController {

    private static final int MAX_LIMIT = 12;

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    /**
     * "Picked for you". {@code visitor} is the storefront's random anonymous id, only sent when the shopper has
     * accepted analytics; without it the answer is popular products.
     */
    @GetMapping("/recommendations/for-you")
    public ForYou forYou(@RequestParam(required = false) String visitor,
                         @RequestParam(required = false) Long exclude,
                         @RequestParam(defaultValue = "8") int limit) {
        String id = visitor == null || visitor.length() > 64 ? null : visitor;
        return recommendations.forYou(id, exclude, Math.clamp(limit, 1, MAX_LIMIT));
    }

    /** "Customers also viewed" for a product page. */
    @GetMapping("/products/{slug}/also-viewed")
    public AlsoViewed alsoViewed(@PathVariable String slug, @RequestParam(defaultValue = "4") int limit) {
        return recommendations.alsoViewed(slug, Math.clamp(limit, 1, MAX_LIMIT));
    }
}
