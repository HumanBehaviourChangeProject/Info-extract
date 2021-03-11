JSON_DIR=../src/main/resources/data/jsons/
JSONFILE=$JSON_DIR/All_annotations_512papers_05March20.json

#Download the Pubmed pretrained vecor file from the Box file https://ibm.box.com/s/rg8jnu2uui6xssgnikj3q4mpv9537pn9
#and unzip it
#This script uses a local path, change it!
PUBMED_PT_WVEC=$HOME/common_data/PubMed-w2v.vec  

grep "AdditionalText" $JSONFILE | sed 's/\"AdditionalText\"://g'|sed 's/\\r/ /g'|sed 's/[\",\.\:;]//g'|sed 's/-/ /g'|sed 's/(/ /g' | sed 's/)/ /g'|sed 's/=/ /g'|sed 's/\// /g'| sed 's/\+/ /g' > words.txt

cat words.txt | awk '{for (i=1;i<=NF;i++) print $i}'|sort |uniq > vocab.hbcp.txt

cat $PUBMED_PT_WVEC | awk '{print $1}'|sort > vocab.pubmed.txt

comm -12 vocab.hbcp.txt vocab.pubmed.txt > commonwords.txt


DATADIR=../src/main/resources/data/pubmed
awk 'FNR==NR{v[$1]=$1; next} {if (length(v[$1])>0) print $0}' commonwords.txt $PUBMED_PT_WVEC > $DATADIR/hbcpcontext.vecs
