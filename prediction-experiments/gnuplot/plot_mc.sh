if [ $# -lt 1 ]
then
    echo "Usage: $0 <results-data file>"
    exit
fi

DAT=$1

#for classification:
#text, LC=false: 0.6344
#1-hot: 0.6456385682026545
BL1=0.6745
BL2=0.6946
MARGIN=0.02
MIN=`cat $DAT|sort -n -k6|head -n1|awk -v min=$BL1 -v margin=$MARGIN '{v=$6; newmin=v<min? v: min; print newmin-margin}'`
MAX=`cat $DAT|sort -n -k6|tail -n1|awk '{print $6+0.01}'`

for dim in 50 100 200 300
do
cat $DAT | grep "^$dim" |awk -v bl1=$BL1 -v bl2=$BL2 '{ y[$3":"$2]=$6; } END {ws="5 10 20"; split(ws,a," "); for (p=1;p<=9;p+=2) { printf("0.%s ", p); for (i in a) printf ("%.4f ", y["0""."p":"a[i]]); printf("%s %s\n", bl1, bl2);}}' > data.pp.$dim

#cat $DAT | grep "^$dim" |awk '{ y[$3":"$2]=$6; } END {ws="5 10 20"; split(ws,a," "); for (p=1;p<=9;p+=2) { printf("0.%s ", p); for (i in a) printf ("%.4f ", y["0""."p":"a[i]]); printf("\n"); }}' > data.pp.$dim

cat > makeplot.$dim.gnu << EOF1

# Set the output file type
set terminal postscript eps enhanced font "Helvetica,20"
# Set the output file name
set output '$dim.eps'
set yrange [$MIN:$MAX]
set format y "%.3f"
#set ytics $MIN, 0.05, $MAX
set xtics 0.1, 0.2, 0.9
set xtics out
set ytics out
set key bottom right

set xlabel 'p'
set ylabel 'Accuracy'

# Now plot the data with lines and points
plot 'data.pp.$dim' using 1:5 w lp pt 3 lw 2 title 'Text-Only', \
     '' using 1:6 w lp pt 4 lw 2 title 'Text+N2V-1Hot', \
     '' using 1:2 w lp pt 7 lw 2 title 'Text+N2V (ws=5)', \
     '' using 1:3 w lp pt 1 lw 2 title 'Text+N2V (ws=10)', \
     '' using 1:4 w lp pt 5 lw 2 title 'Text+N2V (ws=20)' \

EOF1

gnuplot makeplot.$dim.gnu
epspdf $dim.eps

rm data.pp.$dim
rm $dim.eps
rm makeplot.$dim.gnu

done
