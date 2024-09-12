# digital-card-service
Service responsible to auto generate digital card once the registration packet is processed by registration processor module

## Build & run (for developers)
The project requires JDK 1.21.
and mvn version - 3.9.6
1. Build and install:
    ```
    $ cd kernel
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
    ```

### Remove the version-specific suffix (PostgreSQL95Dialect) from the Hibernate dialect configuration
   ```
   hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
   ```
This is for better compatibility with future PostgreSQL versions.

### Configure ANT Path Matcher for Spring Boot 3.x compatibility.
   ```
   spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER
   ```
This is to maintain compatibility with existing ANT-style path patterns.

### Add auth-adapter in a class-path to run a digital-card-service
   ```
   <dependency>
       <groupId>io.mosip.kernel</groupId>
       <artifactId>kernel-auth-adapter</artifactId>
       <version>${kernel.auth.adapter.version}</version>
   </dependency>
   ```

## Configuration files
Digital Card Service uses the following configuration files:
```
application-default.properties
digital-card-default.properties
```

## Configuration
[Configuration-DigitalCard](https://github.com/mosip/mosip-config/blob/develop/digital-card-default.properties) and
[Configuration-Application](https://github.com/mosip/mosip-config/blob/develop/application-default.properties) defined here.

Refer [Module Configuration](https://docs.mosip.io/1.2.0/modules/module-configuration) for location of these files.
