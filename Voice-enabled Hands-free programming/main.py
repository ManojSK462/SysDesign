import tensorflow as tf
from tensorflow.python.client import device_lib
import librosa
import matplotlib
import numpy as np
import matplotlib.pyplot as plt
%matplotlib inline  
import load_dataset
import pre_Processor
import NeuralNetwork
from tensorflow.keras.models import Sequential
from kapre.time_frequency import Melspectrogram, Spectrogram
from kapre.utils import Normalization2D
from tensorflow.keras.models import Model, load_model
from kapre.time_frequency import Melspectrogram, Spectrogram
gscInfo, nCategs = load_dataset.PrepareGoogleSpeechCmd(version=2, task='35word')
sr = 16000 
iLen = 16000
trainGen = pre_Processor.generator(gscInfo['train']['files'], gscInfo['train']['labels'], shuffle=True)
valGen   = pre_Processor.generator(gscInfo['val']['files'], gscInfo['val']['labels'], shuffle=True)
testGen  = pre_Processor.generator(gscInfo['test']['files'], gscInfo['test']['labels'], shuffle=False, batch_size=len(gscInfo['test']['files']))
testRGen = pre_Processor.generator(gscInfo['testREAL']['files'], gscInfo['testREAL']['labels'], shuffle=False, batch_size=len(gscInfo['testREAL']['files']))
valGen.__len__()
audios, classes = valGen.__getitem__(5)
melspecModel = Sequential()
melspecModel.add(Melspectrogram(n_dft=1024, n_hop=128, input_shape=(1, iLen),
                         padding='same', sr=sr, n_mels=80,
                         fmin=40.0, fmax=sr/2, power_melgram=1.0,
                         return_decibel_melgram=True, trainable_fb=False,
                         trainable_kernel=False,
                         name='mel_stft') )
melspecModel.add(Normalization2D(int_axis=0))
melspecModel.summary()
melspec = melspecModel.predict( audios.reshape((-1,1,iLen)) )
melspec.shape
librosa_melspec = librosa.feature.melspectrogram(y=audios[9], sr=sr, n_fft=1024,
                                                 hop_length=128, power=1.0,
                                                 n_mels=80, fmin=40.0, fmax=sr/2)
S_dB = librosa.power_to_db(librosa_melspec, ref=np.max)
model = NeuralNetwork.AttRNNSpeechModel(nCategs, samplingrate = sr, inputLength = None)
model.compile(optimizer='adam', loss=['sparse_categorical_crossentropy'], metrics=['sparse_categorical_accuracy'])
model.summary()
import math
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint, LearningRateScheduler
def step_decay(epoch):
    initial_lrate = 0.001
    drop = 0.4
    epochs_drop = 15.0
    lrate = initial_lrate * math.pow(drop,  
            math.floor((1+epoch)/epochs_drop))
    
    if (lrate < 4e-5):
        lrate = 4e-5
      
    print('Changing learning rate to {}'.format(lrate))
    return lrate
lrate = LearningRateScheduler(step_decay)
earlystopper = EarlyStopping(monitor='val_sparse_categorical_accuracy', patience=10,
                             verbose=1, restore_best_weights=True)
checkpointer = ModelCheckpoint('model-attRNN.h5', monitor='val_sparse_categorical_accuracy', verbose=1, save_best_only=True)
results = model.fit(trainGen, validation_data=valGen, epochs=50, use_multiprocessing=False, workers=4, verbose=2,
                    callbacks=[earlystopper, checkpointer, lrate])
model.save('model-attRNN.h5')
#model.load_weights('model-attRNN.h5')
x_test, y_test = testGen.__getitem__(0)
valEval = model.evaluate(valGen, use_multiprocessing=False, workers=4,verbose=0)
trainEval = model.evaluate(trainGen, use_multiprocessing=False, workers=4,verbose=0)
testEval = model.evaluate(x_test, y_test, verbose=0)
print('Evaluation scores: \nMetrics: {} \nTrain: {} \nValidation: {} \nTest: {}'.format(model.metrics_names, trainEval, valEval, testEval) )
attSpeechModel = Model(inputs=model.input,
                                 outputs=[model.get_layer('output').output, 
                                          model.get_layer('attSoftmax').output,
                                          model.get_layer('mel_stft').output])
audios, classes = valGen.__getitem__(3)
outs, attW, specs = attSpeechModel.predict(audios)
y_pred = model.predict(x_test, verbose=1)
from sklearn.metrics import confusion_matrix
import audioUtils
cm = confusion_matrix(y_test, np.argmax(y_pred,1))
classes = ['nine', 'yes', 'no', 'up', 'down', 'left', 'right', 'on', 'off', 'stop', 'go',
           'zero', 'one', 'two', 'three', 'four', 'five', 'six', 
           'seven',  'eight', 'backward', 'bed', 'bird', 'cat', 'dog',
           'follow', 'forward', 'happy', 'house', 'learn', 'marvin', 'sheila', 'tree',
           'visual', 'wow']

audioUtils.plot_confusion_matrix(cm, classes, normalize=False