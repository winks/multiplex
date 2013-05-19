#!/bin/sh
runfile=`readlink -f $0`
basedir=`dirname ${runfile}`
changesfile="${basedir}/resources/public/md/changes.md"

echo -n '<p><strong>multiplex Version ' > ${changesfile}
sed -n '3p' project.clj | sed -e 's/"//g' | sed -e 's/[ ]*//g' >> ${changesfile}
echo "</strong></p>\n" >> ${changesfile}

grep DONE CHANGES | sed 's/DONE //' >> ${changesfile}

cmd=`cat ${basedir}/Procfile | sed 's/^web: //'`
$cmd
