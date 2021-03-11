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
DATA_DIR=../../../../../core/prediction/
JAVADIR=../../core

# 3 possible combinations tested - [with-graphs=1, with-text=1], [with-graphs=0, with-text=1], and [with-graphs=1, with-text=0]
# the later two - ablation studies

USE_TEXT=true
USE_NODES=true

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
out.train.seqfile=train4api.tsv
out.test.seqfile=test4api.tsv

prediction.graph.coarsening.normalization.numintervals=$QUANTA
prediction.use_larger_context=$LARGER_CONTEXT
#context.vecs=prediction/biobert/context.biobert.vec

EOF1

if [ ! -e sentences ]
then
    mkdir sentences
fi

cd $JAVADIR 

GNODE_FILE=prediction/graphs/nodevecs/$EMBFILE.vec
INODE_FILE=prediction/graphs/nodevecs/$EMBFILE.per_instance.vec

echo "Checking if $GNODE_FILE exists"
if [ ! -e $GNODE_FILE ]
then
echo "'$GNODE_FILE' doesn't exist... regenerating"
echo "Executing node2vec with overriding parameters specified in $PROPFILE..."
mvn exec:java@gensparsevecs -Dexec.args="$PROPFILE"
fi

MERGED_NODE_FILE=prediction/graphs/nodevecs/$EMBFILE.merged.vec
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

echo "Running regression..."
cd ../python-nb/ov-predict/src
echo "current directory $PWD"

python eval_splitdata.py -i ../../../../core/prediction/sentences/train4api.tsv -e ../../../../core/prediction/sentences/test4api.tsv -o ../outputs/predictions.split.$SUFFIX.txt -n ../../../../core/$MERGED_NODE_FILE -m $TYPE

cd -
