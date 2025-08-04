package com.unicorn.tripgen.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.ai.client.AIModelClient;
import com.unicorn.tripgen.ai.dto.RecommendationRequest;
import com.unicorn.tripgen.ai.dto.RecommendationResponse;
import com.unicorn.tripgen.ai.entity.AIRecommendation;
import com.unicorn.tripgen.ai.repository.AIRecommendationRepository;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 추천 정보 생성 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationServiceImpl implements AIRecommendationService {
    
    private final AIRecommendationRepository aiRecommendationRepository;
    private final AIModelClient aiModelClient;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<RecommendationResponse> generatePlaceRecommendations(String placeId, RecommendationRequest request) {
        log.info("AI 추천 정보 생성: placeId={}, placeName={}", placeId, request.getPlaceName());
        
        String userProfileHash = generateUserProfileHash(request.getUserProfile());
        
        // 캐시된 추천 정보 확인
        return getCachedRecommendation(placeId, userProfileHash)
                .switchIfEmpty(generateNewRecommendation(placeId, request, userProfileHash))
                .doOnNext(recommendation -> recordRecommendationAccess(placeId, userProfileHash));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<RecommendationResponse> getCachedRecommendation(String placeId, String userProfileHash) {
        log.debug("캐시된 추천 정보 조회: placeId={}, profileHash={}", placeId, userProfileHash);
        
        return Mono.fromCallable(() -> {
            return aiRecommendationRepository.findValidRecommendation(placeId, userProfileHash, LocalDateTime.now())
                    .map(this::convertToResponse)
                    .orElse(null);
        })
        .doOnNext(recommendation -> {
            if (recommendation != null) {
                log.debug("캐시된 추천 정보 발견: placeId={}", placeId);
            }
        });
    }
    
    @Override
    @Transactional
    public Mono<Void> invalidateRecommendationCache(String placeId) {
        log.info("추천 정보 캐시 무효화: placeId={}", placeId);
        
        return Mono.fromRunnable(() -> {
            List<AIRecommendation> recommendations = aiRecommendationRepository.findByPlaceIdOrderByGeneratedAtDesc(placeId);
            recommendations.forEach(rec -> rec.setCacheExpiresAt(LocalDateTime.now().minusHours(1)));
            aiRecommendationRepository.saveAll(recommendations);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<List<RecommendationResponse>> getPopularRecommendations(int limit) {
        log.debug("인기 추천 정보 조회: limit={}", limit);
        
        return Mono.fromCallable(() -> {
            List<AIRecommendation> popularRecommendations = aiRecommendationRepository
                    .findPopularRecommendations(10)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            return popularRecommendations.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    @Transactional
    public Mono<Integer> cleanupOldRecommendations(int daysOld) {
        log.info("오래된 추천 정보 정리: daysOld={}", daysOld);
        
        return Mono.fromCallable(() -> {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
            List<AIRecommendation> oldRecommendations = aiRecommendationRepository
                    .findUnusedOldRecommendations(cutoffTime);
            
            if (!oldRecommendations.isEmpty()) {
                aiRecommendationRepository.deleteAll(oldRecommendations);
                log.info("오래된 추천 정보 정리 완료: 삭제된 항목 수={}", oldRecommendations.size());
            }
            
            return oldRecommendations.size();
        });
    }
    
    /**
     * 새로운 추천 정보 생성
     */
    private Mono<RecommendationResponse> generateNewRecommendation(String placeId, RecommendationRequest request, String userProfileHash) {
        log.debug("새로운 추천 정보 생성: placeId={}", placeId);
        
        return aiModelClient.selectOptimalModel(false)
                .flatMap(modelType -> {
                    String prompt = buildRecommendationPrompt(request);
                    Map<String, Object> context = Map.of(
                        "placeId", placeId,
                        "userProfileHash", userProfileHash
                    );
                    
                    return aiModelClient.generateRecommendation(modelType, prompt, context)
                            .map(aiResponse -> {
                                try {
                                    // AI 응답을 RecommendationResponse로 파싱
                                    RecommendationResponse response = parseAIResponse(aiResponse, placeId);
                                    
                                    // 데이터베이스에 저장
                                    saveRecommendation(placeId, request, userProfileHash, modelType, aiResponse);
                                    
                                    return response;
                                    
                                } catch (Exception e) {
                                    log.error("추천 정보 생성 실패: placeId=" + placeId, e);
                                    throw new InternalServerException(
                                        ErrorCodes.RECOMMENDATION_GENERATION_FAILED,
                                        "추천 정보 생성 중 오류가 발생했습니다", e
                                    );
                                }
                            });
                });
    }
    
    /**
     * 추천 정보 접근 기록
     */
    private void recordRecommendationAccess(String placeId, String userProfileHash) {
        try {
            aiRecommendationRepository.findValidRecommendation(placeId, userProfileHash, LocalDateTime.now())
                    .ifPresent(recommendation -> {
                        recommendation.recordAccess();
                        aiRecommendationRepository.save(recommendation);
                    });
        } catch (Exception e) {
            log.warn("추천 정보 접근 기록 실패: placeId=" + placeId, e);
        }
    }
    
    /**
     * 사용자 프로필 해시 생성
     */
    private String generateUserProfileHash(RecommendationRequest.UserProfile userProfile) {
        try {
            String profileString = String.format("%s_%s_%s_%s",
                    userProfile.getMemberComposition(),
                    userProfile.getHealthStatus(),
                    userProfile.getTransportMode(),
                    userProfile.getPreferences() != null ? String.join(",", userProfile.getPreferences()) : ""
            );
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(profileString.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException(
                ErrorCodes.INTERNAL_SERVER_ERROR,
                "해시 생성 실패", e
            );
        }
    }
    
    /**
     * 추천 프롬프트 구성
     */
    private String buildRecommendationPrompt(RecommendationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 장소에 대한 맞춤형 추천 정보를 JSON 형태로 제공해주세요.\n\n");
        
        prompt.append("장소 정보:\n");
        prompt.append("- 장소명: ").append(request.getPlaceName()).append("\n");
        prompt.append("- 장소 타입: ").append(request.getPlaceType()).append("\n");
        if (request.getPlaceAddress() != null) {
            prompt.append("- 주소: ").append(request.getPlaceAddress()).append("\n");
        }
        
        prompt.append("\n사용자 프로필:\n");
        var userProfile = request.getUserProfile();
        if (userProfile.getMemberComposition() != null) {
            prompt.append("- 구성원: ").append(userProfile.getMemberComposition()).append("\n");
        }
        if (userProfile.getHealthStatus() != null) {
            prompt.append("- 건강 상태: ").append(userProfile.getHealthStatus()).append("\n");
        }
        if (userProfile.getTransportMode() != null) {
            prompt.append("- 이동수단: ").append(userProfile.getTransportMode()).append("\n");
        }
        if (userProfile.getPreferences() != null && !userProfile.getPreferences().isEmpty()) {
            prompt.append("- 선호도: ").append(String.join(", ", userProfile.getPreferences())).append("\n");
        }
        
        if (request.getTripContext() != null) {
            var tripContext = request.getTripContext();
            prompt.append("\n여행 맥락:\n");
            if (tripContext.getVisitDate() != null) {
                prompt.append("- 방문 날짜: ").append(tripContext.getVisitDate()).append("\n");
            }
            if (tripContext.getVisitTime() != null) {
                prompt.append("- 방문 시간: ").append(tripContext.getVisitTime()).append("\n");
            }
            if (tripContext.getPreviousPlace() != null) {
                prompt.append("- 이전 장소: ").append(tripContext.getPreviousPlace()).append("\n");
            }
            if (tripContext.getNextPlace() != null) {
                prompt.append("- 다음 장소: ").append(tripContext.getNextPlace()).append("\n");
            }
        }
        
        prompt.append("\n다음 형식의 JSON으로 응답해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendations\": {\n");
        prompt.append("    \"reasons\": [\"추천 이유들\"],\n");
        prompt.append("    \"tips\": {\n");
        prompt.append("      \"description\": \"장소 설명\",\n");
        prompt.append("      \"events\": [\"특별 이벤트들\"],\n");
        prompt.append("      \"bestVisitTime\": \"최적 방문 시간\",\n");
        prompt.append("      \"estimatedDuration\": \"예상 소요 시간\",\n");
        prompt.append("      \"photoSpots\": [\"포토 스팟들\"],\n");
        prompt.append("      \"practicalTips\": [\"실용적 팁들\"],\n");
        prompt.append("      \"alternativePlaces\": [{\"name\": \"대체 장소\", \"reason\": \"추천 이유\", \"distance\": \"거리\"}]\n");
        prompt.append("    }\n");
        prompt.append("  }\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    /**
     * AI 응답 파싱
     */
    private RecommendationResponse parseAIResponse(String aiResponse, String placeId) throws JsonProcessingException {
        RecommendationResponse response = objectMapper.readValue(aiResponse, RecommendationResponse.class);
        response.setPlaceId(placeId);
        response.setGeneratedAt(LocalDateTime.now());
        response.setCacheExpiry(LocalDateTime.now().plusDays(7));
        return response;
    }
    
    /**
     * 추천 정보 저장
     */
    @Transactional
    protected void saveRecommendation(String placeId, RecommendationRequest request, String userProfileHash, 
                                    com.unicorn.tripgen.ai.entity.AIModelType modelType, String aiResponse) {
        try {
            AIRecommendation recommendation = AIRecommendation.builder()
                    .placeId(placeId)
                    .placeName(request.getPlaceName())
                    .placeType(request.getPlaceType())
                    .aiModelType(modelType)
                    .userProfileHash(userProfileHash)
                    .recommendationData(aiResponse)
                    .generatedAt(LocalDateTime.now())
                    .cacheExpiresAt(LocalDateTime.now().plusDays(7))
                    .accessCount(0)
                    .build();
            
            aiRecommendationRepository.save(recommendation);
            log.debug("추천 정보 저장 완료: placeId={}", placeId);
            
        } catch (Exception e) {
            log.error("추천 정보 저장 실패: placeId=" + placeId, e);
            // 저장 실패해도 응답은 반환하도록 함
        }
    }
    
    /**
     * AIRecommendation 엔티티를 RecommendationResponse로 변환
     */
    private RecommendationResponse convertToResponse(AIRecommendation recommendation) {
        try {
            RecommendationResponse response = objectMapper.readValue(
                recommendation.getRecommendationData(), 
                RecommendationResponse.class
            );
            response.setPlaceId(recommendation.getPlaceId());
            response.setGeneratedAt(recommendation.getGeneratedAt());
            response.setCacheExpiry(recommendation.getCacheExpiresAt());
            return response;
            
        } catch (JsonProcessingException e) {
            log.error("추천 정보 변환 실패: placeId=" + recommendation.getPlaceId(), e);
            throw new InternalServerException(
                ErrorCodes.AI_RESPONSE_PARSE_ERROR,
                "추천 정보 파싱 실패", e
            );
        }
    }
}