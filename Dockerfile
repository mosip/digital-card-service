# Use the base image
FROM mosipdev/openjdk-21-jre:latest

# Define build-time arguments
ARG SOURCE="default-source"
ARG COMMIT_HASH="default-commit-hash"
ARG COMMIT_ID="default-commit-id"
ARG BUILD_TIME="default-build-time"

LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Set working directory
WORKDIR /home/app

# Create required directories
RUN mkdir -p /home/app/logs /home/app/Glowroot

# Copy application JAR
COPY ./target/digital-card-service-*.jar digital-card-service.jar

# Expose necessary port
EXPOSE 8099

# Run the application as root
CMD wget "${artifactory_url_env}"/artifactory/libs-release-local/pdf-generator/pdf-generator.zip && \
    unzip pdf-generator.zip -d "/home/app/pdf-generator" && \
    rm -rf pdf-generator.zip && \
    wget -q --show-progress "${iam_adapter_url_env}" -O "/home/app/kernel-auth-adapter.jar" && \
    java -Dloader.path="/home/app/pdf-generator" \
         --add-modules=ALL-SYSTEM \
         --add-opens=java.base/java.lang=ALL-UNNAMED \
         -XX:-UseG1GC -XX:-UseParallelGC -XX:-UseShenandoahGC -Xms1g -Xmx2g  -XX:+ExplicitGCInvokesConcurrent -XX:+UseZGC -XX:+ZGenerational -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:+UseCompressedOops -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 \
         -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" digital-card-service.jar;
