
## Behaviour Change Outcome Prediction

  

There are two modes of testing the prediction flow - namely, i) the cross-fold validation setup, and ii) the train:test split setup.
  

Also, you could test both evaluation setups for both regression and multi-class classification (classifying outcome values into 7 different classes).

  

For obtaining everything we need for the experiments, setup the codebase (to the specific commit point `3a4117e`) by executing

```
git clone git@github.ibm.com:HBCP/hbcpIE.git
cd hbcpIE
```

***OPTIONAL*** *To get the latest prediction code, check out the `dev` branch or to reproduce the results below,

check out the code at the specific commit point.*

```
git checkout dev # latest prediction
## OR
git checkout 3a4117e  # to reproduce numbers below
```

  

  

```
mvn compile
```

  

### Train-Test Split Experiments

  

We use a train:test split of 4:1 for this set of experiments.

  

#### SVM baseline

To replicate the results for the SVM regression, go to the project folder (hbcpIE) and execute the script

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
Number of results: 166
RMSE: 12.105757177516614
MAE: 8.869709574370125
Loose Classification Accuracy: 0.05421686746987952
```

  

#### Node Embedding on a Graph of Feature Instances

  

For the nearest neighbor based prediction flow, you need to execute the script  `scripts/predict-n2v.sh` with the following usage pattern.

```
Usage: scripts/predict-n2v.sh <dimension> <n2v.window> <n2v.p> <n2v.q>
```

A sample invokation is

```
sh scripts/predict-n2v.sh 100 10 0.7 0.3
```

for which you should get the following output.

```
Number of results: 166
RMSE: 16.978569491822018
MAE: 13.886148387701288
Loose Classification Accuracy: 0.08433734939759036
```

  

You can play around with some of the default parameters by changing their values in the script code itself, e.g., if you want to increase the number of iterations of `node2vec`, simply change its value in the script.

```
node2vec.niters=10
```

  

#### Deep Sequence Modeling

  

In this set of experiments, you need to set up a `conda` virtual environment with `keras`. To do so, first install the conda package (if you do not already have it) by executing

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
pip install -r prediction/python-nb/requirements.txt
```

  

To replicate the results for the Nearest neighbor based non-parametric graph based prediction and the parametric based regression, execute

```
cd prediction
./eval_split.sh <dimension> <n2v.window> <n2v.p> <n2v.q> <with_words (true/false)> <r/c>
```

The parameters indicate the dimension of the vectors, the window size (for node2vec training), values of `p` and `q` for the biased walk, a value indicating whether you would want to include text (from the context of an entity) for the purpose of prediction, and the mode (`r` for *regression* and `c` for *classification* of outcome values).

An example invocation is

```
./eval_split.sh 200 10 0.9 0.5 true r
```

You should see the following output.

```
RMSE: 12.413304682299943
MAE: 9.574678048780486
Loose Classification Accuracy: 0.09146341463414634
```

  

### Cross-Validation Experiments

  

#### SVM Baselines

  

In this set of experiments, you need to have the Weka software installed. Get the executable jar for your environment from [here.](https://waikato.github.io/weka-wiki/downloading_weka/)

  

After downloading the jar, type

```
java -jar weka.jar
```

  

This should open a GUI, which you could then use to load (clicking on the `Explorer` and then `Open File` buttons from the Weka GUI) the arff file `prediction/weka/train_ovlabels.7.arff`.

  

You then need to hit on the `Classify` tab and then for selecting SVM, click on the `Choose` button followed by clicking on `Functions` and then `SMO`.

You then need to click on the radio-button `Cross-validation Folds` after changing the value in the text-box from `10` to `5`. If you hit on the `Start` button, you should see the following output.

```
Correctly Classified Instances 217 27.0237 %
```

  

For our experiments to be reported in the paper, change the kernel to *Gaussian* by first clicking on the text-box beside `Choose` and then select `RBFKernel` by pressing on the button beside `Choose` (inside the modal dialogue box). After changing  `c` and `gamma` both to `0.1`, you should see the following output.

```
Correctly Classified Instances 168 20.9215 %
```

  

You could play around with other classifiers, such as Random Forests etc. on the supplied arff file.

  

#### Deep Sequence Modeling

  

For generating the classification (or regression) based results, execute

```
cd prediction
./xfold.sh 100 10 0.7 0.3 both c
```

where the arguments (in-order) are i) dimension, ii) window-size, iii) p, iv) q, v) mode which is one of `{both, text, nodes}` (meaning whether to use a text-only, node-only or both sources of embedded vectors). After executing

