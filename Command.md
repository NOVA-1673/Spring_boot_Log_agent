
# 서버 실행
./gradlew bootRun

# docker 
docker compose up -d

    확인
    docker ps -a

docker exec -it obs-postgres psql -U obs -d observability  //이건 cli에서 바로 도커 db 확인할때

#swagger
http://localhost:8080/swagger-ui/index.html