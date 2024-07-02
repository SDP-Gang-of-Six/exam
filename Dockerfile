FROM openjdk:11-jre-slim
COPY ./target/*.jar /opt/apps/exam.jar
EXPOSE 8080
ENTRYPOINT java -jar -Xms4g -Xmx4g /opt/apps/exam.jar --spring.profiles.active=pro
