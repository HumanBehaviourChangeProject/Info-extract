from flask import Flask, jsonify
from flask import request
from model_loader import init_model
from model_loader import init_embedding
from model_loader import predict_outcome_with_dynamic_vocabchange
from model_loader import predict_confidence
from keras import backend as k
import json
import os

#committed the merged file in git
#EMBFILE="../../../../../core/prediction/graphs/nodevecs/nodes_and_words.vec"

#The input to the REST API from the prediction demo isn't going to contain the context of a value.
#Hence we pass on the merged vector file
#EMBFILE="../../../../../core/prediction/graphs/nodevecs/ndvecs.cv.128-10-0.1-0.9-both-0-false.merged.vec"
EMBFILE="../../resources/embfile4api.merged.vec"
NODEVEC_DIM=100

MC_MODEL_WT_FILE='../../saved_models/model.mc.h5'

app = Flask(__name__)
inpH = init_embedding(EMBFILE)


@app.route('/hbcp/api/v1.0/predict/')
def home():
    return "This is the prediction API."

#preprocess the request by replacing '-' with a space
def preprocess_request(data):
    #separate out the number part (the last part of the string). If number append it to the vector 
    pp_string = data.replace("-", " ")

    return pp_string


@app.route('/hbcp/api/v1.0/predict/outcome/batch/', methods=['POST'])
def outcome_batch():
    # load models
    print ("#word-nodes = {}".format(len(inpH.pre_emb)))
    trained_model = init_model(inpH)  # do this everytime...
    trained_model_for_mc = init_model(inpH, saved_model_wts_file=MC_MODEL_WT_FILE,
                                      num_classes=7)  # load the mc model for getting confidences
    # expected data is a query string per line of body, parse it
    queries = request.data.decode('UTF-8').splitlines()
    results = []
    for query in queries:
        pp_data = preprocess_request(query)
        print ("pp_data = {}".format(pp_data))
        #Reshape the vectors... this is not needed as we're using a merged vocab
        #print ("#Reshaping the original w/o context vectors word-nodes")
        #inpH.modifyW2V(pp_data)
        predicted_val = predict_outcome_with_dynamic_vocabchange(inpH, trained_model, pp_data, NODEVEC_DIM)
        confidence = predict_confidence(inpH, trained_model_for_mc, pp_data, NODEVEC_DIM)
        result = {}
        result['value'] = str(predicted_val)
        result['conf'] = str(confidence)
        results.append(result)
    # clear Keras session
    k.clear_session()
    # prepare top level JSON object
    retmap = {'results': results}
    return json.dumps(retmap)


@app.route('/hbcp/api/v1.0/predict/outcome/<string:data>', methods=['GET'])
def outcome(data):
    print ("#word-nodes = {}".format(len(inpH.pre_emb)))

    pp_data = preprocess_request(data)
    print ("pp_data = {}".format(pp_data))

    #Reshape the vectors... this is not needed as we're using a merged vocab
    #print ("#Reshaping the original w/o context vectors word-nodes")
    #inpH.modifyW2V(pp_data)

    trained_model = init_model(inpH) # do this everytime... 
    trained_model_for_mc = init_model(inpH, saved_model_wts_file=MC_MODEL_WT_FILE, num_classes=7) # load the mc model for getting confidences

    predicted_val = predict_outcome_with_dynamic_vocabchange(inpH, trained_model, pp_data, NODEVEC_DIM)
    confidence = predict_confidence(inpH, trained_model_for_mc, pp_data, NODEVEC_DIM)

    k.clear_session()  # clear up keras session

    retmap = {}
    retmap['value'] = str(predicted_val) 
    retmap['conf'] = str(confidence) 

    return json.dumps(retmap)


if __name__ == '__main__':
    try:
        unparsed_port = os.environ['PORT']
        port = int(unparsed_port)
    except (KeyError, ValueError):
        port = 5000
    app.run(host='0.0.0.0', port=port, debug=False, threaded=False)
