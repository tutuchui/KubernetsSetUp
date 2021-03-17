FROM java:8

EXPOSE 9898

ADD target/decision-demo.jar decision-demo.jar

ENTRYPOINT ["java", "-jar", "decision-demo.jar"]