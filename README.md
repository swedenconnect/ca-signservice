
---
# CURRENT BUILD VERSION = 1.0.0
---
# Sign service CA


This repo contains the source code for the sign service CA. The service is adapted to provide a high volume CA for a sign service. This service has no web GUI for management and cert issuance and consequently provides no login support for admin login.

The only option tha manage this CA service is by means of a CMC API and direct access to the CA repository.

The source code builds a Spring Boot application that may be deployed as is, or may be built into a Docker image using any of the provided Dockerfile examples.

This document provides build, deployment and operational instructions. Example files used to illustrate service deployment are provided in the `documentation/sample-config` folder.

The CA service application may hold any number of Certification Authority (CA) services, referred to as "**Instances**".

Each CA instance has its own CA repository and its own revocation services.

**This project holds one complementary tool:**

| Tool                                                   | Descritpion                                                                                                             |
|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| [HSM key generation support](hsm-support)              | Scripts for key generation inside a HSM module to support the CA. A script for software key generation is also provided |

## 1. Building artifacts
### 1.1. Building the source code

Building source codes referred to here requires maven version 3.3 or higher.

To build the Headless CA, a total of 3 projects need to be built in the following order:

 1. https://github.com/swedenconnect/ca-engine (version 1.2.1)
 2. https://github.com/swedenconnect/ca-cmc (version 1.3.0)
 3. https://github.com/swedenconnect/sigvaltrust-service/tree/main/commons (version 1.0.2)
 4. https://github.com/swedenconnect/ca-service-base (version 1.3.4)
 5. https://github.com/swedenconnect/ca-signservice (This repo) (version 1.0.0)

The master branch of each repo holds the latest code under development. This is usually a SNAPSHOT version.
For deployment, it is advisable to build a release version. Each release have a corresponding release branch. To build the source code, select the release branch of the latest release version before building the source code.

Each one of the projects are built by executing the following command from the project folder containing the pom.xml file:

> mvn clean install

### 1.2 Building a docker image

Three sample Dockerfile files are provided:

| Dockerfile         | Description                                                                                                                            |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Dockerfile         | Builds a docker image that exposes all relevant default ports                                                                          |
| Dockerfile-debug   | Builds a docker image that allows attachment of a remote debugger on port 8000                                                         |
| Dockerfile-softhsm | Builds a docker image that includes a SoftHSM and tools to load keys into the SoftHSM. This image is used to test the HSM PKCS#11 API. |

A docker image can be built using the following command:

> docker build -f Dockerfile -t headless-ca .

Please refer to the Docker manual for instructions on how to build and/or modify these docker images.


## 2. Configuration

### 2.1. Environment variables
The following environment variables are essential to the CA application:

| Environment variable                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SPRING_CONFIG_ADDITIONAL_LOCATION` | Specifies the absolute path location of the configuration data folder. This folder must specify a location that is available to the CA application and the CA application must have write access to this folder and all its sub-folders. The absolute path specified by this variable must end with a delimiter ("/") so that `${SPRING_CONFIG_ADDITIONAL_LOCATION}child` specifies the absolute path of the child folder. (Note: this is a rule set by Spring Boot). |
| `SPRING_PROFILES_ACTIVE`            | Specifies an optional application profiles used by the CA service. This variable should be set to the value "`nodb`" if no database implementation of the CA repository is used (Se section on CA repository options).                                                                                                                                                                                                                                                |
| `TZ`                                | Specifies the timezone used by the application. This should be set to "`Europe/Stockholm`"                                                                                                                                                                                                                                                                                                                                                                            |

The `documentation/sample-config` folder contains sample configuration data. A corresponding folder must be available to the CA application at the location specified by `SPRING_CONFIG_ADDITIONAL_LOCATION`.


### 2.2. Configuration files
The configuration folder holds the following files and folders:

| Resource                 | Description                                                                                           |
|--------------------------|-------------------------------------------------------------------------------------------------------|
| `cfg`                    | Holds optional configuration data files applicable to the CA application such as logotype image files |
| `instances`              | holds configuration and data files related to all configured CA instances.                            |
| `application.properties` | Main configuration file                                                                               |

#### 2.2.1. Instances folder

The `instances` folder includes one folder for each instance of the CA service. Each folder has the name equal to the id of the instance defined in the main configuration file `application.properties`. In the sample configuration there are 2 instances named `rot01` and `ca01`, where rot01 is a root CA service and the ca01 instance is a CA used to issue end entity certificates.

Each instance folder has 2 child folders as follows:

| Folder       | Description                                                                                                     |
|--------------|-----------------------------------------------------------------------------------------------------------------|
| `certs`      | This folder is used to store certificates that are used to represent this CA instance.                          |
| `keys`       | This folder contains any necessary key files used to load the private/public key pair of this CA instance.      |

##### 2.2.1.1. Certs folder

The **certs** folder is typically populated by the following files on instance initialization:

| File                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ca-chain.pem`       | This file holds the chain of certificates used by the CA to represent a path to a trusted root certificate. This file is initially created containing a single certificate, being the self issued CA certificate stored in the ca-self-signed.crt file. This file may be modified manually by the administrator if this CA is certified by another CA in order to create a path to a trusted root. If this file is modified, then all certificates including the trusted root certificate must be included. The certificates should be stored in the order from the CA certificate first and the root certificate last. All certificate must be stored in its PEM format. |
| `ca-self-signed.crt` | This file holds the self signed CA certificate created for this CA instance at initialization.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `ocsp.crt`           | This certificate appears if, and only if, the CA is configured include an OCSP responder and a separate key pair is provided for the OSCP responder. The OCSP responder certificate can be re-issued by deleting this certificate and restarting the service. The OSCP responder can be re-keyed by replacing the OCSP responder key pair and deleting this certificate and restarting the service.                                                                                                                                                                                                                                                                       |

