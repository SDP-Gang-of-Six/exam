FROM openjdk:11
COPY ./target/*.jar /opt/apps/exam.jar
EXPOSE 8080
ENTRYPOINT java -jar -Xms2g -Xmx2g /opt/apps/exam.jar --spring.profiles.active=pro
