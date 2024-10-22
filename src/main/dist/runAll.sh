#!/usr/bin/env bash

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPNAME=orthology-paf-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
EMAILLIST=jdepons@mcw.edu

if [ "$SERVER" == "REED" ]; then
  EMAILLIST=rgd.devops@mcw.edu,jrsmith@mcw.edu
fi

# run for all species in RGD, except human (transitive orthologs are made between human and given species)
$APPDIR/run.sh "mRatBN7.2" 372 "GRCh38.p14" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCh37.p13" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "NCBI36" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCm39" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCm38" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "MGSCv37" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "CanFam3.1" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "Sscrofa11.1" 38 "/tmp/test" 2>&1 > $APPDIR/run.log


$APPDIR/run.sh "mRatBN7.2" 372 "GRCh38.p14" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCh38.p14" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCh38.p14" 38 "/tmp/test" 2>&1 > $APPDIR/run.log
$APPDIR/run.sh "mRatBN7.2" 372 "GRCh38.p14" 38 "/tmp/test" 2>&1 > $APPDIR/run.log




mailx -s "[$SERVER] orthology PAF for all jbrowse assemblies" $EMAILLIST < $APPDIR/logs/summary.log
