#!/bin/sh
# Script to initialize mockidentitysystem DB.
## Usage: ./init_db.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=digitalcard
CHART_VERSION=12.0.2

helm repo add mosip https://mosip.github.io/mosip-helm
helm repo update

while true; do
    read -p "CAUTION: Do we already have Postgres installed? Also make sure the digitalcard DB is backed up as the same will be overriden. Do you still want to continue?" yn
    if [ $yn = "Y" ]
      then
        kubectl create ns $NS
        DB_USER_PASSWORD=$( kubectl -n postgres get secrets db-common-secrets -o jsonpath={.data.db-dbuser-password} | base64 -d )

        echo Removing existing digitalcard DB installation
        helm -n $NS delete postgres-init-digitalcard
        kubectl -n $NS delete --ignore-not-found=true secret db-common-secrets

        echo Copy Postgres secrets
        ../helm/digitalcard/copy_cm_func.sh secret postgres-postgresql postgres $NS

        echo Initializing DB
        helm -n $NS install postgres-init-digitalcard mosip/postgres-init -f init_values.yaml \
        --version $CHART_VERSION \
        --set dbUserPasswords.dbuserPassword="$DB_USER_PASSWORD" \
        --wait --wait-for-jobs
        break
      else
        break
    fi
done