On execution of the above command, you should see

```
Metric: 0.43354036927223205
```

Note that this number is not reproducible exactly. However, you should see close numbers. One idea is to run CV-fold experiments 5 times and report the maximum or the average.

With `text` only, I got the result.

```
Metric: 0.4347826051712036
```

Again, results may not be exactly the same.

## Using BERT (instead of PubMed skipgram vectors)

We use the same code flow, the only difference being instead of concatenating the skipgram vectors (from PubMed) with the node vectors, we use the BERT vectors using the `feature_extraction` pipeline of the Huggingface library.

We first need to construct a file comprised of the saved vectors corresponding to the contexts of the text around the annotated instances. To create this file of saved vectors, first you need to install the `transformers` package of Huggingface, which you do simply by typing `pip install transformers`.

Then go to the folder `core/prediction/biobert/` and execute
```
python savebertvecs.py <context file> <output vector file>
```
The `context file` is the output generated by the code `com.ibm.drl.hbcp.predictor`, which essentially filters out 5 words from the left and right of an annotated text span corresponding to every attribute.

The Java class writes out the context for every attribute (each in a new line) in the file `prediction/biobert/context.txt`. You should then pass this file as an input to the Python program `savebertvecs.py`.
For each line of this file, i.e. the context, the program saves its BERT vector.

The rest of the flow remains pretty much the same. Once you have created this file, all you need to do is to include the following property `context.vecs=prediction/biobert/context.biobert.vec` (i.e. to the path of the saved BERT vectors).

The Python script `xfold_bert.sh` automates this property inclusion.


## API Reference

We also provide a Flask-based REST API service for the prediction interface. The REST API uses a pre-trained model to make predictions for a given string encoded representation of an RCT.

To save the model, simply run
```
cd scripts
./train4api.sh 100 10 0.1 0.9
```
The above script uses the entire training set (different to a train:test split or a cross-validation based flow) to train and save a model.

After this script runs a model trained on the entire HBCP dataset is saved in the folder `prediction-experiments/python-nb/ov-predict/saved_models`. The name of the  saved model file (default) is `model.h5`.

After the model is saved, the next step is to setup the web service through the Flask API. This is done by executing the script `predict_app_dev.py`. Starting this initiates the server which waits to receive HTTP requests.

To send HTTP requests, you first need to encode an RCT arm in a space separated sequence of tokens. To do this in batch simply edit the file `prediction-experiments/python-nb/ov-predict/src/api/apitest.sh`.

The code in this script looks like
```
cat > requests.txt << EOF1
C:5579088:25-I:3675717:1-C:5594105:5-I:3673271:1-O:4087191:6.5
C:5579088:25-I:3675717:1-C:5594105:5-I:3673271:1-O:4087191:13.0
...
EOF1

while read line
do
curl -i http://127.0.0.1:5000/hbcp/api/v1.0/predict/outcome/$line
done < requests.txt
``` 

Thus in each line you need to encode an RCT arm, e.g. the first line (`C:5579088:25-I:3675717:1-C:5594105:5-I:3673271:1-O:4087191:6.5`) means that the value of the `C:5579088` attribute (`type:id`) is `25` and so on.

The script calls the API which returns a JSON string of the predicted value.

