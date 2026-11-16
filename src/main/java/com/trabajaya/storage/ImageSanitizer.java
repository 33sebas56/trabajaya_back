package com.trabajaya.storage;

import com.trabajaya.shared.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Decode and re-encode the pixels: never store original metadata or active SVG. */
@Component
public class ImageSanitizer {
    public static final long MAX_INPUT_BYTES = 5L * 1024 * 1024;
    public static final int MAX_OUTPUT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_SIDE = 4096;
    private static final long MAX_PIXELS = 16000000;

    public byte[] sanitize(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_INPUT_BYTES) {
            throw ApiException.bad("Selecciona una imagen PNG o JPEG de hasta 5 MB.");
        }
        try {
            // A memory-backed stream avoids ImageIO temporary disk cache files.
            byte[] original = file.getBytes();
            if (original.length > MAX_INPUT_BYTES) throw ApiException.bad("La imagen supera 5 MB.");
            try (var stream = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(original))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw ApiException.bad("El archivo no es una imagen valida.");
                var reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("png", "jpeg", "jpg").contains(format)) {
                        throw ApiException.bad("Solo se permiten PNG y JPEG.");
                    }
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE
                            || (long) width * height > MAX_PIXELS) {
                        throw ApiException.bad("La imagen no debe superar 4096 px ni 16 megapixeles.");
                    }
                    var image = reader.read(0);
                    if (image == null) throw ApiException.bad("No se pudo decodificar la imagen.");
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, "png", output)) throw ApiException.bad("No se pudo procesar la imagen.");
                    if (output.size() > MAX_OUTPUT_BYTES) {
                        throw ApiException.bad("La imagen procesada es demasiado grande. Reduce su resolucion.");
                    }
                    return output.toByteArray();
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException error) {
            throw ApiException.bad("No se pudo procesar la imagen.");
        }
    }
}
