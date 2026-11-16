package com.trabajaya.storage;

import com.trabajaya.shared.Principal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Object upload(@AuthenticationPrincipal
    Principal p, @RequestParam("file")
    MultipartFile file) {
        return media.upload(p, file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable
    String id, @AuthenticationPrincipal
    Principal p) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(media.read(id, p));
    }
}
