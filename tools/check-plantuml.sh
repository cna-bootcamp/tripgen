#!/bin/bash

# PlantUML 문법 검사 스크립트 (macOS/Linux용)

if [ $# -eq 0 ]; then
    echo "사용법: $0 <PlantUML 파일 경로> [추가 파일들...]"
    exit 1
fi

for file_path in "$@"; do
    echo "========================================="
    echo "PlantUML 문법 검사를 시작합니다: $file_path"
    echo "========================================="
    
    # 파일 존재 확인
    if [ ! -f "$file_path" ]; then
        echo "❌ 오류: 파일을 찾을 수 없습니다 - $file_path"
        continue
    fi
    
    # 파일 읽기
    content=$(cat "$file_path")
    errors=()
    warnings=()
    
    # 1. @startuml/@enduml 쌍 검사
    start_count=$(echo "$content" | grep -c '@startuml')
    end_count=$(echo "$content" | grep -c '@enduml')
    
    if [ "$start_count" -ne "$end_count" ]; then
        errors+=("@startuml과 @enduml의 개수가 일치하지 않습니다. (시작: $start_count, 끝: $end_count)")
    fi
    
    if [ "$start_count" -eq 0 ]; then
        errors+=("@startuml이 없습니다.")
    fi
    
    # 2. 테마 설정 확인
    if ! echo "$content" | grep -q '!theme[[:space:]]\+mono'; then
        warnings+=("권장 테마 '!theme mono'가 설정되지 않았습니다.")
    fi
    
    # 3. 기본 구문 검사
    line_number=0
    while IFS= read -r line; do
        ((line_number++))
        line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')  # trim
        
        if [ -z "$line" ] || [[ "$line" == \'* ]]; then
            continue  # 빈 라인이나 주석 라인 스킵
        fi
        
        # 클래스 정의 검사
        if echo "$line" | grep -q '^class[[:space:]]\+[[:alnum:]_]\+[[:space:]]*{*$'; then
            # 올바른 클래스 정의
            :
        elif echo "$line" | grep -q '^class[[:space:]]\+'; then
            if ! echo "$line" | grep -q '{$' && ! echo "$line" | grep -q '}$'; then
                warnings+=("라인 $line_number: 클래스 정의 후 '{' 또는 완전한 정의가 필요할 수 있습니다.")
            fi
        fi
        
        # 패키지 정의 검사
        if echo "$line" | grep -q '^package[[:space:]]\+"[^"]\+"[[:space:]]*{*$'; then
            # 올바른 패키지 정의
            :
        elif echo "$line" | grep -q '^package[[:space:]]\+'; then
            if ! echo "$line" | grep -q '"[[:space:]]*{*$'; then
                warnings+=("라인 $line_number: 패키지명은 따옴표로 감싸는 것이 좋습니다.")
            fi
        fi
    done < "$file_path"
    
    # 4. 한국어 인코딩 검사
    if echo "$content" | grep -q '[가-힣]'; then
        echo "⚠️  한국어 텍스트가 발견되었습니다. UTF-8 인코딩을 확인하세요."
    fi
    
    # 결과 출력
    echo
    echo "=== 문법 검사 결과 ==="
    
    if [ ${#errors[@]} -eq 0 ]; then
        echo "✅ 심각한 문법 오류가 발견되지 않았습니다."
    else
        echo "❌ 오류 발견:"
        for error in "${errors[@]}"; do
            echo "  - $error"
        done
    fi
    
    if [ ${#warnings[@]} -gt 0 ]; then
        echo "⚠️  경고:"
        for warning in "${warnings[@]}"; do
            echo "  - $warning"
        done
    fi
    
    # 기본 통계
    echo
    echo "=== 기본 통계 ==="
    echo "- 전체 라인 수: $(wc -l < "$file_path")"
    echo "- @startuml 개수: $start_count"
    echo "- @enduml 개수: $end_count"
    echo "- 클래스 개수: $(echo "$content" | grep -c 'class[[:space:]]\+[[:alnum:]_]\+')"
    echo "- 인터페이스 개수: $(echo "$content" | grep -c 'interface[[:space:]]\+[[:alnum:]_]\+')"
    echo "- 패키지 개수: $(echo "$content" | grep -c 'package[[:space:]]\+')"
    echo
done

echo "모든 파일 검사가 완료되었습니다."