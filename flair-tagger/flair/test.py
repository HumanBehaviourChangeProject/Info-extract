from flair.data import Sentence
from flair.models import SequenceTagger
import os, json
from segtok.segmenter import split_single

# load the NER tagger
tagger = SequenceTagger.load('resources/taggers/hbcp-ner/final-model.pt')


path = "/Users/yhou/git/hbcp/flairExp/testfile"
path1 = "/Users/yhou/git/hbcp/flairExp/testfile_entityPrediction"

for filename in os.listdir(path):
    print (filename)
    with open(path + "/"+ filename,'r', encoding='utf-8') as file:
        # for line in file.readlines():
        #     print (line)
        text = file.read().strip()
        # print (text)
        # print ("***sentence splitter result***")
        sentences = [Sentence(sent, use_tokenizer=True) for sent in split_single(text)]
        # for sent in sentences:
        #     print (sent)
        tagger.predict(sentences)
        result = []
        with open(path1 + "/"+ filename + ".json",'w', encoding='utf-8') as file:
            for sent in sentences:
                result.append(sent.to_dict(tag_type='ner'))
            file.write(json.dumps(result))
            # print (sent)
            # for entity in sent.get_spans('ner'):
            #     print (entity)
            #     print (sent.to_dict(tag_type='ner'))
    # os._exit(0)

os._exit(0)

# make a sentence
sentence = Sentence('Smokers in the control group are more likely to quit .')

# load the NER tagger
tagger = SequenceTagger.load('resources/taggers/hbcp-ner/final-model.pt')

# run NER over sentence
tagger.predict(sentence)

print(sentence)
print('The following NER tags are found:')

# iterate over entities and print
for entity in sentence.get_spans('ner'):
    print(sentence.tokens.__len__())
    print(entity)