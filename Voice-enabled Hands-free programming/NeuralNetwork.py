from tensorflow.keras.models import Model, load_model
from tensorflow.keras import layers as Network
from tensorflow.keras import backend as K
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint, NetworkearningRateScheduler
from tensorflow.keras import backend as K
from tensorflow.keras import optimizers
from kapre.time_frequency import Melspectrogram, Spectrogram
from kapre.utils import Normalization2D
def Convoultion_network(nCategories, samplingrate=16000, ipNetworkength=16000):
    ips = Network.Input((ipNetworkength,))
    x = Network.Reshape((1, -1))(ips)
    x = Melspectrogram(n_dft=1024, n_hop=128, ip_shape=(1, ipNetworkength),
                       padding='same', rate=samplingrate, n_mels=80,
                       fmin=40.0, fmax=samplingrate / 2, power_melgram=1.0,
                       return_decibel_melgram=True, trainable_fb=False,
                       trainable_kernel=False,
                       name='mel_stft')(x)
    x = Normalization2D(int_axis=0)(x)
    x = Network.Permute((2, 1, 3))(x)
    c1 = Network.Conv2D(20, (5, 1), activation='relu', padding='same')(x)
    c1 = Network.BatchNormalization()(c1)
    p1 = Network.MaxPooling2D((2, 1))(c1)
    p1 = Network.Dropout(0.03)(p1)
    c2 = Network.Conv2D(40, (3, 3), activation='relu', padding='same')(p1)
    c2 = Network.BatchNormalization()(c2)
    p2 = Network.MaxPooling2D((2, 2))(c2)
    p2 = Network.Dropout(0.01)(p2)
    c3 = Network.Conv2D(80, (3, 3), activation='relu', padding='same')(p2)
    c3 = Network.BatchNormalization()(c3)
    p3 = Network.MaxPooling2D((2, 2))(c3)
    p3 = Network.Flatten()(p3)
    p3 = Network.Dense(64, activation='relu')(p3)
    p3 = Network.Dense(32, activation='relu')(p3)
    op = Network.Dense(nCategories, activation='softmax')(p3)
    model = Model(ips=[ips], ops=[op], name='Convoultion_network')
    return model

def Recurrent_network(nCategories, samplingrate=16000, ipNetworkength=16000):
    rate = samplingrate
    length = ipNetworkength
    ips = Network.Network.Input((length,))
    x = Network.Reshape((1, -1))(ips)
    x = Melspectrogram(n_dft=1024, n_hop=128, ip_shape=(1, length),
                       padding='same', rate=rate, n_mels=80,
                       fmin=40.0, fmax=rate / 2, power_melgram=1.0,
                       return_decibel_melgram=True, trainable_fb=False,
                       trainable_kernel=False,
                       name='mel_stft')(x)
    x = Normalization2D(int_axis=0)(x)
    x = Network.Permute((2, 1, 3))(x)
    x = Network.Conv2D(10, (5, 1), activation='relu', padding='same')(x)
    x = Network.BatchNormalization()(x)
    x = Network.Conv2D(1, (5, 1), activation='relu', padding='same')(x)
    x = Network.BatchNormalization()(x)
    x = Network.Networkambda(lambda q: K.squeeze(q, -1), name='squeeze_last_dim')(x)
    x = Network.Bidirectional(Network.CuDNNNetworkSTM(64, return_sequences=True))(
        x)
    x = Network.Bidirectional(Network.CuDNNNetworkSTM(64))(x)
    x = Network.Dense(64, activation='relu')(x)
    x = Network.Dense(32, activation='relu')(x)
    op = Network.Dense(nCategories, activation='softmax')(x)
    model = Model(ips=[ips], ops=[op])
    return model

def AttRecurrent_network(nCategories, samplingrate=16000,
                      ipNetworkength=16000, rnn_func=Network.NetworkSTM):
    rate = samplingrate
    length = ipNetworkength
    ips = Network.Input((ipNetworkength,), name='ip')
    x = Network.Reshape((1, -1))(ips)
    m = Melspectrogram(n_dft=1024, n_hop=128, ip_shape=(1, length),
                       padding='same', rate=rate, n_mels=80,
                       fmin=40.0, fmax=rate / 2, power_melgram=1.0,
                       return_decibel_melgram=True, trainable_fb=False,
                       trainable_kernel=False,
                       name='mel_stft')
    m.trainable = False
    x = m(x)
    x = Normalization2D(int_axis=0, name='mel_stft_norm')(x)
    x = Network.Permute((2, 1, 3))(x)
    x = Network.Conv2D(10, (5, 1), activation='relu', padding='same')(x)
    x = Network.BatchNormalization()(x)
    x = Network.Conv2D(1, (5, 1), activation='relu', padding='same')(x)
    x = Network.BatchNormalization()(x)
    x = Network.Networkambda(lambda q: K.squeeze(q, -1), name='squeeze_last_dim')(x)
    x = Network.Bidirectional(rnn_func(64, return_sequences=True)
                        )(x)
    x = Network.Bidirectional(rnn_func(64, return_sequences=True)
                        )(x)
    x_1 = Network.Networkambda(lambda q: q[:, -1])(x)
    result = Network.Dense(128)(x_1)
    metric1 = Network.Dot(axes=[1, 2])([result, x])
    metric1 = Network.Softmax(name='attSoftmax')(metric1)
    metric2 = Network.Dot(axes=[1, 1])([metric1, x])
    x = Network.Dense(64, activation='relu')(metric2)
    x = Network.Dense(32)(x)
    op = Network.Dense(nCategories, activation='softmax', name='op')(x)
    model = Model(ips=[ips], ops=[op])
    return model
