\c mosip_digitalcard

GRANT CONNECT
   ON DATABASE mosip_digitalcard
   TO digitalcarduser;

GRANT USAGE
   ON SCHEMA digitalcard
   TO digitalcarduser;

GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES
   ON ALL TABLES IN SCHEMA digitalcard
   TO digitalcarduser;

