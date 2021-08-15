from flask import Flask, jsonify
from flask import request
from model_loader import init_model
from model_loader import init_embedding
from model_loader import predict_regression_outcome
from model_loader import predict_confidence
from model_loader import build_input_sequence
from model_loader import USES_TF_SERVING
from model_loader import MAXLEN
from keras import backend as k
import numpy as np
import json
import os
import logging

LOGGING_LEVEL = logging.getLevelName(os.environ.get('LOGGING_LEVEL', "INFO"))
logging.basicConfig(format="%(asctime)s | %(levelname)s | %(message)s", level=LOGGING_LEVEL)

# committed the merged file in git
# EMBFILE="../../../../../core/prediction/graphs/nodevecs/nodes_and_words.vec"

# The input to the REST API from the prediction demo isn't going to contain the context of a value.
# Hence we pass on the merged vector file
# EMBFILE="../../../../../core/prediction/graphs/nodevecs/ndvecs.cv.128-10-0.1-0.9-both-0-false.merged.vec"
EMBFILE = "../../resources/embfile4api.merged.vec"
NODEVEC_DIM = 100

MC_MODEL_WT_FILE = '../../saved_models/model.mc.h5'

app = Flask(__name__)
inpH = init_embedding(EMBFILE)


@app.route('/hbcp/api/v1.0/predict/')
def home():
    return "This is the prediction API."


@app.route('/hbcp/api/v1.0/predict/outcome/batch/', methods=['POST'])
def outcome_batch():
    # load models
    logging.debug("#word-nodes = {}".format(len(inpH.pre_emb)))
    # expected data is a query string per line of body, parse it
    queries = request.data.decode('UTF-8').splitlines()
    results = get_responses_from_outcome_requests(inpH, queries)
    # prepare top level JSON object
    retmap = {'results': results}
    return json.dumps(retmap)


@app.route('/hbcp/api/v1.0/predict/outcome/<string:data>', methods=['GET'])
def outcome(data):
    logging.debug("#word-nodes = {}".format(len(inpH.pre_emb)))
    retmap = get_responses_from_outcome_requests(inpH, [data])
    return json.dumps(retmap[0])


def get_responses_from_outcome_requests(inpH, queries):
    # prepare the queries
    preprocessed_queries = list(map(preprocess_request, queries))
    logging.info(f"Received {len(preprocessed_queries)} queries.")
    # load models only if not using TF Serving
    trained_model = None
    trained_model_for_mc = None
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
    predicted_values = predict_regression_outcome(trained_model, "hbcppred", input_batch)
    prediction_confidences = predict_confidence(trained_model_for_mc, "hbcppredmc", input_batch)
    # clear up keras session
    if not USES_TF_SERVING:
        k.clear_session()
    # build the final result json
    results = []
    for value, conf in zip(predicted_values, prediction_confidences):
        result = {"value": value, "conf": conf}
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


if __name__ == '__main__':
    try:
        unparsed_port = os.environ['PORT']
        port = int(unparsed_port)
    except (KeyError, ValueError):
        port = 5000
    app.run(host='0.0.0.0', port=port, debug=False, threaded=False)
