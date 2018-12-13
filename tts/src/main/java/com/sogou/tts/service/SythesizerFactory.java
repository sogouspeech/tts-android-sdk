// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import android.content.Context;

public class SythesizerFactory {


    private  static  SythesizerFactory  sInstance;

    public static SythesizerFactory getInstance() {
        if (sInstance == null) {
            synchronized (SythesizerFactory.class) {
                if (sInstance == null) {
                    sInstance = new SythesizerFactory();
                }
            }
        }
        return sInstance;
    }

    private SythesizerFactory(){

    }


    public ISynthesizeTask getSythesizerTask(int type, Context context){
        return new GrpcOnlineSynthesizer(context);
    }
}