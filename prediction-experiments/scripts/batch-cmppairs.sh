MODE=both
TYPE=m
RESFILE=res.$MODE.$TYPE.txt
cp /dev/null $RESFILE

for DIM in 50 100 200 300 
#for DIM in 50
do
    for wsize in 5 10 20
    #for wsize in 5
    do
        for p in 0.1 0.3 0.5 0.7 0.9
        #for p in 0.1
        do
	        q=`echo $p|awk '{print 1-$1}'`
            acc=`./comprcts.sh $DIM $wsize $p $q $MODE 0 false | grep "Test Accuracy" |awk '{print $NF}'`
            echo "$DIM $wsize $p $q $MODE $acc" >> $RESFILE
        done
    done
done 

