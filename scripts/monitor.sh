#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}📊 File Extension API 모니터링 대시보드${NC}"
echo "=================================================="

# 시스템 상태
echo -e "${BLUE}🖥️  시스템 상태${NC}"
echo "CPU 사용률: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
echo "메모리 사용률: $(free | grep Mem | awk '{printf("%.1f%%", $3/$2 * 100.0)}')"
echo "디스크 사용률: $(df -h / | awk 'NR==2{printf "%s", $5}')"
echo

# Docker 컨테이너 상태
echo -e "${BLUE}🐳 Docker 컨테이너 상태${NC}"
cd /opt/file-extension-api
docker-compose ps
echo

# Nginx 상태
echo -e "${BLUE}🌐 Nginx 상태${NC}"
echo "상태: $(sudo systemctl is-active nginx)"
echo "업타임: $(sudo systemctl show nginx --property=ActiveEnterTimestamp | cut -d= -f2)"
echo

# 애플리케이션 헬스 체크
echo -e "${BLUE}🏥 애플리케이션 헬스 체크${NC}"
if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 애플리케이션: 정상${NC}"
else
    echo -e "${RED}❌ 애플리케이션: 비정상${NC}"
fi

if curl -f http://localhost/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Nginx 프록시: 정상${NC}"
else
    echo -e "${RED}❌ Nginx 프록시: 비정상${NC}"
fi
echo

# 최근 접속 통계
echo -e "${BLUE}📈 최근 1시간 접속 통계${NC}"
if [ -f "/var/log/nginx/file-extension-api.access.log" ]; then
    # 최근 1시간 로그
    since_time=$(date -d '1 hour ago' '+%d/%b/%Y:%H:%M:%S')
    
    echo "총 요청 수: $(awk -v since="$since_time" '$4 >= "["since {count++} END {print count+0}' /var/log/nginx/file-extension-api.access.log)"
    
    echo "상위 IP 주소:"
    awk -v since="$since_time" '$4 >= "["since {print $1}' /var/log/nginx/file-extension-api.access.log | sort | uniq -c | sort -nr | head -5
    
    echo "인기 엔드포인트:"
    awk -v since="$since_time" '$4 >= "["since {print $7}' /var/log/nginx/file-extension-api.access.log | sort | uniq -c | sort -nr | head -5
    
    echo "HTTP 상태 코드:"
    awk -v since="$since_time" '$4 >= "["since {print $9}' /var/log/nginx/file-extension-api.access.log | sort | uniq -c | sort -nr
else
    echo "Nginx 액세스 로그를 찾을 수 없습니다."
fi
echo

# 에러 로그 확인
echo -e "${BLUE}⚠️  최근 에러 (마지막 10개)${NC}"
echo "=== Nginx 에러 ==="
if [ -f "/var/log/nginx/file-extension-api.error.log" ]; then
    tail -10 /var/log/nginx/file-extension-api.error.log | grep -v "^\s*$" || echo "에러 없음"
else
    echo "Nginx 에러 로그를 찾을 수 없습니다."
fi

echo
echo "=== 애플리케이션 에러 ==="
docker-compose logs --tail=10 app 2>/dev/null | grep -i error || echo "에러 없음"

echo
echo -e "${GREEN}모니터링 완료!${NC}"
echo "실시간 모니터링: watch -n 30 '$0'"