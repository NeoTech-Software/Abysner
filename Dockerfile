FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

# Disable file system watching, the Gradle daemon and warnings for disabled Kotlin targets
RUN mkdir -p /root/.gradle && cat <<EOF > /root/.gradle/gradle.properties
org.gradle.vfs.watch=false
org.gradle.daemon=false
kotlin.native.ignoreDisabledTargets=true
EOF

WORKDIR /project

# Copy Gradle wrapper first for layer caching
COPY gradlew gradle.properties ./
COPY gradle/wrapper/ gradle/wrapper/
RUN ./gradlew --version

# Copy version catalog and buildSrc (custom Gradle plugins)
COPY gradle/libs.versions.toml gradle/
COPY buildSrc/ buildSrc/

# Copy build files for dependency resolution
COPY settings.gradle.kts ./
COPY build.gradle.kts ./
COPY domain/build.gradle.kts domain/
COPY data/build.gradle.kts data/
COPY composeApp/build.gradle.kts composeApp/
COPY androidApp/build.gradle.kts androidApp/

# Create directories expected by build scripts
RUN mkdir -p iosApp/Configuration

# Pre-fetch all dependencies into a cached layer
RUN ./gradlew dependencies

# Copy git metadata for version info task
COPY .git/ .git/

# Copy source code
COPY domain/src/ domain/src/
COPY data/src/ data/src/
COPY composeApp/src/ composeApp/src/
COPY composeApp/compose-stability.conf composeApp/
