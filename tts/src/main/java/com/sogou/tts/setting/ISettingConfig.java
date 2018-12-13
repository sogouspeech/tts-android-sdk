// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.setting;

public interface ISettingConfig {
    
    public static final int MSG_ERROR = 0;// error
    
    public static final int MSG_AUDIO_PLAY = 1;// audio play
    public static final int MSG_AUDIO_PAUSE = 2;// audio pause
    public static final int MSG_AUDIO_STOP = 3;// audio stop
    public static final int MSG_AUDIO_RESUME = 4;// audio resume
    public static final int MSG_AUDIO_RELEASE = 5;// audio end
    public static final int MSG_AUDIO_BEGIN = 6;// audio begin
    public static final int MSG_AUDIO_END = 7;// audio end
    public static final int MSG_AUDIO_PERIOD = 8;// audio end
    public static final int MSG_AUDIO_COMPLETE = 9;// audio end
    public static final int MSG_AUDIO_DATA = 10;
    
    public static final int MSG_SYNTH_PLAY = 11;// synthesizer play
    public static final int MSG_SYNTH_PAUSE = 12;// synthesizer pause
    public static final int MSG_SYNTH_STOP = 13;// synthesizer stop
    public static final int MSG_SYNTH_RESUME = 14;// synthesizer resume
    public static final int MSG_SYNTH_RELEASE = 15;// audio end
    public static final int MSG_SYNTH_BEGIN = 16;// synthesizer begin
    public static final int MSG_SYNTH_END = 17;// synthesizer end
    public static final int MSG_SYNTH_SEG = 18;// synthesizer seg
    public static final int MSG_SYNTH_DATA = 19;

    public static final int MSG_ADD_QUEUE = 20;
    

    
    

}