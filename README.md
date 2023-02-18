## Build and run

With JDK17+
```bash
mvn package
java -jar target/html-service.jar
```

## Exercise the application
```
curl -v --data-binary @build.log http://localhost:8080/html
```
