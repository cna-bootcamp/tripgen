# PlantUML 문법 검사기 (Windows PowerShell)
# 사용법: .\check-plantuml.ps1 [파일경로]

param(
    [Parameter(Position=0)]
    [string]$InputFile = ""
)

# PlantUML JAR 파일 경로 설정
$PlantUMLJar = "C:\tools\plantuml.jar"

# PlantUML JAR 파일 존재 여부 확인
if (-not (Test-Path $PlantUMLJar)) {
    Write-Host "❌ PlantUML JAR 파일을 찾을 수 없습니다: $PlantUMLJar" -ForegroundColor Red
    Write-Host ""
    Write-Host "PlantUML 설치 방법:" -ForegroundColor Yellow
    Write-Host "1. https://plantuml.com/download 에서 plantuml.jar 다운로드"
    Write-Host "2. C:\tools\ 디렉토리에 plantuml.jar 파일 복사"
    Write-Host "3. Java가 설치되어 있는지 확인 (java -version)"
    exit 1
}

# Java 설치 여부 확인
try {
    $null = java -version 2>&1
} catch {
    Write-Host "❌ Java가 설치되어 있지 않습니다" -ForegroundColor Red
    Write-Host "Java를 설치한 후 다시 시도하세요: https://www.oracle.com/java/technologies/downloads/"
    exit 1
}

# 입력 파일 처리
if ($InputFile -eq "") {
    # 현재 디렉토리의 모든 .puml 파일 검사
    $files = Get-ChildItem -Path "." -Filter "*.puml" -Recurse
    if ($files.Count -eq 0) {
        Write-Host "❌ 검사할 .puml 파일을 찾을 수 없습니다" -ForegroundColor Red
        exit 1
    }
} else {
    # 지정된 파일 검사
    if (-not (Test-Path $InputFile)) {
        Write-Host "❌ 파일을 찾을 수 없습니다: $InputFile" -ForegroundColor Red
        exit 1
    }
    $files = @(Get-Item $InputFile)
}

Write-Host "🔍 PlantUML 문법 검사를 시작합니다..." -ForegroundColor Cyan
Write-Host ""

$totalFiles = 0
$passedFiles = 0
$failedFiles = 0

foreach ($file in $files) {
    $totalFiles++
    Write-Host "📁 검사 중: $($file.Name)" -ForegroundColor White
    
    # PlantUML 문법 검사 실행
    $result = java -jar $PlantUMLJar -checkonly $file.FullName 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ 문법 검사 통과" -ForegroundColor Green
        $passedFiles++
    } else {
        Write-Host "   ❌ 문법 오류 발견" -ForegroundColor Red
        Write-Host "   오류 내용:" -ForegroundColor Yellow
        $result | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
        $failedFiles++
    }
    Write-Host ""
}

# 검사 결과 요약
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "📊 PlantUML 문법 검사 완료" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "총 파일 수: $totalFiles" -ForegroundColor White
Write-Host "통과: $passedFiles" -ForegroundColor Green
Write-Host "실패: $failedFiles" -ForegroundColor Red

if ($failedFiles -eq 0) {
    Write-Host ""
    Write-Host "🎉 모든 파일이 문법 검사를 통과했습니다!" -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "⚠️  $failedFiles 개 파일에서 문법 오류가 발견되었습니다" -ForegroundColor Red
    Write-Host "위의 오류를 수정한 후 다시 검사해주세요" -ForegroundColor Yellow
    exit 1
}