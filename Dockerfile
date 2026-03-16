FROM eclipse-temurin:21-jdk-alpine AS BUILD

WORKDIR /bot

COPY gradle/ gradle/

COPY build.gradle.kts settings.gradle.kts gradlew ./

COPY src/ src/

RUN ./gradlew clean shadowJar

FROM eclipse-temurin:21-jre-alpine

COPY --from=BUILD /bot/build/libs/*.jar /bot/bot.jar

ENTRYPOINT ["java", "-jar" , "/bot/bot.jar"]