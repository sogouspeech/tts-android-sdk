// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.service;

import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Created by xuq on 2017/12/20.
 */

public class HeaderClientInterceptor implements ClientInterceptor {

    private static final String TAG = "HeaderClientInterceptor";

    private Map<String, String> mHeaderMap;

    public HeaderClientInterceptor(Map<String, String> headerMap) {
        mHeaderMap = headerMap;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (mHeaderMap != null) {
                    for (String key : mHeaderMap.keySet()) {
                        Metadata.Key<String> customHeadKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                        headers.put(customHeadKey, mHeaderMap.get(key));
                    }
                }
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {

                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}