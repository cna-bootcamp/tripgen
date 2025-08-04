package com.unicorn.tripgen.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 서비스 인터페이스
 * 파일 업로드, 저장, 삭제를 담당
 */
public interface FileStorageService {
    
    /**
     * 아바타 이미지 업로드
     * 
     * @param userId 사용자 ID
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     */
    String uploadAvatar(String userId, MultipartFile file);
    
    /**
     * 파일 삭제
     * 
     * @param fileUrl 삭제할 파일 URL
     */
    void deleteFile(String fileUrl);
    
    /**
     * 파일 존재 여부 확인
     * 
     * @param fileUrl 확인할 파일 URL
     * @return 존재 여부
     */
    boolean fileExists(String fileUrl);
    
    /**
     * 허용된 파일 형식인지 확인
     * 
     * @param file 확인할 파일
     * @return 허용 여부
     */
    boolean isAllowedFileType(MultipartFile file);
    
    /**
     * 허용된 파일 크기인지 확인
     * 
     * @param file 확인할 파일
     * @return 허용 여부
     */
    boolean isAllowedFileSize(MultipartFile file);
}