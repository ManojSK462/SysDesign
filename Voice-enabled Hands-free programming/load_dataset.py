from tqdm import tqdm
import requests
import math
import os
import tarfile
import numpy as np
import librosa
import pandas as pd
import audioUtils


def PrepareGoogleSpeechCmd(version=2, forceDownload=False, task='20cmd'):
    
    totality = ['12cmd', 'leftright', '35word', '20cmd']
    if task not in totality:
        raise Exception(totality)

    basePath = None
    if version == 2:
        _DownloadGoogleSpeechCmdV2(forceDownload)
        basePath = 'sd_GSCmdV2'
    elif version == 1:
        _DownloadGoogleSpeechCmdV1(forceDownload)
        basePath = 'sd_GSCmdV1'
    else:
        raise Exception('Version must be 1 or 2')

    if task == '12cmd':
        classess = {
            'unknown': 0,
            'silence': 1,
            '_unknown_': 0,
            '_silence_': 1,
            '_background_noise_': 1,
            'yes': 2,
            'no': 3,
            'up': 4,
            'down': 5,
            'left': 6,
            'right': 7,
            'on': 8,
            'off': 9,
            'stop': 10,
            'go': 11}
        numclassess = 12
    elif task == 'leftright':
        classess = {
            'unknown': 0,
            'silence': 0,
            '_unknown_': 0,
            '_silence_': 0,
            '_background_noise_': 0,
            'left': 1,
            'right': 2}
        numclassess = 3
    elif task == '35word':
        classess = {
            'unknown': 0,
            'silence': 0,
            '_unknown_': 0,
            '_silence_': 0,
            '_background_noise_': 0,
            'yes': 2,
            'no': 3,
            'up': 4,
            'down': 5,
            'left': 6,
            'right': 7,
            'on': 8,
            'off': 9,
            'stop': 10,
            'go': 11,
            'zero': 12,
            'one': 13,
            'two': 14,
            'three': 15,
            'four': 16,
            'five': 17,
            'six': 18,
            'seven': 19,
            'eight': 20,
            'nine': 1,
            'backward': 21,
            'bed': 22,
            'bird': 23,
            'cat': 24,
            'dog': 25,
            'follow': 26,
            'forward': 27,
            'happy': 28,
            'house': 29,
            'learn': 30,
            'marvin': 31,
            'sheila': 32,
            'tree': 33,
            'visual': 34,
            'wow': 35}
        numclassess = 36
    elif task == '20cmd':
        classess = {
            'unknown': 0,
            'silence': 0,
            '_unknown_': 0,
            '_silence_': 0,
            '_background_noise_': 0,
            'yes': 2,
            'no': 3,
            'up': 4,
            'down': 5,
            'left': 6,
            'right': 7,
            'on': 8,
            'off': 9,
            'stop': 10,
            'go': 11,
            'zero': 12,
            'one': 13,
            'two': 14,
            'three': 15,
            'four': 16,
            'five': 17,
            'six': 18,
            'seven': 19,
            'eight': 20,
            'nine': 1}
        numclassess = 21

    print('Converting test set ')
    audioUtils.WAV2Numpy(basePath + '/test/')
    print('Converting training set WAVs')
    audioUtils.WAV2Numpy(basePath + '/train/')
    tests = pd.read_csv(basePath + '/train/testing_list.txt',
                           sep=" ", header=None)[0].tolist()
    validation = pd.read_csv(basePath + '/train/validation_list.txt',
                          sep=" ", header=None)[0].tolist()

    tests = [os.path.join(basePath + '/train/', f + '.npy')
                for f in tests if f.endswith('.wav')]
    validation = [os.path.join(basePath + '/train/', f + '.npy')
               for f in validation if f.endswith('.wav')]
    allWAVs = []
    for root, dirs, files in os.walk(basePath + '/train/'):
        allWAVs += [root + '/' + f for f in files if f.endswith('.wav.npy')]
    trains = list(set(allWAVs) - set(validation) - set(tests))

    testsREAL = []
    for root, dirs, files in os.walk(basePath + '/test/'):
        testsREAL += [root + '/' +
                         f for f in files if f.endswith('.wav.npy')]

    # get categories
    T2_labels = [_getFileCategory(f, classess) for f in tests]
    valWAVlabels = [_getFileCategory(f, classess) for f in validation]
    T1_labels = [_getFileCategory(f, classess) for f in trains]
    testWAVREALlabels = [_getFileCategory(f, classess)
                         for f in testsREAL]

    
    backNoiseFiles = [trains[i] for i in range(len(T1_labels))
                      if T1_labels[i] == classess['silence']]
    backNoiseCats = [classess['silence']
                     for i in range(len(backNoiseFiles))]
    if numclassess == 12:
        validation += backNoiseFiles
        valWAVlabels += backNoiseCats

    
    T2_labelsDict = dict(zip(tests, T2_labels))
    valWAVlabelsDict = dict(zip(validation, valWAVlabels))
    T1_labelsDict = dict(zip(trains, T1_labels))
    testWAVREALlabelsDict = dict(zip(testsREAL, testWAVREALlabels))

    
    trainInfo = {'files': trains, 'labels': T1_labelsDict}
    valInfo = {'files': validation, 'labels': valWAVlabelsDict}
    testInfo = {'files': tests, 'labels': T2_labelsDict}
    testREALInfo = {'files': testsREAL, 'labels': testWAVREALlabelsDict}
    gscInfo = {'train': trainInfo,
               'test': testInfo,
               'val': valInfo,
               'testREAL': testREALInfo}

    print('Done preparing Google Speech commands dataset version {}'.format(version))

    return gscInfo, numclassess


