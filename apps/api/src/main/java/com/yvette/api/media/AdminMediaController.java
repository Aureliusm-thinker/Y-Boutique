package com.yvette.api.media;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.staff.Roles;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final MediaStorage storage;

    public AdminMediaController(MediaStorage storage) {
        this.storage = storage;
    }

    public record UploadResult(String url) {}

    @PostMapping
    @PreAuthorize(Roles.CATALOG_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BadRequestException("The file is empty");
        byte[] bytes = file.getBytes();
        // Trust the file's actual bytes, not its name or the browser-sent content type.
        ImageType type = ImageType.detect(bytes)
                .orElseThrow(() -> new BadRequestException("Only JPEG, PNG and WebP images are allowed"));
        return new UploadResult(storage.store(bytes, type.extension, type.contentType));
    }

    enum ImageType {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        static Optional<ImageType> detect(byte[] b) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return Optional.of(JPEG);
            }
            if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return Optional.of(PNG);
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return Optional.of(WEBP);
            }
            return Optional.empty();
        }
    }
}
