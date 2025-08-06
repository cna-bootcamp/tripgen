#\!/bin/bash

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3YTRjMThkNC1jODIzLTRmMTUtODAzOC04NDQ1NzE1NWMwYTAiLCJpYXQiOjE3NTQ0NTg1MTAsImV4cCI6MTc1NDQ2MjExMCwidHlwZSI6ImFjY2VzcyJ9.KT4OxW8X_Rw30eCSBiA0gTonw6HfRX_KEvWtagyofmQ"
BASE_URL="http://localhost:8082/api/v1/locations"

echo "=== 1. Keyword Search Test ==="
curl -X GET "$BASE_URL/search/keyword?keyword=Munich&latitude=48.1374&longitude=11.5755&radius=5000" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 2. Weather Test ==="
curl -X GET "$BASE_URL/weather?latitude=48.1374&longitude=11.5755" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 3. Route Test ==="
curl -X GET "$BASE_URL/route?origin_lat=48.1374&origin_lng=11.5755&dest_lat=48.1351&dest_lng=11.5820" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 4. Popular Locations Test ==="
curl -X GET "$BASE_URL/popular?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 5. Top Rated Locations Test ==="
curl -X GET "$BASE_URL/top-rated?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 6. Autocomplete Test ==="
curl -X GET "$BASE_URL/autocomplete?input=Munich" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  | jq

echo -e "\n=== 7. Nearby Places Test ==="
curl -X POST "$BASE_URL/search/nearby" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "departure": {
      "latitude": 48.1374,
      "longitude": 11.5755
    },
    "transportMode": "walking",
    "timeRange": 15
  }' \
  | jq
