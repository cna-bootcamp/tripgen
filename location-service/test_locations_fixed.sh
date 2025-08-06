#\!/bin/bash

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3YTRjMThkNC1jODIzLTRmMTUtODAzOC04NDQ1NzE1NWMwYTAiLCJpYXQiOjE3NTQ0NTg1MTAsImV4cCI6MTc1NDQ2MjExMCwidHlwZSI6ImFjY2VzcyJ9.KT4OxW8X_Rw30eCSBiA0gTonw6HfRX_KEvWtagyofmQ"
BASE_URL="http://localhost:8082/api/v1/locations"

echo "=== 1. Nearby Places Test (Fixed) ==="
curl -X POST "$BASE_URL/search/nearby" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "origin": {
      "latitude": 48.1374,
      "longitude": 11.5755
    },
    "transportMode": "walking",
    "timeRange": 15
  }' \
  | jq

echo -e "\n=== 2. Place Details Test ==="
curl -X GET "$BASE_URL/places/ChIJU5LMbCt1nkcRGwGLFraPTBg?includeAI=true&includeReviews=true" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 3. Place Recommendations Test ==="
curl -X GET "$BASE_URL/locations/ChIJU5LMbCt1nkcRGwGLFraPTBg/recommendations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 4. Business Hours Test ==="
curl -X GET "$BASE_URL/places/ChIJU5LMbCt1nkcRGwGLFraPTBg/business-hours" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 5. Sync Location Data Test ==="
curl -X POST "$BASE_URL/places/ChIJU5LMbCt1nkcRGwGLFraPTBg/sync?forceUpdate=false" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq
