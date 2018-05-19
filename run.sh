#!/bin/sh
runfile=$(readlink -f "$0")
basedir=$(dirname "${runfile}")
changesfile="${basedir}/resources/public/md/changes.md"

echo -n '## multiplex &middot; changelog &middot; version ' > "${changesfile}"
head project.clj | perl -n -e '/defproject\s+multiplex\s+"([^"]+)"/ && print $1' >> "${changesfile}"
echo "" >> "${changesfile}"

grep 'DONE' ./CHANGES | sed 's/DONE //' | sort -rV >> "${changesfile}"

cmd=`cat ${basedir}/Procfile | sed 's/^web: //'`
$cmd
