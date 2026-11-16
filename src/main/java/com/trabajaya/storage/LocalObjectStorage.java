package com.trabajaya.storage;

import java.nio.file.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {
    private final Path root;

    public LocalObjectStorage(@Value("${app.storage.directory}")
    String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    private Path path(String key) {
        ObjectKey.require(key);
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return path;
    }

    public String provider() {
        return "local";
    }

    public void write(String key, byte[] bytes, String type) {
        try {
            Path p = path(key);
            Files.createDirectories(p.getParent());
            Files.write(p, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new StorageException(StorageException.Reason.UNAVAILABLE);
        }
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(path(key));
        } catch (IOException e) {
            throw new StorageException(e instanceof NoSuchFileException ? StorageException.Reason.MISSING : StorageException.Reason.UNAVAILABLE);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(path(key));
        } catch (IOException e) {
            throw new StorageException(StorageException.Reason.UNAVAILABLE);
        }
    }
}
