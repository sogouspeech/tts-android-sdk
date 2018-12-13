// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.sogou.sogocommon.utils.CommonSharedPreference;
import com.sogou.tts.auth.TokenFetchTask;
import com.sogou.tts.utils.ErrorIndex;
import com.sogou.tts.auth.AuthManager;
import com.sogou.tts.listener.TTSPlayerListener;

import com.sogou.tts.service.AudioTask;
import com.sogou.tts.service.MultiSynthesizerTask;
import com.sogou.tts.setting.ISettingConfig;
import com.sogou.tts.utils.LogUtil;
import com.sogou.tts.utils.Mode;

import org.conscrypt.Conscrypt;


import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;


public class TTSPlayer implements ISettingConfig {
     private static final String TAG = "TTSPlayer";
        private static final String INPUTTEXTS_REGULAR_EXPRESSION = "[。|？|?|！|!|\r|\n|，|,]";
    private static final int MAX_THREAD_AMOUNT = 1;

    public Float sumTime = 0f;
    private int streamType = AudioManager.STREAM_MUSIC;
    private boolean ifWriteLog = false;
    static {
        if (Conscrypt.isAvailable()) {
            Security.insertProviderAt(Conscrypt.newProvider("GmsCore_OpenSSL"), 1);
        }
    }
    private MultiSynthesizerTask mSynthTask = null;


    public static final int QUEUE_FLUSH = 0;
    public static final int QUEUE_ADD = 1;


    private Context mContext;
    public static String sBaseUrl = "";


    public void setWriteLog(boolean b) {
        ifWriteLog = b;
    }


    public Context getContext(){
        return mContext;
    }

    public void writeLog(String str) {
        if (ifWriteLog) {
            LogUtil.w("sogoutts", "write log: " + str);
        }
    }



    // audio state enum
    private static enum AudioState {
        AUDIO_ON_IDLE, AUDIO_ON_PAUSE, AUDIO_ON_PLAY, AUDIO_ON_HANDLING, AUDIO_ON_COMPLETE, AUDIO_ON_RESUME
    }


    // The current state of the audio
    private final AtomicReference<AudioState> audioState = new AtomicReference<AudioState>(
            AudioState.AUDIO_ON_IDLE);



    private AudioTask mAudioTask = null;

    public Boolean mSynthEnd = false;
    public Queue<byte[]> WavQueue = new ConcurrentLinkedQueue<byte[]>();

    public LinkedBlockingQueue<TextModel> textQueue = new LinkedBlockingQueue<TextModel>();
    private TTSPlayerListener ttsPlayerListener = null;
    private List<Object> mInputTexts = null;
    private ExecutorService mThreadPool = null;
    private int mSpeed = 0;
    private int mVolume = 9;
    private int mMulPitch = 0;
    private String mSpeaker = "Male";


    private Locale mLanguage = Locale.CHINA;
    private Object mControlLock = new Object();

    private TextModel mCurrentTextModel;

    private int mMode = Mode.TYPE_ONLINE;



    public Set<Locale> getLanguages() {
        Set<Locale> languages = new HashSet<>();
        languages.add(Locale.CHINA);
        languages.add(Locale.ENGLISH);
        return new HashSet<>();
    }

    public Locale getLanguage() {
        return mLanguage;
    }

    public void setLanguage(Locale language) {
        mLanguage = language;
    }

    public boolean isSpeaking(){
        return audioState.get() == AudioState.AUDIO_ON_PLAY || audioState.get() == AudioState.AUDIO_ON_RESUME;
    }

    // local handler instance to handle message
    private Handler ttsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            String identifier = null;
            Bundle bundle = msg.peekData();
            TextModel textModel = null;
            if (bundle != null){
                identifier = bundle.getString("identifier");
                textModel = (TextModel) bundle.getSerializable("textModel");
            }

