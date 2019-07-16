// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;


import com.sogou.tts.TextModel;

import java.util.Locale;

public interface ISynthesizeTask {
    void synthesizeText(String content, SynthesizeCallback callback);

    void stop();

    public void setSpeed(float speed);

    public void setVolume(float volume);

    public void setPitch(float pitch);

    public void setLocale(Locale locale);

    public void setLocaleLanguage(String localeLanguage);

    public void realease();


    public int setModelIdx(int newIdx);



    public void setTextModel(TextModel model);


    public int init(String libPrefix, String dictName,String sndName);

    public int setSpeaker(String speaker);

}