// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts;


public class ZhiyinInitInfo {
    public String baseUrl;
    public String uuid;
    public String appid;
    public String appkey;
    public String token;
    public long tokenExp;

    private ZhiyinInitInfo(String baseUrl, String uuid, String appid, String appkey, String token,long tokenExp) {
        this.baseUrl = baseUrl;
        this.uuid = uuid;
        this.appid = appid;
        this.appkey = appkey;
        this.token = token;
        this.tokenExp = tokenExp;
    }

    public static class Builder{
        private String baseUrl;
        private String uuid;
        private String appid;
        private String appkey;
        private String token;
        private long tokenExp;

        public ZhiyinInitInfo create(){
            ZhiyinInitInfo info = new ZhiyinInitInfo(baseUrl,uuid,appid,appkey,token,tokenExp);
            return info;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setUuid(String uuid) {
            this.uuid = uuid;
            return this;

        }

        public Builder setAppid(String appid) {
            this.appid = appid;
            return this;

        }

        public Builder setAppkey(String appkey) {
            this.appkey = appkey;
            return this;

        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setTokenExp(long tokenExp){
            this.tokenExp = tokenExp;
            return this;
        }
    }
}