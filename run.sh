#!/bin/sh
this=`readlink -f $0`
here=`dirname $this`
procfile=`cat ${here}/Procfile | sed 's/^web: //'`
$procfile
