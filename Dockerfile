FROM amazoncorretto:21.0.3
COPY ./target/*.jar /opt/apps/exam.jar
EXPOSE 8080
ENTRYPOINT java -jar -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -Xms1024m -Xmx1024m -Xmn256m -Xss256k -XX:SurvivorRatio=8 /opt/apps/exam.jar --spring.profiles.active=pro
