#!/bin/bash

set -e  # Exit immediately if any command fails

echo "Starting the container..."
echo "Artifactory URL: $artifactory_url_env"
echo "IAM Adapter URL: $iam_adapter_url_env"

# Ensure necessary variables are set
if [[ -z "$artifactory_url_env" || -z "$iam_adapter_url_env" ]]; then
    echo "ERROR: Required environment variables are not set!"
    exit 1
fi

# Download pdf-generator.zip
wget "$artifactory_url_env/artifactory/libs-release-local/pdf-generator/pdf-generator.zip" -O pdf-generator.zip
unzip pdf-generator.zip -d "$loader_path_env/pdf-generator"
rm -rf pdf-generator.zip

# Download kernel-auth-adapter.jar
wget -q --show-progress "$iam_adapter_url_env" -O "$loader_path_env/kernel-auth-adapter.jar"

# Set ownership correctly
chown -R root:root "$loader_path_env/pdf-generator"
chown root:root "$loader_path_env/kernel-auth-adapter.jar"

# Switch back to the application user
exec su-exec ${container_user_uid}:${container_user_gid} java \
    -Dloader.path="$loader_path_env,$loader_path_env/pdf-generator" \
    --add-modules=ALL-SYSTEM \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    -XX:-UseG1GC -XX:-UseParallelGC -XX:-UseShenandoahGC \
    -Xms1g -Xmx2g -XX:+ExplicitGCInvokesConcurrent -XX:+UseZGC -XX:+ZGenerational \
    -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError \
    -XX:+UseCompressedOops -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 \
    -jar -Dspring.cloud.config.label="$spring_config_label_env" \
         -Dspring.profiles.active="$active_profile_env" \
         -Dspring.cloud.config.uri="$spring_config_url_env" digital-card-service.jar
