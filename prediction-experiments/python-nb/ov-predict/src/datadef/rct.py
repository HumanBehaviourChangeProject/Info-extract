from keras.preprocessing.text import Tokenizer
from keras.preprocessing.sequence import pad_sequences
from collections import defaultdict
import random as r
import math as m
import numpy as np
from keras import backend as K
from random import Random

INTERVENTION_SIM_THRESHOLD=0.1
MAXLEN = 50
SEED=314159 # first digits of Pi... an elegant seed!

class RCTArm:
    def __init__(self, line, index):
        l = line.strip().split("\t")
        self.docname = ''
        self.id = index
        self.text = l[0]
        self.ov = float(l[1])
        if len(l) >= 3:
            self.docname = l[2]
        
        self.makeInterventionVec()
        
    def __str__(self):
        return 'RCT:({}) OV: {}'.format(self.text[0:10], self.ov)
    
    def rct_2_ivec(self):
        ivec = ""
        for intervention in self.interventions:
            ivec = ivec + " " + intervention
        return ivec
    
    # similarity between RCTs is checked at the level of interventions only
    # this is a quick fix w/o changing the Java code
    def makeInterventionVec(self):
        self.interventions = []
        words = self.text.split(" ")
        for word in words:
            tokens = word.split(":")
            isIntervention = False
            if tokens[0]=='I':
                isIntervention = True
            if isIntervention==False:
                continue
            self.interventions.append(tokens[1])
        self.interventions = set(self.interventions)
            
    def ivec_sim(self, other):
        commonInterventions = self.interventions.intersection(other.interventions)
        unionInterventions = set().union(self.interventions, other.interventions)
        i = len(commonInterventions)
        u = len(unionInterventions)
        if u==0:
            return 0
        return i/u

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
    
    def getRCT(self, index):
        return self.rcts[index]
        
    def getData(self, index):
        return self.x[index]
    
    def computePairwiseSims(self):
        self.rct_sims = np.zeros((len(rcts.rcts), len(rcts.rcts)))
        
        # compute pairwise similarities
        for i in range(len(rcts.rcts)-1):
            rct_a = self.getRCT(i)
            for j in range(i+1, len(rcts.rcts)):
                rct_b = self.getRCT(j)
                rct_a.ivec_sim(rct_b)
                self.rct_sims[i][j] = rct_a.ivec_sim(rct_b)
            
    def create_pairs(self, intervention_sim_threshold=INTERVENTION_SIM_THRESHOLD):
        '''Positive and negative pair creation.
        Alternates between positive and negative pairs.
        '''
        pairs = []
        labels = []
        
        # compute pairwise sims so that we could threshold on them...
        self.computePairwiseSims()        
        
        N = len(self.rcts)
        for i in range (N-1):
            for j in range(i+1,N):
                if self.rct_sims[i][j] < intervention_sim_threshold:
                    continue
                pairs.append([self.x[i], self.x[j]])
                label = 0                
                if self.rcts[i].ov >= self.rcts[j].ov:
                    label = 1
                labels.append(label)
                
        return np.array(pairs), np.array(labels)    
    
    # Cartesian product between two sets
    def create_pairs_from_ids(self, listA, listB, intervention_sim_threshold=INTERVENTION_SIM_THRESHOLD):
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
                
                if self.rct_sims[a][b] < intervention_sim_threshold:
                    continue
                
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
        
        # compute pairwise sims so that we could threshold on them...
        self.computePairwiseSims()        
                
        #collect all pairs from the train ids
        train_pairs, train_labels = self.create_pairs_from_ids(train_ids, train_ids)
        
        #collect all pairs from the test ids - complete subgraph
        self_test_pairs, self_test_labels = self.create_pairs_from_ids(test_ids, test_ids)
        
        # additionally build the cross-pairs, (test, train) pairs
        cross_test_pairs, cross_test_labels = self.create_pairs_from_ids(test_ids, train_ids)
        
        return np.array(train_pairs), np.array(train_labels), np.array(self_test_pairs), np.array(self_test_labels), np.array(cross_test_pairs), np.array(cross_test_labels)
