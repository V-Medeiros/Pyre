FROM eclipse-temurin:21-jdk-alpine AS build
RUN apk add --no-cache unzip wget
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S vesta && adduser -S vesta -G vesta
WORKDIR /app
COPY --from=build /workspace/target/vesta-api-*.jar app.jar
USER vesta
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
