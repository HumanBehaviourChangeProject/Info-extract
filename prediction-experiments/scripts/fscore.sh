cat $1 | awk '{IA=(1-$2); R=$1; print 2*IA*R/(IA+R)}'
