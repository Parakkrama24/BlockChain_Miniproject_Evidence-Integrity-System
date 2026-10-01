package com.evidence.integrity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {
    private final Path baseDir;

    public FileStorageService(@Value("${evidence.storage.dir}") String dir) throws IOException {
        this.baseDir = Paths.get(dir);
        Files.createDirectories(baseDir);
    }

    public String store(long evidenceId, String fileName, MultipartFile file) throws IOException {
        Path target = baseDir.resolve(evidenceId + "_" + fileName);
        file.transferTo(target);
        return target.toString();
    }
}
