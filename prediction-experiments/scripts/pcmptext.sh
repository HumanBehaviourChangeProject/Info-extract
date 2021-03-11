if [ $# -lt 1 ]
then
        echo "Usage: $0 <embfile>"
        exit
fi

FILE=$1
DATA_DIR=../../../../core/prediction/

cat $FILE | awk '{if (NR==1) print $1 " " $2-5; else {for (i=1;i<=NF-5;i++) printf("%s ",$i); printf("\n");}}' > pp.tmp


echo "Running regression..."
cd python-nb/ov-predict/src
python pairwise-ov.py -d $DATA_DIR/sentences/train.tsv -n ../../../pp.tmp
cd -

rm pp.tmp
