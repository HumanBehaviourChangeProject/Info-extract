#!/bin/bash

if [ $# -lt 6 ]
then
        echo "Usage: $0 <armified (0/1)> <gt/ie> <use-effect> <window-size> <p> <q>"
        exit
fi

ARM=$1
SRC=$2
USE_EFFECT=$3
WSIZE=$4
P=$5
Q=$6

JSON="data/jsons/Smoking_AllAnnotations_01Apr19.json"
OVATTRIB=5140146

BASEDIR=$PWD
SCRIPTDIR=$BASEDIR/scripts
PROPFILE=$SCRIPTDIR/ovpred.$ARM.$SRC.properties

cat > $PROPFILE  << EOF1

stopfile=data/stop.txt

ref.json=$JSON

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

#+++Unsupervised Pipeline ---------------------------------------------------------------------------------
#default is window based approach
#window.sizes=10,20,30
window.sizes=50

#To switch to sentence Based Paragraphs
use.sentence.based=false
para.number.of.sentences=1

#To select which attribute to extract (Not sure those are used)(Debasis says that they are still used)
extract.bct=true
extract.outcome=true
extract.context=true
extract.arms=true

#attributes.typedetect.ids=3673271
attributes.typedetect.ids=3673271,3673272,3673274,3673283,3673284,3675717,3673298,3673300,3675611,3675612
#attributes.typedetect.ids_Outcome=3909808,3937167,3937812,4103413,3909809
#attributes.typedetect.ids_Effect=3870686,3870695,3870696,3870697,3870702
attributes.typedetect.ids_Outcome=3909808,3909809
attributes.typedetect.ids_Effect=3870695

#To set up thresholds
attributes.typedetect.threshold=0.2
#attribute specific thresholds - 
#attributes.typedetect.threshold.3673271=0.15
#attributes.typedetect.threshold.3673272=0.15
#attributes.typedetect.threshold.3673274=0.5
#attributes.typedetect.threshold.3673283=0.25
#attributes.typedetect.threshold.3673284=0.15
#attributes.typedetect.threshold.3675717=0.01
#attributes.typedetect.threshold.3673298=0.5
#attributes.typedetect.threshold.3673300=0.25
#attributes.typedetect.threshold.3675611=0.10
#attributes.typedetect.threshold.3675612=0.25

#change this to 12 if u want to run only on sprints 1 and 2, and so on...
#This is for unsupervised evaluation
#sprint 1234 is for non-arfimification annotation; sprint 5 is for armification annotation
sprints=1234
armification=true
#---
#sprints=5
#armification=true
#---

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

# params for nodevecs (separate from wordvecs used in QE)
nodevecs.vecfile=prediction/graphs/nodevecs/refVecs.vec

#+++Unused Items------------------------------------------------------------------------------------
#Word vector training parameters
wordvec.trainfile=data/wvec/wvectrain.txt
#attributes.typedetect.numwanted=5

#Query expansion parameters
# For turning off QE, set this parameter to zero.
wordvecs.readfrom=vec
wordvecs.vecfile=data/wvec/pubmed.vec
qe.nn=0

# this is to learn the threshold from the training set ( does not give good results because we do not have dev)
#Parameter to control grid search of optimal threshold value
#over the training set. 
learn_threshold=false


cloudant.auth=ptommasi:pwd
cloudant.store=lite.eu-de.mybluemix.net
cloudant.dbname=lite

##### APR parameters  ####
com.ibm.drl.hbcp.inforetrieval.apr.index=src/main/resources/indexes/apr.index/
#length in terms of #sentences
com.ibm.drl.hbcp.inforetrieval.apr.mindoclen=2
com.ibm.drl.hbcp.inforetrieval.apr.numqueries=5
com.ibm.drl.hbcp.inforetrieval.apr.qrels=apr/qrels/qrels.txt

#allowable modes - content/ie/supervised
com.ibm.drl.hbcp.inforetrieval.apr.results.basedir=apr
com.ibm.drl.hbcp.inforetrieval.apr.mode=content
com.ibm.drl.hbcp.inforetrieval.apr.content.ntopterms=10
com.ibm.drl.hbcp.inforetrieval.apr.content.lambda=0.4
com.ibm.drl.hbcp.inforetrieval.apr.numwanted=100
#whether to build a new set of qrels in the program flow
com.ibm.drl.hbcp.inforetrieval.apr.buildqrels=false
com.ibm.drl.hbcp.inforetrieval.apr.result.file=apr/res/res.content.txt

###############################
#prediction properties
###############################

prediction.usearms=$ARM

#allowable types: [gt/ie] (manual annotations / automatically extracted values)
prediction.source=$SRC
prediction.basepath=prediction/
prediction.graph.path=prediction/graphs/
prediction.graph.loadfrom=prediction/graphs/relations.graph
prediction.graph.outfile.name=relations

#mean-age,
prediction.attribtype.numerical=3587809,4507456,4507457,4507458,4507460,4507461,4507462,4507463,4507464,4507564,4507435,4507433,4507429
prediction.attribtype.numerical.defaults=4507564:1
prediction.train.ratio=0.8
prediction.attribtype.mixed.gender=4507427

#pregnancy is a type where the value can be ignored. only the presence
#is to be taken care of. List all such attribute ids here
prediction.attribtype.annotation.presence=4507524,4507526,4507426,4507430,4507489,4507490,4507480,4507440
prediction.interventions.absence.nodes=false
prediction.graph.c2o=true
prediction.normalize_in_0_and_1=false

#for types where you would want to exercise control over the number of
#categories you would want to have
prediction.categories.4087172=smoking,abstinence
prediction.categories.4087186=biochemical,co_carbon_monoxide,cotinine,saliva,urine,anabasine,blood_sample,serum
prediction.categories.4087184=question,interview,self-report,telephone
prediction.categories.4087185=family-friend,proxy
prediction.categories.4507426=male,female
prediction.categories.4087178=abstinence,cessation,quit

#language-proficiency
prediction.categories.4507415=inability_English,English-speaking,speak_write_English,dutch,chinese
prediction.categories.4507424=inability_English,English-speaking,speak_write_English,dutch,chinese

prediction.attribtype.week_month=4087188,4087187,4087189,4087191

#most complex and control group respectively
prediction.attribtype.predictable.output=3909808,3909809
#in [0, 1]
prediction.retrain_graph=false

#Allowable features in the query
#mean-age,min-age,max-age,median-age,mixed-genders,all-male,all-female,(<edu-levels>:)
prediction.testquery.population=4507433,4507435,4507434,4507429,4507427,4507430,4507426,4507456,4507457,4507458,4507460,4507461,4507462,4507463,4507464,4507465
#first-follow,longest-follow,self-report,informant-verified,biochemical-verification
prediction.testquery.outcomes=4087187,4087191,4087184,4087185,4087186

prediction.testquery.outcomevalues=$OVATTRIB
prediction.testquery.include_outcomes=true

prediction.numwanted=3

node2vec.layer1_size=128
node2vec.onehop_pref=0.7
node2vec.alpha=0.05
node2vec.directed=false
node2vec.window=$WSIZE
node2vec.ns=10
node2vec.niters=50
node2vec.mincount=1
node2vec.pqsampling=true
#p1 ==> inward nodes
node2vec.p1=$P
#q1 ==> outward nodes
node2vec.q1=$Q

#Evaluation flow
#Allowable values are text/regressionvalue/classifyrange
#evaluation.type=regressionvalue
#evaluation.type=regressionvalue

#primary outcome
regression.outcome.type=V
regression.outcome.variable=3909808

#allowable ones {O,V,I,C} (usually V/I)
prediction.output.type=V
predictor.collapsetowtavg=true

prediction.effects.priors=$USE_EFFECT
prediction.effects.attrib=3870696

EOF1

echo "Extracting information from the index"
mvn exec:java@predict -Dexec.args="$PROPFILE"

