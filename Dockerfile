FROM java:11

COPY --from=test /tmp/code/exam/target/*.jar /opt/apps/springboot.jar
EXPOSE 80
CMD ["/usr/bin/java", "-Dserver.port=80", "-jar", "/opt/apps/springboot.jar"]
