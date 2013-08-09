# multiplex

This is my little tumblelog.
(or soup.io clone, if you like - thanks for hosting me,
but I never really used the social features, I just noticed the downtimes.)

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

For now, you will need MySQL. Postgres support is planned.

## First setup

Fetch all dependencies via:

    lein deps

Copy `src/multiplex/config.clj.dist` to `src/multiplex/config.clj` and edit to your liking.
Especially edit the line starting with `(def mydb` or inject via shell variables:

    export CLEARDB_DATABASE_URL="mysql://USER:PASS@127.0.0.1/DBNAME"

Now load the DB schema by starting `lein repl` and executing this:

    (use 'multiplex.models.schema)
    (create-posts-table multiplex.config/mydb)
    (create-users-table multiplex.config/mydb)

## Running

To start a web server for the application, run:

    lein with-profile production trampoline ring server

or use ```run.sh```

## License

Copyright © 2013 Florian Anderiasch and contributors. Distributed under the Eclipse Public License, the same as Clojure uses. See the file COPYING.
