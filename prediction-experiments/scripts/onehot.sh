if [ $# -lt 1 ]
then
        echo "Usage: $0 <emb file>"
        exit
fi

FILE=$1
DATA_DIR=../../../../core/prediction/

cat $FILE |awk '{if (NR>1) {split($1,a,":"); print a[1]":"a[2]}}'|sort|uniq| awk '{print $1 " " NR}' > words_ids.tmp

awk 'FNR==NR{v[$1]=$2; next} function onehot(x,n) {for(i=1;i<=n;i++) if(i==x)a[i]=1; else a[i]=0; for (i in a) printf("%s ",a[i]);} {if (FNR==1) printf("%s %s\n",length(v), $2+length(v)); else { split($1,a,":"); node=a[1]":"a[2]; printf("%s ",$1); onehot(v[node],length(v)); for(i=2;i<=NF;i++) printf("%s ",$i); printf("\n");} }' words_ids.tmp $FILE > pp.tmp


echo "Running regression..."
cd ../python-nb/ov-predict/src
python eval_cvfold.py -d $DATA_DIR/sentences/train.tsv -n ../../../scripts/pp.tmp -m c
cd -

rm *.tmp
