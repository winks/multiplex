# multiplex

This is my little tumblelog.
(or soup.io clone, if you like - thanks for hosting me,
but I never really used the social features, I just noticed the downtimes.)

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

For now, you will need PostgreSQL, MySQL support is deprecated/untested.

## First setup

  1. Copy `src/multiplex/config.clj.dist` to `src/multiplex/config.clj`.

     Most important are the DB connection and the hostname.

     Look for `(def mydb` or inject via shell variables:

        export CLEARDB_DATABASE_URL="postgres://USER:PASS@127.0.0.1/DBNAME"

     and `(def multiplex`.

  2. Fetch all dependencies via:

        lein deps


  3. Create a postgres user and DB if you haven't already:

        su - postgres
        createuser -P USER
        createdb -O USER DBNAME

  4. Now load the DB schema by starting `lein repl` and executing this:

        (use 'multiplex.models.schema)
        (create-tables)

     Then exit with Ctrl-D. You're done!


## Running

To hack on `multiplex`, run:

    lein trampoline ring server

or use `run_dev.sh`.


For the production setup, run:

    lein with-profile production trampoline ring server

or use ```run.sh```.


## Tests (to be improved)

    lein test


## License

Copyright © 2013-2014 Florian Anderiasch and contributors.
Distributed under the Eclipse Public License, the same as Clojure uses.
See the file COPYING.
