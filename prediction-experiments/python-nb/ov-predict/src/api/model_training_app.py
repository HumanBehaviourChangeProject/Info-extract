import logging
import os
import uuid
import json
import sys
import shutil

from flask import request
from flask import Blueprint
from werkzeug.utils import secure_filename
from tinydb import TinyDB, where

from api_error import ApiError

sys.path.append("..")
import train4api
sys.path.remove("..")

SAVED_MODELS_FOLDER = "../../saved_models"
TF_MODELS_FOLDER = "../../tf_models"
DEFAULT_MODEL_NAME = "userprediction"

model_training_api = Blueprint("model_training_api", __name__)


@model_training_api.route('/hbcp/api/v1.0/predict/models/train/', methods=['POST'])
def train():
    if "train_file" not in request.files or request.files["train_file"].filename == "":
        raise ApiError("No training file provided.", status_code=400)
    if "merged_vec_file" not in request.files or request.files["merged_vec_file"].filename == "":
        raise ApiError("No merged vectors file provided.", status_code=400)
    train_file = request.files["train_file"]
    merged_vec_file = request.files["merged_vec_file"]
    model_name = request.form.get("model_name", default=DEFAULT_MODEL_NAME, type=str)
    model_description = request.form.get("model_description", default="", type=str)
    epochs = request.form.get("epochs", default=10, type=int)
    # saves the train and vec files
    random_prefix = str(uuid.uuid4())
    train_filename = random_prefix + ".train.tsv"
    merged_vec_filename = random_prefix + ".merged.vec"
    train_path = os.path.join(SAVED_MODELS_FOLDER, train_filename)
    merged_vec_path = os.path.join(SAVED_MODELS_FOLDER, merged_vec_filename)
    train_file.save(train_path)
    merged_vec_file.save(merged_vec_path)
    # sanitize the model name as it will be used in filesystem
    model_name = secure_filename(model_name)
    # calls the training function
    train4api.train_and_save(train_path, merged_vec_path, model_name, epochs, model_folder=SAVED_MODELS_FOLDER,
                             tf_models_folder=TF_MODELS_FOLDER, model_description=model_description)
    # deletes train file (not embedding file since we need it at prediction time)
    os.remove(train_path)
    # updates TF Serving models.config
    with get_models_tinydb() as db:
        regenerate_tf_serving_models_config_file(db.all(), TF_MODELS_FOLDER)
    # returns response
    response = {"model_name": model_name, "model_description": model_description}
    return json.dumps(response)


@model_training_api.route("/hbcp/api/v1.0/predict/models/list/", methods=["GET"])
def list_models():
    res = []
    with get_models_tinydb() as db:
        for entry in db.all():
            if not entry["description"].endswith(train4api.MC_MODEL_DESCRIPTION_SUFFIX):
                res.append({"model_name": entry["name"], "model_description": entry["description"]})
    response = {"models": res}
    return json.dumps(response, indent=2)


@model_training_api.route("/hbcp/api/v1.0/predict/models/remove/", methods=["GET"])
def remove_model():
    if not request.args.get("model_name"):
        raise ApiError("Please specify a model name.", status_code=400)
    model_name = request.args["model_name"]
    logging.debug(f"Removing model {model_name} ...")
    res = []
    with get_models_tinydb() as db:
        # searches in TinyDB
        logging.debug("Searching matching models in TinyDB...")
        matching_models = db.search(where("name").one_of([model_name, model_name + "mc"]))
        logging.debug(f"Found {len(matching_models)} matches.")
        for model_entry in matching_models:
            # removes TF Serving folder
            folder = os.path.join(TF_MODELS_FOLDER, model_entry["name"])
            logging.debug(f"Removing TF Serving model folder {folder} ...")
            shutil.rmtree(folder, ignore_errors=True)
            logging.debug("Removed TF Serving model folder")
            # adds to json response
            if not model_entry["description"].endswith(train4api.MC_MODEL_DESCRIPTION_SUFFIX):
                res.append({"model_name": model_entry["name"], "model_description": model_entry["description"]})
        # clears TinyDB
        db.remove(doc_ids=[x.doc_id for x in matching_models])
        # regenerates models.config file
        regenerate_tf_serving_models_config_file(db.all(), TF_MODELS_FOLDER)
    logging.info(f"Successfully removed model {model_name}")
    response = {"removed_models": res}
    return json.dumps(response, indent=2)


def regenerate_tf_serving_models_config_file(tiny_db_model_entries, tf_models_folder):
    with open(os.path.join(tf_models_folder, "models.config"), "w") as f:
        f.write("model_config_list {\n")
        for entry in tiny_db_model_entries:
            name = entry["name"]
            f.write("  config {\n")
            f.write("    name: '" + name + "'\n")
            f.write("    base_path: '" + os.path.join("/models", name, "") + "'\n")
            f.write("    model_platform: 'tensorflow'\n")
            f.write("  }\n")
        f.write("}\n")


def get_models_tinydb():
    return TinyDB(os.path.join(SAVED_MODELS_FOLDER, train4api.MODELS_TINYDB_FILENAME))

