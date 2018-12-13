// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.listener;

//TTSPlayer callback interface
public interface TTSPlayerListener {
    // TTSPlayer start callback
    void onStart(String identifier);

    // TTSPlayer end callback
    void onEnd(String identifier,String text);

    // TTSPlayer error callback
    void onError(String identifier,int errCode);
    
    void onPause(String identifier);
    
    void onSynEnd(String identifier,Float sumTime);

    void onResume(String identifier);
}