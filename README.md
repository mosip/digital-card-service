# digital-card-service
Service responsible to auto generate digital card once the registration packet is processed by registration processor module

## Databases
Refer to [SQL scripts](db_scripts).

## Build & run (for developers)
The project requires JDK 21.0.3
and mvn version - 3.9.6
1. Build and install:
    ```
    $ cd kernel
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
    ```
2. Build Docker for a service:
    ```
    $ cd <service folder>
    $ docker build -f Dockerfile
    ```

## Configuration files
Digital Card Service uses the following configuration files that are accessible in this [repository](https://github.com/mosip/mosip-config/tree/master).
Please refer to the required released tagged version for configuration.
```
application-default.properties
digital-card-default.properties
```

## Configuration
[Configuration-DigitalCard](https://github.com/mosip/mosip-config/blob/master/digital-card-default.properties) and
[Configuration-Application](https://github.com/mosip/mosip-config/blob/master/application-default.properties) defined here.

Refer [Module Configuration](https://docs.mosip.io/1.2.0/modules/module-configuration) for location of these files.

## Deploy
To deploy Commons services on Kubernetes cluster using Dockers refer to [Sandbox Deployment](https://docs.mosip.io/1.2.0/deploymentnew/v3-installation).

## APIs
API documentation is available [here](https://mosip.github.io/documentation/1.2.0/1.2.0.html).

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
