FROM amazoncorretto:21.0.3
COPY ./target/*.jar /opt/apps/exam.jar
EXPOSE 8080
ENTRYPOINT java -server -Xms1024m -Xmx1024m -jar /opt/apps/exam.jar --spring.profiles.active=pro
