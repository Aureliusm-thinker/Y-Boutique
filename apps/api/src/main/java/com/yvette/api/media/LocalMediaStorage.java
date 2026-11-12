package com.yvette.api.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Saves uploads to a local folder and serves them at /media/** (development only). */
@Component
public class LocalMediaStorage implements MediaStorage, WebMvcConfigurer {

    private final Path directory;
    private final String publicBaseUrl;

    public LocalMediaStorage(@Value("${yvette.media.local-dir:./data/media}") String directory,
                             @Value("${yvette.media.public-base-url:http://localhost:8080/media}") String publicBaseUrl) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @Override
    public String store(byte[] content, String extension, String contentType) {
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save upload", e);
        }
        return publicBaseUrl + "/" + name;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(directory.toUri().toString() + "/")
                .setCachePeriod(31_536_000); // file names are unique, so cache for a year
    }
}
