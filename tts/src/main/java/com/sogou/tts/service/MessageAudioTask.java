// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sogou.tts.TTSPlayer;
import com.sogou.tts.TextModel;
import com.sogou.tts.setting.IRecordAudioConfig;
import com.sogou.tts.setting.ISettingConfig;
import com.sogou.tts.utils.ErrorIndex;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;


public class MessageAudioTask extends Thread implements ISettingConfig, IRecordAudioConfig {

    public static final int MESSAGE_PLAY = 1000;
    public static final int MESSAGE_QUIT = 1001;
    public static final int MESSAGE_RESUME = 1002;
    public static final int MESSAGE_PAUSE = 1003;
    public static final int MESSAGE_END = 1004;

    private Handler ttsHandler = null;
    private AudioTrack mAudioTrack = null;
    private int mPlayOffset = 0;
    private int minBufferSize = -1;
    private TTSPlayer ttsPlayer;
    private int streamType = AudioManager.STREAM_MUSIC;
    private int sampleRate = DEFAULT_HIGH_AUDIO_SAMPLE_RATE;
    private int channelConfig = MONO;
    private int audioFormat = PCM_16BIT;
    private boolean mCheckAvailable = false;
    private Handler mHandler = null;

    private float playProgress = 0;
    private String identifier;
    private TextModel textModel;

    BlockingDeque<byte[]> mAudioStreamQueue = new LinkedBlockingDeque<>();

    private boolean mSynthing = false;

    public MessageAudioTask(TTSPlayer ttsPlayer, Handler ttsHandler, int streamType) {
        super("MessageAudioTask");
        this.ttsHandler = ttsHandler;
        this.ttsPlayer = ttsPlayer;
        this.streamType = streamType;
        if (!onPrepare()) {
            mCheckAvailable = false;
            sendErrorMsg();
        } else {
            mCheckAvailable = true;
        }
    }