##### 2.2.1.2 Keys folder
The **keys** folder is used to store files holding key pair data for the CA instance. The files stored here are different depending on the key type specified in the configuration data as follows:

| Key type | Content in keys folder                                                                                                                                                                                                                                     |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `jks`    | Stores Java key store files for the CA and for the OCSP responder (if applicatble). File names must end with `ca.jks` for CA keys and `ocsp.jks` for OCSP keys.                                                                                            |
| `pkcs12` | Stores PKCS#12 files for the CA and for the OCSP responder (if applicatble). File names must end with `ca.p12` for CA keys and `ocsp.p12` for OCSP keys.                                                                                                   |
| `pkcs11` | Applicable if the private key is stored in an external HSM device. This folder must contain a certificate file for the public key of the corresponding private key in the HSM. File names must end with `ca.crt` for CA keys and `ocsp.crt` for OCSP keys. |
| `pem`    | Stores private keys and public key certificates in separate files. File names must end with `ca.key` for CA keys, `ca.crt` for CA public key certificates, `ocsp.key` for OCSP keys and `ocsp.crt` for OCSP public key certificates.                       |
| `create` | empty                                                                                                                                                                                                                                                      |
| `none`   | empty                                                                                                                                                                                                                                                      |

Note that `create` option is only for test and causes the service to generate new keys at every startup. The option `none` means no key and this option is selected for OCSP responders that will issue OCSP responses using the CA key instead of a separate key for a separate OCSP responder entity.

##### 2.2.1.3 Repository folder

The repository folder is internally used by the CA instance to store data related to the CA repository and issued revocation data.

#### 2.2.2. Application properties configuration

Configuration data for the service an all CA instances are specified in the `application.properties` file. Properties settings are divided into sub categories as follows:

##### 2.2.2.1 Process logging levels

Process logging levels are set accoreding to Spring Boot conventions. Two logging levels are preset:

| Logging level property                         | Description                                              |
|------------------------------------------------|----------------------------------------------------------|
| logging.level.se.swedenconnect.ca              | Setting logging level for the CA service.                |
| logging.level.se.swedenconnect.opensaml.pkcs11 | Setting logging level for processes related to HSM usage |

Logging levels can be set to the values: `TRACE`, `DEBUG`, `INFO`, `WARN` or `ERROR`


##### 2.2.2.2 Main service settings

All regular general Spring Boot application.properties settings apply to this service. In addition to these properties, this service define a number of its own general service settings.
The following properties should be set to appropriate values:

