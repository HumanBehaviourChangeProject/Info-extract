
## Behaviour Change Outcome Prediction

Setup the code by executing
```
git clone git@github.ibm.com:HBCP/hbcpIE.git
cd hbcpIE
mvn complile
```

To replicate the results for SVM regression, go to the project folder (hbcpIE) and execute the script
```
sh scripts/svmreg.sh 0.1 0.1
```
where the supplied arguments are the values of `C` (reguralization) and `gamma` (kernel width)  parameters respectively. Higher values of `C` tend to avoid over-fitting and higher values of `gamma` tend to make the SVM kernel function more close to a linear function.

The script in turn calls a Java main class `com.ibm.drl.hbcp.predictor.WekaRegressionFlow` with the supplied values of `C` and `gamma`
```
mvn exec:java@svmreg -Dexec.args=...
``` 
You should observe the following output.
```
Number of results: 171
RMSE: 11.200228033627882
MAE: 8.135101080250243
Loose Classification Accuracy: 0.09941520467836257
```

For the nearest neighbor based prediction flow, you need to set up a `conda` virtual environment with `keras`. To do so, first install the conda package (if you do not already have it) by executing
```
wget https://repo.anaconda.com/archive/Anaconda3-2019.10-Linux-x86_64.sh
```
on Linux and
```
wget https://repo.anaconda.com/archive/Anaconda3-2019.10-MacOSX-x86_64.sh
```
on Mac OS.

Next, execute the following two commands
```
conda create -n hbcp python=3.6
source activate hbcp
```
which creates a new virtual environment, named `hbcp` and activates this environment. Next, within **this virtual environment**, type
```
pip install -r prediction/requirements.txt
pip install -I tensorflow
pip install -I keras
```

To replicate the results for the Nearest neighbor based non-parametric graph based prediction and the parametric based regression, execute
```
cd prediction
./keras_reg.sh <wsize> <p> <q>
```
An example invocation is
```
./keras_reg.sh 5 0.9 0.1
```
You should see the following outputs (one set of results for NN and the other for regression on node vectors).
```
RMSE: 12.45612062774125
MAE: 9.127005741545371
Loose Classification Accuracy: 0.09941520467836257
```
and
```
RMSE: 12.163693116231103
MAE: 9.382063157894734
Loose Classification Accuracy: 0.10526315789473684
```

