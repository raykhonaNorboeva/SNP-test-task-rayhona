FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY . /app

RUN ./gradlew build --no-daemon

CMD ["java", "-jar", "build/libs/your-project-name.jar"]
