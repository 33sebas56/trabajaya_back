package com.trabajaya;

import com.trabajaya.shared.ApiException;
import com.trabajaya.storage.ImageSanitizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class ImageSanitizerTest {
    private final ImageSanitizer sanitizer = new ImageSanitizer();

    @Test void acceptsPixelsAndReencodesJpegAsPng() throws Exception {
        var input = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpeg", input);
        byte[] result = sanitizer.sanitize(new MockMultipartFile("file", "photo.jpg", "image/jpeg", input.toByteArray()));
        assertArrayEquals(new byte[] {(byte)137, 80, 78, 71}, java.util.Arrays.copyOf(result, 4));
    }

    @Test void rejectsFalseImageAndSvg() {
        assertThrows(ApiException.class, () -> sanitizer.sanitize(new MockMultipartFile("file", "fake.png", "image/png", "not pixels".getBytes())));
        assertThrows(ApiException.class, () -> sanitizer.sanitize(new MockMultipartFile("file", "active.svg", "image/svg+xml", "<svg/>".getBytes())));
    }

    @Test void rejectsOversizedInputAndDimensions() throws Exception {
        assertThrows(ApiException.class, () -> sanitizer.sanitize(new MockMultipartFile("file", "large.png", "image/png", new byte[6 * 1024 * 1024])));
        var input = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", input);
        assertThrows(ApiException.class, () -> sanitizer.sanitize(new MockMultipartFile("file", "wide.png", "image/png", input.toByteArray())));
    }
}
