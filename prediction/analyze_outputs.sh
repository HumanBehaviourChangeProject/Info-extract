ofile=o_values.txt
cp /dev/null $ofile

for id in `grep "O:" graphs/relations.graph | awk -F '\t' '{print $2}' | awk -F ':' '{print $2}' | sort |uniq`
do
	echo $id >> $ofile 
	grep $id graphs/relations.graph | awk -F '\t' '{print $2}' | awk -F ':' '{print $3}' | sort |uniq >> $ofile
done

