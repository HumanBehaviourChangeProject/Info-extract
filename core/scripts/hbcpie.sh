#Index all files from the data folder.
#The program would create three o/p index directories - i) index, ii) para.index and iii) para.index.all

echo "Indexing documents from the collection"
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer"

#Writes out the file to train word vectors
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.inforetrieval.indexer.WordVecTrainingFileGenerator"

#Invokes the word2vec executable to learn the word vectors
cd utils
./word2vec -train ../data/com.ibm.drl.hbcp.core.wvec/wvectrain.txt -output data/com.ibm.drl.hbcp.core.wvec/pubmed -size 200 -window 5 -sample 1e-4 -negative 5 -cbow 1
cd -

echo "Extracting information from the index"
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.extractor.InformationExtractor"

