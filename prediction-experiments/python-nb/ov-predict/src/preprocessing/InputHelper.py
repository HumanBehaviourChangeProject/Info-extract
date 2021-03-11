from sklearn.preprocessing import OneHotEncoder
import numpy as np
from keras.preprocessing.text import Tokenizer
from keras.preprocessing.text import text_to_word_sequence
from scipy import stats

NUM_VALS = 5
PUBMED_VEC_SIZE = 200

def mapToUniformlySpacedIntervals(y_i, numClasses):
    MAX = 100
    DELTA = MAX/numClasses
    y_i = float(y_i)
    y_i = int(y_i/DELTA)
    return y_i

def mapToNonUniformlySpacedIntervals(y, cutoffs):
    #cutoffs is a precomputed list of cutoff points based on the y_train distribution
    #the intervals are: [0, cutoff_0) [cutoff_0, cutoff_1).... [cutoff_n, 100]
    interval = 0
    for cutoff in cutoffs:
        if float(y) < float(cutoffs[interval]):
            return interval
        interval+=1
        
    return len(cutoffs)

def appendvec(vec, value):
    val = '0'
    if value.isnumeric():
        val = value

    appended_vec = vec + ' '
    for i in range(PUBMED_VEC_SIZE):
        appended_vec += '0 '

    for i in range(NUM_VALS):
        appended_vec += val
        appended_vec += ' '

    appended_vec = appended_vec[0:-1]
    return appended_vec

def vec_to_str(vec):
    vecbuff = ''
    for x in vec:
        vecbuff += str(x) + ' '
    vecbuff = vecbuff[0:-1]
    return vecbuff

def mapToNonUniformlySpacedIntervals_Fixed(y_i):
    #[0,5] [5,10] [10, 15] [15,20] [20,30] [30,50] [50,100]
    y_i = float(y_i)
    if y_i < 5:
        y_i = 0
    elif y_i>=5 and y_i<10:
        y_i = 1
    elif y_i>=10 and y_i<15:
        y_i = 2
    elif y_i>=15 and y_i<20:
        y_i = 3
    elif y_i>=20 and y_i<30:
        y_i = 4
    elif y_i>=30 and y_i<50:
        y_i = 5
    else:
        y_i = 6

    return y_i

