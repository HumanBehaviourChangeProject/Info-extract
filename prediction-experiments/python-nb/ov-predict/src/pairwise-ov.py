from keras.preprocessing.text import Tokenizer
from keras.preprocessing.sequence import pad_sequences
from collections import defaultdict
import random as r
import math as m
import numpy as np
from keras import backend as K
from random import Random
import pandas as pd
from keras.preprocessing import sequence
from keras.models import Sequential, Model
from keras.layers import Dense, Dropout, Flatten, Embedding, LSTM, Bidirectional, Concatenate
from keras.layers import Input, Lambda
from keras.optimizers import Adam
from keras.optimizers import RMSprop
from sklearn.model_selection import train_test_split
from keras.layers.merge import concatenate
import sys, getopt
import os

MAXLEN=50
SEED=314159
LSTM_DIM=32
DROPOUT=0.2
EPOCHS=2
BATCH_SIZE=1000

class RCTArm:
    def __init__(self, line, index):
        l = line.strip().split("\t")
        self.id = index
        self.text = l[0]
        self.ov = float(l[1])
        
    def __str__(self):
        return 'RCT:({}) OV: {}'.format(self.text, self.ov)


class RCTArm:
    def __init__(self, line, index):
        l = line.strip().split("\t")
        self.id = index
        self.text = l[0]
        self.ov = float(l[1])
        
    def __str__(self):
        return 'RCT:({}) OV: {}'.format(self.text, self.ov)
    
class RCTArms:
    
    def __init__(self, train_file):
        self.rcts = []
        
        line_number = 0
        for line in open(train_file):
            rct_arm = RCTArm(line, line_number)
            self.rcts.append(rct_arm)
            line_number += 1 
                
    def convertWordsToIds(self, maxlen=MAXLEN):        
        self.maxlen = maxlen

        all_text = []
        for rct in self.rcts:
            all_text.append(rct.text)
            
        self.keras_tokenizer = Tokenizer(num_words=None, filters=[], lower=False, split=' ')
        self.keras_tokenizer.fit_on_texts(all_text)
        
        self.vsize = len(self.keras_tokenizer.word_index) + 1
        self.x = self.keras_tokenizer.texts_to_sequences(all_text)
        self.x = pad_sequences(self.x, padding='post', maxlen=maxlen)        
    
    def create_embedding_matrix(self, embfile):
        in_dict = 0
        
        with open(embfile) as f:
            line_number=0
            
            for line in f:
                if line_number==0:
                    tokens = line.strip().split(" ")
                    self.embedding_dim = int(tokens[1])
                    self.embedding_matrix = np.zeros((self.vsize, self.embedding_dim))
                else:
                    word, *vector = line.split()
                    if word in self.keras_tokenizer.word_index:
                        idx = self.keras_tokenizer.word_index[word]
                        in_dict += 1
                        self.embedding_matrix[idx] = np.array(
                            vector, dtype=np.float32)[:self.embedding_dim]
                
                line_number += 1
        
        return in_dict
    
    def getData(self, index):
        return self.x[index]
    
    def create_pairs(self):
        '''Positive and negative pair creation.
        Alternates between positive and negative pairs.
        '''
        pairs = []
        labels = []
        
        N = len(self.rcts)
        for i in range (N-1):
            for j in range(i+1,N):                
                pairs.append([self.x[i], self.x[j]])
                label = 0                
                if self.rcts[i].ov >= self.rcts[j].ov:
                    label = 1
                labels.append(label)
                
        return np.array(pairs), np.array(labels)    
    
    # Cartesian product between two sets
    def create_pairs_from_ids(self, listA, listB):
        pairs = []
        labels = []
        seen_pairs = {}
        
        self_pair_count = 0
        seen_pair_count = 0
        
        for a in listA:
            for b in listB:
                '''
                Avoid self-pairs; also ignore the order
                '''
                if a==b:
                    self_pair_count+=1
                    continue
                key = str(a) + ' ' + str(b)
                revkey = str(b) + ' ' + str(a)
                if not key in seen_pairs and not revkey in seen_pairs:
                    seen_pairs[key] = True
                    seen_pairs[revkey] = True
                else:
                    seen_pair_count+=1
                    continue # this pair has already been added
                    
                pairs.append([self.x[a], self.x[b]])
                label = 0                
                if self.rcts[a].ov >= self.rcts[b].ov:
                    label = 1
                labels.append(label)
                
        print ('Ignored {} (self) and {} (seen) duplicate pairs'.format(self_pair_count, seen_pair_count))
        return pairs, labels 
        
    def create_split_aware_pairs(self, train_ratio=0.9):
        #First split the rcts into train and test
        r = Random(SEED)
        r.shuffle(self.rcts)
        
        ids = list(map(lambda r: r.id, self.rcts)) # list of shuffled ids
        ntrain = int(len(ids)*train_ratio)
        train_ids = ids[0:ntrain]
        test_ids = ids[ntrain:]
        
        #collect all pairs from the train ids
        train_pairs, train_labels = self.create_pairs_from_ids(train_ids, train_ids)
        
        #collect all pairs from the test ids - complete subgraph
        self_test_pairs, self_test_labels = self.create_pairs_from_ids(test_ids, test_ids)
        
        # additionally build the cross-pairs, (test, train) pairs
        cross_test_pairs, cross_test_labels = self.create_pairs_from_ids(test_ids, train_ids)
        
        test_pairs = self_test_pairs + cross_test_pairs 
        test_labels = self_test_labels + cross_test_labels 
        
        return np.array(train_pairs), np.array(train_labels), np.array(test_pairs), np.array(test_labels)        
    

