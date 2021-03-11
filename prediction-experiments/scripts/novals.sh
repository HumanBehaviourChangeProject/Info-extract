if [ $# -lt 1 ]
then
        echo "Usage: $0 <file>"
        exit
fi

FILE=$1
DATA_DIR=../../../../core/prediction/

cat $FILE |awk '{if (NR>1) { for(i=1;i<=NF-5;i++) printf("%s ",$i); printf("\n"); } else printf("%s %s\n",$1,$2-5)}' > pp.tmp

echo "Running regression..."
cd python-nb/ov-predict/src
python eval_cvfold.py -d $DATA_DIR/sentences/train.tsv -n ../../../pp.tmp -m c
cd -

rm *.tmp