            switch (msg.what) {
                // handle error message
                case MSG_SYNTH_DATA:
                    break;
                case MSG_ERROR:
                    handleErrorMsg(textModel,msg.arg1);
                    stop();
                    break;
                // handle audio stop message
                case MSG_AUDIO_COMPLETE:
                    handleAudioState(textModel,AudioState.AUDIO_ON_COMPLETE);
                    break;
                case MSG_AUDIO_STOP:
                    //mAudioTask = null;
                    handleAudioState(textModel,AudioState.AUDIO_ON_IDLE);
                    break;
                // handle audio pause message
                case MSG_AUDIO_PAUSE:
                    handleAudioState(textModel,AudioState.AUDIO_ON_PAUSE);
                    break;
                // handle audio resume message
                case MSG_AUDIO_RESUME:
                    handleAudioState(textModel,AudioState.AUDIO_ON_RESUME);
                    break;
                // handle audio play message
                case MSG_AUDIO_PLAY:
                    handleAudioState(textModel,AudioState.AUDIO_ON_PLAY);
                    break;
                // handle synthesizer begin message
                case MSG_SYNTH_BEGIN:
                    synchronized (mSynthEnd) {
                        mSynthEnd = false;
                    }
                    break;
                // handle synthesizer end message
                case MSG_SYNTH_END:
                    synchronized (mSynthEnd) {
                        mSynthEnd = true;
                        sumTime = (Float) msg.obj;
                        ttsPlayerListener.onSynEnd(identifier,sumTime);
                    }
                    break;
            }
        }

    };



    // TTSPlayer constructor
    public TTSPlayer(){
        if (TextUtils.isEmpty(sBaseUrl)){
            throw new IllegalArgumentException("no baseUrl!");
        }
    }



    // handle audio state ,do some callback method
    private void handleAudioState(TextModel model,AudioState state) {
        String identifier = "";
        if (model != null){
            identifier = model.identifier;
        }
        if (state == audioState.get()) {

            return;
        }

        audioState.set(state);

        switch (state) {
            case AUDIO_ON_IDLE:
                String a = null;
                TextModel textModel = mCurrentTextModel;
                if (textModel != null) {
                    a = textModel.text;
                    if (textModel == model){
                        mCurrentTextModel = null;
                    }
                }
                if (a == null) {
                    a = "";
                }
                ttsPlayerListener.onEnd(identifier,a);
                playNext();
                break;
            case AUDIO_ON_PLAY:
                ttsPlayerListener.onStart(identifier);
                break;
            case AUDIO_ON_PAUSE:
                ttsPlayerListener.onPause(identifier);
                break;
            case AUDIO_ON_RESUME:
                ttsPlayerListener.onResume(identifier);
                audioState.set(AudioState.AUDIO_ON_PLAY);
                break;
            default:
                break;
        }
    }

    // handle error message
    private void handleErrorMsg(TextModel textModel,int errCode) {
        if (ttsPlayerListener != null) {
            handleAudioState(textModel,AudioState.AUDIO_ON_IDLE);
            if (textModel != null){
                ttsPlayerListener.onError(textModel.identifier,errCode);
            }else {
                ttsPlayerListener.onError("",errCode);
            }

            if (errCode == ErrorIndex.ERROR_AUDIO_FAIL) {
                try {
                    textQueue.poll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public int init(Context mContext,
                    TTSPlayerListener ttsPlayerListener) {
        return init(mContext, null, null, null, ttsPlayerListener, streamType);
    }

    public int init(Context mContext,
                    TTSPlayerListener ttsPlayerListener, int streamType) {
        return init(mContext, null, null, null, ttsPlayerListener, streamType);
    }

    // init synthesizerJNI
    private int init(Context mContext, String libPath, String dictName, String sndName,
                    TTSPlayerListener ttsPlayerListener, int streamType) {
        if (mContext != null
                && ttsPlayerListener != null) {
            mThreadPool = Executors.newFixedThreadPool(MAX_THREAD_AMOUNT);

            this.ttsPlayerListener = ttsPlayerListener;
            this.streamType = streamType;
            this.mContext = mContext;

            mSynthTask = new MultiSynthesizerTask(this, ttsHandler, Mode.TYPE_OFFLINE);

            String libPrefix;
            if (libPath == "" || libPath == null)
                libPrefix = mContext.getApplicationInfo().nativeLibraryDir;
            else
                libPrefix = libPath;
            int result = mSynthTask.init(libPrefix,dictName,sndName);
            if (result < 0) {
                sendErrorMsg(ErrorIndex.ERROR_INITIALIZE_FAIL);
                return result;
            }

            return result;
        } else {
            sendErrorMsg(ErrorIndex.ERROR_INITIALIZE_FAIL);
            return -1;
        }
    }

    public void shutdown() {
        if (mAudioTask != null) {
            mAudioTask.onStop();
            mAudioTask.onRelease();
            mAudioTask = null;
        }
        if (mSynthTask != null) {
            mSynthTask.onStop();
            mSynthTask.onRelease();
            mSynthTask = null;
        }
        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
            mThreadPool = null;
        }
        ttsHandler.removeCallbacksAndMessages(null);
        textQueue.clear();
    }


    private void play(TextModel textModel) {

        mCurrentTextModel = textModel;

        synchronized (mControlLock) {
            innerStop();

            audioState.set(AudioState.AUDIO_ON_HANDLING);


            mInputTexts = splitInputText(textModel.text);

            if (mInputTexts == null || mInputTexts.size() <= 0) {
                return;
            }

            mAudioTask = new AudioTask(this, ttsHandler, streamType);
            mAudioTask.setIdentifier(textModel.identifier);
            mAudioTask.setTextModel(textModel);
            mThreadPool.execute(mAudioTask);

            mSynthTask = new MultiSynthesizerTask(this, ttsHandler, textModel.mode);

            mSynthTask.setLocale(mLanguage);
            mSynthTask.setIdentifier(textModel.identifier);
            mSynthTask.setTextModel(textModel);
            mSynthTask.start();
        }


    }

    public void speak(String text){
        speak(text,QUEUE_FLUSH,"");
    }

    public void speak(String text,String identifier){
        speak(text,QUEUE_FLUSH,identifier);
    }

    public void speak(String text, int queuemode, String identifier){
        speak(text,queuemode,identifier,mMode);
    }

    public void speak(String text, int queuemode, String identifier,int mode) {

        TextModel textModel = new TextModel(identifier, text, mode, mMulPitch, mVolume, mSpeed,mSpeaker,mLanguage);
        if (queuemode == QUEUE_ADD && mCurrentTextModel != null ){
            boolean addFlag = textQueue.add(textModel);

            if (!addFlag) {
                return;
            }
        }else {
            if (queuemode == QUEUE_FLUSH){
                textQueue.clear();
            }
            play(textModel);
        }
    }


    // TTSPlayer pause
    public void pause() {
        if (audioState.get() == AudioState.AUDIO_ON_PLAY) {
            if (mAudioTask != null) {
                mAudioTask.onPause();
            }
        }
    }

    // TTSPlayer resume

    public void resume() {
        if (audioState.get() == AudioState.AUDIO_ON_PAUSE) {
            if (mAudioTask != null) {
                mThreadPool.execute(mAudioTask);
            }
        }
    }

    private void innerStop(){
        if (mAudioTask != null) {
            mAudioTask.onStop();
        }

        synchronized (mSynthEnd) {
            mSynthEnd = false;
        }
        if (mSynthTask != null) {
            mSynthTask.onStop();
        }
        try {
            if (mSynthTask != null) {
                mSynthTask.join();
                mSynthTask = null;
            }
            if (mAudioTask != null) {
                mAudioTask.join();
                mAudioTask = null;
            }

        } catch (Exception e) {

        }

        WavQueue.clear();

        if (mInputTexts != null) {
            mInputTexts.clear();
        }

    }

    // TTSPlayer stop
    public void stop() {
        textQueue.clear();
        innerStop();

    }

    // splite content String to single text
    private List<Object> splitInputText(String content) {
        if (content == null || content.length() <= 0) {
            return null;
        }
        List<Object> inputTexts = new ArrayList<Object>();

            if (content == null || content.length() <= 0) {
                return null;
            }
            String[] contents = content.split(INPUTTEXTS_REGULAR_EXPRESSION);
            for (String c : contents) {
                c = c.trim();
                if (c.length() > 0) {
                    while (c.length() > LENGTH){
                        String text = c.substring(0,LENGTH);
                        inputTexts.add(text);
                        c = c.substring(LENGTH);
                    }
                    inputTexts.add(c);
                }
            }

        return inputTexts;
    }

    private static final int LENGTH = 40;

    public List<Object> getInputTexts() {
        return mInputTexts;
    }

    public int getStreamType() {
        return streamType;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    // send error message to callback
    private void sendErrorMsg(int errCode) {
        if (ttsHandler != null) {
            Message msg = ttsHandler.obtainMessage(MSG_ERROR);
            msg.arg1 = errCode;
            msg.sendToTarget();
        }
    }

    public int getSpeed() {
        return mSpeed;
    }

    public void setSpeed(int mSpeed) {
        // speed 0.5~2.0
        if (mSpeed < -5 || mSpeed > 5) {

        } else {
            this.mSpeed = mSpeed;
        }

    }

    public float getVolume() {
        return mVolume;
    }

    public void setVolume(int mVolume) {
        // speed 0.5~2.0
        if (mVolume < 0 || mVolume > 9) {
            return;
        }

        this.mVolume = mVolume;


    }

    public void setPitch(int mPitch) {
        if (mPitch < -5 || mPitch > 5) {
            return;
        }

        this.mMulPitch = mPitch;


    }

    public void setSpeaker(String speaker){
        mSpeaker = speaker;
    }

    private void playNext(){
        TextModel pair = textQueue.poll();
        if (pair != null){
            play(pair);
        }
    }

    public static void initZhiyinInfo(Context context,ZhiyinInitInfo info){
        if (TextUtils.isEmpty(info.baseUrl)){
            throw new IllegalArgumentException("no baseUrl!");

        }

        if (TextUtils.isEmpty(info.uuid)){
            throw new IllegalArgumentException("no uuid!");

        }

        if (TextUtils.isEmpty(info.appid)){
            throw new IllegalArgumentException("no appid!");

        }

        sBaseUrl = info.baseUrl;
        CommonSharedPreference.getInstance(context).setString("uuid",info.uuid);
        CommonSharedPreference.getInstance(context).setString("appid",info.appid);
        if (!TextUtils.isEmpty(info.token)) {
            CommonSharedPreference.getInstance(context).setString(CommonSharedPreference.TOKEN, info.token);

        }else if(!TextUtils.isEmpty(info.appkey)) {
            CommonSharedPreference.getInstance(context).setString("appkey", info.appkey);

           AuthManager.getInstance().init(context);
        }else {
            throw new IllegalArgumentException("no token or appkey!");
        }
    }

}