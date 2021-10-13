#!/bin/sh

PROPFILE=$PWD/keras_reg.properties

if [ $# -lt 4 ]
then
        echo "Usage: $0 <dimension> <n2v.window> <n2v.p> <n2v.q>"
        exit
fi

DIM=$1
WINDOW=$2
P1=$3
Q1=$4
MODE=both
TYPE=r
QUANTA=0
LARGER_CONTEXT=false

SUFFIX=$DIM-$WINDOW-$P1-$Q1-$MODE-$QUANTA-$LARGER_CONTEXT
EMBFILE=embfile4api

POMDIR=..
PYTHON_DIR=prediction-experiments/python-nb/ov-predict

# In this script, we write out the necessary resource files into a folder 'resources' created
#within the folder 'prediction-experiments/python-nb/ov-predict'

# 3 possible combinations tested - [with-graphs=1, with-text=1], [with-graphs=0, with-text=1], and [with-graphs=1, with-text=0]
# the later two - ablation studies

USE_TEXT=true
USE_NODES=true

GNODE_FILE=resources/$EMBFILE.vec
INODE_FILE=resources/$EMBFILE.per_instance.vec
MERGED_NODE_FILE=resources/$EMBFILE.merged.vec
SEQ_DIR=resources
TRAIN_SEQ_FILE=resources/train4api.tsv
TEST_SEQ_FILE=resources/test4api.tsv

cat > $PROPFILE  << EOF1
prediction.graph.numeric.edges=false

node2vec.layer1_size=$DIM
node2vec.window=$WINDOW
node2vec.p1=$P1
node2vec.q1=$Q1
node2vec.ns=10
node2vec.niters=5
node2vec.pqsampling=true
nodevecs.vecfile=$GNODE_FILE
seqmodel.modified_dict=$INODE_FILE
seqmodel.use_text=$USE_TEXT
seqmodel.use_graph=$USE_NODES
prediction.train.ratio=1
out.seqfiles.dir=$SEQ_DIR
out.train.seqfile=$TRAIN_SEQ_FILE
out.test.seqfile=$TEST_SEQ_FILE

prediction.graph.coarsening.normalization.numintervals=$QUANTA
prediction.use_larger_context=$LARGER_CONTEXT
#context.vecs=prediction/biobert/context.biobert.vec

EOF1

cd $POMDIR

echo "Checking if $GNODE_FILE exists"
if [ ! -e $GNODE_FILE ]
then
echo "'$GNODE_FILE' doesn't exist... regenerating"
echo "Executing node2vec with overriding parameters specified in $PROPFILE..."
mvn exec:java@gensparsevecs -Dexec.args="$PROPFILE"
fi

cat $GNODE_FILE $INODE_FILE > $MERGED_NODE_FILE 

#For training prediction flow for the API call, create a merged vocab file with the global and the per-instance vecs, used during API call (test) and training respectively.

echo "Padding with zero and value vectors"
#The global vectors need to be 'zero padded' for the PubMed word contexts and value-padded for numerical ones.
tail -n+2 $GNODE_FILE | awk 'function is_num(x) {return is_float(x) || is_integer(x);} function is_float(x) { return x+0 == x && int(x) != x } function is_integer(x)  { return x+0 == x && int(x) == x } {num_parts=split($1,nameparts,":"); num=nameparts[num_parts]; s=""; v=""; for(i=1;i<=200;i++) s=s"0.00 "; if (!is_num(num)) num=0; for(j=1;j<=5;j++) v=v""num" "; print $0" "s" "v} ' > tmp_g

tail -n+2 $INODE_FILE > tmp_i

cat tmp_g tmp_i > tmp_m
VOCABSIZE=`wc -l tmp_m| awk '{print $1}'`
DIM=`head -n1 $INODE_FILE | awk '{print $2}'`

echo "$VOCABSIZE $DIM" > $MERGED_NODE_FILE
cat tmp_m >> $MERGED_NODE_FILE

rm tmp*

cd -

rm $PROPFILE