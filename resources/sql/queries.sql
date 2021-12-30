-- :name create-user! :<! :raw
-- :doc creates a new user record
INSERT INTO mpx_users
(uid, username, hostname, email, password,
  apikey, title, signupcode, avatar, theme,
  is_active, is_private, updated, last_login)
VALUES (nextval('mpx_posts_id_seq'), :username, :hostname, :email, :password,
  '', :title, '', :avatar, :theme,
  true, false, now(), now())
RETURNING uid

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the uid
DELETE
FROM mpx_users
WHERE uid = :uid

--------------------

-- :name get-login :? :1
-- :doc retrieves a user record given the username
SELECT uid, username, password
FROM mpx_users
WHERE is_active = true AND username = :username

-- :name get-profile :? :1
-- :doc retrieves a user record given the uid
SELECT uid, username, hostname, title, avatar, theme, is_private, email, apikey, created
FROM mpx_users
WHERE is_active = true AND uid = :uid

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

------------------------------------------------------------------------------
-- :name create-post! :<! :raw
-- :doc creates a new post
INSERT INTO mpx_posts
(id, author, itemtype, url, txt, meta, tag, updated)
VALUES (nextval('mpx_posts_id_seq'), :author, :itemtype, :url, :txt, :meta, :tag, now())
RETURNING id

-- :name update-post! :! :n
-- :doc updates an existing post
UPDATE mpx_posts
SET url = :url, txt = :txt, tag = :tag, updated = now()
WHERE id = :id

-- :name delete-post! :! :n
-- :doc deletes a post given the id
DELETE
FROM mpx_posts
WHERE id = :id

--------------------

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

-----

-- :name get-posts-count :? :1
-- :doc retrieves posts count
SELECT count(p.id)
FROM mpx_posts p
WHERE p.author = :author

-- :name get-posts-filtered-count :? :1
-- :doc retrieves posts given the uid and itemtype
SELECT count(p.id)
FROM mpx_posts p
WHERE p.author = :author AND p.itemtype = :itemtype

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

-----

-- :name get-all-posts-count :? :1
-- :doc retrieves posts count
SELECT count(p.id)
FROM mpx_posts p

-- :name get-all-posts-filtered-count :? :1
-- :doc retrieves posts given the uid and itemtype
SELECT count(p.id)
FROM mpx_posts p
WHERE p.itemtype = :itemtype