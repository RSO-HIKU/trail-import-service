FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/trail-import-service-1.0.0.jar ./trail-import-service.jar
EXPOSE 8084
CMD ["java", "-jar", "trail-import-service.jar"]