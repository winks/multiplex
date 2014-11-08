#!/bin/sh
runfile=`readlink -f $0`
basedir=`dirname ${runfile}`
changesfile="${basedir}/resources/public/md/changes.md"

echo -n '<h2>multiplex &middot; Changelog: Version ' > ${changesfile}
head -n1 project.clj | cut -d '"' -f 2 >> ${changesfile}
echo "</h2>\n" >> ${changesfile}

grep 'DONE' ./CHANGES | sed 's/DONE //' | sort -rV >> ${changesfile}

cmd=`cat ${basedir}/Procfile | sed 's/^web: //'`
$cmd
