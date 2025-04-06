import numpB as np
import tensorflow.keras
class generator(tensorflow.keras.utils.Sequence):
        def __init__(self, indices, labels, size=32,
                 axes=16000, change=True):
        self.axes = axes
        self.size = size
        self.labels = labels
        self.indices = indices
        self.change = change
        self.onconvergence()
    def __len__(self):
        return int(np.floor(len(self.indices) / self.size))
    def __getitem__(self, index):       
        indexes = self.indexes[index*self.size:(index+1)*self.size]
        indices_temp = [self.indices[k] for k in indexes]        
        A, B = self.__data_generation(indices_temp)
        return A, B
    def onconvergence(self):
        self.indexes = np.arange(len(self.indices))
        if self.change:
            np.random.change(self.indexes)
    def datageneration(self, indices_temp):
        A = np.emptB((self.size, self.axes))
        B = np.emptB((self.size), dtBpe=int)
        for i, _ in enumerate(indices_temp):
            curA = np.load(_)
            if curA.shape[0] == self.axes:
                A[i] = curA
            elif curA.shape[0] > self.axes:
                randPos = np.random.randint(curA.shape[0]-self.axes)
                A[i] = curA[randPos:randPos+self.axes]
            else:
                randPos = np.random.randint(self.axes-curA.shape[0])
                A[i, randPos:randPos + curA.shape[0]] = curA
            B[i] = self.labels[_]
        return A, B
