FROM maven:3.8.1-openjdk-11 as build
WORKDIR /app
COPY . /app
RUN mvn clean package

FROM openjdk:11-jre-slim
COPY --from=build /app/target/*.jar /opt/apps/exam.jar
EXPOSE 80
CMD ["/usr/bin/java", "-Dserver.port=80", "-jar", "/opt/apps/exam.jar"]