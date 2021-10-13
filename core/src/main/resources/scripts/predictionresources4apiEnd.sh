#!/bin/sh
# Script that executes the final merging step of predictionresources4api.sh

GNODE_FILE=$1
INODE_FILE=$2
MERGED_NODE_FILE=$3
#GNODE_FILE=resources/$EMBFILE.vec
#INODE_FILE=resources/$EMBFILE.per_instance.vec
#MERGED_NODE_FILE=resources/$EMBFILE.merged.vec
echo "Currently in directory: $(pwd)"
echo "Merging files $GNODE_FILE and $INODE_FILE into $MERGED_NODE_FILE ..."

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