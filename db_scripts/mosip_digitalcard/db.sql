CREATE DATABASE mosip_digitalcard
	ENCODING = 'UTF8' 
	LC_COLLATE = 'en_US.UTF-8' 
	LC_CTYPE = 'en_US.UTF-8' 
	TABLESPACE = pg_default 
	OWNER = postgres
	TEMPLATE  = template0;

COMMENT ON DATABASE mosip_digitalcard IS 'digitalcard-service related logs and the data is stored in this database';

\c mosip_digitalcard postgres

DROP SCHEMA IF EXISTS digitalcard CASCADE;
CREATE SCHEMA digitalcard;
ALTER SCHEMA digitalcard OWNER TO postgres;
ALTER DATABASE mosip_digitalcard SET search_path TO master,pg_catalog,public;

