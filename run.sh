#!/bin/sh
grep DONE CHANGES >resources/public/md/changes.md

this=`readlink -f $0`
here=`dirname ${this}`
cmd=`cat ${here}/Procfile | sed 's/^web: //'`
$cmd