class InputHelper(object):
    emb_dim = 0
    pre_emb = dict() # word--> vec
    vocab_size = 0
    tokenizer = None
    embedding_matrix = None
    
    def cleanText(self, s):
        s = re.sub(r"[^\x00-\x7F]+"," ", s)
        s = re.sub(r'[\~\!\`\^\*\{\}\[\]\#\<\>\?\+\=\-\_\(\)]+',"",s)
        s = re.sub(r'( [0-9,\.]+)',r"\1 ", s)
        s = re.sub(r'\$'," $ ", s)
        s = re.sub('[ ]+',' ', s)
        return s.lower()

    #the tokenizer needs to be trained on the pre-trained node vectors
    #join the names of the nodes in a string so that tokenizer could be fit on it
    def getAllNodes(self, emb_path):
        print("Collecting node names...")
        line_count = 0        
        node_names = []
        for line in open(emb_path):
            l = line.strip().split()
            if (line_count > 0):
                node_names.append(l[0])
            
            line_count = line_count + 1
        
        self.vocab_size = line_count # includes the +1
        print("Collected node names...")
        return node_names

    # call convertWordsToIds first followed by loadW2V
    def convertWordsToIds(self, emb_path):
        allNodeNames = self.getAllNodes(emb_path)
        
        print ("Converting words to ids...")
        # Map words to ids        
        self.tokenizer = Tokenizer(num_words=self.vocab_size, filters=[], lower=False, split=" ")
        self.tokenizer.fit_on_texts(allNodeNames)

        self.id2word = dict(map(reversed, self.tokenizer.word_index.items()))
        print ("Finished converting words to ids...")
    
    # Assumes that the tokenizer already has been fit on some text (for the time being the node vec names)
    def loadW2V(self, emb_path, include_wordvecs=False):
        print("Loading W2V data...")
        line_count = 0
        found = 0

        for line in open(emb_path):
            l = line.strip().split()
            if (line_count == 0): # the first line -- supposed to be <vocab-size> <dimension>
                self.emb_dim = int(l[1])
                #Initialize the embedding matrix...
                self.embedding_matrix = np.zeros((self.vocab_size, self.emb_dim))
            else:
                try:
                    st = l[0]
                    self.pre_emb[st] = np.asarray(l[1:]) # rest goes as the vector components

                    if st in self.tokenizer.word_index:
                        idx = self.tokenizer.word_index[st]
                        self.embedding_matrix[idx] = np.array(l[1:], dtype=np.float32)[:self.emb_dim]
                        found += 1
                    else:
                        print ("Word '{}' not found in vocabulary..".format(st))
                except ValueError:
                    print ("skipping word {}".format(st))
                
            line_count = line_count + 1
            
        print("loaded word2vec for {} nodes".format(len(self.pre_emb)))
        print ("{} words out of {} not found".format(line_count - found, line_count))
        print ("DEBUG: shape of embedding: {}".format(self.embedding_matrix.shape))
        print ("DEBUG: include_wordvecs = {}".format(include_wordvecs))

    # Override the vectors during API call - append 0's for the missing text part and the values for activation
    # This function first loads the vectors (w/o contexts) from refVecs.vec file
    # For the context part (200 dimensions), we append 0 and for the value part (5 dimensions), we
    # append the numeric value
    #
    def modifyW2V(self, testinstance_encoding):
        print("Modifying W2V data for API instantiation...")
    
        tokens = testinstance_encoding.split(' ')
        for token in tokens:
            if not token in self.tokenizer.word_index:
                print('token {} OOV'.format(token))
                continue

            vec = self.pre_emb[token]
            parts = token.split(':')
            value = parts[-1]
            appended_vec = ''

            vec = vec_to_str(vec)
            print (vec)
            appended_vec = appendvec(vec, value)
            idx = self.tokenizer.word_index[token]

            appended_vec_numeric = [float(x) for x in appended_vec.split()]
            print (self.embedding_matrix.shape)
            self.embedding_matrix[idx] = np.asarray(appended_vec_numeric, dtype=np.float32)
    
    # Load the data as two matrices - X and Y
    def getTsvData(self, filepath):
        print("Loading data from " + filepath)
        x = []
        y = []
        
        # positive samples from file
        for line in open(filepath):
            l = line.strip().split("\t")
            if float(l[1]) > 0:            
                y.append(l[1])
                words = l[0].split(" ")
                x.append(words)
            
        return np.asarray(x), np.asarray(y)
    
    # Build sequences from each data instance
    def getSequenceData(self, tsvDataPath):
        x_text, y = self.getTsvData(tsvDataPath)
        
        # Convert each sentence (node name sequence) to a sequence of integer ids
        x = self.tokenizer.texts_to_sequences(x_text)
        
        return x, np.asarray(y)
    
    def loadData(self, emb_file, train_file, test_file=None):
    
        #Load the training and the test sets
        #Load the text as a sequence of inputs
    
        x_train, y_train = self.getSequenceData(train_file)
        x_test = None
        y_test = None
        if not test_file == None:
            x_test, y_test = self.getSequenceData(test_file)

        #First few sequences
        #for i in range(4):
        #    print ("x[{}][:5]..., y[{}] = {}, {}".format(i, i, x_train[i][:5], y_train[i]))    
       
        '''
        This is most likely not reqd. here... TODO: Revisit. OneHotEncoder is called in transformLabels 
        if (numClasses > 0):    
            encoder = OneHotEncoder(sparse=False, categories='auto')
       
            if y_test==None:
                y_all = np.array(y_train)
            else: 
                y_all = np.append(y_train, y_test)

            encoder.fit(y_all.reshape(-1,1))

            y_train = encoder.transform(y_train.reshape(-1, 1))
            if not y_test == None:
                y_test = encoder.transform(y_test.reshape(-1, 1))
       
            for i in range(2):
                print ("y_train[{}] = {}".format(i, y_train[i]))
                #print ("y_test[{}] = {}".format(i, y_test[i]))
        '''
            
        #Load the word vectors
        self.loadW2V(emb_file)
    
        #Print the loaded words
        nwords=0
        for w in self.pre_emb:
            print ("Dimension of vectors: {}".format(self.pre_emb[w].shape))
            print ("{} {}".format(w, self.pre_emb[w][0:5]))
            nwords = nwords+1
            if (nwords >= 2): break

        print ("vocab size: {}".format(self.vocab_size))
        print ("emb-matrix: {}...".format(self.embedding_matrix[1][:5]))
        print (self.embedding_matrix.shape)
    
        return x_train, y_train, x_test, y_test

    #Load data from tsv file with fold info
    def loadDataWithFolds(self, data_file):
        x, y = self.getSequenceData(data_file)
        return x, y

