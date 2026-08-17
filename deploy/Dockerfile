FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts ./
COPY src ./src

RUN chmod +x gradlew && ./gradlew --no-daemon :link-service:bootJar

FROM eclipse-temurin:21-jre-jammy AS extract
WORKDIR /builder

COPY --from=build /workspace/src/link-service/build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy
WORKDIR /application

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=extract --chown=spring:spring /builder/extracted/dependencies/ ./
COPY --from=extract --chown=spring:spring /builder/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=spring:spring /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring /builder/extracted/application/ ./

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
