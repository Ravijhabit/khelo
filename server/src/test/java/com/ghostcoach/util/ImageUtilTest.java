package com.ghostcoach.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUtilTest {

    @Test
    void isValidContentType_jpeg_returnsTrue() {
        assertThat(ImageUtil.isValidContentType("image/jpeg")).isTrue();
    }

    @Test
    void isValidContentType_png_returnsTrue() {
        assertThat(ImageUtil.isValidContentType("image/png")).isTrue();
    }

    @Test
    void isValidContentType_gif_returnsFalse() {
        assertThat(ImageUtil.isValidContentType("image/gif")).isFalse();
    }

    @Test
    void isValidContentType_null_returnsFalse() {
        assertThat(ImageUtil.isValidContentType(null)).isFalse();
    }

    @Test
    void isValidContentType_emptyString_returnsFalse() {
        assertThat(ImageUtil.isValidContentType("")).isFalse();
    }

    @Test
    void getExtension_jpeg_returnsJpg() {
        assertThat(ImageUtil.getExtension("image/jpeg")).isEqualTo(".jpg");
    }

    @Test
    void getExtension_png_returnsPng() {
        assertThat(ImageUtil.getExtension("image/png")).isEqualTo(".png");
    }

    @Test
    void getMediaType_pngPath_returnsImagePng() {
        assertThat(ImageUtil.getMediaType("abc123.png")).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void getMediaType_jpgPath_returnsImageJpeg() {
        assertThat(ImageUtil.getMediaType("abc123.jpg")).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void getMediaType_nullPath_returnsImageJpeg() {
        assertThat(ImageUtil.getMediaType(null)).isEqualTo(MediaType.IMAGE_JPEG);
    }
}
