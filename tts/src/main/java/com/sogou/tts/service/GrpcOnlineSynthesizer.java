// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import android.content.Context;
import android.text.TextUtils;

import com.google.protobuf.ByteString;
import com.sogou.sogocommon.utils.CommonSharedPreference;
import com.sogou.sogocommon.utils.HttpsUtil;
import com.sogou.sogocommon.utils.LogUtil;
import com.sogou.speech.tts.v1.AudioConfig;
import com.sogou.speech.tts.v1.SynthesisInput;
import com.sogou.speech.tts.v1.SynthesizeConfig;
import com.sogou.speech.tts.v1.SynthesizeRequest;
import com.sogou.speech.tts.v1.SynthesizeResponse;
import com.sogou.speech.tts.v1.VoiceConfig;
import com.sogou.speech.tts.v1.ttsGrpc;
import com.sogou.tts.TTSPlayer;
import com.sogou.tts.TextModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.NegotiationType;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

public class GrpcOnlineSynthesizer implements ISynthesizeTask {

    private ttsGrpc.ttsStub ttsStub;
    private Context mContext;
    private final static int SIZE_PER_PACKAGE = 8000;
    private boolean isRunning = true;

    private float mSpeed = 1;
    private float mVolume = 9;
    private float mPitch = 1;
    private String mLocale = "zh-cmn-Hans-CN";
    private String mSpeaker = "Male";

    private ManagedChannel channel = null;


    public GrpcOnlineSynthesizer(Context context){
        mContext = context;
        createGrpcClient();
    }

    private void startOnlineTts(String input, final SynthesizeCallback callback){

        LogUtil.e("input is "+input);
        LogUtil.e("config:  mPitch:"+mPitch+"   mSpeed:"+mSpeed+"   mVolume:"+mVolume+"   mSpeaker:"+mSpeaker+"   mLocale:"+mLocale);
        SynthesisInput synthesisInput = SynthesisInput.newBuilder().setText(input).build();
        AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioConfig.AudioEncoding.LINEAR16).
                setPitch(mPitch).setSpeakingRate(mSpeed).setVolume(mVolume).build();
        VoiceConfig voiceConfig = VoiceConfig.newBuilder().setSpeaker(mSpeaker).setLanguageCode(mLocale).build();
        SynthesizeConfig synthesizeConfig = SynthesizeConfig.newBuilder().setAudioConfig(audioConfig).setVoiceConfig(voiceConfig).build();
        SynthesizeRequest synthesizeRequest = SynthesizeRequest.newBuilder().setConfig(synthesizeConfig).setInput(synthesisInput).build();
        ttsStub.streamingSynthesize(synthesizeRequest, new StreamObserver<SynthesizeResponse>() {
            @Override
            public void onNext(SynthesizeResponse value) {

                ByteString byteString = value.getAudioContent();

                final byte[] buffer = byteString.toByteArray();
                int offset = 0;
                while (offset < buffer.length && isRunning){
                    int length = Math.min(SIZE_PER_PACKAGE,buffer.length - offset);
                    byte[] oneBuffer = new byte[length];
                    System.arraycopy(buffer,offset,oneBuffer,0,length);
                    if (callback != null){
                        callback.onSuccess(oneBuffer);
                    }
                    offset = offset + length;

                }
//                if (callback != null){
//                    callback.onSuccess(buffer);
//                }
//                FileUtils.writeByteArray2SDCard("/sdcard/sogou/ttsResult/","ruTest.pcm",buffer,true);

            }

            @Override
            public void onError(Throwable t) {
                if (callback != null){
                    callback.onFailed(0,t);
                }
            }

            @Override
            public void onCompleted() {
                if (callback != null){
                    callback.onResultCount();
                }
            }
        });
    }

    private void createGrpcClient() {
        HashMap<String, String> headerParams = new HashMap<>();
        headerParams.put("Authorization", "Bearer " + CommonSharedPreference.getInstance(mContext).getString(CommonSharedPreference.TOKEN,""));
        headerParams.put("appid",  CommonSharedPreference.getInstance(mContext).getString("appid",""));
        headerParams.put("uuid", CommonSharedPreference.getInstance(mContext).getString("uuid",""));

        if(channel == null) {
            channel = new OkHttpChannelProvider()
                    .builderForAddress(TTSPlayer.sBaseUrl,
                            443)
                    .overrideAuthority(TTSPlayer.sBaseUrl
                            + ":443")
                    .negotiationType(NegotiationType.TLS)
                    .sslSocketFactory(HttpsUtil.getSSLSocketFactory(null, null, null))
                    .intercept(new HeaderClientInterceptor(headerParams))
                    .build();
        }
        ttsStub = ttsGrpc.newStub(channel);
    }

    private void saveToSDcard(SynthesizeResponse value){
        String SDCardPATH = "/sdcard/sogou/";
        String filePath = SDCardPATH + "carnews"+File.separator+System.currentTimeMillis()+".wav";
        File file = new File(filePath);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            value.getAudioContent().writeTo(new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void synthesizeText(String content, SynthesizeCallback callback) {
        startOnlineTts(content,callback);
    }

    @Override
    public void stop() {
        isRunning = false;
        final ManagedChannel channel = (ManagedChannel) ttsStub.getChannel();
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdownNow();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void setSpeed(float speed) {
        if (speed < -5 || speed > 5) {
            return;
        }
        this.mSpeed = getValue(-5,5,0.72f,1.28f,speed);

    }

    @Override
    public void setVolume(float volume) {
        // speed 0.5~2.0
        if (mVolume < 0 || mVolume > 9) {
            return;
        }
        mVolume = getValue(0,9,0.72f,1.28f,volume);
    }

    @Override
    public void setPitch(float pitch) {
        // speed 0.5~2.0
        if (mPitch < -5 || mPitch > 5) {
            return;
        }
        mPitch = getValue(-5,5,0.82f,1.18f,pitch);
    }

    @Override
    public void setLocale(Locale locale) {
//        if (locale == Locale.CHINA){
//            mLocale = "zh-cmn-Hans-CN";
//        }else {
//            mLocale = "en-US";
//        }

        if (locale == Locale.ENGLISH || locale == Locale.UK || locale == Locale.US) {
            mLocale = "en-US";
        } else if (locale == Locale.JAPAN) {
            mLocale = "ja-JP";
        } else if (locale == Locale.KOREA) {
            mLocale = "ko-KR";
        } else {
            mLocale = "zh-cmn-Hans-CN";
        }
    }

    @Override
    public void setLocaleLanguage(String localeLanguage) {
        if(!TextUtils.isEmpty(localeLanguage)){
            mLocale = localeLanguage;
        }
    }

    @Override
    public int setModelIdx(int newIdx) {
        return 0;
    }

    @Override
    public void realease() {
        ttsStub = null;
        mContext = null;
    }

    @Override
    public void setTextModel(TextModel model) {

    }

    @Override
    public int init(String libPrefix, String dictName, String sndName) {
        return 0;
    }


    @Override
    public int setSpeaker(String speaker) {
        mSpeaker = speaker;
        return 0;
    }

    private float getValue(int oldBegin,int oldEnd,float newBegin, float newEnd, float oldValue){
        float result = newBegin + (newEnd - newBegin) * (oldValue - oldBegin) / (oldEnd - oldBegin);
        return result;
    }


}