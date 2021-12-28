CREATE SEQUENCE mpx_posts_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
--;;
CREATE TABLE mpx_posts (
  id       integer NOT NULL PRIMARY KEY DEFAULT nextval('mpx_posts_id_seq'),
  author   integer NOT NULL,
  itemtype character varying(100) DEFAULT NULL::character varying,
  url      text NOT NULL,
  txt      text NOT NULL,
  meta     text NOT NULL,
  tag      text NOT NULL,
  created  timestamp(0) without time zone DEFAULT now() NOT NULL,
  updated  timestamp(0) without time zone NOT NULL);
--;;
CREATE TRIGGER update_mpx_posts_updated BEFORE
  UPDATE ON mpx_posts FOR EACH ROW
  EXECUTE PROCEDURE update_updated_column();