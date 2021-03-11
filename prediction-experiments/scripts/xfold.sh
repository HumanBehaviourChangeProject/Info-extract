PROPFILE=$PWD/xfold.properties

if [ $# -lt 8 ]
then
        echo "Usage: $0 <dimension> <n2v.window> <n2v.p> <n2v.q> <mode (nodes/text/both)> <r/c/m> <num_quanta (e.g. 20; set to 0 for no quantization) <larger context (true/false)>>"
        exit
fi

DIM=$1
WINDOW=$2
P1=$3
Q1=$4
MODE=$5
TYPE=$6
QUANTA=$7
LARGER_CONTEXT=$8

SUFFIX=$DIM-$WINDOW-$P1-$Q1-$MODE-$QUANTA-$LARGER_CONTEXT
EMBFILE=ndvecs.cv.$SUFFIX
DATA_DIR=../../../../core/prediction/
JAVADIR=../../core

# 3 possible combinations tested - [with-graphs=1, with-text=1], [with-graphs=0, with-text=1], and [with-graphs=1, with-text=0]
# the later two - ablation studies

USE_TEXT=false
USE_NODES=false

if [ $MODE = "nodes" ]
then
    USE_NODES=true
elif [ $MODE = "text" ]
then
    USE_TEXT=true
elif [ $MODE = "both" ]
then
    USE_TEXT=true
    USE_NODES=true
else
    echo "Unrecognized mode '$MODE'. Exiting program"
    exit 
fi

cat > $PROPFILE  << EOF1
prediction.graph.numeric.edges=false

node2vec.layer1_size=$DIM
node2vec.window=$WINDOW
node2vec.p1=$P1
node2vec.q1=$Q1
node2vec.ns=10
node2vec.niters=5
node2vec.pqsampling=true
nodevecs.vecfile=prediction/graphs/nodevecs/$EMBFILE.vec
seqmodel.modified_dict=prediction/graphs/nodevecs/${EMBFILE}.per_instance.vec
seqmodel.use_text=$USE_TEXT
seqmodel.use_graph=$USE_NODES
prediction.train.ratio=1

prediction.graph.coarsening.normalization.numintervals=$QUANTA
prediction.use_larger_context=$LARGER_CONTEXT

EOF1

if [ ! -e sentences ]
then
    mkdir sentences
fi

if [ $USE_NODES = "true" ]
then
    if [ $USE_TEXT = "false" ]
    then
        NODEFILE=prediction/graphs/nodevecs/$EMBFILE.vec
    else
        NODEFILE=prediction/graphs/nodevecs/$EMBFILE.per_instance.vec
    fi
else
    NODEFILE=prediction/graphs/nodevecs/$EMBFILE.per_instance.vec
fi

cd $JAVADIR 
echo $NODEFILE

if [ ! -e $NODEFILE ]
then
echo "Executing node2vec with overriding parameters specified in $PROPFILE..."
mvn exec:java@gensparsevecs -Dexec.args="$PROPFILE"
fi

cd -

echo "Running regression..."
cd ../python-nb/ov-predict/src

echo "python eval_cvfold.py -d $DATA_DIR/sentences/train.tsv -n ../../../../core/$NODEFILE -m $TYPE"
python eval_cvfold.py -d $DATA_DIR/sentences/train.tsv -n ../../../../core/$NODEFILE -m $TYPE

cd -
