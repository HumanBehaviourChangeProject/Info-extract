from flair.data import Sentence
from flair.models import SequenceTagger
import os
import json
from segtok.segmenter import split_single

# these point to the relevant DLaaS buckets
DATA_FOLDER = os.environ["DATA_DIR"]
RESULTS_FOLDER = os.environ["RESULT_DIR"]


# load the NER tagger
tagger = SequenceTagger.load(DATA_FOLDER + '/best-model.pt')

path = DATA_FOLDER + "/testfile/"
# path1 = RESULTS_FOLDER + "/testfile_entityPrediction/"

for filename in os.listdir(path):
    print(filename)
    with open(path + filename, 'r', encoding='utf-8') as file:
        text = file.read().strip()
        sentences = [Sentence(sent, use_tokenizer=True) for sent in split_single(text)]
        tagger.predict(sentences)
        result = []
        with open(RESULTS_FOLDER + '/' + filename + ".json", 'w', encoding='utf-8') as file_out:
            for sent in sentences:
                result.append(sent.to_dict(tag_type='ner'))
            file_out.write(json.dumps(result))
