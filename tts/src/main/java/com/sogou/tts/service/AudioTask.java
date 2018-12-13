// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.sogou.tts.TTSPlayer;
import com.sogou.tts.TextModel;
import com.sogou.tts.setting.IRecordAudioConfig;
import com.sogou.tts.setting.ISettingConfig;
import com.sogou.tts.utils.ErrorIndex;


public class AudioTask extends Thread implements ISettingConfig, IRecordAudioConfig {

    private Handler ttsHandler = null;
    private AudioTrack mAudioTrack = null;
    private int mPlayOffset = 0;
    private int minBufferSize = -1;
    private TTSPlayer ttsPlayer;
    private boolean isThreadRunning = false;
    private int streamType = AudioManager.STREAM_MUSIC;
    private int sampleRate = DEFAULT_HIGH_AUDIO_SAMPLE_RATE;
    private int channelConfig = MONO;
    private int audioFormat = PCM_16BIT;
    private boolean mCheckAvailable = false;
    private boolean mAudioBeginingFlag = true;
    private boolean hasPaused = false;

    private float playProgress = 0;
    private String identifier;
    private TextModel textModel;

    public AudioTask(TTSPlayer ttsPlayer, Handler ttsHandler, int streamType) {

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
        isThreadRunning = false;
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }

    }

    // audio stop
    public boolean onStop() {
        isThreadRunning = false;
        if (mAudioTrack != null) {
            synchronized (mAudioTrack) {
                if (mAudioTrack != null) {
                    mAudioTrack.pause();
                    mAudioTrack.flush();
                    mAudioTrack.release();
                    mAudioBeginingFlag = false;

                    mAudioTrack = null;
                    sendOnStopMsg();
                }
            }

        }
        playProgress = 0;
        return true;

    }

    // audio pause
    public boolean onPause() {
        isThreadRunning = false;
        if (mAudioTrack != null
                && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.pause();
            mAudioBeginingFlag = true;
            hasPaused = true;
            sendOnPauseMsg();
        }
        return true;

    }


    public boolean isThreadRunning() {
        return isThreadRunning;
    }

    public void setThreadRunning(boolean isThreadRunning) {
        this.isThreadRunning = isThreadRunning;
    }

    @Override
    public void run() {


        isThreadRunning = true;
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
        while (isThreadRunning) {
            if (ttsPlayer.WavQueue.size() == 0 && ttsPlayer.mSynthEnd) {
                synchronized (ttsPlayer.mSynthEnd) {

                    if (ttsPlayer.WavQueue.size() == 0 && ttsPlayer.mSynthEnd) {
                        try {
                            int sleepTime = (int) ((ttsPlayer.sumTime - playProgress) * 5 + 0.5);
                            if (sleepTime < 2) {
                                sleepTime = 2;
                            }
                            for (int k = 0; k < sleepTime; k++)
                                if (isThreadRunning) {
                                    Thread.sleep(200);
                                }
                                else {
                                    break;
                                }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        onStop();
                        return;
                    }

                }
            } else if (ttsPlayer.WavQueue.size() > 0) {

                if (ttsPlayer.WavQueue.size() > 0) {
                    byte[] mAudioBuffer = ttsPlayer.WavQueue.peek();
                    if (hasPaused && isThreadRunning) {
                        sendOnResumeMsg();
                        hasPaused = false;
                    } else if (mAudioBeginingFlag) {
                        sendOnPlayMsg();
                    }


                    while (mPlayOffset < mAudioBuffer.length) {
                        if (!isThreadRunning || mAudioTrack == null)
                            return;
                        try {
                            int readsize = mAudioBuffer.length
                                    - mPlayOffset > minBufferSize ? minBufferSize
                                    : mAudioBuffer.length - mPlayOffset;
                            int curBufferSize;
                            synchronized (mAudioTrack) {

                                if (mAudioTrack != null
                                        && isThreadRunning) {
                                    curBufferSize = mAudioTrack.write(
                                            mAudioBuffer, mPlayOffset,
                                            readsize);
                                } else {
                                    return;
                                }
                            }
                            if (curBufferSize == AudioTrack.ERROR_INVALID_OPERATION) {
                                sendErrorMsg();
                                return;
                            } else if (curBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                                sendErrorMsg();
                                return;
                            } else {
                                mPlayOffset += curBufferSize;

                            }
                        } catch (Exception e) {
                            sendErrorMsg();
                            return;
                        }
                    }
                    mPlayOffset = 0;

                    if (!isThreadRunning)
                        return;
                    ttsPlayer.WavQueue.poll();
                }

            }
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