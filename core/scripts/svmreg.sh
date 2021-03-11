#mvn exec:java@regeval -Dexec.args="res.tsv"

if [ $# -lt 2 ]
then
        echo "Usage: $0 <C> <gamma>"
        exit
fi

C=$1
GAMMA=$2

BASEDIR=$PWD
SCRIPTDIR=$BASEDIR/scripts

PROPFILE=$SCRIPTDIR/svmreg.properties

cat > $PROPFILE  << EOF1
svm.c=$C
svm.gamma=$GAMMA

EOF1

mvn exec:java@svmreg -Dexec.args="$PROPFILE"

