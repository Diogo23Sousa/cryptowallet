FROM openjdk:21-jdk-slim
LABEL authors="Diogo Sousa"
WORKDIR /app
COPY target/crypto-wallet-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
