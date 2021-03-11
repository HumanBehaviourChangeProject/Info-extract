#!/bin/bash

if [ $# -lt 3 ]
then
        echo "Usage: $0 <n-fold (1/5/10)> <sim-threshold for BCT classification (1 for learning threshold)> <ntopterms>"
        exit
fi

CVSIZE=$1
THRESH=$2
NTOPTERMS=$3

if [ $THRESH -eq 1 ]
then
	LEARN_THRESH="true"
else
	LEARN_THRESH="false"
fi

echo "Learning thresholds set to $LEARN_THRESH"

BASEDIR=$PWD
SCRIPTDIR=$BASEDIR/scripts
PROPFILE=$SCRIPTDIR/bcts.supervised.properties

cat > $PROPFILE  << EOF1

coll=data/pdfs/judged/
stopfile=data/stop.txt

#+++Index paths

#Document level index
index=src/main/resources/indexes/index/

#Extracted Info index (to be put in the default resource folder of maven)
ie.index=src/main/resources/indexes/ie.index/

#Root directory of paragraph index of various different sizes
para.index=src/main/resources/indexes/para.index/

#All paragraph sizes merged in a single index
para.index.all=src/main/resources/indexes/para.index.all/

#---

window.sizes=10,20
numwanted=3

ref.json=data/jsons/Sprint2_Codeset2_Codeset1_merge.json


#BCT extraction

extract.bct=true

#attribute specific thresholds
attributes.typedetect.threshold=$THRESH

#List of attribute ids for which we want to execute supervised Information Extractor
attributes.typedetect.supervised=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612

#value of n for n-fold cross-validation
attributes.typedetect.supervised.cv=$CVSIZE
learn_threshold=$LEARN_THRESH
attributes.typedetect.supervised.ntopterms=$NTOPTERMS


EOF1

echo "Extracting information from the index"
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.extractor.InformationExtractor" -Dexec.args="$PROPFILE"

