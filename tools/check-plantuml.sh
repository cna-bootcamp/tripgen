#!/bin/bash
# PlantUML 문법 검사기 (Linux/Mac)
# 사용법: ./check-plantuml.sh [파일경로]

# PlantUML JAR 파일 경로 설정
PLANTUML_JAR="/usr/local/bin/plantuml.jar"

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# PlantUML JAR 파일 존재 여부 확인
if [ ! -f "$PLANTUML_JAR" ]; then
    echo -e "${RED}❌ PlantUML JAR 파일을 찾을 수 없습니다: $PLANTUML_JAR${NC}"
    echo ""
    echo -e "${YELLOW}PlantUML 설치 방법:${NC}"
    echo "1. https://plantuml.com/download 에서 plantuml.jar 다운로드"
    echo "2. sudo mv plantuml.jar /usr/local/bin/"
    echo "3. Java가 설치되어 있는지 확인 (java -version)"
    echo ""
    echo -e "${YELLOW}또는 Homebrew로 설치:${NC}"
    echo "brew install plantuml"
    exit 1
fi

# Java 설치 여부 확인
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java가 설치되어 있지 않습니다${NC}"
    echo "Java를 설치한 후 다시 시도하세요"
    echo "macOS: brew install openjdk"
    exit 1
fi

# 입력 파일 처리
if [ $# -eq 0 ]; then
    files=$(find . -name "*.puml" -type f)
    if [ -z "$files" ]; then
        echo -e "${RED}❌ 검사할 .puml 파일을 찾을 수 없습니다${NC}"
        exit 1
    fi
else
    if [ ! -f "$1" ]; then
        echo -e "${RED}❌ 파일을 찾을 수 없습니다: $1${NC}"
        exit 1
    fi
    files="$1"
fi

echo -e "${CYAN}🔍 PlantUML 문법 검사를 시작합니다...${NC}"
echo ""

total_files=0
passed_files=0
failed_files=0

for file in $files; do
    ((total_files++))
    filename=$(basename "$file")
    echo -e "${WHITE}📁 검사 중: $filename${NC}"
    
    if java -jar "$PLANTUML_JAR" -checkonly "$file" 2>/dev/null; then
        echo -e "   ${GREEN}✅ 문법 검사 통과${NC}"
        ((passed_files++))
    else
        echo -e "   ${RED}❌ 문법 오류 발견${NC}"
        echo -e "   ${YELLOW}오류 내용:${NC}"
        java -jar "$PLANTUML_JAR" -checkonly "$file" 2>&1 | sed 's/^/   /'
        ((failed_files++))
    fi
    echo ""
done

echo -e "${CYAN}===========================================${NC}"
echo -e "${CYAN}📊 PlantUML 문법 검사 완료${NC}"
echo -e "${CYAN}===========================================${NC}"
echo -e "${WHITE}총 파일 수: $total_files${NC}"
echo -e "${GREEN}통과: $passed_files${NC}"
echo -e "${RED}실패: $failed_files${NC}"

if [ $failed_files -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 모든 파일이 문법 검사를 통과했습니다!${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}⚠️  $failed_files 개 파일에서 문법 오류가 발견되었습니다${NC}"
    echo -e "${YELLOW}위의 오류를 수정한 후 다시 검사해주세요${NC}"
    exit 1
fi
