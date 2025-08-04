package com.unicorn.tripgen.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 데이터를 포함한 API 응답 클래스
 * 
 * @param <T> 응답 데이터 타입
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> extends BaseResponse {
    
    /**
     * 응답 데이터
     */
    private T data;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 기본 생성자
     */
    public ApiResponse() {
        super();
    }
    
    /**
     * 데이터 포함 생성자
     * 
     * @param data 응답 데이터
     */
    public ApiResponse(T data) {
        super();
        this.data = data;
    }
    
    /**
     * 데이터와 메시지 포함 생성자
     * 
     * @param data 응답 데이터
     * @param message 응답 메시지
     */
    public ApiResponse(T data, String message) {
        super();
        this.data = data;
        this.message = message;
    }
    
    /**
     * 성공 응답 생성 팩토리 메소드
     * 
     * @param <T> 데이터 타입
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
    
    /**
     * 성공 응답 생성 팩토리 메소드 (메시지 포함)
     * 
     * @param <T> 데이터 타입
     * @param data 응답 데이터
     * @param message 응답 메시지
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }
    
    /**
     * 실패 응답 생성 팩토리 메소드
     * 
     * @param <T> 데이터 타입
     * @param message 실패 메시지
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}