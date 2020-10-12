FROM openjdk:8-jdk-alpine
COPY build/libs/ais_poc-all.jar /ais_poc-all.jar
COPY app_start.sh /app_start.sh
RUN chmod +x /app_start.sh
EXPOSE 8080
ENTRYPOINT ["./app_start.sh"]