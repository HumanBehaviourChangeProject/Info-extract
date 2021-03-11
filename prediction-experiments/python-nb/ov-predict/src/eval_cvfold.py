#Load datasets and embedded vectors
from sklearn.model_selection import StratifiedKFold
import numpy as np
from sklearn.preprocessing import OneHotEncoder
from model.lstm import buildModel
from model.lstm import rmse
from model.lstm import create_model
from preprocessing.InputHelper import InputHelper
import sys, getopt
import os
from keras.preprocessing.sequence import pad_sequences
from preprocessing.InputHelper import mapToNonUniformlySpacedIntervals
from preprocessing.InputHelper import transformLabels
from sklearn.metrics import mean_squared_error
from math import sqrt
import statistics
from sklearn.metrics import confusion_matrix
from keras import backend as k 
from common.utils import plotHistogram
from common.utils import getSelectedData
from common.utils import printWordVecs
from common.utils import convertSoftmaxToLabels
from common.utils import computePerIntervalStats
from common.utils import computeTwoStagedRMSE

EPOCHS=30

#epsilon = 0.1 means 10% relative error
def intervalMatch(y_hat, y, epsilon=0.2):
    if abs(y-y_hat)/y < epsilon:
        return 1
    else:
        return 0

def convertSoftmaxToConfidences(y_preds):
    confs=[]
    for i in range(y_preds.shape[0]):
        confs.append(np.amax(y_preds[i]))
    return confs

def trainModelOnFold(fold_number, model, x_train, y_train, x_test, y_test, maxlen, num_classes, type, epochs, y_train_vals, y_test_vals):
    
    x_train = pad_sequences(x_train, padding='post', maxlen=maxlen)
    x_test = pad_sequences(x_test, padding='post', maxlen=maxlen)
    
    BATCH_SIZE = int(len(x_train)/20) # 5% of the training set size
    
    print ("Training model...")
    model.fit(x_train, y_train,
        epochs=epochs,
        verbose=True,
        validation_split=0.1,
        batch_size=BATCH_SIZE)

    loss, accuracy = model.evaluate(x_test, y_test, verbose=True)
    if (num_classes > 0):
        print("Fold {}: Cross-entropy loss: {:.4f}, Accuracy: {:.4f}".format(fold_number, loss, accuracy))
    else:
        print("Fold {}: Loss: {:.4f}, RMSE: {:.4f}".format(fold_number, loss, accuracy))    
        
    y_preds = model.predict(x_test)
    if not type=='r':
        y_confs = convertSoftmaxToConfidences(y_preds)
        y_preds = convertSoftmaxToLabels(y_preds)

    #here we compute the acceptability metric (internal evaluation for minimum acceptability)
    #as per Robert's email, a prediction is acceptable if it falls within 20% of the ref value
    interval_acc = 0
    #print ('y_preds:')
    #print (y_preds)
    #print (y_test_vals)
    #print ("|y_preds| = {}, |y_ref| = {}".format(len(y_preds), len(y_test_vals)))

    conf_sum = 0
    correct_conf_sum = 0
    #avg_conf = 1/float(num_classes)  # completely uniform

    if not type=='c':
        for i in range(len(y_preds)):
            y_pred = y_preds[i]
            y_ref = y_test_vals[i]
            interval_acc += intervalMatch(float(y_pred), float(y_ref))

        interval_acc /= len(y_preds)
    else:
        #print a confidence weighted accuracy (also report this as the same as part of the same variable)
        #print('y_confs = {}'.format(y_confs))
        y_gt_vals = convertSoftmaxToLabels(y_test)

        for i in range(len(y_preds)):
            y_pred = y_preds[i]
            y_conf = float(y_confs[i])
            y_ref = y_gt_vals[i]

            #print ('y_pred={}, y_ref={}, conf={}'.format(y_pred, y_ref, y_conf))

            if y_pred == y_ref:
                correct_conf_sum += y_conf
            conf_sum += y_conf
    
    if num_classes > 0:
        plotHistogram(y_preds, "Distribution of predicted class labels in {}-th fold".format(fold_number))

        #print the confusion matrix on this fold
        y_gt_vals = convertSoftmaxToLabels(y_test)
        print ('Confusion matrix for fold {}'.format(fold_number))
        c_matix = confusion_matrix(y_gt_vals, y_preds)
        print(c_matix)

    # in this part of the code use the true values (in case of classification) to predict values and compute rmse...
    if type=='m':
        # perform and evaluate 2-step regression... classify and then sample a value around the median from the interval
        accuracy = computeTwoStagedRMSE(num_classes, fold_number, y_preds, y_train_vals, y_test_vals)
    
    if type == 'c':
        print ('correct_conf_sum/conf_sum = {}/{}'.format(correct_conf_sum, conf_sum))

        interval_acc = correct_conf_sum/float(conf_sum)
    return accuracy, interval_acc

