# multiplex

This is my little tumblelog.
(or soup.io clone, if you like - thanks for hosting me,
but I never really used the social features, I just noticed the downtimes.)

This is a complete rewrite started in late 2021, the original version ran
from 2013 through 2021.

## Prerequisites

You will need [Leiningen][1] 2.0 or above and PostgreSQL installed.

[1]: https://leiningen.org


## Setup


  1. Fetch all dependencies via:

```
        lein deps
```


  2. Create a postgres user and DB if you haven't already:

```
        su - postgres
        createuser -P USER
        createdb -O USER DBNAME
```

  3. Now load the DB schema: @TODO

  4. `cp run-example.sh run.sh`

  5. Configure `run.sh`


## Building

```
lein uberjar
```


## Running

For the production setup, run:

```
    ./run.sh
```


## Tests (to be improved)

```
    lein test
```


## License

Copyright © 2013-2023 Florian Anderiasch and contributors.
Distributed under the Eclipse Public License, the same as Clojure uses.
See the file COPYING.

Icons by [Maps Icons Collection](https://mapicons.mapsmarker.com), CC-BY-SA 3.0
