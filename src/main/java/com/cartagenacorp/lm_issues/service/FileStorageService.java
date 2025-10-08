package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import com.cartagenacorp.lm_issues.exceptions.FileStorageException;
import com.cartagenacorp.lm_issues.repository.DescriptionFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-access-url}")
    private String uploadAccessUrl;

    private final DescriptionFileRepository descriptionFileRepository;

    public FileStorageService(DescriptionFileRepository descriptionFileRepository) {
        this.descriptionFileRepository = descriptionFileRepository;
    }

    public List<DescriptionFile> saveFiles(Description description, MultipartFile[] files) {
        logger.info("Iniciando guardado de archivos adjuntos para la descripción con ID={}", description.getId());
        List<DescriptionFile> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            logger.info("Procesando archivo: {}", file.getOriginalFilename());
            String fileName = saveFileToStorage(file);
            String fileUrl = uploadAccessUrl + fileName;

            DescriptionFile descFile = new DescriptionFile();
            descFile.setDescription(description);
            descFile.setFileName(fileName);
            descFile.setFileUrl(fileUrl);

            DescriptionFile saved = descriptionFileRepository.save(descFile);
            savedFiles.add(saved);
            logger.info("Archivo guardado en base de datos con ID={}, URL={}", saved.getId(), fileUrl);
        }

        logger.info("Todos los archivos han sido procesados correctamente.");
        return savedFiles;
    }

    private String saveFileToStorage(MultipartFile file) {
        try {
            Path directory = Paths.get(uploadDir);
            if (!Files.exists(directory)) {
                logger.info("Directorio de subida no existe. Creando: {}", directory.toAbsolutePath());
                Files.createDirectories(directory);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = directory.resolve(fileName);

            logger.info("Guardando archivo físicamente en: {}", filePath.toAbsolutePath());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileAccessUrl = uploadAccessUrl + fileName;
            logger.info("Archivo guardado correctamente. URL de acceso: {}", fileAccessUrl);
            return fileName;
        } catch (IOException e) {
            logger.error("Error al guardar archivo: {}", file.getOriginalFilename(), e);
            throw new FileStorageException("rror guardando el archivo " + file.getOriginalFilename(), e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            logger.info("Intentando eliminar archivo: {}", fileUrl);
            int lastSeparatorIndex = fileUrl.lastIndexOf('/');
            if (lastSeparatorIndex == -1) {
                throw new IllegalArgumentException("URL de archivo no válida: " + fileUrl);
            }
            String fileName = fileUrl.substring(lastSeparatorIndex + 1);
            Path path = Paths.get(uploadDir, fileName);
            Files.deleteIfExists(path);
            logger.info("Archivo eliminado: {}", path);
        } catch (IOException e) {
            logger.error("Error deleting file {}", fileUrl, e);
            throw new FileStorageException("Error eliminado el archivo: " + fileUrl, e);
        }
    }
}
