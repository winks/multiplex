ALTER TABLE mpx_posts ADD COLUMN tags jsonb;
--;;
UPDATE mpx_posts SET tags = to_json(string_to_array(tag,','));