# Used when the Render service's Root Directory is the repo root.
# (If Root Directory is set to "backend", Render uses backend/Dockerfile instead.)
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY backend/ .

RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

CMD java -jar $(ls target/*.jar | head -n 1)