def _getFileCategory(file, catDict):
    
    categ = os.path.basename(os.path.dirname(file))
    return catDict.get(categ, 0)


def _DownloadGoogleSpeechCmdV2(forceDownload=False):
    
    if os.path.isdir("sd_GSCmdV2/") and not forceDownload:
        print('Google Speech commands dataset version 2 already exists. Skipping download.')
    else:
        if not os.path.exists("sd_GSCmdV2/"):
            os.makedirs("sd_GSCmdV2/")
        trainFiles = 'http://download.tensorflow.org/data/speech_commands_v0.02.tar.gz'
        testFiles = 'http://download.tensorflow.org/data/speech_commands_test_set_v0.02.tar.gz'
        _downloadFile(testFiles, 'sd_GSCmdV2/test.tar.gz')
        _downloadFile(trainFiles, 'sd_GSCmdV2/train.tar.gz')

    
    if not os.path.isdir("sd_GSCmdV2/test/"):
        _extractTar('sd_GSCmdV2/test.tar.gz', 'sd_GSCmdV2/test/')

    if not os.path.isdir("sd_GSCmdV2/train/"):
        _extractTar('sd_GSCmdV2/train.tar.gz', 'sd_GSCmdV2/train/')


def _DownloadGoogleSpeechCmdV1(forceDownload=False):
    
    if os.path.isdir("sd_GSCmdV1/") and not forceDownload:
        print('Google Speech commands dataset version 1 already exists. Skipping download.')
    else:
        if not os.path.exists("sd_GSCmdV1/"):
            os.makedirs("sd_GSCmdV1/")
        trainFiles = 'http://download.tensorflow.org/data/speech_commands_v0.01.tar.gz'
        testFiles = 'http://download.tensorflow.org/data/speech_commands_test_set_v0.01.tar.gz'
        _downloadFile(testFiles, 'sd_GSCmdV1/test.tar.gz')
        _downloadFile(trainFiles, 'sd_GSCmdV1/train.tar.gz')

    if not os.path.isdir("sd_GSCmdV1/test/"):
        _extractTar('sd_GSCmdV1/test.tar.gz', 'sd_GSCmdV1/test/')

    if not os.path.isdir("sd_GSCmdV1/train/"):
        _extractTar('sd_GSCmdV1/train.tar.gz', 'sd_GSCmdV1/train/')




def _downloadFile(url, fName):
    r = requests.get(url, stream=True)
    total_size = int(r.headers.get('content-length', 0))
    block_size = 1024
    wrote = 0
    print('Downloading {} into {}'.format(url, fName))
    with open(fName, 'wb') as f:
        for data in tqdm(r.iter_content(block_size),
                         total=math.ceil(total_size // block_size),
                         unit='KB',
                         unit_scale=True):
            wrote = wrote + len(data)
            f.write(data)
    if total_size != 0 and wrote != total_size:
        print(" ")


def _extractTar(fname, folder):
    print('Extracting {} into {}'.format(fname, folder))
    if (fname.endswith("tar.gz")):
        tar = tarfile.open(fname, "r:gz")
        tar.extractall(path=folder)
        tar.close()
    elif (fname.endswith("tar")):
        tar = tarfile.open(fname, "r:")
        tar.extractall(path=folder)
        tar.close()
