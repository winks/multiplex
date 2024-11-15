extern crate getopts;
extern crate postgres;

use getopts::{Fail, Options};
use postgres::{Client, NoTls};
use postgres::types::ToSql;
use std::env;
use std::io;
use std::process;
use time::PrimitiveDateTime;

const TBL_USERS: &str = "mpx_users";
const TBL_POSTS: &str = "mpx_posts";

#[allow(dead_code)]
struct User {
    uid: i32,
    username: String,
    email: String,
    password: String,
    apikey: String,
    signupcode: String,
    created: Option<PrimitiveDateTime>,
    updated: Option<PrimitiveDateTime>,
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
    created: Option<PrimitiveDateTime>,
    updated: Option<PrimitiveDateTime>,
}

fn parse_int(s: String) -> i32 {
    let n: Option<i32> = s.trim().parse().ok();
    n.unwrap_or_default()
}

fn print_usage(program: &str, opts: Options) {
    let brief = format!("Usage: {} [options]", program);
    print!("{}", opts.usage(&brief));
}

fn exit_with_err(e: Fail) {
    println!("Error: {}", e);
    println!("Exiting.");
    process::exit(1);
}

fn delete_post(id: i32, conn: &mut Client) -> u64 {
    let sql = format!("UPDATE {} SET deleted = 1 WHERE id = $1", TBL_POSTS);
    conn.execute(&sql, &[&id]).unwrap()
}

fn get_users(uid: i32, conn: &mut Client) -> Vec<Option<User>> {
    let mut sql = format!("
    SELECT uid, username, email, password, apikey, signupcode,
           created, updated, hostname, title, theme
      FROM {}", TBL_USERS);
    let p_all: &[&(dyn ToSql + Sync)] = &[];
    let p_single: &[&(dyn ToSql + Sync)] = &[&uid];
    let mut p = p_all;
    if uid > 0 {
        sql = format!("{} WHERE uid = $1 LIMIT 1", &sql);
        p = p_single;
    } else {
        sql = format!("{} ORDER BY uid ASC", &sql);
    }

    let mut users = vec![];
    let query = conn.prepare(&sql).unwrap();
    for row in &conn.query(&query, p).unwrap() {
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

    users
}

fn get_posts(limit: i32, offset: i32, author: i32, conn: &mut Client) -> Vec<Option<Post>> {
    let mut sql = format!("
    SELECT id, author, itemtype, url, txt,
           meta, tag, created, updated
      FROM {}", TBL_POSTS);
    let p_all: &[&(dyn ToSql + Sync)] = &[];
    let p_single: &[&(dyn ToSql + Sync)] = &[&author];
    let mut p = p_all;
    if author > 0 {
        sql = format!("{} WHERE author = $1", &sql);
        p = p_single;
    }
    sql = format!("{} ORDER BY id ASC", &sql);
    if limit > 0 {
        sql = format!("{} LIMIT {}", &sql, limit);
        if offset > 0 {
            sql = format!("{} OFFSET {}", &sql, offset);
        }
    }

    println!("{}", sql);
    let query = conn.prepare(&sql).unwrap();
    let mut posts = vec![];
    for row in &conn.query(&query, p).unwrap() {
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

    posts
}

fn show_post(post: &Post, author: User) {
    println!("{}\t{}\t{}\t{}", post.id, post.itemtype, post.created.unwrap(), post.updated.unwrap());
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

fn show_userlist(conn: &mut Client) {
    let users = get_users(0, conn);
    for user in users {
        show_user(user.unwrap());
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let program = args[0].clone();

    let mut opts = Options::new();
    opts.optflag("s", "show", "show entries");
    opts.optflag("u", "userlist", "list users");
    opts.optflag("h", "help", "show help");

    opts.optopt("a", "author", "AUTHOR X for get_posts", "AUTHOR");
    opts.optopt("l", "limit", "LIMIT X for get_posts", "LIMIT");
    opts.optopt("o", "offset", "OFFSET X for get_posts", "OFFSET");
    opts.optopt("d", "delete", "DELETE post #ID", "ID");

    let matches = opts.parse(&args[1..]).map_err(exit_with_err).unwrap();

    if matches.opt_present("h") || env::args().count() < 2 {
        print_usage(&program, opts);
        return;
    }
    let opt_limit = match matches.opt_str("l") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let opt_offset = match matches.opt_str("o") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let opt_author = match matches.opt_str("a") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let opt_delete = match matches.opt_str("d") {
        Some(s) => parse_int(s),
        None => 0,
    };
    let opt_userlist = matches.opt_present("u");
    let opt_entries = matches.opt_present("s");

    let key = "DB_MULTIPLEX";
    let db_string = match env::var(key) {
        Ok(val) =>  val,
        Err(_) => "postgres://multiplex:multiplex@127.0.0.1".to_string(),
    };

    let mut client = Client::connect(&db_string, NoTls).unwrap();

    if opt_userlist {
        show_userlist(&mut client);
        return;
    }

    if opt_delete > 0 {
        let mut buffer = String::new();
        println!("Do you really want to delete post #{}? y/n?", opt_delete);
        io::stdin().read_line(&mut buffer).expect("Failed to read line");

        if "y" == buffer.trim().to_lowercase() {
            println!("yes");
            let updates = delete_post(opt_delete, &mut client);
            println!("{} rows were updated", updates);
        }
        return;
    }

    if opt_entries {
        let posts = get_posts(opt_limit, opt_offset, opt_author, &mut client);
        for post in posts {
            let p = post.unwrap();
            let users = get_users(p.author, &mut client);
            for user in users {
                show_post(&p, user.unwrap());
            }
        }
        return;
    }

    // fallback
    print_usage(&program, opts);
}
