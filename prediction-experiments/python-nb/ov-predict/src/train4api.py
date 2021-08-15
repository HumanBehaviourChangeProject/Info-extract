import tensorflow as tf
from model.lstm import buildModel
from model.lstm import rmse
from preprocessing.InputHelper import InputHelper
from preprocessing.InputHelper import transformLabels
import sys, getopt
import os
from keras.preprocessing.sequence import pad_sequences

# For quick testing set this to a small number
EPOCHS = 3

# About the right size
# EPOCHS=30

'''
Trains two models, one with regression and the other with classification, for API.
'''
def train(model, x_train, y_train, hidden_layer_dim=20, num_classes=0, epochs=EPOCHS, maxlen=300):
    batch_size = int(len(x_train) / 20)  # 5% of the training set size

    history = model.fit(x_train, y_train,
                        epochs=epochs,
                        verbose=True,
                        validation_split=0.1,
                        batch_size=batch_size)

    saveModel(model, num_classes=num_classes)

    return model


def saveModel(model, num_classes=0):
    model_header_name = ""
    model_name = ""
    if num_classes == 0:
        model_header_name = "../saved_models/model.json"
        model_name = "../saved_models/model.h5"
    else:
        model_header_name = "../saved_models/model.mc.json"  # model for saving the parameters for multi-class classification
        model_name = "../saved_models/model.mc.h5"

    model_json = model.to_json()
    with open(model_header_name, "w") as json_file:
        json_file.write(model_json)
    # serialize weights to HDF5
    model.save(model_name)
    save_model_for_serving_api(model_name, "mc" if num_classes > 0 else "")


def save_model_for_serving_api(original_model_path, model_name_suffix):
    # Export model for runtime API
    model = tf.keras.models.load_model(original_model_path, compile=False)
    export_path = "out/hbcppred" + model_name_suffix + "/1"
    print("Exporting trained model to", export_path)
    tf.saved_model.save(model, export_path)
    print("Done exporting!")


def main(argv):
    TRAIN_FILE = None
    EMB_FILE = None

    MAXLEN = 50
    NUM_CLASSES = 0

    try:
        opts, args = getopt.getopt(argv, "h:i:e:o:n:m:", ["trainfile=", "nodevecs=", "model="])

        for opt, arg in opts:
            if opt == '-h':
                print('eval_splitdata.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs>')
                sys.exit()
            elif opt in ("-i", "--trainfile"):
                TRAIN_FILE = arg
            elif opt in ("-n", "--nodevecs"):
                EMB_FILE = arg

    except getopt.GetoptError:
        print('usage: NodeSequenceRegression.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs>')
        sys.exit()

    if TRAIN_FILE == None or EMB_FILE == None:
        print('usage: NodeSequenceRegression.py -i <trainfile> -e <testfile> -o <resfile> -n <nodevecs>')
        sys.exit()

    print("Training file: %s" % (TRAIN_FILE))
    print("Emb file: %s" % (EMB_FILE))

    inpH = InputHelper()

    print("converting words to ids...")
    inpH.convertWordsToIds(EMB_FILE)

    for NUM_CLASSES in [0, 7]:
        print("loading input data...")

        x_train, y_train, x_test, y_test = inpH.loadData(EMB_FILE, TRAIN_FILE, None)

        y_train, y_test = transformLabels(y_train, y_test, NUM_CLASSES)

        x_train = pad_sequences(x_train, padding='post', maxlen=MAXLEN)
        if not x_test == None:
            x_test = pad_sequences(x_test, padding='post', maxlen=MAXLEN)

        print("building model (numclasses = {})...".format(NUM_CLASSES))
        print("DEBUG: During model saving - o/p dimension: {}".format(inpH.emb_dim))
        print("DEBUG: During model saving - emb matrix shape: {}".format(inpH.embedding_matrix.shape))
        model = buildModel(NUM_CLASSES, inpH.vocab_size, inpH.emb_dim, MAXLEN, inpH.embedding_matrix)

        print("training model with num_classes={}...".format(NUM_CLASSES))
        model = train(model, x_train, y_train, maxlen=MAXLEN, num_classes=NUM_CLASSES)


if __name__ == "__main__":
    main(sys.argv[1:])
