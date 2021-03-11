#Load datasets and embedded vectors
def loadData(inpH, emb_file, train_file, test_file, numClasses=0):
    
    #Load the training and the test sets
    #Load the text as a sequence of inputs
    
    x_train, y_train = inpH.getSequenceData(train_file, numClasses)
    x_test, y_test = inpH.getSequenceData(test_file, numClasses)

    #First few sequences
    for i in range(4):
        print ("x[{}][:5]..., y[{}] = {}, {}".format(i, i, x_train[i][:5], y_train[i]))    
        
    if (numClasses > 0):    
        encoder = OneHotEncoder(sparse=False, categories='auto')
        
        #y_all = np.vstack((y_train, y_test))
        y_all = np.append(y_train, y_test)

        encoder.fit(y_all.reshape(-1,1))

        y_train = encoder.transform(y_train.reshape(-1, 1))
        y_test = encoder.transform(y_test.reshape(-1, 1))
        
        for i in range(2):
            print ("y_train[{}] = {}".format(i, y_train[i]))
            print ("y_test[{}] = {}".format(i, y_test[i]))
            
    #Load the word vectors
    inpH.loadW2V(emb_file)
    
    #Print the loaded words
    nwords=0
    for w in inpH.pre_emb:
        print ("Dimension of vectors: {}".format(inpH.pre_emb[w].shape))
        print ("{} {}".format(w, inpH.pre_emb[w][0:5]))
        nwords = nwords+1
        if (nwords >= 2): break

    print ("vocab size: {}".format(inpH.vocab_size))
    print ("emb-matrix: {}...".format(inpH.embedding_matrix[1][:5]))
    print (inpH.embedding_matrix.shape)
    
    return x_train, y_train, x_test, y_test
