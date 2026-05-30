package com.ghostcoach.util;

import org.springframework.http.MediaType;

public final class ImageUtil {

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";

    private ImageUtil() {}

    public static boolean isValidContentType(String contentType) {
        return MIME_JPEG.equals(contentType) || MIME_PNG.equals(contentType);
    }

    public static String getExtension(String contentType) {
        return MIME_PNG.equals(contentType) ? ".png" : ".jpg";
    }

    public static MediaType getMediaType(String imagePath) {
        if (imagePath != null && imagePath.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }
}
