
# coding: utf-8

# ## HBCP: Testing with FLAIR
# 
# The sentence objects holds a sentence that we may want to embed or tag
from flair.data import Sentence
from flair.models import SequenceTagger
from flair.data import Corpus
from flair.datasets import ColumnCorpus
import os


# define columns
columns = {0: 'filename', 1: 'text', 2: 'pos', 3: 'ner'}

# this is the folder in which train, test and dev files reside.
# These point to DLaaS-specific mount points
data_folder = os.environ["DATA_DIR"]
results_folder= os.environ["RESULT_DIR"]

# init a corpus using column format, data folder and the names of the train, dev and test files
corpus: Corpus = ColumnCorpus(data_folder, columns,
                              train_file='train.csv',
                              test_file='test.csv',
                              dev_file='test.csv')

# ### Import embeddings and define NER entities
from flair.embeddings import TokenEmbeddings, WordEmbeddings, StackedEmbeddings, FlairEmbeddings
from typing import List


# 2. what tag do we want to predict?
tag_type = 'ner'

# 3. make the tag dictionary from the corpus
tag_dictionary = corpus.make_tag_dictionary(tag_type=tag_type)
print(tag_dictionary.idx2item)


### Define and load pre-trained embedding, stack them and train the model

# 4. initialize embeddings
embedding_types: List[TokenEmbeddings] = [
	WordEmbeddings('glove'),
    # comment in this line to use character embeddings
    CharacterEmbeddings(),
    # comment in these lines to use flair embeddings
    FlairEmbeddings('pubmed-forward'),
    FlairEmbeddings('pubmed-backward'),
]

embeddings: StackedEmbeddings = StackedEmbeddings(embeddings=embedding_types)

# 5. initialize sequence tagger
from flair.models import SequenceTagger

tagger: SequenceTagger = SequenceTagger(hidden_size=256,
                                        embeddings=embeddings,
                                        tag_dictionary=tag_dictionary,
                                        tag_type=tag_type,
                                        use_crf=True)

# 6. initialize trainer
from flair.trainers import ModelTrainer

trainer: ModelTrainer = ModelTrainer(tagger, corpus)


# 7. start training
trainer.train(results_folder,
              learning_rate=0.1,
              mini_batch_size=32,
              max_epochs=150)

print ("END!")
# 8. plot training curves (optional)
#from flair.visual.training_curves import Plotter
#plotter = Plotter()
#plotter.plot_training_curves(results_folder+'/loss.tsv')
#plotter.plot_weights(results_folder+'/weights.txt')



