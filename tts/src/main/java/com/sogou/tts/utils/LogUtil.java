// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by xuq on 2018/1/15.
 */

public class LogUtil {
    private final static String tag = "SogouSpeech";
    private static boolean debug = true;
    private static String filter = "-->";

    public static void v(String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.v(tag, filter + content);
        }
    }


    public static void d(String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.d(tag, filter + content);
        }
    }

    public static void e(String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.e(tag, filter + content);
        }
    }

    public static void w(String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.w(tag, filter + content);
        }
    }



    public static void v(String personalTag, String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.v(personalTag, filter + content);
        }
    }

    public static void d(String personalTag, String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.d(personalTag, filter + content);
        }
    }

    public static void e(String personalTag, String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.e(personalTag, filter + content);
        }
    }

    public static void w(String personalTag, String content){
        if(TextUtils.isEmpty(content)){
            return ;
        }
        if(debug){
            Log.w(personalTag, filter + content);
        }
    }

    public static boolean isDebug() {
        return debug;
    }

    public static String getFilter() {
        return filter;
    }

    public static void setDebug(boolean debug) {
        LogUtil.debug = debug;
    }

    public static void setFilter(String filter) {
        LogUtil.filter = filter;
    }
}