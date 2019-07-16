// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts;

import java.io.Serializable;
import java.util.Locale;

public class TextModel implements Serializable{
    public String identifier;
    public String text;
    public int mode;
    public int pitch = 0;
    public int volume = 9;
    public int speed = 0;
    public String speaker;
    public Locale locale;
    public String localeLanguage;


    public TextModel(String identifier, String text,int mode,int pitch,int volume, int speed,String speaker,Locale locale,String localeLanguage){
        this.identifier = identifier;
        this.text = text;
        this.mode = mode;
        this.pitch = pitch;
        this.volume = volume;
        this.speed = speed;
        this.speaker = speaker;
        this.locale = locale;
        this.localeLanguage = localeLanguage;
    }
}