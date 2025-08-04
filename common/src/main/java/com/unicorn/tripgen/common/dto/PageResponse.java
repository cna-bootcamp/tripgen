package com.unicorn.tripgen.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 처리된 응답 클래스
 * 
 * @param <T> 페이징 대상 데이터 타입
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> extends BaseResponse {
    
    /**
     * 페이징된 데이터 목록
     */
    private List<T> content;
    
    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int page;
    
    /**
     * 페이지 크기
     */
    private int size;
    
    /**
     * 전체 요소 개수
     */
    private long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private int totalPages;
    
    /**
     * 첫 번째 페이지 여부
     */
    private boolean first;
    
    /**
     * 마지막 페이지 여부
     */
    private boolean last;
    
    /**
     * 기본 생성자
     */
    public PageResponse() {
        super();
    }
    
    /**
     * Spring Data Page 객체로부터 PageResponse 생성
     * 
     * @param page Spring Data Page 객체
     */
    public PageResponse(Page<T> page) {
        super();
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
    
    /**
     * Spring Data Page 객체로부터 PageResponse 생성하는 팩토리 메소드
     * 
     * @param <T> 데이터 타입
     * @param page Spring Data Page 객체
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
    
    /**
     * 페이징 정보를 직접 설정하는 팩토리 메소드
     * 
     * @param <T> 데이터 타입
     * @param content 데이터 목록
     * @param page 현재 페이지
     * @param size 페이지 크기
     * @param totalElements 전체 요소 개수
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / size));
        response.setFirst(page == 0);
        response.setLast(page == response.getTotalPages() - 1);
        return response;
    }
}