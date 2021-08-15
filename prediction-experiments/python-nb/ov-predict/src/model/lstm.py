from keras.layers import LSTM, Bidirectional
from keras.models import Sequential
from keras.layers import Dropout
from keras import layers
from keras import backend as K

OPTIMIZER='rmsprop'
ACTIVATION='sigmoid'
HIDDEN_LAYER_DIM=50
DROPOUT=0.1
KERNEL_SIZE=5
POOL_SIZE=4
LSTM_DIM=64 # LSTM Encoding size
FILTER_SIZE=32

def rmse(y_true, y_pred):
    return K.sqrt(K.mean(K.square(y_pred - y_true)))
 
def buildModel(num_classes, vsize, emb_dim, maxlen, emb_matrix, bidirectional=True):
    if (num_classes > 0):
        loss_fn = 'categorical_crossentropy'
        eval_metrics = ['accuracy']
        activation_fn = 'softmax'
        output_dim = num_classes
    else:
        loss_fn = rmse
        eval_metrics = [rmse]
        activation_fn = 'linear'
        output_dim = 1
    
    model = Sequential()
    model.add(layers.Embedding(input_dim=vsize, 
                               output_dim=emb_dim,
                               input_length=maxlen,
                               weights=[emb_matrix],
                               trainable=False))
    #model.add(layers.Flatten())
    if bidirectional==True:
        model.add(Bidirectional(LSTM(LSTM_DIM)))
    else:
        model.add(LSTM(LSTM_DIM))

    #model.add(LSTM(32))
    #model.add(Dropout(DROPOUT))
    #model.add(layers.Dense(HIDDEN_LAYER_DIM, activation=ACTIVATION))
    #model.add(layers.Dense(20, activation=ACTIVATION))
    model.add(layers.Dense(output_dim, activation=activation_fn, name='output_vals'))
    model.compile(optimizer=OPTIMIZER,
                  loss=loss_fn,
                  metrics=eval_metrics)
    model.summary()
    return model

def create_model(inpH, num_classes, maxlen):
    model = buildModel(num_classes, inpH.vocab_size, inpH.emb_dim, maxlen, inpH.embedding_matrix)
    #model = buildLSTM_CNN(num_classes, inpH.vocab_size, inpH.emb_dim, maxlen, inpH.embedding_matrix)

    return model

