<?php
$user = "multiplex";
$pass = "multiplex";

$db1 = new PDO("mysql:host=localhost;dbname=multiplex", $user, $pass);
$db1->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);

$user = "multiplex";
$pass = "multiplex";

$db2 = new PDO("pgsql:host=localhost;port=5432;dbname=multiplex", $user, $pass);
#################################################################################
$sql = "SELECT * FROM clj ORDER BY id ASC";
$q1 = $db1->query($sql);
$errors = array();

while ($row = $q1->fetch()) {
    $sql = "INSERT INTO clj (id, author, itemtype, url, txt, meta, tag, created, updated)".
           " VALUES('%s','%s','%s','%s','%s','%s','%s','%s','%s');";
    $sql = sprintf(
        $sql,
        $row['id'],
        $row['author'],
        $row['itemtype'],
        $row['url'],
        str_replace("'", "''", $row['txt']),
        str_replace("'", "''", $row['meta']),
        $row['tag'],
        $row['created'],
        $row['updated']
    );
    $ret = $db2->query($sql);
    if ($ret === false) {
        echo $row["id"] . " ";
        $errors[] = $sql;
    }
}

echo "Post errors: ".join(PHP_EOL, $errors).PHP_EOL;
#################################################################################
$sql = "SELECT * FROM users ORDER BY uid ASC";
$q1 = $db1->query($sql);
$errors = array();

while ($row = $q1->fetch()) {
    $sql = "INSERT INTO users(uid,username,email,password,apikey,signupcode,created,updated)".
           " VALUES('%s','%s','%s','%s','%s','%s','%s','%s')";
    $sql = sprintf(
        $sql,
        $row['uid'],
        $row['username'],
        $row['email'],
        $row['password'],
        $row['apikey'],
        $row['signupcode'],
        $row['created'],
        $row['updated']
    );

    $ret = $db2->query($sql);
    if ($ret === false) {
        echo $row["uid"] . " ";
        $errors[] = $sql;
    }
}

echo "User errors: ".join(PHP_EOL, $errors).PHP_EOL;
#################################################################################
