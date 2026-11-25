package com.yvette.api.catalog.csv;

import com.yvette.api.catalog.csv.ProductCsvService.ImportResult;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.CurrentStaff;
import com.yvette.api.staff.Roles;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Product spreadsheet export and import for the back office. */
@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductCsvController {

    /** About 5,000 rows with long descriptions. */
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    private final ProductCsvService csv;

    public ProductCsvController(ProductCsvService csv) {
        this.csv = csv;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize(Roles.CATALOG_READ)
    public ResponseEntity<byte[]> export() {
        byte[] body = csv.export().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("yvette-products-" + LocalDate.now() + ".csv").build().toString())
                .body(body);
    }

    /**
     * Checks a product CSV (the export's format) and, unless {@code dryRun}, saves it all or nothing.
     * Always answers 200 with the result; problems are listed per spreadsheet line in {@code errors}.
     */
    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain", "application/octet-stream"})
    @PreAuthorize(Roles.CATALOG_WRITE)
    public ImportResult importCsv(@RequestBody byte[] body,
                                  @RequestParam(defaultValue = "true") boolean dryRun,
                                  @AuthenticationPrincipal Jwt jwt) {
        if (body.length > MAX_BYTES) throw new BadRequestException("The file is larger than 5 MB; split it into smaller files");
        return csv.importCsv(decode(body), dryRun, CurrentStaff.id(jwt));
    }

    /** UTF-8 (Google Sheets, Excel "CSV UTF-8"), else Windows-1252 (Excel's plain "CSV"). */
    static String decode(byte[] body) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(body)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            return new String(body, java.nio.charset.Charset.forName("windows-1252"));
        }
    }
}
