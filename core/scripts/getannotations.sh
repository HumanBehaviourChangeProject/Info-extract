#!/bin/bash

if [ $# -lt 2 ]
then
        echo "Usage: $0 <json file> <attribute-id>"
        exit
fi

grep -A10 $2 $1 |egrep -w "Text|DocTitle"

grep -A10 $2 $1 |egrep -w "Text" | awk '{for(i=1;i<=NF;i++) print $i}' | sort|uniq -c|sort -nr -k1|head -n20
