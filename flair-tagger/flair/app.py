from flask import abort, Flask, jsonify, request
from flair.models import SequenceTagger
from flair.data import Sentence
import os
# import os, json

app = Flask(__name__)

tagger = SequenceTagger.load('best-model.pt')


@app.route('/api/v1/extractEntitiesSingleSent', methods=['POST'])
def extractEntitiesSingleSent():
    if not request.json or not 'sentence' in request.json:
        abort(400)
    message = request.json['sentence']
    sentence = Sentence(message, use_tokenizer=True)
    tagger.predict(sentence)
    response = {'result': sentence.to_dict(tag_type='ner')}
    return jsonify(response), 200


@app.route('/api/v1/extractEntitiesMultiSent', methods=['POST'])
def extractEntitiesMultiSent():
    if not request.json or not 'sentences' in request.json:
        abort(400)
    message = request.json['sentences']
    sentences = []
    for sent in message:
        sentence = Sentence(sent, use_tokenizer=True)
        if (len(sentence))>300:
            continue
        emptyToken = False
        for token in sentence.tokens:
            if len(token.text) <=0:
                emptyToken = True
                break;
        if len(sentence) > 5 and not emptyToken:
            sentences.append(sentence)
    tagger.predict(sentences)
    results = []
    for sent in sentences:
        results.append(sent.to_dict(tag_type='ner'))
    response = {'result': results}
    return jsonify(response), 200

if __name__ == "__main__":
    from waitress import serve
    try:
        unparsed_port = os.environ['PORT']
        port = int(unparsed_port)
    except (KeyError, ValueError):
        port = 8080
    serve(app, host="0.0.0.0", port=port)