    public void setTextModel(TextModel textModel) {
        this.textModel = textModel;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void playBuffer(byte[] buffer){
        mAudioStreamQueue.offer(buffer);
        Message message = new Message();
        message.what = MESSAGE_PLAY;
        message.obj = buffer;
        if (mHandler!= null) {

            mHandler.sendMessage(message);
        }
    }

    public void onSynBegin() {
        mSynthing = true;
    }

    public void onSynEnd(){
        mSynthing = false;
//        if (mHandler!= null) {
//            Message message = mHandler.obtainMessage(MESSAGE_END);
//            mHandler.sendMessage(message);
//        }
    }

    public boolean onPrepare() {
        onRelease();
        if (minBufferSize <= 0) {
            minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    channelConfig, audioFormat);
            if (minBufferSize <= 0) {
                if (sampleRate == DEFAULT_HIGH_AUDIO_SAMPLE_RATE) {
                    sampleRate = DEFAULT_LOW_AUDIO_SAMPLE_RATE;
                    minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                            channelConfig, audioFormat);
                    if (minBufferSize <= 0) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        minBufferSize /= 2;

        mAudioTrack = new AudioTrack(streamType, DEFAULT_HIGH_AUDIO_SAMPLE_RATE, channelConfig,
                audioFormat, minBufferSize * 8, AudioTrack.MODE_STREAM);
        mAudioTrack.setPositionNotificationPeriod((int) (DEFAULT_PERIOD * IRecordAudioConfig.DEFAULT_HIGH_AUDIO_SAMPLE_RATE));
        mAudioTrack.setPlaybackPositionUpdateListener(
                new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack track) {

                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack arg0) {

                        sendOnPeriodicMsg();
                    }
                });
        if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            return false;
        }
        return true;
    }

    // audio release
    public void onRelease() {
        if(mLock.getCount() > 0){
            mLock.countDown();
        }
        if (mHandler!= null) {
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MESSAGE_QUIT));
        }
    }

    // audio stop
    public boolean onStop() {
        if(mLock.getCount() > 0){
            mLock.countDown();
        }
        if (mHandler!= null) {
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MESSAGE_QUIT));
        }

        return true;

    }

    // audio pause
    public boolean onPause() {
        if (mHandler!= null) {
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MESSAGE_PAUSE));
        }
        return true;

    }

    public boolean onResume() {
        if (mHandler!= null) {
            if(mLock.getCount() > 0){
                mLock.countDown();
            }
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MESSAGE_RESUME));
        }
        return true;
    }

    private boolean isStart = false;

    public void run() {
        Looper.prepare();
        if (!mCheckAvailable) {
            sendErrorMsg();
            return;
        }
        if (mAudioTrack == null) {
            sendErrorMsg();
            return;
        }
        try {
            mAudioTrack.play();
        } catch (Exception e) {
            sendErrorMsg();
        }
        mHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MESSAGE_END:
                        sendMessage(obtainMessage(MESSAGE_QUIT));
                        break;
                    case MESSAGE_PLAY:
                        byte[] result = (byte[]) msg.obj;
                        handleAudioPlay(result);
                        break;
                    case MESSAGE_PAUSE:
                        if (mAudioTrack != null
                                && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                            mAudioTrack.pause();
                            sendOnPauseMsg();
                        }
                        mLock = new CountDownLatch(1);
                        try {
                            mLock.await();
                        } catch (InterruptedException e) {
                        }
                        break;
                    case MESSAGE_RESUME:
                        mAudioTrack.play();
                        sendOnResumeMsg();
                        break;
                    case MESSAGE_QUIT:
                        if (mAudioTrack != null) {
                            mAudioTrack.pause();
                            mAudioTrack.flush();
                            mAudioTrack.release();
                            mAudioTrack = null;
                            sendOnStopMsg();
                        }
                        removeAllMessage();
                        playProgress = 0;
                        if (mHandler!= null) {
                            mHandler.getLooper().quit();
                        }
                        break;
                }

            }
        };
        Looper.loop();
    }

    private CountDownLatch mLock = new CountDownLatch(1);

    private void removeAllMessage(){
        if (mHandler!= null) {
            mHandler.removeMessages(MESSAGE_PLAY);
            mHandler.removeMessages(MESSAGE_PAUSE);
            mHandler.removeMessages(MESSAGE_RESUME);
            mHandler.removeMessages(MESSAGE_END);
        }
    }

    private void handleAudioPlay(byte[] data) {
        byte[] mAudioBuffer = data;

        if (!isStart){
            isStart = true;
            sendOnPlayMsg();
        }
        while (mPlayOffset < mAudioBuffer.length) {
            if (mAudioTrack == null)
                return;
            try {
                int readsize = mAudioBuffer.length
                        - mPlayOffset > minBufferSize ? minBufferSize
                        : mAudioBuffer.length - mPlayOffset;
                int curBufferSize = mAudioTrack.write(
                        mAudioBuffer, mPlayOffset,
                        readsize);

                if (curBufferSize == AudioTrack.ERROR_INVALID_OPERATION) {
                    sendErrorMsg();
                    break;
                } else if (curBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                    sendErrorMsg();
                    break;
                } else {
                    mPlayOffset += curBufferSize;

                }
            } catch (Exception e) {
                sendErrorMsg();
                break;
            }
        }
        mPlayOffset = 0;
        mAudioStreamQueue.poll();
        if(!mSynthing && mAudioStreamQueue.isEmpty()) {
            Message message = mHandler.obtainMessage(MESSAGE_END);
            mHandler.sendMessageDelayed(message, 500);
        }
    }


    private void sendErrorMsg() {
        onRelease();
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_ERROR);
            msg.arg1 = ErrorIndex.ERROR_AUDIO_FAIL;
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    private void sendOnPlayMsg() {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_AUDIO_PLAY);
            setIdentifier(msg);
            msg.sendToTarget();
        }

    }

    private void sendOnPeriodicMsg() {
        if (ttsHandler != null && ttsPlayer != null) {
            playProgress += DEFAULT_PERIOD;
            Object obj = (Float) playProgress;
            Message msg = ttsHandler.obtainMessage(MSG_AUDIO_PERIOD, obj);
            setIdentifier(msg);
            msg.sendToTarget();
        }

    }

    private void sendOnResumeMsg() {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_AUDIO_RESUME);
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    boolean isStop = false;

    private void sendOnStopMsg() {
        if (ttsHandler != null && ttsPlayer != null && !isStop) {
            Message msg = ttsHandler.obtainMessage(MSG_AUDIO_STOP);
            setIdentifier(msg);
            msg.sendToTarget();
            isStop = true;
        }
    }

    private void sendOnPauseMsg() {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_AUDIO_PAUSE);
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    private void setIdentifier(Message msg) {
        Bundle bundle = new Bundle();
        bundle.putString("identifier", identifier);
        bundle.putSerializable("textModel", textModel);
        msg.setData(bundle);

    }
}