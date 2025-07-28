# PlantUML 문법 검사 스크립트
param(
    [Parameter(Mandatory=$true)]
    [string]$FilePath
)

Write-Host "PlantUML 문법 검사 시작: $FilePath" -ForegroundColor Green

# 파일 존재 확인
if (!(Test-Path $FilePath)) {
    Write-Host "ERROR: File not found: $FilePath" -ForegroundColor Red
    exit 1
}

# PlantUML 기본 문법 검사
$content = Get-Content $FilePath -Raw
$errors = @()
$warnings = @()

# 1. @startuml/@enduml 쌍 검사
$startCount = ([regex]::Matches($content, '@startuml')).Count
$endCount = ([regex]::Matches($content, '@enduml')).Count

if ($startCount -eq 0) {
    $errors += "ERROR: @startuml tag missing"
}
if ($endCount -eq 0) {
    $errors += "ERROR: @enduml tag missing"
}
if ($startCount -ne $endCount) {
    $errors += "ERROR: @startuml and @enduml count mismatch (start: $startCount, end: $endCount)"
}

# 2. !theme 검사
if ($content -notmatch '!theme\s+mono') {
    $warnings += "WARNING: !theme mono not set"
}

# 3. title 검사
if ($content -notmatch 'title\s+.+') {
    $warnings += "WARNING: title not set"
}

# 4. 클래스 정의 문법 검사
$classDefinitions = [regex]::Matches($content, 'class\s+(\w+)\s*\{')
foreach ($match in $classDefinitions) {
    $className = $match.Groups[1].Value
    Write-Host "Found class: $className" -ForegroundColor Cyan
}

# 5. 인터페이스 정의 문법 검사
$interfaceDefinitions = [regex]::Matches($content, 'interface\s+(\w+)\s*\{')
foreach ($match in $interfaceDefinitions) {
    $interfaceName = $match.Groups[1].Value
    Write-Host "Found interface: $interfaceName" -ForegroundColor Cyan
}

# 6. enum 정의 문법 검사
$enumDefinitions = [regex]::Matches($content, 'enum\s+(\w+)\s*\{')
foreach ($match in $enumDefinitions) {
    $enumName = $match.Groups[1].Value
    Write-Host "Found enum: $enumName" -ForegroundColor Cyan
}

# 7. 패키지 정의 검사
$packageDefinitions = [regex]::Matches($content, 'package\s+"([^"]+)"')
foreach ($match in $packageDefinitions) {
    $packageName = $match.Groups[1].Value
    Write-Host "Found package: $packageName" -ForegroundColor Cyan
}

# 8. 관계 정의 검사
$relationships = [regex]::Matches($content, '(\w+)\s*(-->|\.\.>\|>|<\|--|<\|\.\.|\|>|o--|*--)\s*(\w+)')
$relationshipCount = $relationships.Count
Write-Host "Found relationships: $relationshipCount" -ForegroundColor Cyan

# 결과 출력
Write-Host ""
Write-Host "=== CHECK RESULTS ===" -ForegroundColor Yellow

if ($errors.Count -eq 0) {
    Write-Host "No syntax errors found" -ForegroundColor Green
} else {
    Write-Host "Errors found:" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "  $error" -ForegroundColor Red
    }
}

if ($warnings.Count -gt 0) {
    Write-Host "Warnings:" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "  $warning" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== STATISTICS ===" -ForegroundColor Cyan
Write-Host "- Classes: $($classDefinitions.Count)"
Write-Host "- Interfaces: $($interfaceDefinitions.Count)"
Write-Host "- Enums: $($enumDefinitions.Count)"
Write-Host "- Packages: $($packageDefinitions.Count)"
Write-Host "- Relationships: $relationshipCount"

# 종료 코드 반환
if ($errors.Count -eq 0) {
    Write-Host ""
    Write-Host "PLANTUML SYNTAX CHECK COMPLETED!" -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "PLANTUML SYNTAX CHECK FAILED!" -ForegroundColor Red
    exit 1
}