# Use the base image
FROM mosipdev/openjdk-21-jre:latest

# Define build-time arguments with default values
ARG SOURCE="default-source"
ARG COMMIT_HASH="default-commit-hash"
ARG COMMIT_ID="default-commit-id"
ARG BUILD_TIME="default-build-time"

LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Build-time environment variables
ARG spring_config_label
ARG active_profile
ARG spring_config_url
ARG is_glowroot
ARG artifactory_url
ARG iam_adapter_url

# Runtime environment variables
ENV active_profile_env=${active_profile}
ENV spring_config_label_env=${spring_config_label}
ENV is_glowroot_env=${is_glowroot}
ENV artifactory_url_env=${artifactory_url}
ENV iam_adapter_url_env=${iam_adapter_url}

# Define container user properties
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1002
ARG container_user_gid=1001

# Install necessary packages and create user
RUN apt-get update && \
    apt-get install -y --no-install-recommends sudo && \
    groupadd -g ${container_user_gid} ${container_user_group} && \
    useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/bash -m ${container_user}

# Set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}
ARG loader_path=${work_dir}/additional_jars/

# Create necessary directories
RUN mkdir -p ${loader_path}

ENV loader_path_env=${loader_path}

# Define persistent storage locations
VOLUME [ "${work_dir}/logs", "${work_dir}/Glowroot" ]

# Copy JAR file
COPY ./target/digital-card-service-*.jar digital-card-service.jar

# Change file ownership
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# Set container user
USER ${container_user_uid}:${container_user_gid}

# Expose the application port
EXPOSE 8099

# Download dependencies and run application
CMD wget "${artifactory_url_env}/artifactory/libs-release-local/pdf-generator/pdf-generator.zip" && \
    unzip pdf-generator.zip -d "${loader_path_env}/pdf-generator" && \
    rm -rf pdf-generator.zip && \
    wget -q --show-progress "${iam_adapter_url_env}" -O "${loader_path_env}/kernel-auth-adapter.jar" && \
    java -Dloader.path="${loader_path_env},${loader_path_env}/pdf-generator" \
         --add-modules=ALL-SYSTEM \
         --add-opens=java.base/java.lang=ALL-UNNAMED \
         -XX:-UseG1GC -XX:-UseParallelGC -XX:-UseShenandoahGC -Xms1g -Xmx2g  -XX:+ExplicitGCInvokesConcurrent -XX:+UseZGC -XX:+ZGenerational -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:+UseCompressedOops -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 \
         -jar digital-card-service.jar \
         -Dspring.cloud.config.label="${spring_config_label_env}" \
         -Dspring.profiles.active="${active_profile_env}"  \
         -Dspring.cloud.config.uri="${spring_config_url_env}"
