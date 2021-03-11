#!/bin/bash

if [ $# -lt 1 ]
then
        echo "Usage: $0 <csv file>"
        exit
fi

cat $1| sort -k 2 |awk -F '\t' 'BEGIN{prev=""} {if($2!=prev) {print b; b=$2 "\t";} else {b=b "\t(" $1 ":" $3 ")\t";} prev=$2} END{print b}'