def trainModel(inpH, x, y, fold_info, maxlen, num_classes, type, epochs=EPOCHS):
    i=0
    avg_metric_value = 0
    avg_acc_val = 0

    model = create_model(inpH, num_classes, maxlen)

    for train_indexes, test_indexes in fold_info.split(x, y):
    
        #Load the training and the test sets
        x_train, y_train_vals = getSelectedData(x, y, train_indexes)
        x_test, y_test_vals = getSelectedData(x, y, test_indexes)

        #transform to 1-hot if y's are to be treated as class labels
        y_train, y_test = transformLabels(y_train_vals, y_test_vals, num_classes, useMedians=True)

        metric_val, acceptability_val = trainModelOnFold(i, model,
                                       x_train, y_train, x_test, y_test,
                                       maxlen, num_classes, type, epochs, y_train_vals, y_test_vals)

        avg_metric_value += metric_val
        avg_acc_val += acceptability_val

        i=i+1

    print ('Interval Acc: {}'.format(avg_acc_val))

    return avg_metric_value/float(i), avg_acc_val/float(i)

def main(argv):
    MAXLEN=50
    DATA_FILE = None
    EMB_FILE = None
    TYPE = None
    FOLD=5
    NUM_CLASSES=0
    SEED=314159
    NUM_EXPERIMENTS = 1
    
    try:
        opts, args = getopt.getopt(argv,"h:d:n:m:", ["datafile=", "nodevecs=", "model="])
    
        for opt, arg in opts:
            if opt == '-h':
                print ('eval_cvfold.py -d/--datafile= <datafile> -n/--nodevecs= <nodevecs> -m <r (regression)/ c(classification)/ m(classification-with-sample-around-mean)>')
                sys.exit()
            elif opt in ("-d", "--datafile"):
                DATA_FILE = arg
            elif opt in ("-n", "--nodevecs"):
                EMB_FILE = arg
            elif opt in ("-m", "--model"):
                TYPE = arg
                
    except getopt.GetoptError:
        print ('usage: eval_cvfold.py -d <datafile> -n <nodevecs> -m <r/c/m>')
        sys.exit()
            
    if DATA_FILE == None or TYPE == None or EMB_FILE == None:
        print ('usage: eval_cvfold.py -d <datafile> -n <nodevecs> -m <r/c/m>')
        sys.exit()

    print ("Data file: %s" % (DATA_FILE))
    print ("Emb file: %s" % (EMB_FILE))
    print ("Type: %s" % (TYPE))

    if TYPE == 'r':
        NUM_CLASSES = 0
    else:
        NUM_CLASSES = 7  # we set it to 7

    print ("NUM_CLASSES: %d" % (NUM_CLASSES))

    inpH = InputHelper()
    inpH.convertWordsToIds(EMB_FILE)

    #Load the word vectors
    print ("Loading pre-trained vectors...")
    inpH.loadW2V(EMB_FILE)
    printWordVecs(inpH);
   
    # Try out 5 different folds and choose the average results...
    average_across_experiments = 0
    average_acc_across_experiments = 0
    for i in range (0, NUM_EXPERIMENTS):

        skf = StratifiedKFold(n_splits=FOLD, random_state=SEED)    
        x, y = inpH.loadDataWithFolds(DATA_FILE)
    
        avg_metric_value_for_folds, avg_interval_acc = trainModel(inpH, x, y, skf, MAXLEN, NUM_CLASSES, TYPE, epochs=EPOCHS)
        average_across_experiments += avg_metric_value_for_folds;
        average_acc_across_experiments += avg_interval_acc;

    print ("Metric: {} {}".format(average_across_experiments/NUM_EXPERIMENTS, average_acc_across_experiments/NUM_EXPERIMENTS))
 
if __name__ == "__main__":
    main(sys.argv[1:])
