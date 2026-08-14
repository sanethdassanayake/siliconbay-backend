package com.hogger.siliconbay.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public final class ProductImageStorage {
    private static final Path STORAGE_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "products");
    private static final String PUBLIC_PREFIX = "/api/backend/uploads/products/";

    private ProductImageStorage() {
    }

    public static String save(FormDataBodyPart part) throws IOException {
        if (part == null || part.getFormDataContentDisposition() == null) {
            return null;
        }

        FormDataContentDisposition contentDisposition = part.getFormDataContentDisposition();
        String originalName = contentDisposition.getFileName();
        if (originalName == null || originalName.isBlank()) {
            return null;
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extensionOf(originalName);
        Path storageDir = ensureStorageDir();
        Path target = storageDir.resolve(fileName).normalize();

        try (InputStream inputStream = part.getValueAs(InputStream.class)) {
            Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        return toPublicUrl(fileName);
    }

    public static Path resolveFile(String fileName) throws IOException {
        Path storageDir = ensureStorageDir();
        Path resolved = storageDir.resolve(fileName).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IOException("Invalid file path");
        }
        return resolved;
    }

    public static String toPublicUrl(String fileName) {
        return PUBLIC_PREFIX + fileName;
    }

    private static Path ensureStorageDir() throws IOException {
        Files.createDirectories(STORAGE_DIR);
        return STORAGE_DIR;
    }

    private static String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex);
    }
}