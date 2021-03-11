import sys
sys.path.append('..')

'''
This is a stand-alone test for the python API service. It doesn't use Flask.
'''

import numpy as np

from keras.models import model_from_json
from preprocessing.InputHelper import InputHelper
from model.lstm import rmse
from model.lstm import buildModel
from keras.preprocessing.sequence import pad_sequences

OPTIMIZER='rmsprop'
NUM_CLASSES = 0
MAXLEN=50
SAVED_MODEL_FILE='../../saved_models/model.h5'

PUBMED_DIM=200
VAL_DIMENSIONS=5

class FuzzyMatchInfo:
    def __init__(self, closestToken, origValue, replacedValue):
        self.closestToken = closestToken
        self.origValue = origValue
        self.replacedValue = replacedValue

def get_model_json(saved_model):
    print ("Loading model from file {}".format(saved_model))
    json_file = open(saved_model, 'r')
    json_str = json_file.read()
    json_file.close()
    return json_str

def predict_outcome(inpH, model, test_instance_str):

    x = inpH.tokenizer.texts_to_sequences([test_instance_str])
    x = pad_sequences(x, padding='post', maxlen=MAXLEN)
    y_preds = model.predict(x, steps=1)
    return y_preds[0]

def predict_outcome_with_dynamic_vocabchange(inpH, model, test_instance_str, nodevec_dim):
    changeLogDict, x_test = build_input_sequence(inpH, test_instance_str, nodevec_dim) # string to token-id sequence
    print ("x_test = {}".format(x_test))

    y_preds = model.predict(x_test, steps=1)
    print ('y_preds = {}'.format(y_preds)) 

    #After prediction revert back the values that we changed from the vocab-vector map
    for changeInfo in changeLogDict.values():
        inpH.pre_emb[changeInfo.closestToken][-VAL_DIMENSIONS] = changeInfo.origValue

    return y_preds[0][0]

def predict_confidence(inpH, model, test_instance_str, nodevec_dim):
    changeLogDict, x_test = build_input_sequence(inpH, test_instance_str, nodevec_dim) # string to token-id sequence
    print ("x_test = {}".format(x_test))

    y_preds = model.predict(x_test, steps=1)
    print ('MC o/p: y_preds = {}'.format(y_preds)) 

    #After prediction revert back the values that we changed from the vocab-vector map
    for changeInfo in changeLogDict.values():
        inpH.pre_emb[changeInfo.closestToken][-VAL_DIMENSIONS] = changeInfo.origValue

    return max(y_preds[0])

def init_embedding(embfile):
    inpH = InputHelper()
    print ("converting words to ids...")
    inpH.convertWordsToIds(embfile)
    print ("vocab size = {}".format(inpH.vocab_size))

    inpH.loadW2V(embfile)
    return inpH


#Replace a node if the form C:<x>:0.1 with C:<x>:0.2 (the closest value with the same attrib-id in our vocabulary)
def getClosestNode(inpH, node):
    keytokens = node.split(':')
    keynode = keytokens[1]
    keyvalue = keytokens[2]
    if is_number(keyvalue)==False:
        return None
    keyvalue = float(keyvalue)

    mindiff = 10000
    closestFound = None
    tobeReplacedWith = 0
 
    #Match the AttribType:Id part
    for token in inpH.pre_emb:
        parts = token.split(':')
        nodename = parts[1]
        if nodename==keynode:
            if is_number(parts[2])==False:
                continue
            x = float(parts[2])
            diff = abs(keyvalue - x)
            if diff < mindiff:
                mindiff = diff
                closestFound = token
                tobeReplacedWith = x

    return FuzzyMatchInfo(closestFound, keyvalue, tobeReplacedWith)

def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        return False

def replaceAVPSeqWithNN(inpH, avpseq, nodevec_dim):
    tokens = avpseq.split(' ')
    modified_avpseq = []
    changedTokens = {}  # to keep track of the changes for reverting back

    for token in tokens:
        fuzzyMatchInfo = getClosestNode(inpH, token)
        if fuzzyMatchInfo==None:
            continue # check if continue works as expected in Python

        changedTokens[fuzzyMatchInfo.closestToken] = fuzzyMatchInfo

        instvec = []
        attrvec = inpH.pre_emb[fuzzyMatchInfo.closestToken]
        #change the dimension corresponding to the value in our vocabulary dict

        #replace the nodevec part of instvec with attrvec
        for i in range(nodevec_dim):
            instvec.append(float(attrvec[i]))

        #context part comes from the current instance
        for i in range(nodevec_dim, nodevec_dim+PUBMED_DIM+VAL_DIMENSIONS):
            instvec.append(float(inpH.pre_emb[fuzzyMatchInfo.closestToken][i]))
            
        instvec_array = np.asarray(instvec)
            
        instvec_array[-VAL_DIMENSIONS] = fuzzyMatchInfo.replacedValue #new followup value
        inpH.pre_emb[fuzzyMatchInfo.closestToken] = instvec_array # modified instvec

        modified_avpseq.append(fuzzyMatchInfo.closestToken) 

    return changedTokens, ' '.join(modified_avpseq) 


def build_input_sequence(inpH, x_text, nodevec_dim):

    changeLogDict, modified_x_text = replaceAVPSeqWithNN(inpH, x_text, nodevec_dim)

    # Convert each sentence (node name sequence) to a sequence of integer ids
    x = inpH.tokenizer.texts_to_sequences([modified_x_text])
    x = pad_sequences(x, padding='post', maxlen=MAXLEN)
    return changeLogDict, x

def init_model(inpH, saved_model_wts_file=SAVED_MODEL_FILE, num_classes=NUM_CLASSES):
    #saved_model_meta_file = '../../saved_models/model.json'
    #json_str = get_model_json(saved_model_meta_file)
    #print (json_str)
    #trained_model = model_from_json(json_str)

    #rebuild the original model
    print ("DEBUG: During API call - emb matrix o/p dimension: {}".format(inpH.embedding_matrix.shape[1]))
    print ("DEBUG: During API call - emb matrix shape: {}".format(inpH.embedding_matrix.shape))
    trained_model = buildModel(num_classes, inpH.vocab_size, inpH.embedding_matrix.shape[1], MAXLEN, inpH.embedding_matrix)

    # load weights into new model
    trained_model.load_weights(saved_model_wts_file)
    trained_model.summary()

    return trained_model

def init_model_and_embedding(embfile, modelfile=SAVED_MODEL_FILE):
    inpH = init_embedding(embfile)
    trained_model = init_model(inpH, modelfile)
    return inpH, trained_model 

def main(argv):
    NODEVEC_DIM=100
    EMBFILE="../../../../../core/prediction/graphs/nodevecs/embfile4api.merged.vec"

    #one sample line from test data file
    TESTDATA_ROW = "C:5579689:18 I:3675717:1"
    TESTDATA_ROW2 = "C:5579689:18 I:3675717:1 C:5579088:35 I:3673272:1"

    inpH, trained_model = init_model_and_embedding(EMBFILE)

    #try executing a test instance on the loaded model
    predicted_val = predict_outcome_with_dynamic_vocabchange(inpH, trained_model, TESTDATA_ROW, NODEVEC_DIM)
    print (predicted_val)

    predicted_val = predict_outcome_with_dynamic_vocabchange(inpH, trained_model, TESTDATA_ROW2, NODEVEC_DIM)
    print (predicted_val)

if __name__ == "__main__":
    main(sys.argv[1:])
