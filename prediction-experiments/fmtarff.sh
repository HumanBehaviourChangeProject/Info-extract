#!/bin/bash

#Process for PCA
#1. Keep only numeric features
#2. Replace BCTs as numeric, i.e. {0,1} --> 0 or 1 (change type to numeric)

if [ $# -lt 1 ]
then
        echo "Usage: $0 <arff file>"
        exit
fi

cat $1 | awk '{if ($1=="@attribute") print $1 " ATTRIB-" NR " numeric" ; else print $0}' | awk -F ',' 'BEGIN{state=0} {if ($0=="@data") state=1; if (state==1) {print $0; state=2;} else if (state==2) {for (i=1;i<=NF;i++) { if ($i+0 == $i) printf($i); else printf("0"); if (i==NF) printf("\n"); else printf (",");}} else print $0 }' > $1.simple
cat $1.simple | awk '{if ($0=="@data") state=1; if (state==2) print $0; if (state==1) state=2}' > $1.csv
