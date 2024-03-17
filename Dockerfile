FROM openjdk:11-jdk
COPY /exam/target/*.jar /opt/apps/exam.jar
EXPOSE 8080
CMD  ["java", "-jar","/opt/apps/exam.jar"]