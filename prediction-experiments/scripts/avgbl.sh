avg=`cat prediction/weka/training.arff | awk -F ',' '{if ($1=="@data")s=1; else if (s==1) {y+=$NF;c++}} END{print y/c}'`
cat prediction/weka/test.arff | awk -F ',' -v avg=$avg '{if ($1=="@data")s=1; else if (s==1) print $NF "\t" avg}' > prediction/avgpred.res
mvn exec:java@regeval -Dexec.args="prediction/avgpred.res" > avres
rmse=`grep RMSE avres | awk '{print $NF}'`
iacc=`grep Accuracy avres | awk '{print $NF}'`
echo "$rmse $iacc" > avres
cat avres
sh prediction/fscore.sh avres
rm avres
