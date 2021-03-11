from flask import Flask, jsonify
from flask import request
from model_loader import init_model
from model_loader import init_embedding
from model_loader import predict_outcome
from keras import backend as k
from waitress import serve

#EMBFILE="../../../../../core/prediction/graphs/nodevecs/nodes_and_words.vec"

#The input to the REST API from the prediction demo isn't going to contain the context of a value.
#Hence we pass on the merged vector file
EMBFILE="../../../../../core/prediction/graphs/nodevecs/ndvecs.cv.128-10-0.1-0.9-both-0-false.merged.vec"

app = Flask(__name__)
inpH = init_embedding(EMBFILE)

#preprocess the request by replacing '-' with a space
def preprocess_request(data):
    #separate out the number part (the last part of the string). If number append it to the vector 
    pp_string = data.replace("-", " ")

    return pp_string

@app.route('/hbcp/api/v1.0/predict/outcome/<string:data>', methods=['GET'])
def outcome(data):
    print ("#word-nodes = {}".format(len(inpH.pre_emb)))

    pp_data = preprocess_request(data)
    print ("pp_data = {}".format(pp_data))

    #Reshape the vectors... this is not needed as we're using a merged vocab
    #print ("#Reshaping the original w/o context vectors word-nodes")
    #inpH.modifyW2V(pp_data)

    trained_model = init_model(inpH) # do this everytime... 
    predicted_val = predict_outcome(inpH, trained_model, pp_data)
    k.clear_session()  # clear up keras session

    return jsonify({'value': str(predicted_val)})

if __name__ == '__main__':
    serve(app, host="0.0.0.0", port=5000)
