package com.yvette.api.media;

/**
 * Where uploaded product images live. {@link LocalMediaStorage} is used for development;
 * a Cloudinary/S3 implementation replaces it in production (docs/05 — Media).
 */
public interface MediaStorage {

    /** Stores the bytes and returns the public URL. */
    String store(byte[] content, String extension, String contentType);
}
