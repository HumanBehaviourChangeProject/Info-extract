#!/bin/bash

if [ $# -lt 2 ]
then
        echo "Usage: $0 <num queries> [an integer (say 50)] <build qrels> [true/false]"
        exit
fi

SCRIPTDIR=scripts/com.ibm.drl.hbcp.inforetrieval.apr/
PROPFILE=$SCRIPTDIR/com.ibm.drl.hbcp.inforetrieval.apr.retrieve.properties

NUM_QUERIES=$1
BUILD_QRELS=$2
QRELS_FILE=com.ibm.drl.hbcp.inforetrieval.apr/qrels/qrels.txt
RES_FILE=com.ibm.drl.hbcp.inforetrieval.apr/res/res.content.txt

cat > $PROPFILE  << EOF1

##### APR parameters  ####
com.ibm.drl.hbcp.inforetrieval.apr.index=src/main/resources/indexes/com.ibm.drl.hbcp.inforetrieval.apr.index/
#length in terms of #sentences
com.ibm.drl.hbcp.inforetrieval.apr.mindoclen=2
com.ibm.drl.hbcp.inforetrieval.apr.numqueries=$NUM_QUERIES
com.ibm.drl.hbcp.inforetrieval.apr.qrels=$QRELS_FILE
com.ibm.drl.hbcp.inforetrieval.apr.result.file=$RES_FILE

#allowable modes - content/ie/supervised
com.ibm.drl.hbcp.inforetrieval.apr.results.basedir=com.ibm.drl.hbcp.inforetrieval.apr
com.ibm.drl.hbcp.inforetrieval.apr.mode=content
com.ibm.drl.hbcp.inforetrieval.apr.content.ntopterms=10
com.ibm.drl.hbcp.inforetrieval.apr.content.lambda=0.4
com.ibm.drl.hbcp.inforetrieval.apr.numwanted=100
#whether to build a new set of qrels in the program flow
com.ibm.drl.hbcp.inforetrieval.apr.buildqrels=$BUILD_QRELS
#####

### other init.properties values ###
sprints=1234
coll=data/pdfs_Sprint1234/
stopfile=data/stop.txt

ref.json=data/jsons/Sprint1234_merge.json

#+++Index paths
#Document level index
index=src/main/resources/indexes/index/
#Extracted Info index (to be put in the default resource folder of maven)
ie.index=src/main/resources/indexes/ie.index/
#Root directory of paragraph index of various different sizes
para.index=src/main/resources/indexes/para.index/
#All paragraph sizes merged in a single index
para.index.all=src/main/resources/indexes/para.index.all/

#+++Other Paths
models.dir=src/main/resources/models/
org.slf4j.simpleLogger.defaultLogLevel=info

window.sizes=20

#To switch to sentence Based Paragraphs
use.sentence.based=false
para.number.of.sentences=2

#To select which attribute to extract (Not sure those are used)(Debasis says that they are still used)
extract.bct=true
extract.outcome=false
extract.context=true

#attributes.typedetect.ids=3673271
attributes.typedetect.ids=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612
#attributes.typedetect.ids_Outcome=3909808,3937167,3937812,4103413,3909809
#attributes.typedetect.ids_Effect=3870686,3870695,3870696,3870697,3870702
#attributes.typedetect.ids_Outcome=3909808,3909809
#attributes.typedetect.ids_Effect=3870695

#To set up thresholds
attributes.typedetect.threshold=0.2

#change this to 12 if u want to run only on sprints 1 and 2, and so on...
#This is for unsupervised evaluation

#+++ # the queries are used only in the unsupersived flow.------------------------------------------------
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

#Outcome (behaviour) value

#outcome value intervention 1
attributes.typedetect.query.3909808=success\ rate\ result\ outcome\ intervention\  
#outcome value intervention 2
attributes.typedetect.query.3937167=success\ rate\ result\ outcome\ control\ group\ intervention\ 
#outcome value intervention 3
attributes.typedetect.query.3937812=success\ rate\ result\ outcome\ control\ group\ intervention\ 
#outcome value intervention 4
attributes.typedetect.query.4103413=success\ rate\ result\ outcome\ control\ group\ intervention\ 
#outcome value control
attributes.typedetect.query.3909809=control\ group\

#effect size type
#Odds Ratio
#attributes.typedetect.query.3870686=odd\ AND\ ratio\ OR\ OR=\
##Rate Ratio
#attributes.typedetect.query.3870687=rate\ AND\ \ratio
#Risk Ratio
#attributes.typedetect.query.3870690=risk\ AND\ \ratio
#Risk Difference
#attributes.typedetect.query.3870691=rate\ AND\ \difference
#Mean Difference
#attributes.typedetect.query.3870692=mean\ AND\ \difference
#Median Difference
#attributes.typedetect.query.3870693=median\ AND\ \difference
#Hazard Ratio
#attributes.typedetect.query.3870694=hazard\ AND\ \ratio
#Relative Risk
#attributes.typedetect.query.3933095=relative\ AND\ \risk
#Chi square
#attributes.typedetect.query.4096686=chi\ AND\ \square

#effect size estimate
attributes.typedetect.query.3870695=effect\ outcome\ OR\ OR=\
#effect size p value
attributes.typedetect.query.3870696=effect\ OR\ outcome\ AND\ p=\
#effect size lower 95% CI
#attributes.typedetect.query.3870697=95%\ AND\ CI\
attributes.typedetect.query.3870697=CI\
#effect size upper 95% CI
attributes.typedetect.query.3870702=CI\

#+++Supervised Flow---------------------------------------------------------------------------------

#List of attribute ids for which we want to execute supervised Information Extractor
#attributes.typedetect.supervised=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612
#attributes.typedetect.supervised=3673273,3673275,3675715,3673817,3673818,3673279,3673280,3673281,3673282,3673285,3673286,3673287,3673288,3675716,3675718,3675719,3673293,3675720,3673294,3673295,3673296,4085489,3673297,3673299,3673301,3673302,3673303,3673304,3673305,3673306,3673307,3673778,3673779,3673780,3673781,3674248,3674252,3674253,3674254,3674255,3675723,3675724,3675725,3675726,3675727,3675728,3675729,3675730,3674256,3674257,3674258,3674259,3674260,3674261,3674262,3674263,3674264,3674265,3674266,3674267,3674268,3674269,3674270,3674271,3675610,3675613,3675678,3675679,3675680,3675681,3675682,3675683,3675684,3675685,3675686,3675687,3675688,3675689,3675690,3675691,3675692,3675694,3675695,3675697,3675698,3675699,3675700,3675701,3675702,3675703,3675704,3675705,3675707,3675708,3675709,3675710,3675711,3675712,3675713,3675714,3719285,3719286,3719287,3719288,3723321,3730876,3731379,3731381,4090468,4116069,4319891,4542875

#attributes.typedetect.supervised=3673271

#value of n for n-fold cross-validation
attributes.typedetect.supervised.cv=5

# number of terms in the query
attributes.typedetect.supervised.ntopterms=2

#this true is flow with classification (  3) 
#If this is true then apply Naive Bayes for classification
bct.classifier=false

# Those parameters affect the generation of negative examples for the classifier.
# Only relevant when bct.classifier=true
negativesampling.windowsize=200
numwanted=1

#ToDoTrain/Test with supervised evaluation. Setting the first element to false means cross validation approach based on sprints
traintest.activate=true
train.sprints=12
test.sprints=34

EOF1

mvn compile
mvn exec:java@apretrieve -Dexec.args=$PROPFILE

cd trec_eval
./trec_eval -mndcg_cut.5,10 -mndcg -mrecall.1000 -m P.5 ../$QRELS_FILE ../$RES_FILE
cd -
