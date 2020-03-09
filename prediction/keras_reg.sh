PROPFILE=keras_reg.properties

if [ $# -lt 3 ]
then
        echo "Usage: $0 <n2v.window> <n2v.p> <n2v.q>"
        exit
fi

WINDOW=$1
P1=$2
Q1=$3

cat > $PROPFILE  << EOF1
prediction.graph.numeric.edges=false

node2vec.window=$WINDOW
node2vec.p1=$P1
node2vec.q1=$Q1
node2vec.ns=10
node2vec.niters=10
node2vec.pqsampling=true

EOF1

cd ..

echo "Executing node2vec with overriding parameters specified in $PROPFILE..."
mvn exec:java@nnpredict -Dexec.args="prediction/$PROPFILE"
#mvn exec:java@gensparsevecs -Dexec.args="prediction/$PROPFILE"
cd -

echo "Running regression..."
cd python-nb
#python NodeSequenceRegression.py
python NodeSequenceRegression.py -i ../sentences/train.tsv -e ../sentences/test.tsv -o predictions.txt -n ../graphs/nodevecs/refVecs.vec
python NodeSequenceRegression.py -i ../sentences/train.tsv -e ../sentences/test.tsv -o predictions_wvals.txt -a -n ../graphs/nodevecs/refVecs.vec
cd -

cd ..

echo "Results with NN"
mvn exec:java@regeval -Dexec.args="res.tsv"

echo "Results with regression without values added"
mvn exec:java@regeval -Dexec.args="prediction/python-nb/predictions.txt"

echo "Results with regression with values added"
mvn exec:java@regeval -Dexec.args="prediction/python-nb/predictions_wvals.txt"

cd -

