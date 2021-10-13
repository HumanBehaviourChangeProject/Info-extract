import getopt
import logging
import os
import sys

import tensorflow as tf
from keras.preprocessing.sequence import pad_sequences
from tinydb import TinyDB, where

from model.lstm import buildModel
from preprocessing.InputHelper import InputHelper
from preprocessing.InputHelper import transformLabels

# set logging
LOGGING_LEVEL = logging.getLevelName(os.environ.get('LOGGING_LEVEL', "INFO"))
logging.basicConfig(format="%(asctime)s | %(levelname)s | %(message)s", level=LOGGING_LEVEL)

MAX_LEN = 50
# For quick testing set this to a small number like 3
DEFAULT_EPOCHS = 10
DEFAULT_MODEL_FOLDER = os.path.join("..", "saved_models")
DEFAULT_TF_MODELS_FOLDER = DEFAULT_MODEL_FOLDER
DEFAULT_MODEL_NAME = "default_model"
MC_MODEL_DESCRIPTION_SUFFIX = " (confidence scoring)"

MODELS_TINYDB_FILENAME = "models_tinydb.json"

# About the right size
# EPOCHS=30


def train_and_save(train_tsv_path, merged_vec_path, model_name, epochs=DEFAULT_EPOCHS,
                   model_folder=DEFAULT_MODEL_FOLDER, tf_models_folder=DEFAULT_TF_MODELS_FOLDER, model_description=""):
    inpH = InputHelper()
    logging.info("converting words to IDs...")
    inpH.convertWordsToIds(merged_vec_path)
    for num_classes in [0, 7]:
        logging.info("loading input data...")
        x_train, y_train, x_test, y_test = inpH.loadData(merged_vec_path, train_tsv_path, None)
        y_train, y_test = transformLabels(y_train, y_test, num_classes)
        x_train = pad_sequences(x_train, padding='post', maxlen=MAX_LEN)
        if x_test is not None:
            x_test = pad_sequences(x_test, padding='post', maxlen=MAX_LEN)
        logging.info("building model (num_classes = {})...".format(num_classes))
        logging.debug("DEBUG: During model saving - o/p dimension: {}".format(inpH.emb_dim))
        logging.debug("DEBUG: During model saving - emb matrix shape: {}".format(inpH.embedding_matrix.shape))
        model = buildModel(num_classes, inpH.vocab_size, inpH.emb_dim, MAX_LEN, inpH.embedding_matrix)
        logging.info("training model with num_classes={}...".format(num_classes))
        model = fit(model, x_train, y_train, num_classes=num_classes, epochs=epochs, maxlen=MAX_LEN)
        logging.info(f"saving model {model_name} and multi-class version for confidence scoring...")
        save_model(model, model_name, merged_vec_path, num_classes=num_classes, model_folder=model_folder,
                   tf_models_folder=tf_models_folder, model_description=model_description)


def fit(model, x_train, y_train, hidden_layer_dim=20, num_classes=0, epochs=DEFAULT_EPOCHS, maxlen=300):
    """
    Trains two models, one with regression and the other with classification, for API.
    """
    batch_size = int(len(x_train) / 20)  # 5% of the training set size
    history = model.fit(x_train, y_train,
                        epochs=epochs,
                        verbose=True,
                        validation_split=0.1,
                        batch_size=batch_size)
    return model


def save_model(model, model_name, merged_vec_path, num_classes=0,
               model_folder=DEFAULT_MODEL_FOLDER, tf_models_folder=DEFAULT_TF_MODELS_FOLDER, model_description=""):
    # original saving code (mostly for local experiments? but might be useless now)
    model_h5_path = save_model_h5(model, model_folder, num_classes)
    # save models for TensorFlow Serving
    model_suffix = "mc" if num_classes > 0 else ""
    save_model_for_serving_api(model_name, model_suffix, model_h5_path, tf_models_folder)
    # save model name and embedding file path in the TinyDB database
    save_model_in_tinydb(model_name, model_suffix,
                         model_description + (MC_MODEL_DESCRIPTION_SUFFIX if num_classes > 0 else ""),
                         merged_vec_path, model_folder)


def save_model_h5(model, model_folder, num_classes):
    model_header_name = os.path.join(model_folder, "model." + ("mc." if num_classes > 0 else "") + "json")
    model_h5_name = os.path.join(model_folder, "model." + ("mc." if num_classes > 0 else "") + "h5")
    model_json = model.to_json()
    with open(model_header_name, "w") as json_file:
        json_file.write(model_json)
    # serialize weights to HDF5
    model.save(model_h5_name)
    return model_h5_name


def save_model_for_serving_api(model_name, model_name_suffix, original_model_path, model_folder):
    # Export model for runtime API
    model = tf.keras.models.load_model(original_model_path, compile=False)
    export_path = os.path.join(model_folder, model_name + model_name_suffix, "1")
    logging.info(f"Exporting trained model to {export_path}")
    tf.saved_model.save(model, export_path)
    logging.info("Done exporting!")


def save_model_in_tinydb(model_name, model_name_suffix, model_description, merged_vec_path, model_folder):
    model_id = model_name + model_name_suffix
    with TinyDB(os.path.join(model_folder, MODELS_TINYDB_FILENAME)) as db:
        # first remove existing models with this name
        existing_models = db.search(where("name") == model_id)
        db.remove(doc_ids=[x.doc_id for x in existing_models])
        # insert the model
        db.insert({"name": model_id, "description": model_description, "emb_path": merged_vec_path})


def main(argv):
    TRAIN_FILE = None
    EMB_FILE = None
    model_name = DEFAULT_MODEL_NAME
    epochs = DEFAULT_EPOCHS
    help_string = "train4api.py -i <trainfile> -n <merged.vecfile> -m <modelname> -e <epochs>"

    try:
        opts, args = getopt.getopt(argv, "h:i:n:m:e:", ["trainfile=", "nodevecs=", "modelname=", "epochs="])
        for opt, arg in opts:
            if opt == '-h':
                print(help_string)
                sys.exit()
            elif opt in ("-i", "--trainfile"):
                TRAIN_FILE = arg
            elif opt in ("-n", "--nodevecs"):
                EMB_FILE = arg
            elif opt in ("-m", "--modelname"):
                model_name = arg
            elif opt in ("-e", "--epochs"):
                epochs = int(arg)
    except getopt.GetoptError:
        print(help_string)
        sys.exit()

    if TRAIN_FILE is None or EMB_FILE is None:
        print(help_string)
        sys.exit()

    print("Training file: %s" % TRAIN_FILE)
    print("Emb file: %s" % EMB_FILE)
    train_and_save(TRAIN_FILE, EMB_FILE, model_name, epochs)


if __name__ == "__main__":
    main(sys.argv[1:])
