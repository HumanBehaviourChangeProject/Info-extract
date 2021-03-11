#!/bin/bash

if [ $# -lt 4 ]
then
        echo "Usage: $0 <dimension> <n2v.window> <n2v.p> <n2v.q>"
        exit
fi

DIM=$1
WINDOW=$2
P1=$3
Q1=$4

SUFFIX=$DIM-$WINDOW-$P1-$Q1
PROPFILE=predict.nn.${SUFFIX}.properties

cat > $PROPFILE  << EOF1
prediction.graph.numeric.edges=false

node2vec.layer1_size=$DIM
node2vec.window=$WINDOW
node2vec.p1=$P1
node2vec.q1=$Q1
node2vec.ns=10
node2vec.niters=10
node2vec.pqsampling=true
node2vec.outfile=prediction/graphs/nodevecs/ndvecs.$SUFFIX.vec

EOF1

echo "Executing node2vec with overriding parameters specified in $PROPFILE..."
mvn exec:java@nnpredict -Dexec.args="$PROPFILE"

echo "Results with NN"
mvn exec:java@regeval -Dexec.args="res.tsv"

