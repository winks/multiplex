-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-all-users :? :*
-- :doc retrieves all users
SELECT * FROM mpx_users

-- :name get-user :? :1
-- :doc retrieves a user record given the uid
SELECT uid, username, hostname, title, avatar, theme, is_private FROM mpx_users
WHERE uid = :uid AND is_active = true

-- :name get-user-by-hostname :? :1
-- :doc retrieves a user record given the hostname
SELECT uid, username, hostname, title, avatar, theme, is_private FROM mpx_users
WHERE hostname = :hostname AND is_active = true

-- :name delete-user! :! :n
-- :doc deletes a user record given the uid
DELETE FROM mpx_users
WHERE uid = :uid

------------------------------------------------------------------------------

-- :name get-posts :? :*
-- :doc retrieves posts given the uid
SELECT * FROM mpx_posts
WHERE author = :author
ORDER BY updated DESC