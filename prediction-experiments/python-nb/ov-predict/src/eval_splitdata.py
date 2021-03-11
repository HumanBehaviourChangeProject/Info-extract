from model.lstm import buildModel
import numpy as np
from model.lstm import rmse
from preprocessing.InputHelper import InputHelper
from preprocessing.InputHelper import transformLabels
import sys, getopt
import os
from keras.preprocessing.sequence import pad_sequences

EPOCHS=30

def train(model, x_train, y_train, hidden_layer_dim=20, num_classes=0, epochs=EPOCHS, maxlen=300):
    batch_size = int(len(x_train)/20) # 5% of the training set size
    
    history = model.fit(x_train, y_train,
        epochs=epochs,
        verbose=True,
        validation_split=0.1,
        batch_size=batch_size)
   
    saveModel(model)
 
    return model

def test(model, x_test, y_test, res_file, num_classes=0):
    loss, accuracy = model.evaluate(x_test, y_test, verbose=True)
    if (num_classes > 0):
        print("Cross-entropy loss: {:.4f}, Accuracy: {:.4f}".format(loss, accuracy))
    else:
        print("RMSE: {:.4f}".format(accuracy))    
        
    #Print each prediction    
    y_preds = model.predict(x_test)
   
    f = open(res_file, "w")
    for i in range(y_preds.shape[0]):
        if (num_classes > 0):
            f.write ("{:d}\t{:d}\n".format(np.argmax(y_test[i]), np.argmax(y_preds[i])))
        else:
            f.write ("{}\t{:.4f}\n".format(y_test[i], y_preds[i][0]))

    f.close()

def saveModel(model):
    model_json = model.to_json()
    with open("../saved_models/model.json", "w") as json_file:
        json_file.write(model_json)
    # serialize weights to HDF5
    model.save_weights("../saved_models/model.h5")

def main(argv):
    TRAIN_FILE = None
    TEST_FILE = None
    TYPE = None
    EMB_FILE = None
    RES_FILE = None

    MAXLEN=50
    NUM_CLASSES=0

    try:
        opts, args = getopt.getopt(argv,"h:i:e:o:n:m:", ["trainfile=", "testfile=", "resfile=", "nodevecs=", "model="])
    
        for opt, arg in opts:
            if opt == '-h':
                print ('eval_splitdata.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs> - m <r (regression)/ c(classification)>')
                sys.exit()
            elif opt in ("-i", "--trainfile"):
                TRAIN_FILE = arg
            elif opt in ("-e", "--testfile"):
                TEST_FILE = arg
            elif opt in ("-n", "--nodevecs"):
                EMB_FILE = arg
            elif opt in ("-o", "--resfile"):
                RES_FILE = arg
            elif opt in ("-m", "--model"):
                TYPE = arg
                
    except getopt.GetoptError:
        print ('usage: NodeSequenceRegression.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs> -m <r/c>')
        sys.exit()

    if TRAIN_FILE == None or TEST_FILE == None or TYPE == None or EMB_FILE == None or RES_FILE == None:
        print ('usage: NodeSequenceRegression.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs> -m <r/c>')
        sys.exit()
 
    print ("Training file: %s" % (TRAIN_FILE))
    print ("Test file: %s" % (TEST_FILE))
    print ("Res file: %s" % (RES_FILE))
    print ("Emb file: %s" % (EMB_FILE))

    inpH = InputHelper()

    print ("converting words to ids...")
    inpH.convertWordsToIds(EMB_FILE)
    
    print ("loading input data...")
    if TYPE == 'r':
        NUM_CLASSES = 0
    else:
        NUM_CLASSES = 7  # we set it to 7

    print ("num_classes: %d" % (NUM_CLASSES))
 
    x_train, y_train, x_test, y_test = inpH.loadData(EMB_FILE, TRAIN_FILE, TEST_FILE)
    y_train, y_test = transformLabels(y_train, y_test, NUM_CLASSES)
    
    x_train = pad_sequences(x_train, padding='post', maxlen=MAXLEN)
    x_test = pad_sequences(x_test, padding='post', maxlen=MAXLEN)

    #print (x_train[0])
    #print (x_test[0])
    
    print ("building model...")
    print ("DEBUG: During model saving - o/p dimension: {}".format(inpH.emb_dim))
    print ("DEBUG: During model saving - emb matrix shape: {}".format(inpH.embedding_matrix.shape))
    model = buildModel(NUM_CLASSES, inpH.vocab_size, inpH.emb_dim, MAXLEN, inpH.embedding_matrix)
    
    print ("training model...")
    model = train(model, x_train, y_train, maxlen=MAXLEN, num_classes=NUM_CLASSES)

    print ("testing model and generating output...")
    if len(x_test)>0:
        test(model, x_test, y_test, RES_FILE, num_classes=NUM_CLASSES)

if __name__ == "__main__":
    main(sys.argv[1:])
