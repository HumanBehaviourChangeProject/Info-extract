#!/bin/bash

for (( NTOPTERMS=5; NTOPTERMS<=25; NTOPTERMS+=5 ))
do

for (( NEGWS=20; NEGWS<=50; NEGWS+=10 ))
do

CVSIZE=5
LAMBDA=0.5

BASEDIR=$PWD
SCRIPTDIR=$BASEDIR/scripts
LOGDIR=$BASEDIR/logs/
PROPFILE=$SCRIPTDIR/bcts.supervised.com.ibm.drl.hbcp.nb.properties
LOGFILE=$LOGDIR/res.s.com.ibm.drl.hbcp.nb-$NTOPTERMS.$NEGWS.txt

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
negativesampling.windowsize=$NEGWS
numwanted=3

ref.json=data/jsons/Sprint2_Codeset2_Codeset1_merge.json


#BCT extraction

extract.bct=true

#attribute specific thresholds
attributes.typedetect.threshold=0.25

#List of attribute ids for which we want to execute supervised Information Extractor
attributes.typedetect.supervised=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612

#value of n for n-fold cross-validation
attributes.typedetect.supervised.cv=$CVSIZE
#value of n for n-fold cross-validation
#Learning threshold has no effect in NB
#learn_threshold=true

attributes.typedetect.supervised.ntopterms=$NTOPTERMS
attributes.typedetect.supervised.lambda=$LAMBDA

#If this is true then apply Naive Bayes for classification
bct.classifier=true

EOF1

echo "Extracting information from the index"
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.extractor.InformationExtractor" -Dexec.args="$PROPFILE" > $LOGFILE 2>&1

echo "Finished extracting..."

a=`grep "^Accuracy = " $LOGFILE | awk '{print $NF}'`
p=`grep "^Precision = " $LOGFILE | awk '{print $NF}'`
r=`grep "^Recall = " $LOGFILE | awk '{print $NF}'`
f=`grep "^F-score = " $LOGFILE | awk '{print $NF}'`
m=`grep "^Avg-METEOR = " $LOGFILE | awk '{print $NF}'`

echo "Mean-average results: "
echo "$NTOPTERMS\t$NEGWS\t$a\t$p\t$r\t$f\t$m"

echo "$NTOPTERMS\t$NEGWS\t$a\t$p\t$r\t$f\t$m" >> tmp

done
done

cat tmp
rm tmp*

