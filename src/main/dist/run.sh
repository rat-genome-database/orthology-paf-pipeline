#!/usr/bin/env bash
#
# generate JBrowse2 synteny track files (BED, .anchors, .json) for all configured assemblies

. /etc/profile

APPNAME=orthology-paf-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR

rm -rf "${APPDIR}/out/*"

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "${APPDIR}/out" 2>&1

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
if [ "$SERVER" == "REED" ]; then
  scp "${APPDIR}/out/*" rgdpub@pipelines.rgd.mcw.edu:/data/data/jbrowse2/orthology/
fi
