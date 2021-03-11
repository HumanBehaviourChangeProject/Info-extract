#Writes out the file to train word vectors
mvn exec:java -Dexec.mainClass="com.ibm.drl.hbcp.inforetrieval.indexer.WordVecTrainingFileGenerator"

#Invokes the word2vec executable to learn the word vectors
cd utils
./word2vec -train ../data/com.ibm.drl.hbcp.core.wvec/wvectrain.txt -output ../data/com.ibm.drl.hbcp.core.wvec/pubmed -size 200 -window 5 -sample 1e-4 -negative 5 -cbow 1
cd -
