package com.designstudio.api;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class StaticFileController {

    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Получаем рабочую директорию приложения
            String workingDir = System.getProperty("user.dir");

            // Пытаемся найти файл
            Path filePath = Paths.get(workingDir, UPLOAD_DIR, filename);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                // Пробуем без workingDir (если уже в правильной директории)
                filePath = Paths.get(UPLOAD_DIR, filename);
                file = filePath.toFile();
            }

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}