def get_percentile_cutoffs(yvals, numClasses):
    pcntile_pt = 0
    i=0
    cutoffs = []
    yvals = [float(i) for i in yvals]

    while i < numClasses-1:
        pcntile_pt += 100/numClasses
        cutoff = np.percentile(yvals, int(pcntile_pt))
        cutoffs.append(cutoff)
        i+=1
        
    return cutoffs

def print_counts(cutoffs, yvals):
    cutoffs.append(100) # max bound
    num_intervals = len(cutoffs) 
    for i in range(1, num_intervals):
        count = len(list(filter(lambda x: float(cutoffs[i-1]) <= x and x < float(cutoffs[i]), yvals)))
        print ('{} {}'.format(i-1, count))

def transformLabels_aux(y_vals, numClasses, useMedians=False):
    y_classes = []

    #print ('size(y_vals)= {}'.format(len(y_vals)))
    #print (y_vals)
    if useMedians==False:
        cutoffs = [5, 10, 15, 20, 30, 50]  # hard-coded into 7 classes
    else:
        cutoffs = get_percentile_cutoffs(y_vals, numClasses)         

    for y_val in y_vals:
        y_classes.append(mapToNonUniformlySpacedIntervals(y_val, cutoffs))

    return y_classes

def transformLabels(y_train, y_test, numClasses=0, useMedians=False):
    y_test_classes = None
    if y_test==None:
        y_test = []
    if numClasses == 0:
        if len(y_test)==0:
            return np.asarray(y_train), None
        return np.asarray(y_train), np.asarray(y_test)

    y_train_classes = transformLabels_aux(y_train, numClasses, useMedians)
    if len(y_test)>0:
        y_test_classes = transformLabels_aux(y_test, numClasses, useMedians)

    y_train_classes = np.asarray(y_train_classes)
    y_test_classes = np.asarray(y_test_classes)

    print ("Training label dist:")
    print_counts(list(range(0, numClasses)), y_train_classes)
    if len(y_test)>0:
        print ("Test label dist:")
        print_counts(list(range(0, numClasses)), y_test_classes)

    if len(y_test)>0:
        y_all = np.append(np.asarray(y_train_classes), np.asarray(y_test_classes))
    else:
        y_all = np.asarray(y_train_classes)

    encoder = OneHotEncoder(sparse=False, categories='auto')
    encoder.fit(y_all.reshape(-1, 1))

    # change the variables that were passed as arguments...
    y_train = encoder.transform(np.asarray(y_train_classes).reshape(-1, 1))
    if len(y_test)>0:
        y_test = encoder.transform(np.asarray(y_test_classes).reshape(-1, 1))

    return y_train, y_test
