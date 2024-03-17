FROM openjdk:11-jdk
COPY ./target/*.jar /opt/apps/exam.jar
EXPOSE 8080
ENTRYPOINT java -jar -Xms128m -Xms128m /opt/apps/exam.jar -Dspring.profiles.active=pro
