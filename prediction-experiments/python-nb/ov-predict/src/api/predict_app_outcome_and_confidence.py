from flask import Flask, jsonify
from flask import request
from gevent.pywsgi import WSGIServer
from model_loader import init_model
from model_loader import init_embedding
from model_loader import predict_regression_outcome
from model_loader import predict_confidence
from model_loader import build_input_sequence
from model_loader import USES_TF_SERVING
from model_loader import MAXLEN
from api_error import ApiError
from model_training_app import model_training_api
from model_training_app import get_models_tinydb
from keras import backend as k
import numpy as np
import json
import os
import logging
from logging import handlers
import sys

from tinydb import Query

sys.path.append("..")
from train4api import DEFAULT_MODEL_NAME
sys.path.remove("..")

LOGGING_LEVEL = logging.getLevelName(os.environ.get('LOGGING_LEVEL', "INFO"))
logging.basicConfig(format="%(asctime)s | %(levelname)s | %(message)s",
                    level=LOGGING_LEVEL,
                    handlers=[
                        logging.handlers.RotatingFileHandler("api.log", maxBytes=500000, backupCount=5),
                        logging.StreamHandler(sys.stdout)
                    ])

# The input to the REST API from the prediction demo isn't going to contain the context of a value.
# Hence we pass on the merged vector file
EMBFILE = "../../resources/embfile4api.merged.vec"
NODEVEC_DIM = 100
#DEFAULT_MODEL_NAME = "default_model"

MC_MODEL_WT_FILE = '../../saved_models/model.mc.h5'

app = Flask(__name__)
app.register_blueprint(model_training_api)

# Removed global inpH
inpH = init_embedding(EMBFILE)


@app.route('/hbcp/api/v1.0/predict/')
def home():
    return "This is the prediction API."


@app.route('/hbcp/api/v1.0/predict/outcome/batch/', methods=['POST'])
@app.route('/hbcp/api/v1.0/predict/outcome/batch/<model_name>', methods=['POST'])
def outcome_batch(model_name=DEFAULT_MODEL_NAME):
    # expected data is a query string per line of body, parse it
    queries = request.data.decode('UTF-8').splitlines()
    # load the correct embedding for the query
    merged_vec_path = get_matching_merged_vec_file(model_name)
    results = get_responses_from_outcome_requests(model_name, merged_vec_path, queries)
    # prepare top level JSON object
    retmap = {'results': results}
    return json.dumps(retmap)


@app.route('/hbcp/api/v1.0/predict/outcome/<string:data>', methods=['GET'])
def outcome(data):
    """This call is deprecated, as it does not handle the selection of a prediction model, use the batch call instead"""
    retmap = get_responses_from_outcome_requests(DEFAULT_MODEL_NAME, EMBFILE, [data])
    return json.dumps(retmap[0])


def get_responses_from_outcome_requests(model_name, merged_vec_path, queries):
    # prepare the queries
    preprocessed_queries = list(map(preprocess_request, queries))
    logging.info(f"Received {len(preprocessed_queries)} queries.")
    # load models only if not using TF Serving
    trained_model = None
    trained_model_for_mc = None
    inpH = init_embedding(merged_vec_path)
    # load models
    logging.debug("#word-nodes = {}".format(len(inpH.pre_emb)))
    if not USES_TF_SERVING:
        trained_model = init_model(inpH)
        # load the mc model for getting confidences
        trained_model_for_mc = init_model(inpH, saved_model_wts_file=MC_MODEL_WT_FILE, num_classes=7)
    # get input batch
    input_list = list(build_input_sequences(preprocessed_queries, inpH, NODEVEC_DIM))
    # this is supposed to have shape (-1, 1, MAXLEN) at this point
    input_batch = np.array(input_list)
    input_batch = np.reshape(input_batch, (-1, MAXLEN))
    # run the prediction models on the batched input
    predicted_values = predict_regression_outcome(trained_model, model_name, input_batch)
    prediction_confidences = predict_confidence(trained_model_for_mc, model_name + "mc", input_batch)
    # clear up keras session
    if not USES_TF_SERVING:
        k.clear_session()
    # build the final result json
    results = []
    for value, conf in zip(predicted_values, prediction_confidences):
        # converts numpy.float32 to native Python float type
        result = {"value": value.item(), "conf": conf.item()}
        results.append(result)
    return results


def build_input_sequences(preprocessed_queries, inpH, nodevec_dim):
    for query in preprocessed_queries:
        yield build_input_sequence(inpH, query, nodevec_dim)


# preprocess the request by replacing '-' with a space
def preprocess_request(data):
    # separate out the number part (the last part of the string). If number append it to the vector
    pp_string = data.replace("-", " ")
    return pp_string


def get_matching_merged_vec_file(model_name):
    with get_models_tinydb() as db:
        Model = Query()
        matching_models = db.search(Model.name == model_name)
        if len(matching_models) == 0:
            raise ApiError(f"Model {model_name} does not exist.", status_code=400)
        return matching_models[0]["emb_path"]


@app.errorhandler(ApiError)
def handle_invalid_usage(error):
    response = jsonify(error.to_dict())
    response.status_code = error.status_code
    return response


if __name__ == '__main__':
    try:
        unparsed_port = os.environ['PORT']
        port = int(unparsed_port)
    except (KeyError, ValueError):
        port = 5000
    # I don't think this does anything
    app.debug = True
    server = WSGIServer(("0.0.0.0", port), app)
    server.start()
    logging.info("The hbcp-prediction-experiments server is now accepting connections...")
    server.serve_forever()
    #app.run(host='0.0.0.0', port=port, debug=False, threaded=False)
