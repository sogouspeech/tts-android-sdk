// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.sogou.tts.TTSPlayer;
import com.sogou.tts.TextModel;
import com.sogou.tts.setting.ISettingConfig;
import com.sogou.tts.utils.ErrorIndex;

import java.util.List;
import java.util.Locale;

public class MultiSynthesizerTask extends Thread implements ISettingConfig {

    private static final String TAG = "MultiSynthesizerTask";

    private Handler mSynthesizerHandler;
    private List<Object> mInputTexts = null;

    private TTSPlayer ttsPlayer = null;
    private Handler ttsHandler = null;
    private int curTextIndex = 0;
    public final static int SYNCTHESIZER_DATA_START = 1;
    public final static int REMOVE_ALL_MESSAGE = 2;
    public final static int SYNCTHESIZER_DATA_GOT = 3;
    public final static int SYNCTHESIZER_DATA = 4;
    private ISynthesizeTask mISynthesizeTask;


    Float sumTime = 0f;
    private boolean isThreadRunning = true;
    private String identifier;
    private TextModel mTextModel;

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setTextModel(TextModel textModel){
        mTextModel = textModel;
        setPitch(mTextModel.pitch);
        setSpeed(mTextModel.speed);
        setVolume(mTextModel.volume);
        setLocale(mTextModel.locale);
        setSpeaker(mTextModel.speaker);
    }


    /**
     * @param ttsPlayer
     * @param ttsHandler
     * @param type
     */
    public MultiSynthesizerTask(TTSPlayer ttsPlayer, Handler ttsHandler,
                                int type) {
        super();
        mISynthesizeTask = SythesizerFactory.getInstance().getSythesizerTask(type,ttsPlayer.getContext());
        this.ttsPlayer = ttsPlayer;
        this.ttsHandler = ttsHandler;
        this.mInputTexts = ttsPlayer.getInputTexts();
    }

    @Override
    public void run() {
        sendSynthStartMsg();
        Looper.prepare();
        Log.v("MultiSynthesizerTask","MultiSynthesizerTask");
        mSynthesizerHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SYNCTHESIZER_DATA:
                        synthesizerData();
                        break;

                    case REMOVE_ALL_MESSAGE:

                        Looper.myLooper().quit();
                        mSynthesizerHandler = null;
                        break;
                    case SYNCTHESIZER_DATA_GOT:
                        byte[] result = (byte[]) msg.obj;
                        synthesizerDataGot(result);
                        break;
                    case SYNCTHESIZER_DATA_START:
                        curTextIndex = 0;
                        synthesizerData();
                        break;
                }
            }
        };
        mSynthesizerHandler.obtainMessage(SYNCTHESIZER_DATA).sendToTarget();
        Looper.loop();
    }

    private void synthesizerData() {
        try {
                String text = (String) mInputTexts.get(curTextIndex);
                curTextIndex++;

                if (!isThreadRunning) {
                    return;
                }

                mISynthesizeTask.synthesizeText(text, new SynthesizeCallback() {
                    @Override
                    public void onSuccess(byte[] seg) {
                        if (!isThreadRunning) {
                            return;
                        }

                        Message message = Message.obtain(mSynthesizerHandler, SYNCTHESIZER_DATA_GOT);
                        message.obj = seg;
                        mSynthesizerHandler.sendMessage(message);

                    }

                    @Override
                    public void onResultCount() {
                        if (!isThreadRunning) {
                            return;
                        }

                        if (curTextIndex != mInputTexts.size()) {
                            Message message = Message.obtain(mSynthesizerHandler, SYNCTHESIZER_DATA);
                            mSynthesizerHandler.sendMessage(message);
                        }else {
                            onStop();
                            sendSynthStopMsg(sumTime);
                        }
                    }

                    @Override
                    public void onFailed(int erroCode, Throwable exceptionMsg) {
                        if (!isThreadRunning) {
                            return;
                        }

                        sendErrorMsg(ErrorIndex.ERROR_SYNTHESIZER_FAIL);
                    }
                });

        } catch (Exception e) {
            sendErrorMsg(ErrorIndex.ERROR_SYNTHESIZER_FAIL);
            return;
        }
    }

    private void synthesizerDataGot(byte[] result) {
        sumTime += result.length / 2.0f / 16000;
        sendSynthSegMsg(result);

    }

    public boolean isThreadRunning() {
        return isThreadRunning;
    }

    public void setThreadRunning(boolean isThreadRunning) {
        this.isThreadRunning = isThreadRunning;
    }

    // send synth start message
    private void sendSynthStartMsg() {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_SYNTH_BEGIN);
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    // send synth start message
    private void sendSynthSegMsg(Object obj) {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_SYNTH_SEG, obj);
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    // send synth stop message
    private void sendSynthStopMsg(Float sumTime) {
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_SYNTH_END, sumTime);
            setIdentifier(msg);
            msg.sendToTarget();
        }
    }

    // send synth error message
    private void sendErrorMsg(int errCode) {
        onStop();
        if (ttsHandler != null && ttsPlayer != null) {
            Message msg = ttsHandler.obtainMessage(MSG_ERROR);
            setIdentifier(msg);
            msg.arg1 = errCode;
            msg.sendToTarget();
        }
    }

    private void setIdentifier(Message msg) {
        Bundle bundle = new Bundle();
        bundle.putString("identifier", identifier);
        bundle.putSerializable("textModel",mTextModel);

        msg.setData(bundle);

    }

    // synth stop
    public void onStop() {
        if (mSynthesizerHandler != null) {
            isThreadRunning = false;
            mSynthesizerHandler.obtainMessage(REMOVE_ALL_MESSAGE).sendToTarget();
            mISynthesizeTask.stop();
        }
    }

    // synth release
    public void onRelease() {
        try {
            ttsPlayer.writeLog("begin release");
            mISynthesizeTask.realease();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            sendErrorMsg(ErrorIndex.ERROR_RELEASE_FAIL);
            return;
        }
    }

    public void setSpeed(float speed){
        mISynthesizeTask.setSpeed(speed);
    }

    public void setVolume(float volume){
        mISynthesizeTask.setVolume(volume);
    }

    public void setPitch(float pitch){
        mISynthesizeTask.setPitch(pitch);
    }

    public void setLocale(Locale locale){mISynthesizeTask.setLocale(locale);}

    public void setLocaleLanguageCode(String localeLanguage){mISynthesizeTask.setLocaleLanguage(localeLanguage);}

    public void setSpeaker(String speaker){mISynthesizeTask.setSpeaker(speaker);}


    public int setModelIdx(int newIdx){return  mISynthesizeTask.setModelIdx(newIdx);}


    public int init(String libPrefix, String dictName, String sndName) {
        return mISynthesizeTask.init(libPrefix, dictName,sndName);
    }
}