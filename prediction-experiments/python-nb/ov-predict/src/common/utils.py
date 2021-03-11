import numpy as np
import pandas as pd
from preprocessing.InputHelper import transformLabels_aux
from collections import Counter


def plotHistogram(valueList, caption):
    freqs = pd.Series(valueList).value_counts()
    print (caption)
    print (freqs)
    #freqs.plot(kind='bar')
    #plt.suptitle(caption)
    #plt.show()

def getSelectedData(x, y, indexes):
    x_sel = []
    y_sel = []

    for index in indexes:
        x_sel.append(x[index])
        y_sel.append(y[index])

    return np.asarray(x_sel), y_sel
    
def printWordVecs(inpH):
    #Print the loaded words
    nwords=0
    for w in inpH.pre_emb:
        print ("Dimension of vectors: {}".format(inpH.pre_emb[w].shape))
        print ("{} {}".format(w, inpH.pre_emb[w][0:5]))
        nwords = nwords+1
        if (nwords >= 2): break

    print ("vocab size: {}".format(inpH.vocab_size))
    print ("emb-matrix: {}...".format(inpH.embedding_matrix[1][:5]))
    print (inpH.embedding_matrix.shape)
    
def convertSoftmaxToLabels(y_preds):
    labels=[]
    for i in range(y_preds.shape[0]):
        labels.append(np.argmax(y_preds[i]))
    #print (labels)
    return labels

def computePerIntervalStats(y_vals, num_classes):
    #[0,5] [5,10] [10, 15] [15,20] [20,30] [30,50] [50,100]
    binned_y_vals = {0: [], 1: [], 2: [], 3: [], 4: [], 5: [], 6: []}
    interval_stats = {0: [2.5], 1: [7.5], 2: [12.5], 3: [17.5], 4: [25], 5: [40], 6: [75]}
   
    y_classes = transformLabels_aux(y_vals, num_classes) 
    i = 0
    for y_class in y_classes:
        binned_y_vals[y_class].append(y_vals[i])
        i+=1 

    for key in binned_y_vals:
        values_in_range = binned_y_vals.get(key)
        if len(values_in_range) > 0:
            values_in_range = np.asarray(values_in_range, dtype=np.float32)
            median = np.median(values_in_range)

        interval_stats[key] = float(median)
        
    return interval_stats

# predict the median of values within a predicted class to be the o/p value
#compute rmse wrt to the true values
def computeTwoStagedRMSE(num_classes, fold_id, y_preds, y_train_vals, y_test_vals):
    # y_preds are discrete integers (classes), y_test_vals are real numbers
    # first obtain the medians for each class from the y_test_vals

    interval_stats = computePerIntervalStats(y_train_vals, num_classes);

    y_pred_vals = []
    for y_pred in y_preds:
        #print ("y_pred = {}".format(y_pred))
        y_pred_vals.append(float(interval_stats[y_pred]))

    #print ("y_test_vals = {}".format(y_test_vals))

    y = np.asarray(y_test_vals, dtype=np.float32)
    y_hats = np.asarray(y_pred_vals, dtype=np.float32)
    mse = (np.square(y - y_hats)).mean(axis=None)
    rmse = sqrt(mse)

    print ("Fold {}: Two-step RMSE: {}".format(fold_id, rmse))
    return rmse

