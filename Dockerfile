FROM openjdk:11-jdk
COPY /exam/target/*.jar /opt/apps/exam.jar
EXPOSE 80
CMD ["/usr/bin/java", "-Dserver.port=80", "-jar", "/opt/apps/exam.jar"]