| Propety                                  | Value                                                                                                                                                                                              |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| server.port                              | The main server port. Default 8080                                                                                                                                                                 |
| ca-service.config.control-port           | The alternative internal server port. Default 8006                                                                                                                                                 |
| ca-service.config.base-url               | The base URL of the CA service (Only protocol and host name such as: https://service.example.com), not including any path information.                                                             |
| ca-service.config.verbose-cert-print     | Determines the level of detail in presentation of certificate information. Default set to `false`. Setting this to true will for example print the content of signature values and key parameters. |
| ca-service.policy.admin.enabled-ui-ports | The ports that are allowed to expose service front page. See details below.                                                                                                                        |
| ca-service.config.logo                   | The path to the logo of the service. Typically set to ${ca-service.config.data-directory}cfg/logo.svg                                                                                              |
| ca-service.config.icon                   | The path to the icon of the service. Typically set to ${ca-service.config.data-directory}cfg/icon.svg                                                                                              |

Note: The `ca-service.policy.admin.enabled-ui-ports` property includes a list of allowed ports for the front page. A typical setting is: ${ca-service.config.control-port} to allow the front page to be shown on the local network but not via the open internet.

**Certified key constraints**

These settings define the allowed public key types and minimum key length requirements for this CA to allow certification of a presented public key in a certification request.

| Propety                            | Value                                                |
|------------------------------------|------------------------------------------------------|
| ca-service.policy.rsa-keys-allowed | Set to true to allow RSA public keys                 |
| ca-service.policy.rsa-min-key-len  | Minimum RSA key length. Default 3072                 |
| ca-service.policy.ec-keys-allowed  | Set to true to allow Elliptic Curve (EC) public keys |
| ca-service.policy.ec-min-key-len   | Minimum EC key length. Default 256                   |

##### 2.2.2.3 TLS configuration

The Spring boot application may provide its services over encrypted TLS as alternative to the main service port. This communication is allways offered on port 8443. TLS is configured using the following standard Spring Boot properties

| Propety                       | Value                                                                                                         |
|-------------------------------|---------------------------------------------------------------------------------------------------------------|
| server.ssl.key-store          | Location of the TLS server key and certificate such as ${ca-service.config.data-directory}cfg/sslSnakeOil.p12 |
| server.ssl.key-store-type     | Key store type, such as `PKCS12`                                                                              |
| server.ssl.key-store-password | Key store password                                                                                            |
| server.ssl.key-password       | Key password                                                                                                  |
| server.ssl.enabled            | Set to true to enable TLS                                                                                     |

These settings are disabled by default

##### 2.2.2.4 AJP port configuration

AJP is disabled by default. AJP protocol support can be enabled ans specified using the following property settings:

| Propety            | Value                                                                                                                                                                                                                                       |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| tomcat.ajp.enabled | Set to true to enable AJP support                                                                                                                                                                                                           |
| tomcat.ajp.port    | Set the AJP port. Typically 8009                                                                                                                                                                                                            |
| tomcat.ajp.secret  | Sets an AJP secret to enahce security between the application and the web server. If this secret is absent, then AJP is provided with secure mode set to false and without AJP secret. This is not recommended for production environments. |

##### 2.2.2.5 Syslog configuration

The CA service offers process logging for the purpose of monitoring the service and to debug problems.
This log does not need to be maintained over time.

In addition to this the CA service provides an audit log with information on important events to support audit of the service. This log is intended to be archived to allow future audits.

The audit log can be directed to external syslog servers. For details on syslog configuration, see section 5.

##### 2.2.2.6 PKCS#11 configuration

PKCS#11 configuration is only applicable if the service is connected to a HSM key source. For more details on HSM configuration, see chapter 4.

##### 2.2.2.7 CA instance configuration
The CA service may provide any number of CA instances where each instance have its own set of keys, CA identity, CA settings and revocation services.

A typical configuration is to create a CA service with 2 instances, where one instance is a root CA and the other is the CA used to issue certificates to access points.

For a more secure setup, the Root CA should be established as a separate service deployment, completely separated from the CA issuing end entity certificates.

###### 2.2.2.7.1 CA instance key source configuration

The basic asset of a CA instance is its key sources. A CA may have one or two key sources:

1. The CA key source used to sign certificates and CRL (Certificate revocation lists).
2. An optional OCSP key source used if the OCSP responder is provided by a separate entity with its own issuing key.

If no OCSP key source is specified, then this instance will issue OCSP responses (if OCSP is enabled) using the CA key instead of using a separate OCSP key.

A key sources are set up using the following parameters :

| Key soruce parameter | Value                                                                                                                                                                                                                                                                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type                 | Specifies the type of key source. See section 2.2.1.2 for the different key source types                                                                                                                                                                                                                                                                           |
| alias                | The alias/name of the key. for jks and pkcs12 key sources this value is the key alias in the key store. for pkcs11 this is the id of the key in the HSM and for pem keys this parameter is ignored.                                                                                                                                                                |
| pass                 | Password/secret used to access the key. for pem keys this is the key decryption password if the key is encrypted. If the pem key is no encrypted, this paramter is ignored.                                                                                                                                                                                        |
| reloadable-keys      | A parameter relevant to HSM keys only. A value of true introduce a key test before using the HSM key to make sure the key is still attached to the service. If the key is no longer available, and attempt to reload the key is done before usage. Setting this to true have a small performance penalty, but generally should be set to true for HSM key sources. |

The property name format for a ca key is:
> ca-service.instance.conf.{instance-id}.ca.key-source.{parameter-name}

The property name format for an ocsp key is:
> ca-service.instance.conf.{instance-id}.ocsp.key-source.{parameter-name}

Example of key source configuration for the CA instance named ca01 with ca and ocsp key:

```
ca-service.instance.conf.ca01.ca.key-source.type=pkcs11
ca-service.instance.conf.ca01.ca.key-source.alias=ca01-key
ca-service.instance.conf.ca01.ca.key-source.pass=1234
ca-service.instance.conf.ca01.ca.key-source.reloadable-keys=true
ca-service.instance.conf.ca01.ocsp.key-source.type=jks
ca-service.instance.conf.ca01.ocsp.key-source.alias=ocsp
ca-service.instance.conf.ca01.ocsp.key-source.pass=secret

```

###### 2.2.2.7.2 CA instance service configuration

Instance service configuration is provided as a default configuration profile followed by
specific configuration parameters for each instance that differs from the default parmeter.

property parameter naming for instance service configuration follows the following structure:

> ca-service.instance.conf.{instance-id}.{entity=ca/ocsp}.{parameter-name}

The instance-id for the default profile is "`default`"

The following parameters are available for the "ca" entity type:

| Parameter                     | Value                                                                                                                                                                    |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type                          | The type of CA instance. The available values are "ca" and "root". The value determines the certificate content profile for issued certificates.                         |
| description                   | A Short description of the instance CA service.                                                                                                                          |
| algorithm                     | URI identifier of the algorithm used to sign certificates and revocation data. E.g. `http://www.w3.org/2001/04/xmldsig-more#rsa-sha256`                                  |
| allow-v1                      | True if this CA is allowed to issue X.509 V1 certificates. Note that certificates with extensions are always V3 certificates. Default false.                             |
| self-issued-valid-years       | The number of years the autogenerated self issued certificate will be valid                                                                                              |
| validity.start-offset-sec     | The number of seconds before the actual issue time, issued certificates will be valid from. This allows time skew between the issuing service and certificate verifiers. |
| validity.unit                 | The unit type for validity period settings of issued certificates. Unit alternatives are "M", "H", "D" or "Y" for Minute, Hour, Day or Year (case insensitive)           |
| validity.amount               | The amount of time units issued certificates will be valid                                                                                                               |
| crl-validity.start-offset-sec | Same as `validity.start-offset-sec`, but for CRL validity period.                                                                                                        |
| crl-validity.unit             | same as `validity.unit`, but for CRL validity period.                                                                                                                    |
| crl-validity.amount           | same as `validity.amount`, but for CRL validity period.                                                                                                                  |

The following parameters are available for the "ocsp" entity type:

| Parameter                 | Value                                                                                                                                                                                                                                                                                               |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enabled                   | A value of true enable OCSP responder for this CA instance                                                                                                                                                                                                                                          |
| algorithm                 | URI identifier of the algorithm used to sign OCSP responses                                                                                                                                                                                                                                         |
| validity.start-offset-sec | Same as `validity.start-offset-sec`, but for OCSP validity period.                                                                                                                                                                                                                                  |
| validity.unit             | Same as `validity.unit`, but for OCSP validity period.                                                                                                                                                                                                                                              |
| validity.amount           | Same as `validity.amount`, but for OCSP validity period. However a value of "0" means that the OCSP response will not have a "Next update" time set. This is the typical configuration when OCSP responses is issued directly based on the CA repository source (The information is allways fresh). |

THe following example illustrates typical default and instance service configuration for the instances named ca01 and rot01:

```
ca-service.instance.conf.default.ca.algorithm=http://www.w3.org/2001/04/xmldsig-more#rsa-sha256
ca-service.instance.conf.default.ca.allow-v1=false
ca-service.instance.conf.default.ca.self-issued-valid-years=10
ca-service.instance.conf.default.ca.validity.start-offset-sec=-20
ca-service.instance.conf.default.ca.validity.unit=Y
ca-service.instance.conf.default.ca.validity.amount=2
ca-service.instance.conf.default.ca.crl-validity.start-offset-sec=0
ca-service.instance.conf.default.ca.crl-validity.unit=H
ca-service.instance.conf.default.ca.crl-validity.amount=2
ca-service.instance.conf.default.ocsp.enabled=true
ca-service.instance.conf.default.ocsp.algorithm=${ca-service.instance.conf.default.ca.algorithm}
ca-service.instance.conf.default.ocsp.validity.start-offset-sec=-10
ca-service.instance.conf.default.ocsp.validity.unit=H
ca-service.instance.conf.default.ocsp.validity.amount=0


ca-service.instance.conf.rot01.enabled=true
ca-service.instance.conf.rot01.ca.type=root
ca-service.instance.conf.rot01.ca.description=Root CA service for certification of CA services
ca-service.instance.conf.rot01.ca.validity.amount=10
ca-service.instance.conf.rot01.ca.name.common-name=ROOT CA service
ca-service.instance.conf.rot01.ca.self-issued-valid-years=20
ca-service.instance.conf.rot01.ca.crl-validity.unit=D
ca-service.instance.conf.rot01.ca.crl-validity.amount=35
ca-service.instance.conf.rot01.ocsp.enabled=false

ca-service.instance.conf.ca01.enabled=true
ca-service.instance.conf.ca01.ca.type=ca
ca-service.instance.conf.ca01.ca.description=CA service for issuing end entity certificates
ca-service.instance.conf.ca01.ca.validity.amount=2
ca-service.instance.conf.ca01.ca.name.common-name=Common name for this CA
ca-service.instance.conf.ca01.ca.self-issued-valid-years=10
ca-service.instance.conf.ca01.ocsp.name.common-name=OCSP Responder
```

##### 2.2.2.8 CA repository configuration

This CA service includes the alternatives to use file storage or database storage for the CA repository.

Use of database storage is the default option which requires `application.properties` to include appropriate settings for the database connection
for the repository. The option to use file based storage is mainly intended for test, but may be used in production environments where the operational
security advantages of a complete database is not required or desired. In order to use the file based CA repository it is necessary to opt-out of
database usage. This can be done by activating the Spring profile "`nodb`" (See section 2.1). Using this profile causes the application to use a file based
repository and causes the application to ignore any database settings below.

Database implementation use Spring Boot JPA (Jakarta Persistence API). This implementation allows a wide range of settings to optimize connection to
any type of database. A useful guide is available here ([A guide to JPA with Spring](https://www.baeldung.com/the-persistence-layer-with-spring-and-jpa)).

The dependencies of this project includes necessary dependencies for MySQL and PostgreSQL. To use any other DB service, relevant dependencies
must be added to the project.

###### 2.2.2.8.1 General settings

Database is configured using Spring Boot property settings for JPA and spring datasource properties as described in this section. The following general settings are allways relevant.

| Property                      | Description                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| spring.jpa.hibernate.ddl-auto | This property decides wether the appikcation should create a new data base on startup. The choices are: `create`, `update`, `create-drop`, `validate`, and `none`. create is used to create the database on first startup unless the database is created using a script or by other means. Once the database table is created, the service should allways use the `none` option. |
| spring.datasource.url         | Specifies the url to the database. e.g: `jdbc:mysql://10.1.1.2:3306/headless_ca`                                                                                                                                                                                                                                                                                                 |
| spring.datasource.username    | The username of the db account accessing the database                                                                                                                                                                                                                                                                                                                            |
| spring.datasource.password    | The password of the db account user                                                                                                                                                                                                                                                                                                                                              |

These are the only options that are needed in order to connect to a MySQL database.

###### 2.2.2.8.2 PostgreSQL
When connection to a PostgreSQL database, the following additional properties should be set:

| Property                                                                | Description                                                                                                                                                                                                                                  |
|-------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spring.jpa.database=POSTGRESQL`                                        | Setting database to PostgreSQL in JPA                                                                                                                                                                                                        |
| `spring.datasource.platform=postgres`                                   | Setting database to PostgreSQL in Spring datasource                                                                                                                                                                                          |
| `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true` | This entry is put just to avoid a warning message in the logs when you start the spring-boot application. This bug is from hibernate which tries to retrieve some metadata from postgresql db and failed to find that and logs as a warning. |

###### 2.2.2.8.3 Optional

The following property settings could be considered in addition to the settings above:

| Property                     | Description                                                                                                                                                            |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| spring.jpa.generate-ddl=true | Work as a master switch for the `spring.jpa.hibernate.ddl-auto` setting described above. If theis setting is set to false, then all autogenerate actions are disabled. |
| spring.jpa.show-sql=true     | Setting this property to true sends SQL query messages to standard out.                                                                                                |

###### 2.2.2.8.4 Examples
Here are some typical configuration examples:

**MySQL**

```
spring.jpa.hibernate.ddl-auto=none
spring.datasource.url=jdbc:mysql://10.1.1.2:3306/headless_ca
spring.datasource.username=cadbuser
spring.datasource.password=S3crEt

```

**PostgreSQL**

```
spring.jpa.database=POSTGRESQL
spring.datasource.platform=postgres
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.show-sql=true
spring.jpa.generate-ddl=true
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

```

###### 2.2.2.8.4 Database table creation

The CA repository requires a table named `dbcertificate_record`. This table can be created programmatically using `spring.jpa.hibernate.ddl-auto=create`.
For more control, it may be advisable to manually create the database using a SQL create statement. The precise syntax of such create statement may differ for different
databases. The following create statement can be used to create the necessary table in MySQL:

```
CREATE TABLE `dbcertificate_record` (
`id` varchar(255) NOT NULL,
`certificate` blob NOT NULL,
`expiry_date` bigint DEFAULT NULL,
`instance` varchar(255) DEFAULT NULL,
`issue_date` bigint DEFAULT NULL,
`reason` int DEFAULT NULL,
`revocation_time` bigint DEFAULT NULL,
`revoked` bit(1) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
```




## 3. Operation
### 3.1. Running the docker container

The following provides a sample docker start command to run the service as docker container.
The image name of this example is set to `headless-ca`. This name can however be set to any suitable tag name when building the docker image.


```
docker run -d --name headless-ca --restart=always \
  -p 9070:8080 -p 9079:8009 -p 9073:8443 -p 9077:8000 -p 9076:8006 -p 9078:8008 \
  -e "SPRING_CONFIG_ADDITIONAL_LOCATION=/opt/ca/" \
  -e "SPRING_PROFILES_ACTIVE=nodb" \
  -e "TZ=Europe/Stockholm" \
  -v /etc/localtime:/etc/localtime:ro \
  -v /opt/docker/headless-ca-hsm:/opt/ca \
  headless-ca
```

### 3.2 Setting up a new CA instance

A new CA instance is created using the following procedure:

1. Create an instance data folder with the instance-id as the folder name, and then create a subfolder named "keys" inside the instance folder.
2. Generate keys for the CA and its OCSP responder and store the applicable files in the "keys" folder.
3. Update the application.properties file with appropriate property settings for the new instance.
4. Restart the CA service. This will cause the service to initialize the new instance CA repository and to generate new self issued CA certificate and OCSP certificate in the "repository" and the "certs" folders.

If this CA instance is a root CA (is not signed by another CA higher in the trust path), then stop here.
If this CA is a CA that should be signed by a root CA, then proceed as described in the next steps below. These instructions assume that the new instance created through step 1-4 is named "new-ca".

5. Locate the self-issued CA certificate in the "certs" folder of "new-ca".
6. Obtain a new CA certificate for this CA by the signing CA higher in the trust chain. This could be another CA provided by another instance of this CA service. In such case the self signed certificate in "ca-chain.pem" can be used to request a new CA certificate.
7. Replace the content of the file "ca-chain.pem" in the instance "new-ca". Remove the existing self-signed certificate and place instead the following certificates in this order:<br>
   a. The new CA certificate issued for this service in step 6<br>
   b. The self issued certificate of the CA that signed che certificate in step 6.
8. Restart the CA service

### 3.3 Issue and revoke certificates

Issuing and revoking certificates in the CA service provided by this application is exclusively done through the CMC API. Please refer to the documentation of the ca-cmc library for documentation of the CMC API.
This library also provides code for implementing a compatible CMC client used to request and revoke certificates.


## 4 HSM configuration

External PKCS#11 tokens, as well as softhsm PKCS#11 tokens can be configured through the following properties in application.properties:

| Parameter                                     | Value                                                                                                                                                                                             |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ca-service.pkcs11.lib`                       | location of pkcs#11 library                                                                                                                                                                       |
| `ca-service.pkcs11.name`                      | A chosen name of the PKCS#11 provider. The actual name of the provider will be "SunPKCS11-{this name}-{index}".                                                                                   |
| `ca-service.pkcs11.slotListIndex`             | The start slot index to use (default 0).                                                                                                                                                          |
| `ca-service.pkcs11.slotListIndexMaxRange`     | The maximum number of slots after start index that will be used if present. Default null. A null value means that only 1 slot will be used.                                                       |
| `ca-service.pkcs11.slot`                      | The actual name of the slot to use. If this parameter is set, then slotListIndex should not be set.                                                                                               |
| `ca-service.pkcs11.external-config-locations` | Specifies an array of file paths to PKCS#11 configuration files used to setup PKCS11 providers. **If this option is set, all other options above are ignored**.                                   |
| `ca-service.pkcs11.reloadable-keys`           | Specifies if private keys shall be tested and reloaded if connection to the key is lost, prior to each usage. Using this option (**true**) have performance penalties but may increase stability. |

Soft HSM properties in addition to generic PKCS#11 properties above. Note that for soft hsm, the parameters slot, slotListIndex and slotListIndexMaxRange are ignored.

| Parameter                               | Value                                                                                                                          |
|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `ca-service.pkcs11.softhsm.keylocation` | The location of keys using the name convention alias.key and alias.crt.                                                        |
| `ca-service.pkcs11.softhsm.pass`        | The pin/password for the soft hsm slot to use. This pin/password should be configured as the password for each configured key. |

## 5 Audit logging to syslog

Audit logs can be sent to Syslog by:

1. Setting `application.properties` property `ca-service.syslog.enabled` to the value **true**
2. Configuring available syslog hosts in the configuration file `application.properties`.

**Note:** If syslog is **not** enabled according to step 1 above, then the audit log will be available through the management port according to section 6, under the relative path "auditevents". (E.g http://localhost:8008/manage/auditevents).

Parameters are specified in the form: `ca-service.syslog.config[index].{parameter}` illustrated by the example below.

**Sample syslog.properties file:**

```
# Parameters:
# - host           : Hostname or IP addresses
# - port           : TCP or UDP port
# - protocol       : udp, tcp or ssl
# - bsd            : Using message format RFC_3164 when set to true. Using RFC_5424 (UDP) or RFC_5425
#                  : (TCP) when false
# - facility       : The syslog facility identifier (0-23)
# - loglevel       : The log level for logging (Default INFORMATIONAL) or set to either numerical or
#                  : label that is one of:
#                  : 0 - EMERGENCY, 1 - ALERT, 2 - CRITICAL, 3 - ERROR, 4 - WARNING, 5 - NOTICE,
#                  : 6 - INFORMATIONAL, 7 - DEBUG
# - clienthostname : Name of the sending client host. If absent, the client host is set to the value
#                  : of the environment variable "HOSTNAME".
# - clientapp      : Name of the sending client application

ca-service.syslog.enabled=true
ca-service.syslog.config[0].host=10.1.1.101
ca-service.syslog.config[0].port=601
ca-service.syslog.config[0].protocol=tcp
ca-service.syslog.config[0].bsd=false
ca-service.syslog.config[0].facility=13
ca-service.syslog.config[0].clientapp=ca-service
ca-service.syslog.config[1].host=10.1.1.102
ca-service.syslog.config[1].port=514
ca-service.syslog.config[1].protocol=udp
ca-service.syslog.config[1].bsd=false
ca-service.syslog.config[1].facility=14
ca-service.syslog.config[1].clientapp=ca-service
```

## 6 Monitoring service

The CA application can be monitored remotely through configurable url and port. Monitoring is provided using Spring boot actuator and is configured through settings in application.properties as follows.

| parameter                                 | Value                                                           |
|-------------------------------------------|-----------------------------------------------------------------|
| management.server.base-path               | The path to the monitoring service. Typically set to "/manage"  |
| management.server.port                    | The tcp port at which the information is available              |
| management.server.ssl.enabled             | Set to true to enable SSL/TLS protection                        |
| management.endpoint.info.enabled          | Enabling the "info" feed on /info if set to `true`              |
| management.endpoint.health.enabled        | Enabling the health feed on /health if set to `true`            |
| management.endpoint.auditevents.enabled   | Enabling the audit events feed on /auditevents if set to `true` |
| management.endpoints.web.exposure.include | Default "*"                                                     |
| management.endpoints.web.base-path        | Default "/"                                                     |
| management.server.ssl.key-store=          | Key store path                                                  |
| management.server.ssl.key-store-password  | Key store password                                              |
| management.server.ssl.key-password        | Key password                                                    |
| management.server.ssl.key-store-type      | Keystore type (e.g. "PKCS12")                                   |
| management.server.ssl.key-alias           | Keystore alias                                                  |

URL:s provide information about the service as follows:

> http[s]://{service-IP-address}:{management.port}{management.context-path}{resource-name}

A typical URL for the "info" resource is:

> http://localhost:8008/manage/info

The following resources are available:

| resource-name | Description                                 |
|---------------|---------------------------------------------|
| health        | Service health indication                   |
| info          | Information about the service               |
| auditevents   | Audit log (If no syslog server is assigned) |

**Example: "health"**

```
{"status":"UP"}
```

**Example: "info"**

```
{
    "build": {
        "artifact": "ca-service-base",
        "name": "ca-service-base",
        "time": "2021-03-28T19:13:31.355Z",
        "version": "1.0.0",
        "group": "se.swedenconnect.ca"
    },
    "CA-service-information": {
        "serviceUrl": "https://service.example.com/ca",
        "contextPath": "/ca",
        "servicePort": 8080,
        "adminPort": 8006,
        "managePort": 8008,
        "ajpConfig": {
            "port": 8009,
            "secret": true
        },
        "caInstances": [
            {
                "id": "ca01",
                "enabled": true,
                "serviceType": "ca",
                "keySourceType": "pkcs11",
                "keyInfo": {
                    "keyType": "RSA",
                    "keyLength": 3072
                },
                "algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                "dn": "C=SE,O=Myndigheten för digital förvaltning,OU=SDK,organizationIdentifier=202100-6883,CN=Accesspunkt CA-SDK-TEST",
                "caPath": [
                    "C=SE,O=Org,OU=Org Unit,organizationIdentifier=202100-6883,CN=CommonName",
                    "C=SE,O=Org,OU=Org Unit,organizationIdentifier=202100-6883,CN=Rot CA-TEST"
                ],
                "crlDistributionPoints": ["https://service.example.com/ca/crl/ca01.crl"],
                "oscpEnabled": true,
                "ocspInfo": {
                    "ocspServiceUrl": "https://service.example.com/ca/ocsp/ca01",
                    "separateEntity": true,
                    "ocspEntity": {
                        "dn": "C=SE,O=Org,OU=Org Unit,organizationIdentifier=202100-6883,CN=CommonName OCSP Responder",
                        "keySourceType": "pkcs11",
                        "keyInfo": {
                            "keyType": "RSA",
                            "keyLength": 3072
                        },
                        "algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"
                    }
                }
            },
            {
                "id": "rot01",
                "enabled": true,
                "serviceType": "root",
                "keySourceType": "pkcs11",
                "keyInfo": {
                    "keyType": "RSA",
                    "keyLength": 3072
                },
                "algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                "dn": "C=SE,O=Org,OU=Org Unit,organizationIdentifier=202100-6883,CN=Rot CA-TEST",
                "caPath": ["C=SE,O=Org,OU=Org Unit,organizationIdentifier=202100-6883,CN=Rot CA-TEST"],
                "crlDistributionPoints": ["https://service.example.com/ca/crl/rot01.crl"],
                "oscpEnabled": false,
                "ocspInfo": null
            }
        ]
    }
}
```
