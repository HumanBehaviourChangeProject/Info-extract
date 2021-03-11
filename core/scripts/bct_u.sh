#!/bin/bash

if [ $# -lt 1 ]
then
        echo "Usage: $0 <sim-threshold for BCT classification>"
        exit
fi

THRESH=$1

BASEDIR=$PWD
SCRIPTDIR=$BASEDIR/scripts
PROPFILE=$SCRIPTDIR/bcts.unsupervised.properties
LOGFILE=$SCRIPTDIR/res.u.$THRESH.txt

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

window.sizes=10,20,30
numwanted=3

ref.json=data/jsons/Sprint2_Codeset2_Codeset1_merge.json


#BCT extraction

extract.bct=true
attributes.typedetect.ids=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612

#attribute specific thresholds
attributes.typedetect.threshold=$THRESH

#BCT queries
attributes.typedetect.query.3673271=goal\ OR\ target\ AND\ quit\ OR\ plan
attributes.typedetect.query.3673272=cope\ overcome\ identify\ problem\ relapse
attributes.typedetect.query.3673274=action\ plan\ intention\ quit
attributes.typedetect.query.3673283=patient\ feedback
attributes.typedetect.query.3673284=self\ monitor\ diary\ track
attributes.typedetect.query.3675717=quit\ instruction\ advice\ training
attributes.typedetect.query.3673298=provide\ information\ harmful\ effects\ health\ hazard\ smoking
attributes.typedetect.query.3673300=harmful\ chemical\ environmental\ consequences
attributes.typedetect.query.3675611=nicotine\ gum\ patch\ NRT\ transdermal
attributes.typedetect.query.3675612=negative\ emotion\ stress

EOF1

echo "Extracting information from the index"
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.extractor.InformationExtractor" -Dexec.args="$PROPFILE" > $LOGFILE 2>&1

grep "= Accuracy: " $LOGFILE | awk '{print $NF}' > tmp1
grep "Precision of" $LOGFILE | awk '{print $NF}' | sed 's/(//'|sed 's/)//' > tmp2
grep "Recall of" $LOGFILE | awk '{print $NF}' | sed 's/(//'|sed 's/)//' > tmp3
grep "F-score of" $LOGFILE | awk '{print $NF}' | sed 's/(//'|sed 's/)//' > tmp4

paste tmp1 tmp2 tmp3 tmp4 > tmp
cat tmp

rm tmp*

