#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 파일 확장자 API 배포 시작${NC}"

# 애플리케이션 디렉토리
APP_DIR="/opt/file-extension-api"

# 디렉토리 존재 확인
if [ ! -d "$APP_DIR" ]; then
    echo -e "${YELLOW}📁 애플리케이션 디렉토리 생성: $APP_DIR${NC}"
    sudo mkdir -p $APP_DIR
    sudo chown ubuntu:ubuntu $APP_DIR
fi

cd $APP_DIR

# Git repository 확인 및 설정
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}📦 Git repository 클론${NC}"
    git clone https://github.com/$1 .
else
    echo -e "${YELLOW}🔄 최신 코드 pull${NC}"
    git pull origin main
fi

# Nginx 설정 업데이트
echo -e "${YELLOW}🌐 Nginx 설정 업데이트${NC}"
if [ -f "nginx/file-extension-api.conf" ]; then
    sudo cp nginx/file-extension-api.conf /etc/nginx/sites-available/
    sudo nginx -t
    if [ $? -eq 0 ]; then
        sudo systemctl reload nginx
        echo -e "${GREEN}✅ Nginx 설정 업데이트 완료${NC}"
    else
        echo -e "${RED}❌ Nginx 설정 오류 발생${NC}"
        exit 1
    fi
fi

# 필요한 디렉토리 생성
echo -e "${YELLOW}📁 로그 및 데이터 디렉토리 생성${NC}"
mkdir -p logs data uploads

# 기존 컨테이너 중지
echo -e "${YELLOW}🛑 기존 서비스 중지${NC}"
docker-compose down 2>/dev/null || true

# Docker 이미지 정리 (선택사항)
echo -e "${YELLOW}🧹 미사용 Docker 이미지 정리${NC}"
docker system prune -f

# 새 컨테이너 빌드 및 실행
echo -e "${YELLOW}🔨 애플리케이션 빌드 및 실행${NC}"
docker-compose up --build -d

# 컨테이너 상태 확인
echo -e "${YELLOW}📊 서비스 상태 확인${NC}"
sleep 15
docker-compose ps

# 헬스 체크 (내부 포트)
echo -e "${YELLOW}🏥 헬스 체크 수행${NC}"
for i in {1..10}; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 애플리케이션이 정상적으로 실행 중입니다!${NC}"
        break
    else
        echo -e "${YELLOW}⏳ 애플리케이션 시작 대기 중... ($i/10)${NC}"
        sleep 5
    fi
done

# Nginx를 통한 헬스 체크
echo -e "${YELLOW}🌐 Nginx 프록시 헬스 체크${NC}"
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)
for i in {1..5}; do
    if curl -f http://$PUBLIC_IP/health >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Nginx 리버스 프록시가 정상 작동 중입니다!${NC}"
        break
    else
        echo -e "${YELLOW}⏳ Nginx 프록시 확인 중... ($i/5)${NC}"
        sleep 3
    fi
done

# 서비스 상태 확인
echo -e "${YELLOW}📊 서비스 상태 요약${NC}"
echo "Docker 컨테이너:"
docker-compose ps
echo
echo "Nginx 상태:"
sudo systemctl is-active nginx

# 최근 로그 표시
echo -e "${YELLOW}📝 최근 애플리케이션 로그:${NC}"
docker-compose logs --tail=20 app

echo -e "${GREEN}🎉 배포 완료!${NC}"
echo -e "${GREEN}🌐 애플리케이션 URL: http://$PUBLIC_IP${NC}"
echo -e "${GREEN}📋 API 문서: http://$PUBLIC_IP/swagger-ui/index.html${NC}"