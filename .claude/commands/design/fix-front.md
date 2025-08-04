---
command: "/design:fix-front"
---

**3.오류수정**   
```
@fix as @front  
아래 오류를 해결해 주세요.  
{오류내용} 
```

**Tip) 이미지를 제공하는 방법**   
CLAUDE.md 파일에 파일 위치를 정의한 @error, @info 약어가 있음   
- @error: debug/error.png 
- @info: debug/info.png  

1)에러 화면인지 정보 제공 화면인지에 따라 화면을 캡처하여 파일로 저장  
2)프롬프트팅에 약어를 이용   
사용예시)
```
@fix as @front 
아래 오류를 해결해 주세요. see @error 
{오류내용} 
```