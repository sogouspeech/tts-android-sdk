// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

public interface SynthesizeCallback {

    void onSuccess(byte[] seg);

    void onResultCount();

    void onFailed(int erroCode, Throwable exceptionMsg);

}