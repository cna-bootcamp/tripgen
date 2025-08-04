package com.unicorn.tripgen.user.service;

import com.unicorn.tripgen.common.constant.ErrorCodes;
import com.unicorn.tripgen.common.constant.CommonMessages;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

/**
 * 로컬 파일 시스템 기반 파일 저장 서비스
 */
@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {
    
    private final String uploadPath;
    private final String baseUrl;
    private final long maxFileSize;
    
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };
    
    public LocalFileStorageService(
            @Value("${app.file.upload-path:/app/uploads}") String uploadPath,
            @Value("${app.file.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.file.max-size:5242880}") long maxFileSize) { // 5MB default
        
        this.uploadPath = uploadPath;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.maxFileSize = maxFileSize;
        
        // 업로드 디렉토리 생성
        createUploadDirectories();
    }
    
    @Override
    public String uploadAvatar(String userId, MultipartFile file) {
        log.info("아바타 업로드 시작: userId={}, fileName={}", userId, file.getOriginalFilename());
        
        // 파일 검증
        validateFile(file);
        
        try {
            // 파일명 생성
            String fileName = generateFileName(userId, file);
            
            // 저장 경로 생성
            Path avatarDir = Paths.get(uploadPath, "avatars");
            Files.createDirectories(avatarDir);
            
            Path filePath = avatarDir.resolve(fileName);
            
            // 기존 아바타 파일 삭제 (선택적)
            deleteExistingAvatar(userId, avatarDir);
            
            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // URL 생성
            String fileUrl = baseUrl + "/uploads/avatars/" + fileName;
            
            log.info("아바타 업로드 완료: userId={}, fileUrl={}", userId, fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("아바타 업로드 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCodes.FILE_ERROR, CommonMessages.FILE_ERROR);
        }
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        try {
            // URL에서 파일 경로 추출
            String relativePath = fileUrl.replace(baseUrl, "").replace("/uploads/", "");
            Path filePath = Paths.get(uploadPath, relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 완료: {}", fileUrl);
            }
            
        } catch (IOException e) {
            log.error("파일 삭제 실패: fileUrl={}, error={}", fileUrl, e.getMessage(), e);
        }
    }
    
    @Override
    public boolean fileExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }
        
        try {
            String relativePath = fileUrl.replace(baseUrl, "").replace("/uploads/", "");
            Path filePath = Paths.get(uploadPath, relativePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.debug("파일 존재 확인 실패: fileUrl={}", fileUrl);
            return false;
        }
    }
    
    @Override
    public boolean isAllowedFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        if (originalFilename == null || contentType == null) {
            return false;
        }
        
        // 확장자 검증
        String lowerCaseFilename = originalFilename.toLowerCase();
        boolean validExtension = Arrays.stream(ALLOWED_EXTENSIONS)
                .anyMatch(lowerCaseFilename::endsWith);
        
        // Content-Type 검증
        boolean validContentType = Arrays.asList(ALLOWED_CONTENT_TYPES)
                .contains(contentType.toLowerCase());
        
        return validExtension && validContentType;
    }
    
    @Override
    public boolean isAllowedFileSize(MultipartFile file) {
        return file.getSize() > 0 && file.getSize() <= maxFileSize;
    }
    
    /**
     * 파일 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_FILE_TYPE, "파일이 없습니다");
        }
        
        if (!isAllowedFileType(file)) {
            throw new BusinessException(ErrorCodes.INVALID_FILE_TYPE, CommonMessages.UNSUPPORTED_FILE_TYPE);
        }
        
        if (!isAllowedFileSize(file)) {
            throw new BusinessException(ErrorCodes.FILE_SIZE_EXCEEDED, CommonMessages.FILE_TOO_LARGE);
        }
    }
    
    /**
     * 파일명 생성
     */
    private String generateFileName(String userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomString = SecurityUtils.generateSecureRandomString(8);
        
        return String.format("%s-%s-%s%s", userId, timestamp, randomString, extension);
    }
    
    /**
     * 기존 아바타 파일 삭제
     */
    private void deleteExistingAvatar(String userId, Path avatarDir) {
        try {
            Files.list(avatarDir)
                    .filter(path -> path.getFileName().toString().startsWith(userId + "-"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.debug("기존 아바타 파일 삭제: {}", path);
                        } catch (IOException e) {
                            log.warn("기존 아바타 파일 삭제 실패: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("기존 아바타 파일 확인 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 업로드 디렉토리 생성
     */
    private void createUploadDirectories() {
        try {
            Path uploadDir = Paths.get(uploadPath);
            Files.createDirectories(uploadDir);
            
            Path avatarDir = uploadDir.resolve("avatars");
            Files.createDirectories(avatarDir);
            
            log.info("업로드 디렉토리 생성 완료: {}", uploadPath);
        } catch (IOException e) {
            log.error("업로드 디렉토리 생성 실패: {}", uploadPath, e);
            throw new RuntimeException("업로드 디렉토리 생성에 실패했습니다", e);
        }
    }
}