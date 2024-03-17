FROM openjdk:11-jre-slim
COPY --from=build /exam/target/*.jar /opt/apps/exam.jar
EXPOSE 80
CMD ["/usr/bin/java", "-Dserver.port=80", "-jar", "/opt/apps/exam.jar"]