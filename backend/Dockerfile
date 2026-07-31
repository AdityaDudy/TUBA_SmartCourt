# ---------- Stage 1: Build the jar with Maven (no wrapper needed) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first for dependency layer caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 2: Run the jar with Playwright browser deps ----------
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Minimal base tools; Playwright's own --with-deps installer
# figures out the correct package names for whatever OS this is.
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget gnupg ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Install Playwright's Chromium browser + correct OS deps automatically
RUN java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI \
    org.springframework.boot.loader.launch.PropertiesLauncher install --with-deps chromium || true

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]