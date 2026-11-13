package com.yvette.api.content;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.content.ContentService.AdminPage;
import com.yvette.api.content.ContentService.PageView;
import com.yvette.api.content.ContentService.SiteBlock;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentControllers {

    public record NewPage(@NotBlank @Size(max = 80) String slug, @NotBlank @Size(max = 200) String title) {}

    public record PageDraft(@NotBlank @Size(max = 200) String title, @NotNull @Size(max = 50000) String body) {}

    private final ContentService content;

    public ContentControllers(ContentService content) {
        this.content = content;
    }

    // ---- public (published only)

    @GetMapping("/api/v1/content/pages/{slug}")
    public PageView page(@PathVariable String slug) {
        return content.publishedPage(slug);
    }

    @GetMapping("/api/v1/content/pages")
    public List<String> pageSlugs() {
        return content.publishedSlugs();
    }

    @GetMapping("/api/v1/content/site")
    public Map<String, Map<String, Object>> site() {
        return content.publishedSite();
    }

    // ---- back office

    @GetMapping("/api/v1/admin/content/pages")
    @PreAuthorize(Roles.MARKETING)
    public List<AdminPage> adminPages() {
        return content.pages();
    }

    @GetMapping("/api/v1/admin/content/pages/{slug}")
    @PreAuthorize(Roles.MARKETING)
    public AdminPage adminPage(@PathVariable String slug) {
        return content.page(slug);
    }

    @PostMapping("/api/v1/admin/content/pages")
    @PreAuthorize(Roles.MARKETING)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPage create(@Valid @RequestBody NewPage req, @AuthenticationPrincipal Jwt jwt) {
        return content.createPage(req.slug(), req.title(), CurrentStaff.id(jwt));
    }

    @PutMapping("/api/v1/admin/content/pages/{slug}")
    @PreAuthorize(Roles.MARKETING)
    public AdminPage saveDraft(@PathVariable String slug, @Valid @RequestBody PageDraft req, @AuthenticationPrincipal Jwt jwt) {
        return content.saveDraft(slug, req.title(), req.body(), CurrentStaff.id(jwt));
    }

    @PostMapping("/api/v1/admin/content/pages/{slug}/publish")
    @PreAuthorize(Roles.MARKETING)
    public AdminPage publish(@PathVariable String slug, @AuthenticationPrincipal Jwt jwt) {
        return content.publish(slug, CurrentStaff.id(jwt));
    }

    @PostMapping("/api/v1/admin/content/pages/{slug}/unpublish")
    @PreAuthorize(Roles.MARKETING)
    public AdminPage unpublish(@PathVariable String slug, @AuthenticationPrincipal Jwt jwt) {
        return content.unpublish(slug, CurrentStaff.id(jwt));
    }

    @GetMapping("/api/v1/admin/content/site")
    @PreAuthorize(Roles.MARKETING)
    public List<SiteBlock> adminSite() {
        return content.site();
    }

    @PutMapping("/api/v1/admin/content/site/{key}")
    @PreAuthorize(Roles.MARKETING)
    public SiteBlock saveSite(@PathVariable String key, @RequestBody Map<String, Object> draft, @AuthenticationPrincipal Jwt jwt) {
        return content.saveSiteDraft(key, draft, CurrentStaff.id(jwt));
    }

    @PostMapping("/api/v1/admin/content/site/{key}/publish")
    @PreAuthorize(Roles.MARKETING)
    public SiteBlock publishSite(@PathVariable String key, @AuthenticationPrincipal Jwt jwt) {
        return content.publishSite(key, CurrentStaff.id(jwt));
    }
}
