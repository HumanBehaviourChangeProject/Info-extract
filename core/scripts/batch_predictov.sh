cp /dev/null log.txt
cp /dev/null tmp

for ARM in false true
do
	for SRC in gt ie
	do
		for EFFECT in true
		do
			for WSIZE in 5 10 20
			do
				for P in 5
				do
					Q=`expr 10 - $P`
					echo "$ARM $SRC $EFFECT $WSIZE 0.$P 0.$Q" >> tmp   
					sh scripts/predictov.sh $ARM $SRC $EFFECT $WSIZE 0.$P 0.$Q >> log.txt 
					cat log.txt | grep "Mean-average RMSE" | awk -F ':' '{print $2}'
				done
			done
		done
	done
done

cat log.txt | grep "Mean-average RMSE" | awk -F ':' '{print $2}' > allres.txt
paste tmp allres.txt 
