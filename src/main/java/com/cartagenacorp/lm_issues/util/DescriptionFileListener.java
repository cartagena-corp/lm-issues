package com.cartagenacorp.lm_issues.util;

import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import com.cartagenacorp.lm_issues.service.FileStorageService;
import jakarta.persistence.PreRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DescriptionFileListener {

    private static FileStorageService fileStorageService;

    @Autowired
    public void init(FileStorageService fileStorageService) {
        DescriptionFileListener.fileStorageService = fileStorageService;
    }

    @PreRemove
    public void onPreRemove(DescriptionFile file) {
        if (file.getFileUrl() != null) {
            fileStorageService.deleteFile(file.getFileUrl());
        }
    }
}


