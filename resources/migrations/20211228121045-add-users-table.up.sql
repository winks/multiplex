CREATE SEQUENCE mpx_users_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
--;;
CREATE TABLE mpx_users (
  uid        integer NOT NULL PRIMARY KEY DEFAULT nextval('mpx_users_id_seq'),
  username   character varying(255) NOT NULL UNIQUE,
  hostname   character varying(255) NOT NULL UNIQUE,
  email      character varying(255) NOT NULL UNIQUE,
  password   character varying(255) NOT NULL,
  apikey     character varying(64) NOT NULL UNIQUE,
  title      character varying(255) NOT NULL default '',
  signupcode character varying(64) DEFAULT NULL::character varying,
  avatar     character varying(255) NOT NULL DEFAULT '/img/default-avatar.png',
  theme      text NOT NULL default '',
  is_active  boolean NOT NULL DEFAULT '1',
  is_private boolean NOT NULL DEFAULT '0',
  created    timestamp(0) without time zone DEFAULT now(),
  updated    timestamp(0) without time zone NOT NULL,
  last_login timestamp(0) without time zone NOT NULL);
--;;
CREATE FUNCTION update_updated_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
  BEGIN
   NEW.updated = now();
   RETURN NEW;
  END;
$$;
--;;
CREATE TRIGGER update_mpx_users_updated BEFORE
  UPDATE ON mpx_users FOR EACH ROW
  EXECUTE PROCEDURE update_updated_column();