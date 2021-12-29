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

-----------

-- :name get-login :? :1
-- :doc retrieves a user record given the username
SELECT uid, username, password
FROM mpx_users
WHERE is_active = true AND username = :username

-- :name get-all-users :? :*
-- :doc retrieves all users
SELECT uid, username, hostname, title, avatar, theme, is_private
FROM mpx_users
WHERE is_active = true
ORDER BY hostname ASC

-- :name get-user :? :1
-- :doc retrieves a user record given the uid
SELECT uid, username, hostname, title, avatar, theme, is_private
FROM mpx_users
WHERE is_active = true AND uid = :uid

-- :name get-user-by-hostname :? :1
-- :doc retrieves a user record given the hostname
SELECT uid, username, hostname, title, avatar, theme, is_private
FROM mpx_users
WHERE is_active = true AND hostname = :hostname

-- :name delete-user! :! :n
-- :doc deletes a user record given the uid
DELETE
FROM mpx_users
WHERE uid = :uid

------------------------------------------------------------------------------

-- :name get-post :? :*
-- :doc retrieves post given the id
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid AND p.id = :id

-----------

-- :name get-posts :? :*
-- :doc retrieves posts given the uid
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid AND p.author = :author
ORDER BY p.created DESC
LIMIT :limit OFFSET :offset

-- :name get-posts-filtered :? :*
-- :doc retrieves posts given the uid and itemtype
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid AND p.author = :author AND p.itemtype = :itemtype
ORDER BY p.created DESC
LIMIT :limit OFFSET :offset

-----------

-- :name get-all-posts :? :*
-- :doc retrieves posts, filtered by itemtype
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid
ORDER BY p.created DESC
LIMIT :limit OFFSET :offset

-- :name get-all-posts-filtered :? :*
-- :doc retrieves posts, filtered by itemtype
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid AND itemtype = :itemtype
ORDER BY p.created DESC
LIMIT :limit OFFSET :offset