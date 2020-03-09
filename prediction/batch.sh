cp /dev/null nohup.out
cp /dev/null ../batchres.txt

for wsize in 5 10 20
do
	for p in 0.1 0.3 0.5 0.7 0.9  
	do
		#for q in 5 1 0.5 0.1
		#do
			q=`echo $p|awk '{print 1-$1}'`
			sh keras_reg.sh $wsize $p $q >> nohup.out 2>&1
			cd ..
			mvn exec:java@regeval -Dexec.args="res.tsv" > log
			acc=`grep 'Accuracy:' log | awk '{print $NF}'`
			rmse=`grep 'RMSE:' log | awk '{print $NF}'`
			mvn exec:java@regeval -Dexec.args="prediction/python-nb/predictions.txt" > log
			acc_p=`grep 'Accuracy:' log | awk '{print $NF}'`
			rmse_p=`grep 'RMSE:' log | awk '{print $NF}'`
			echo $wsize $p $q $rmse $acc $rmse_p $acc_p >> batchres.txt
			cd -
		#done
	done
done
