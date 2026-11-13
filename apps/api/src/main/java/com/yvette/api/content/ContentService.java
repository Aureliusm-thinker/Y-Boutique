package com.yvette.api.content;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Staff-edited content (FR-AD-10): pages (legal, delivery, size guide…) and the home page / announcement bar.
 * Everything has a draft and a published version, so changes can be previewed before going live.
 */
@Service
@Transactional
public class ContentService {

    static final Set<String> SITE_KEYS = Set.of("home", "announcement");

    public record PageView(String slug, String title, String body, Instant publishedAt) {}

    public record AdminPage(Long id, String slug, String title, String body, String draftTitle, String draftBody,
                            boolean published, boolean hasUnpublishedChanges, Instant publishedAt, Instant updatedAt) {}

    public record SiteBlock(String key, Map<String, Object> draft, Map<String, Object> published, boolean hasUnpublishedChanges,
                            Instant updatedAt) {}

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public ContentService(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ------------------------------------------------------------------ public

    @Transactional(readOnly = true)
    public PageView publishedPage(String slug) {
        return jdbc.sql("SELECT slug, title, body, published_at FROM content_pages WHERE slug = :s AND published")
                .param("s", slug)
                .query((rs, n) -> new PageView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()))
                .optional().orElseThrow(() -> new NotFoundException("Page not found"));
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> publishedSite() {
        Map<String, Map<String, Object>> out = new java.util.LinkedHashMap<>();
        jdbc.sql("SELECT key, published::text FROM site_content").query((rs, n) -> {
            out.put(rs.getString(1), readMap(rs.getString(2)));
            return null;
        }).list();
        return out;
    }

    @Transactional(readOnly = true)
    public List<String> publishedSlugs() {
        return jdbc.sql("SELECT slug FROM content_pages WHERE published ORDER BY slug").query(String.class).list();
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<AdminPage> pages() {
        return jdbc.sql(ADMIN_SELECT + " ORDER BY title").query(this::adminRow).list();
    }

    @Transactional(readOnly = true)
    public AdminPage page(String slug) {
        return jdbc.sql(ADMIN_SELECT + " WHERE slug = :s").param("s", slug).query(this::adminRow).optional()
                .orElseThrow(() -> new NotFoundException("Page not found"));
    }

    public AdminPage createPage(String slug, String title, Long staffId) {
        if (!slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$")) throw new BadRequestException("Use lower-case letters, numbers and dashes");
        int exists = jdbc.sql("SELECT count(*) FROM content_pages WHERE slug = :s").param("s", slug).query(Integer.class).single();
        if (exists > 0) throw new ConflictException("A page with this address already exists");
        jdbc.sql("""
                INSERT INTO content_pages (slug, title, draft_title, draft_body, updated_by)
                VALUES (:s, :t, :t, '', :u)""").param("s", slug).param("t", title.strip()).param("u", staffId).update();
        return page(slug);
    }

    public AdminPage saveDraft(String slug, String title, String body, Long staffId) {
        int n = jdbc.sql("""
                        UPDATE content_pages SET draft_title = :t, draft_body = :b, updated_at = now(), updated_by = :u
                        WHERE slug = :s""")
                .param("t", title.strip()).param("b", body).param("u", staffId).param("s", slug).update();
        if (n == 0) throw new NotFoundException("Page not found");
        return page(slug);
    }

    public AdminPage publish(String slug, Long staffId) {
        int n = jdbc.sql("""
                        UPDATE content_pages SET title = coalesce(draft_title, title), body = coalesce(draft_body, body),
                               published = TRUE, published_at = now(), updated_at = now(), updated_by = :u
                        WHERE slug = :s""")
                .param("u", staffId).param("s", slug).update();
        if (n == 0) throw new NotFoundException("Page not found");
        return page(slug);
    }

    public AdminPage unpublish(String slug, Long staffId) {
        jdbc.sql("UPDATE content_pages SET published = FALSE, updated_at = now(), updated_by = :u WHERE slug = :s")
                .param("u", staffId).param("s", slug).update();
        return page(slug);
    }

    @Transactional(readOnly = true)
    public List<SiteBlock> site() {
        return jdbc.sql("SELECT key, draft::text, published::text, updated_at FROM site_content ORDER BY key")
                .query((rs, n) -> new SiteBlock(rs.getString(1), readMap(rs.getString(2)), readMap(rs.getString(3)),
                        !readMap(rs.getString(2)).equals(readMap(rs.getString(3))), rs.getTimestamp(4).toInstant()))
                .list();
    }

    public SiteBlock saveSiteDraft(String key, Map<String, Object> draft, Long staffId) {
        checkKey(key);
        jdbc.sql("UPDATE site_content SET draft = CAST(:d AS jsonb), updated_at = now(), updated_by = :u WHERE key = :k")
                .param("d", json.writeValueAsString(draft)).param("u", staffId).param("k", key).update();
        return site().stream().filter(b -> b.key().equals(key)).findFirst().orElseThrow();
    }

    public SiteBlock publishSite(String key, Long staffId) {
        checkKey(key);
        jdbc.sql("UPDATE site_content SET published = draft, updated_at = now(), updated_by = :u WHERE key = :k")
                .param("u", staffId).param("k", key).update();
        return site().stream().filter(b -> b.key().equals(key)).findFirst().orElseThrow();
    }

    private static final String ADMIN_SELECT = """
            SELECT id, slug, title, body, draft_title, draft_body, published,
                   (draft_title IS DISTINCT FROM title OR draft_body IS DISTINCT FROM body), published_at, updated_at
            FROM content_pages""";

    private AdminPage adminRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new AdminPage(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getBoolean(7), rs.getBoolean(8),
                rs.getTimestamp(9) == null ? null : rs.getTimestamp(9).toInstant(), rs.getTimestamp(10).toInstant());
    }

    private static void checkKey(String key) {
        if (!SITE_KEYS.contains(key)) throw new NotFoundException("Unknown section " + key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String text) {
        return json.readValue(text, Map.class);
    }
}