def complete_model(rcts):
    
    input_a = Input(shape=(rcts.maxlen, ))    
    print (input_a.shape)
    
    emb_a = Embedding(rcts.embedding_matrix.shape[0],
                  rcts.embedding_matrix.shape[1],
                  weights=[rcts.embedding_matrix])(input_a)
    print (emb_a.shape)
    
    input_b = Input(shape=(rcts.maxlen, ))    
    print (input_b.shape)
    
    emb_b = Embedding(input_dim=rcts.embedding_matrix.shape[0],
                  output_dim=rcts.embedding_matrix.shape[1],
                  weights=[rcts.embedding_matrix])(input_b)
    print (emb_b.shape)
    
    shared_lstm = LSTM(LSTM_DIM)

    # because we re-use the same instance `base_network`,
    # the weights of the network
    # will be shared across the two branches
    processed_a = shared_lstm(emb_a)
    processed_a = Dropout(DROPOUT)(processed_a)
    processed_b = shared_lstm(emb_b)
    processed_b = Dropout(DROPOUT)(processed_b)

    merged_vector = concatenate([processed_a, processed_b], axis=-1)
    # And add a logistic regression (2 class - sigmoid) on top
    # used for backpropagating from the (pred, true) labels
    predictions = Dense(1, activation='sigmoid')(merged_vector)
    
    model = Model([input_a, input_b], outputs=predictions)
    return model

def main(argv):
    DATA_FILE = None
    EMB_FILE = None
    
    try:
        opts, args = getopt.getopt(argv,"h:d:n:", ["datafile=", "nodevecs="])
    
        for opt, arg in opts:
            if opt == '-h':
                print ('eval_cvfold.py -d/--datafile= <datafile> -n/--nodevecs= <nodevecs>')
                sys.exit()
            elif opt in ("-d", "--datafile"):
                DATA_FILE = arg
            elif opt in ("-n", "--nodevecs"):
                EMB_FILE = arg
                
    except getopt.GetoptError:
        print ('usage: eval_cvfold.py -d <datafile> -n <nodevecs>')
        sys.exit()
            
    if DATA_FILE == None or EMB_FILE == None:
        print ('usage: eval_cvfold.py -d <datafile> -n <nodevecs> -m <r/c/m>')
        sys.exit()

    print ("Data file: %s" % (DATA_FILE))
    print ("Emb file: %s" % (EMB_FILE))

    rcts = RCTArms(DATA_FILE)
    rcts.convertWordsToIds()

    # Load embeddings from dictionary 
    nwords_in_dict = rcts.create_embedding_matrix(EMB_FILE)

    # Print Vocab overlap
    #nonzero_elements = np.count_nonzero(np.count_nonzero(rcts.embedding_matrix, axis=1))
    #print(nonzero_elements / rcts.vsize)
    print ('#words in vocab: {}'.format(nwords_in_dict))

    x_train, y_train, x_test, y_test = rcts.create_split_aware_pairs()
    print ("#Train pairs: {}".format(x_train.shape[0]))
    print ("#Test pairs: {}".format(x_test.shape[0]))

    freqs = pd.Series(y_train).value_counts()
    print ('Train Class distribution: ')
    print (freqs)
    freqs = pd.Series(y_test).value_counts()
    print ('Test Class distribution: ')
    print (freqs)

    x_train, x_val, y_train, y_val = train_test_split(x_train, y_train, test_size=0.1)
    print ("Training set: {}, {}".format(x_train.shape, y_train.shape))
    print ("Valid set: {}, {}".format(x_val.shape, y_val.shape))
    print ("Test set: {}, {}".format(x_test.shape, y_test.shape))

    model = complete_model(rcts)
    model.compile(optimizer='rmsprop',
              loss='binary_crossentropy',
              metrics=['accuracy'])
    model.summary()

    model.fit([x_train[:, 0], x_train[:, 1]], y_train,
          batch_size=BATCH_SIZE,
          epochs=EPOCHS,
          validation_data=([x_val[:, 0], x_val[:, 1]], y_val),
          verbose=True
         )

    #model.save_weights("pairwise-ov-comp-model.h5")

    # compute final accuracy on test set
    loss, acc = model.evaluate([x_test[:, 0], x_test[:, 1]], y_test, verbose=True)
    print ('Test Accuracy: {}'.format(acc))

if __name__ == "__main__":
    main(sys.argv[1:])
 
