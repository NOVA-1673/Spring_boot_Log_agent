
# 서버 실행
./gradlew bootRun

# docker 
docker compose up -d

    확인
    docker ps -a

docker exec -it obs-postgres psql -U obs -d observability  //이건 cli에서 바로 도커 db 확인할때

#swagger
http://localhost:8080/swagger-ui/index.html

#git branch
git checkout main
git pull origin main
git branch -d feature/incident-analysis-controller
git push origin --delete feature/incident-analysis-controller 브렌치 다쓰고 지우기