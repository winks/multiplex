extern crate getopts;
extern crate postgres;
extern crate chrono;

use getopts::Options;
use postgres::{Connection, TlsMode};
use postgres::types::ToSql;
use std::env;
use std::io;
use std::ops::IndexMut;
use std::ops::Index;
use chrono::NaiveDateTime;


struct User {
    uid: i32,
    username: String,
    email: String,
    password: String,
    apikey: String,
    signupcode: String,
    created: NaiveDateTime,
    updated: NaiveDateTime,
    hostname: String,
    title: String,
    theme: String,
}

struct Post {
    id: i32,
    author: i32,
    itemtype: String,
    url: String,
    txt: String,
    meta: String,
    tag: String,
    created: NaiveDateTime,
    updated: NaiveDateTime,
}

fn parse_int(s: String) -> i32 {
    let n: Option<i32> = s.trim().parse().ok();
    let num = match n {
        Some(num) => num,
        None => 0,
    };

    return num;
}

fn print_usage(program: &str, opts: Options) {
    let brief = format!("Usage: {} [options]", program);
    print!("{}", opts.usage(&brief));
}

fn delete_post(id: i32, conn: &Connection) -> u64 {
    let sql = "UPDATE mpx_posts SET deleted = 1 WHERE id = $1";
    let updates = conn.execute(&sql, &[&id]).unwrap();
    return updates;
}

fn get_users(uid: i32, conn: &Connection) -> Vec<Option<User>> {
    let mut sql = "SELECT uid, username, email, password, apikey, signupcode,
                          created, updated, hostname, title, theme
                     FROM mpx_users".to_string();
    let p_all: &[&ToSql] = &[];
    let p_single: &[&ToSql] = &[&uid];
    let mut p = p_all;
    if uid > 0 {
        sql = format!("{} WHERE uid = $1 LIMIT 1", &sql);
        p = p_single;
    } else {
        sql = format!("{} ORDER BY uid ASC", &sql);
    }

    let mut users = vec![];
    let query = conn.prepare(&sql).unwrap();
    for row in &query.query(p).unwrap() {
        let user = Some(User {
            uid: row.get(0),
            username: row.get(1),
            email: row.get(2),
            password: row.get(3),
            apikey: row.get(4),
            signupcode: row.get(5),
            created: row.get(6),
            updated: row.get(7),
            hostname: row.get(8),
            title: row.get(9),
            theme: row.get(10),
        });
        users.push(user);
    };

    return users;
}

fn get_posts(limit: i32, offset: i32, author: i32, conn: &Connection) -> Vec<Option<Post>> {
    let mut sql = "SELECT id, author, itemtype, url, txt,
                          meta, tag, created, updated
                     FROM mpx_posts".to_string();
    let p_all: &[&ToSql] = &[];
    let p_single: &[&ToSql] = &[&author];
    let mut p = p_all;
    if author > 0 {
        sql = format!("{} WHERE author = $1 ", &sql);
        p = p_single;
    } else {
        sql = format!("{} ORDER BY id ASC", &sql);
        if limit > 0 {
            sql = format!("{} LIMIT {}", &sql, limit);
            if offset > 0 {
                sql = format!("{} OFFSET {}", &sql, offset);
            }
        }
    }
    println!("{}", sql);
    let query = conn.prepare(&sql).unwrap();
    let mut posts = vec![];
    for row in &query.query(p).unwrap() {
        let post = Some(Post {
            id: row.get(0),
            author: row.get(1),
            itemtype: row.get(2),
            url: row.get(3),
            txt: row.get(4),
            meta: row.get(5),
            tag: row.get(6),
            created: row.get(7),
            updated: row.get(8),
        });
        posts.push(post);
    };

    return posts;
}

fn show_post(post: &Post, author: User) {
    println!("{}\t{}\t{}\t{}", post.id, post.itemtype, post.created, post.updated);
    println!("\tAuthor: ({}, {}, {})", author.uid, author.username, author.email);
    println!("\t{}", post.url);
    println!("\t{}", post.txt);
    println!("\t{}", post.tag);
    println!("\t{}", post.meta);
}

fn show_user(user: User) {
    println!("id       : {}", user.uid);
    println!("username : {}", user.username);
    println!("hostname : {}", user.hostname);
    println!("email    : {}", user.email);
    println!("apikey   : {}", user.apikey);
    println!("title    : {}", user.title);
    println!("theme    : {}", user.theme);
    println!("---------------------------------");
}

fn show_userlist(conn: &Connection) {
    let users = get_users(0, &conn);
    for user in users {
        show_user(user.unwrap());
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let program = args[0].clone();

    let mut opts = Options::new();
    opts.optopt("l", "limit", "LIMIT X for get_posts", "LIMIT");
    opts.optopt("o", "offset", "OFFSET X for get_posts", "OFFSET");
    opts.optopt("a", "author", "AUTHOR X for get_posts", "AUTHOR");
    opts.optopt("d", "delete", "DELETE post #ID", "ID");
    opts.optflag("u", "userlist", "list users");
    opts.optflag("h", "help", "show help");

    let matches = match opts.parse(&args[1..]) {
        Ok(m) => {
            m
        }
        Err(f) => {
            panic!(f.to_string())
        }
    };

    if matches.opt_present("h") {
        print_usage(&program, opts);
        return;
    }
    let limit = match matches.opt_str("l") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let offset = match matches.opt_str("o") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let author = match matches.opt_str("a") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let delete = match matches.opt_str("d") {
        Some(s) => parse_int(s),
        None => 0,
    };

    let key = "DB_MULTIPLEX";
    let db_string = match env::var(key) {
        Ok(val) =>  val,
        Err(_) => "postgres://multiplex:multiplex@127.0.0.1".to_string(),
    };

    let conn = Connection::connect(db_string, TlsMode::None).unwrap();
    println!("");

    if matches.opt_present("u") {
        show_userlist(&conn);
        return;
    }


    if delete > 0 {
        let mut buffer = String::new();
        println!("Do you really want to delete post #{}? y/n?", delete);
        io::stdin().read_line(&mut buffer).expect("Failed to read line");

        if "y" == buffer.trim().to_lowercase() {
            println!("yes");
            let updates = delete_post(delete, &conn);
            println!("{} rows were updated", updates);
            return;
        }
        return;
    }

    let posts = get_posts(limit, offset, author, &conn);
    for post in posts {
        let p = post.unwrap();
        let users = get_users(p.author, &conn);
        for user in users {
            show_post(&p, user.unwrap());
        }
    }

}
