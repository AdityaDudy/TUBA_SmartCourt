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

# System libraries Playwright's Chromium needs to run headless
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget gnupg ca-certificates \
    libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 \
    libxkbcommon0 libxcomposite1 libxdamage1 libxfixes3 libxrandr2 \
    libgbm1 libasound2 libpangocairo-1.0-0 libpango-1.0-0 \
    fonts-liberation libu2f-udev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Install Playwright's Chromium browser binaries into the image
RUN java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI \
    org.springframework.boot.loader.launch.PropertiesLauncher install --with-deps chromium || true

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]