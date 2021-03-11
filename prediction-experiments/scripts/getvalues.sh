#!/bin/bash
  
if [ $# -lt 2 ]
then
        echo "Usage: $0 <C/O> <attrib-id>"
        exit
fi

if [ $1 = "C" ]
then
  field=1
else
  field=2
fi

grep $2 graphs/relations.graph | awk -F '\t' -v field=$field '{print $field}' | awk -F ':' '{print $3}' | sort |uniq
