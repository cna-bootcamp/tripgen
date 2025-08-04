package com.unicorn.tripgen.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.ai.client.AIModelClient;
import com.unicorn.tripgen.ai.client.LocationServiceClient;
import com.unicorn.tripgen.ai.client.WeatherServiceClient;
import com.unicorn.tripgen.ai.dto.*;
import com.unicorn.tripgen.ai.entity.AIJob;
import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.AISchedule;
import com.unicorn.tripgen.ai.entity.JobStatus;
import com.unicorn.tripgen.ai.repository.AIScheduleRepository;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.InternalServerException;
import com.unicorn.tripgen.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 일정 생성 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIScheduleServiceImpl implements AIScheduleService {
    
    private final AIJobService aiJobService;
    private final AIScheduleRepository aiScheduleRepository;
    private final AIModelClient aiModelClient;
    private final LocationServiceClient locationServiceClient;
    private final WeatherServiceClient weatherServiceClient;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<GenerateScheduleResponse> generateSchedule(GenerateScheduleRequest request) {
        log.info("AI 일정 생성 요청: tripId={}", request.getTripId());
        
        return validateScheduleRequest(request)
                .flatMap(validRequest -> selectAIModel(true))
                .flatMap(modelType -> {
                    try {
                        // 비동기 작업 생성
                        String requestData = objectMapper.writeValueAsString(request);
                        AIJob job = aiJobService.createJob(
                            request.getTripId(), 
                            "SCHEDULE_GENERATION", 
                            modelType, 
                            requestData
                        );
                        
                        // 백그라운드에서 일정 생성 시작
                        processScheduleGeneration(job, request, modelType)
                                .subscribe(
                                    result -> log.info("일정 생성 완료: requestId={}", job.getRequestId()),
                                    error -> {
                                        log.error("일정 생성 실패: requestId=" + job.getRequestId(), error);
                                        aiJobService.failJob(job.getRequestId(), error.getMessage());
                                    }
                                );
                        
                        return Mono.just(GenerateScheduleResponse.success(job.getRequestId(), "약 3-5분"));
                        
                    } catch (JsonProcessingException e) {
                        return Mono.error(new InternalServerException(
                            ErrorCodes.INTERNAL_SERVER_ERROR,
                            "요청 데이터 직렬화 실패", e
                        ));
                    }
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<GenerationStatusResponse> getGenerationStatus(String requestId) {
        log.debug("일정 생성 상태 조회: requestId={}", requestId);
        
        return Mono.fromCallable(() -> aiJobService.getJobByRequestId(requestId))
                .map(jobOptional -> {
                    if (jobOptional.isEmpty()) {
                        throw new NotFoundException(
                            ErrorCodes.AI_JOB_NOT_FOUND,
                            "요청 ID를 찾을 수 없습니다: " + requestId
                        );
                    }
                    
                    AIJob job = jobOptional.get();
                    
                    switch (job.getStatus()) {
                        case QUEUED:
                            return GenerationStatusResponse.builder()
                                    .requestId(requestId)
                                    .status("queued")
                                    .progress(0)
                                    .currentStep("대기 중")
                                    .estimatedTime(job.getEstimatedTime())
                                    .build();
                        
                        case PROCESSING:
                            return GenerationStatusResponse.processing(
                                requestId,
                                job.getProgress(),
                                job.getCurrentStep(),
                                buildStepInfo(job),
                                job.getEstimatedTime()
                            );
                        
                        case COMPLETED:
                            return GenerationStatusResponse.completed(requestId);
                        
                        case FAILED:
                            return GenerationStatusResponse.failed(requestId, job.getErrorMessage());
                        
                        case CANCELLED:
                            return GenerationStatusResponse.builder()
                                    .requestId(requestId)
                                    .status("cancelled")
                                    .progress(0)
                                    .currentStep("취소됨")
                                    .build();
                        
                        default:
                            throw new BusinessException(
                                ErrorCodes.AI_JOB_FAILED,
                                "알 수 없는 작업 상태: " + job.getStatus()
                            );
                    }
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<GeneratedScheduleResponse> getGeneratedSchedule(String requestId) {
        log.debug("생성된 일정 조회: requestId={}", requestId);
        
        return Mono.fromCallable(() -> {
            // 작업 상태 확인
            AIJob job = aiJobService.getJobByRequestId(requestId)
                    .orElseThrow(() -> new NotFoundException(
                        ErrorCodes.AI_JOB_NOT_FOUND,
                        "요청 ID를 찾을 수 없습니다: " + requestId
                    ));
            
            if (job.getStatus() != JobStatus.COMPLETED) {
                throw new BusinessException(
                    ErrorCodes.AI_JOB_FAILED,
                    "아직 완료되지 않은 작업입니다"
                );
            }
            
            // 일정 데이터 조회
            AISchedule schedule = aiScheduleRepository.findByRequestId(requestId)
                    .orElseThrow(() -> new NotFoundException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "생성된 일정을 찾을 수 없습니다: " + requestId
                    ));
            
            try {
                return objectMapper.readValue(schedule.getScheduleData(), GeneratedScheduleResponse.class);
            } catch (JsonProcessingException e) {
                throw new InternalServerException(
                    ErrorCodes.AI_RESPONSE_PARSE_ERROR,
                    "일정 데이터 파싱 실패", e
                );
            }
        });
    }
    
    @Override
    public Mono<Void> cancelGeneration(String requestId) {
        log.info("일정 생성 취소: requestId={}", requestId);
        
        return Mono.fromRunnable(() -> aiJobService.cancelJob(requestId));
    }
    
    @Override
    public Mono<GenerateScheduleResponse> regenerateDaySchedule(RegenerateScheduleRequest request) {
        log.info("일자별 일정 재생성: tripId={}, day={}", request.getTripId(), request.getDay());
        
        return selectAIModel(false)
                .flatMap(modelType -> {
                    try {
                        String requestData = objectMapper.writeValueAsString(request);
                        AIJob job = aiJobService.createJob(
                            request.getTripId(), 
                            "DAY_SCHEDULE_REGENERATION", 
                            modelType, 
                            requestData
                        );
                        
                        // 백그라운드에서 재생성 시작
                        processScheduleRegeneration(job, request, modelType)
                                .subscribe(
                                    result -> log.info("일정 재생성 완료: requestId={}", job.getRequestId()),
                                    error -> {
                                        log.error("일정 재생성 실패: requestId=" + job.getRequestId(), error);
                                        aiJobService.failJob(job.getRequestId(), error.getMessage());
                                    }
                                );
                        
                        return Mono.just(GenerateScheduleResponse.success(job.getRequestId(), "약 2분"));
                        
                    } catch (JsonProcessingException e) {
                        return Mono.error(new InternalServerException(
                            ErrorCodes.INTERNAL_SERVER_ERROR,
                            "요청 데이터 직렬화 실패", e
                        ));
                    }
                });
    }
    
    @Override
    public Mono<WeatherImpactResponse> analyzeWeatherImpact(String tripId, WeatherImpactRequest request) {
        log.info("날씨 영향 분석: tripId={}", tripId);
        
        return selectAIModel(false)
                .flatMap(modelType -> {
                    String prompt = buildWeatherImpactPrompt(request);
                    Map<String, Object> context = Map.of("tripId", tripId);
                    
                    return aiModelClient.generateRecommendation(modelType, prompt, context)
                            .map(response -> {
                                try {
                                    return objectMapper.readValue(response, WeatherImpactResponse.class);
                                } catch (JsonProcessingException e) {
                                    throw new InternalServerException(
                                        ErrorCodes.AI_RESPONSE_PARSE_ERROR,
                                        "날씨 영향 분석 결과 파싱 실패", e
                                    );
                                }
                            });
                });
    }
    
    /**
     * 일정 생성 요청 유효성 검증
     */
    private Mono<GenerateScheduleRequest> validateScheduleRequest(GenerateScheduleRequest request) {
        // 기본 유효성 검증은 @Valid 어노테이션으로 처리됨
        // 추가 비즈니스 로직 검증 수행
        
        if (request.getDestinations().stream()
                .anyMatch(dest -> dest.getStartDate().isAfter(dest.getEndDate()))) {
            return Mono.error(new BusinessException(
                ErrorCodes.VALIDATION_FAILED,
                "시작 날짜가 종료 날짜보다 늦을 수 없습니다"
            ));
        }
        
        return Mono.just(request);
    }
    
    /**
     * AI 모델 선택
     */
    private Mono<AIModelType> selectAIModel(boolean requireHighPerformance) {
        return aiModelClient.selectOptimalModel(requireHighPerformance)
                .doOnNext(model -> log.debug("선택된 AI 모델: {}", model));
    }
    
    /**
     * 일정 생성 처리 (비동기)
     */
    private Mono<String> processScheduleGeneration(AIJob job, GenerateScheduleRequest request, AIModelType modelType) {
        return Mono.fromCallable(() -> aiJobService.startJob(job.getRequestId()))
                .flatMap(startedJob -> {
                    // 1단계: 위치 정보 수집
                    aiJobService.updateProgress(job.getRequestId(), 20, "위치 정보 수집 중");
                    return collectLocationData(request);
                })
                .flatMap(locationData -> {
                    // 2단계: 날씨 정보 수집
                    aiJobService.updateProgress(job.getRequestId(), 40, "날씨 정보 수집 중");
                    return collectWeatherData(request, locationData);
                })
                .flatMap(contextData -> {
                    // 3단계: AI 프롬프트 생성 및 실행
                    aiJobService.updateProgress(job.getRequestId(), 60, "AI 일정 생성 중");
                    String prompt = buildSchedulePrompt(request, contextData);
                    return aiModelClient.generateSchedule(modelType, prompt, contextData);
                })
                .flatMap(aiResponse -> {
                    // 4단계: 결과 저장
                    aiJobService.updateProgress(job.getRequestId(), 80, "결과 저장 중");
                    return saveScheduleResult(job, aiResponse);
                })
                .doOnSuccess(result -> {
                    aiJobService.updateProgress(job.getRequestId(), 100, "완료");
                    aiJobService.completeJob(job.getRequestId(), result);
                });
    }
    
    /**
     * 일정 재생성 처리 (비동기)
     */
    private Mono<String> processScheduleRegeneration(AIJob job, RegenerateScheduleRequest request, AIModelType modelType) {
        return Mono.fromCallable(() -> aiJobService.startJob(job.getRequestId()))
                .flatMap(startedJob -> {
                    aiJobService.updateProgress(job.getRequestId(), 30, "기존 일정 분석 중");
                    Map<String, Object> contextData = new HashMap<>();
                    contextData.put("request", request);
                    return Mono.just(contextData);
                })
                .flatMap(contextData -> {
                    aiJobService.updateProgress(job.getRequestId(), 60, "AI 재생성 중");
                    String prompt = buildRegenerationPrompt(request);
                    return aiModelClient.generateSchedule(modelType, prompt, contextData);
                })
                .flatMap(aiResponse -> {
                    aiJobService.updateProgress(job.getRequestId(), 90, "결과 저장 중");
                    return saveScheduleResult(job, (String) aiResponse);
                })
                .doOnSuccess(result -> {
                    aiJobService.completeJob(job.getRequestId(), result);
                });
    }
    
    /**
     * 위치 데이터 수집
     */
    private Mono<Map<String, Object>> collectLocationData(GenerateScheduleRequest request) {
        // Location Service에서 여행지 정보 수집
        return Mono.fromCallable(() -> {
            Map<String, Object> locationData = new HashMap<>();
            
            for (GenerateScheduleRequest.DestinationInfo destination : request.getDestinations()) {
                try {
                    var searchResult = locationServiceClient.searchLocations(destination.getDestinationName(), 5);
                    locationData.put(destination.getDestinationName(), searchResult);
                } catch (Exception e) {
                    log.warn("위치 정보 수집 실패: {}", destination.getDestinationName(), e);
                    locationData.put(destination.getDestinationName(), Map.of("error", e.getMessage()));
                }
            }
            
            return locationData;
        });
    }
    
    /**
     * 날씨 데이터 수집
     */
    private Mono<Map<String, Object>> collectWeatherData(GenerateScheduleRequest request, Map<String, Object> locationData) {
        return Mono.fromCallable(() -> {
            Map<String, Object> contextData = new HashMap<>(locationData);
            
            for (GenerateScheduleRequest.DestinationInfo destination : request.getDestinations()) {
                try {
                    var weatherData = weatherServiceClient.getWeatherForecast(
                            0.0, 0.0, // 실제로는 locationData에서 좌표 추출
                            destination.getStartDate(),
                            destination.getEndDate()
                    );
                    contextData.put(destination.getDestinationName() + "_weather", weatherData);
                } catch (Exception e) {
                    log.warn("날씨 정보 수집 실패: {}", destination.getDestinationName(), e);
                }
            }
            
            return contextData;
        });
    }
    
    /**
     * 일정 생성 프롬프트 구성
     */
    private String buildSchedulePrompt(GenerateScheduleRequest request, Map<String, Object> contextData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 정보를 기반으로 최적화된 여행 일정을 JSON 형태로 생성해주세요.\n\n");
        prompt.append("여행 정보:\n");
        prompt.append("- 여행명: ").append(request.getTripName()).append("\n");
        prompt.append("- 이동수단: ").append(request.getTransportMode()).append("\n");
        prompt.append("- 시작 시간: ").append(request.getStartTime()).append("\n");
        
        prompt.append("\n멤버 정보:\n");
        for (GenerateScheduleRequest.MemberInfo member : request.getMembers()) {
            prompt.append("- ").append(member.getName())
                    .append(" (").append(member.getAge()).append("세, ")
                    .append(member.getGender()).append(", ")
                    .append("건강상태: ").append(member.getHealthStatus()).append(")\n");
        }
        
        prompt.append("\n여행지 정보:\n");
        for (GenerateScheduleRequest.DestinationInfo destination : request.getDestinations()) {
            prompt.append("- ").append(destination.getDestinationName())
                    .append(" (").append(destination.getStartDate())
                    .append(" ~ ").append(destination.getEndDate()).append(")\n");
        }
        
        if (request.getSpecialRequests() != null) {
            prompt.append("\n특별 요청사항: ").append(request.getSpecialRequests()).append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 재생성 프롬프트 구성
     */
    private String buildRegenerationPrompt(RegenerateScheduleRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 조건에 따라 ").append(request.getDay()).append("일차 일정을 재생성해주세요.\n\n");
        
        if (request.getExistingSchedules() != null && !request.getExistingSchedules().isEmpty()) {
            prompt.append("기존 일정 (연속성 유지):\n");
            for (RegenerateScheduleRequest.ExistingSchedule schedule : request.getExistingSchedules()) {
                prompt.append("- ").append(schedule.getDay()).append("일차: ")
                        .append(String.join(", ", schedule.getPlaces())).append("\n");
            }
        }
        
        if (request.getSpecialRequests() != null) {
            prompt.append("\n특별 요청사항: ").append(request.getSpecialRequests()).append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 날씨 영향 분석 프롬프트 구성
     */
    private String buildWeatherImpactPrompt(WeatherImpactRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 일정에 대한 날씨 변화의 영향을 분석하고 대응 방안을 JSON으로 제공해주세요.\n\n");
        
        prompt.append("일정 정보:\n");
        for (WeatherImpactRequest.ScheduleInfo schedule : request.getSchedules()) {
            prompt.append("- ").append(schedule.getDay()).append("일차 (")
                    .append(schedule.getDate()).append("): ");
            if (schedule.getPlaces() != null) {
                prompt.append(schedule.getPlaces().size()).append("개 장소");
            }
            prompt.append("\n");
        }
        
        prompt.append("\n날씨 변화:\n");
        for (WeatherImpactRequest.WeatherChange change : request.getWeatherChanges()) {
            prompt.append("- ").append(change.getDate()).append(": ")
                    .append(change.getPreviousCondition()).append(" → ")
                    .append(change.getNewCondition())
                    .append(" (심각도: ").append(change.getSeverity()).append(")\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 일정 결과 저장
     */
    private Mono<String> saveScheduleResult(AIJob job, String aiResponse) {
        return Mono.fromCallable(() -> {
            try {
                AISchedule schedule = AISchedule.builder()
                        .requestId(job.getRequestId())
                        .tripId(job.getTripId())
                        .aiModelType(job.getAiModelType())
                        .scheduleData(aiResponse)
                        .generationTime(calculateGenerationTime(job))
                        .generatedAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
                
                aiScheduleRepository.save(schedule);
                return aiResponse;
                
            } catch (Exception e) {
                throw new InternalServerException(
                    ErrorCodes.INTERNAL_SERVER_ERROR,
                    "일정 결과 저장 실패", e
                );
            }
        });
    }
    
    /**
     * 생성 시간 계산
     */
    private Integer calculateGenerationTime(AIJob job) {
        if (job.getStartedAt() != null) {
            return (int) java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).getSeconds();
        }
        return null;
    }
    
    /**
     * 단계 정보 구성
     */
    private List<GenerationStatusResponse.StepInfo> buildStepInfo(AIJob job) {
        List<String> steps = List.of(
            "위치 정보 수집", "날씨 정보 수집", "AI 일정 생성", "결과 저장"
        );
        
        return steps.stream()
                .map(step -> {
                    String status = step.equals(job.getCurrentStep()) ? "processing" : 
                                   steps.indexOf(step) < steps.indexOf(job.getCurrentStep()) ? "completed" : "pending";
                    return GenerationStatusResponse.StepInfo.builder()
                            .step(step)
                            .status(status)
                            .build();
                })
                .collect(Collectors.toList());
    }
}