-- :name create-user! :<! :raw
-- :doc creates a new user {:username, :hostname, :email, :password, :apikey, :title, :signupcode, :avatar, :theme, :is_active, :is_private}
INSERT INTO mpx_users
(uid, username, hostname, email, password,
  apikey, title, signupcode, avatar, theme,
  is_active, is_private, updated, last_login)
VALUES (nextval('mpx_users_id_seq'), :username, :hostname, :email, :password,
  :apikey, :title, :signupcode, :avatar, :theme,
  :is_active, :is_private, now(), now())
RETURNING uid

-- :name update-user! :! :n
-- :doc updates an existing user {:uid :username :hostname :email :avatar :title :theme :is_private}
UPDATE mpx_users
SET username = :username, hostname = :hostname, email = :email, avatar = :avatar,
    title = :title, theme = :theme, is_private = :is_private
WHERE uid = :uid

-- :name change-password! :! :n
-- :doc updates an existing user {:uid :password}
UPDATE mpx_users
SET password = :password
WHERE uid = :uid

-- :name change-last-login! :! :n
-- :doc updates an existing user {:uid}
UPDATE mpx_users
SET last_login = now()
WHERE uid = :uid

-- :name delete-user! :! :n
-- :doc deletes a user given the uid
DELETE
FROM mpx_users
WHERE uid = :uid

--------------------

-- :name get-login :? :1
-- :doc retrieves a user for login given the username
SELECT uid, username, password
FROM mpx_users
WHERE is_active = true AND username = :username

-- :name get-profile :? :1
-- :doc retrieves a user profile given the uid
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
-- :doc retrieves a user given the uid
SELECT uid, username, hostname, title, avatar, theme, is_private
FROM mpx_users
WHERE is_active = true AND uid = :uid

-- :name get-user-by-hostname :? :1
-- :doc retrieves a user given the hostname
SELECT uid, username, hostname, title, avatar, theme, is_private
FROM mpx_users
WHERE is_active = true AND hostname = :hostname

------------------------------------------------------------------------------

-- :name create-post! :<! :raw
-- :doc creates a new post {:author :itemtype :url :txt :meta :tag :tags}
INSERT INTO mpx_posts
(id, author, itemtype, url, txt, meta, tag, updated, tags)
VALUES (nextval('mpx_posts_id_seq'), :author, :itemtype, :url, :txt, :meta, :tag, now(), :sql:tags_raw)
RETURNING id

-- :name update-post! :! :n
-- :doc updates an existing post
UPDATE mpx_posts
SET url = :url, txt = :txt, tag = :tag, tags = :sql:tags_raw, updated = now()
WHERE id = :id

-- :name delete-post! :! :n
-- :doc deletes a post given the id
DELETE
FROM mpx_posts
WHERE id = :id

--------------------

-- :name get-post-by-id :? :*
-- :doc retrieves post given the id
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid AND p.id = :id

-----------

-- :name get-posts-filtered :? :*
-- :doc retrieves posts given the uid and optional itemtype/tag
SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
FROM mpx_posts p, mpx_users u
WHERE p.author = u.uid :sql:posts_crit_raw
ORDER BY p.created DESC
LIMIT :limit OFFSET :offset

-- :name get-posts-filtered-count :? :1
-- :doc retrieves posts given the uid and optional itemtype/tag
SELECT count(p.id)
FROM mpx_posts p
WHERE p.author = :author :sql:posts_crit_raw

--- :name get-posts-search :? :1
--- :doc search posts
--- SELECT p.*, u.username, u.hostname, u.title, u.avatar, u.theme, u.is_private
--- FROM mpx_posts p, mpx_users u
--- WHERE p.author = u.uid AND p.author = :uid AND txt LIKE :like
--- ORDER BY p.created DESC
--- LIMIT :limit OFFSET :offset
-- -: sql:posts_like_raw

--------------------

-- :name get-all-users-internal :? :*
-- :doc retrieves all users
SELECT *
FROM mpx_users
ORDER BY uid ASC

-- :name get-all-posts-internal :? :*
-- :doc retrieves all posts
SELECT *
FROM mpx_posts
ORDER BY uid ASC