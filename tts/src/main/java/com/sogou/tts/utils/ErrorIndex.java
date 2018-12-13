// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts.utils;

/**
 * Date:2018/6/4
 * Author:zhangxiaobei
 * Describe:
 */
public interface ErrorIndex {

    //7:鉴权相关
    //离线鉴权本地验证失败
    int ERR_VERIFY_LICENCSE_CHAIN_FAIL = -100000;
    //离线鉴权网络不可用
    int ERR_NETWORK_UNAVAILABLE = -100001;
    //离线鉴权获取应用包名/appid失败
    int ERR_OBTAIN_APPID_PACKAGE_NAME = -100002;
    //离线云端鉴权失败（云端判断appid错误，status=3）
    int ERR_CLOUD_AUTENTICATION_FAIL = -100003;
    //离线云端鉴权响应异常（400，appid、包名、标识符三个参数有缺失）
    int ERR_ILLEGAL_HTTP_RESPONSE_STATUS = -100004;
    //离线鉴权云端请求异常
    int ERR_EXCEPTION_WITHIN_AUTHENTICATION_HTTP_REQEUST = -100005;
    //离线鉴权线程处理失败
    int ERR_EXECUTOR_SHUTDOWN_WHEN_PERFORM_HTTP_REQUEST = -100006;
    //离线鉴权解析响应错误
    int ERR_SDK_PARSE_HTTP_RESPONSE = -100007;
    //本地获取appid失败
    int ERR_OBTAIN_APPID = -100008;
    //本地获取包名失败
    int ERR_OBTAIN_PACKAGE_NAME = -100009;
    //本地获取的appid与license中的不一致
    int ERR_INVALID_APPID = -100010;
    //本地获得的包名与license中的不一致
    int ERR_INVALID_PACKAGE_NAME = -100011;
    //访问授权配置文件失败，status=1
    int ERR_SERVER_GET_APPID_FAILED = -100012;
    //配置与约定不匹配，status=2
    int ERR_SERVER_APPID_INFO_WRONG_FORMAT = -100013;
    //转发服务无法访问鉴权服务，status= 4
    int ERR_SERVER_AUTHENTICATE_SPEECH_SERVER_INAVAILABLE = -100014;
    //超出次数限制， status =5
    int ERR_SERVER_AUTHENTICATE_EXCEEDS_MAX_LIMIT = -100015;
    //上传包名错误，与注册信息不匹配，status=6
    int ERR_SERVER_AUTHENTICATE_WRONG_PACKAGE_NAME = -100016;
    //时间过期，status=7
    int ERR_SERVER_AUTHENTICATE_DATE_EXPIRE = -100017;
    // 请求参数异常
    int ERR_SERVER_WRONG_PARAMS = -100018;

    // 初始化失败
    public static final int ERROR_INITIALIZE_FAIL=  -100021;

    // 声音播放出错
    public static final int ERROR_AUDIO_FAIL= -100022;

    // 语音合成失败
    public static final int ERROR_SYNTHESIZER_FAIL= -100023;

    // 释放资源失败
    public static final int ERROR_RELEASE_FAIL= -100024;


    //1：网络相关
    //网络连接不可用
    int ERROR_NETWORK_UNAVAILABLE = 1000;
    //网络连接超时
    int ERROR_NETWORK_TIMEOUT = 1001;
    //网络IO异常
    int ERROR_NETWORK_IO = 1002;
    //网络连接异常
    int ERROR_NETWORK_CONN = 1003;
    //网络协议异常
    int ERROR_NETWORK_PROTOCOL = 1004;
    //网络异常且超过重传次数
    int ERROR_NETWORK_EXCEED_RETRY_TIMES = 1005;
    //URL异常
    int ERROR_NETWORK_MALFORMED_URL = 1006;
    //解码异常
    int ERROR_NETWORK_UNSUPPORTED_ENCODING = 1007;
    //域名解析出错时，报出java.lang.SecurityException
    int ERROR_NETWORK_SECURITY_EXCEPTION = 1008;
    //其他网络错误
    int ERROR_NETWORK_OTHER = 1009;


    //2：录音相关
    //录音权限被禁用
    int ERROR_AUDIO_FORBIDDEN = 2000;
    //AudioRecord为null
    int ERROR_AUDIO_IS_NULL = 2001;
    //采样率传入错误
    int ERROR_AUDIO_ILLEGAL_SAMPLE_RATE = 2002;
    //BUFFER_SIZE获取失败
    int ERROR_AUDIO_ILLEGAL_BUFFER_SIZE = 2003;
    //构建AudioRecord时抛出IllegalArgumentException
    int ERROR_AUDIO_ILLEGAL_ARGUMENT = 2004;
    //AudioRecord.read()报错
    int ERROR_AUDIO_READ_FAILED = 2005;
    ///录音后立即停止录音(未读到数据)
    int ERROR_AUDIO_END_WHEN_START_AUDIO = 2006;
    //录音初始化失败
    int ERROR_AUDIO_INITIALIZE_FAILED = 2007;
    //录音启动失败
    int ERROR_AUDIO_START_FAILED = 2008;
    //录音停止失败
    int ERROR_AUDIO_STOP_FAILED = 2009;


    //3:预处理相关，AGC&&AEC&&VAD&&speex
    // AGC处理出错
    int ERROR_AGC_PROCESS = 3000;
    //AGC初始化失败
    int ERROR_AGC_INIT_FAILED = 3001;
    //AEC处理出错
    int ERROR_AEC_PROCESS = 3100;
    //AEC初始化失败,init方法返回了非0值
    int ERROR_AEC_INIT_FAILED = 3101;
    //未检测到有效语音
    int ERROR_VAD_SPEECH_TIMEOUT = 3201;
    //语音超出最大规定时长
    int ERROR_VAD_SPEECH_TOO_LONG = 3202;
    //音频长度太短，且未检测到有效声音。
    int ERROR_VAD_AUDIO_TOO_SHORT = 3203;
    //传入的音频长度超过规定长度
    int ERROR_VAD_AUDIO_INPUT_TOO_LONG = 3204;
    //Speex编码错误，Speex编码后返回的数据是null
    int ERROR_SPEEX_ENCODE = 3300;


    //4:离线引擎相关
    //解码器初始化失败
    int ERROR_BF_DECODER_INIT_FAILED = 4000;
    //启动解码失败
    int ERROR_BF_DECODER_START_FAILED = 4001;
    //解码失败
    int ERROR_BF_DECODER_DECODE_FAILED = 4002;
    //模型文件不可用(模型文件路径传入错误)
    int ERROR_BF_DECODER_MODEL_PATH_ERROR = 4003;
    //语音处理器错误
    int ERROR_BF_DECODER_ERROR = 4007;
    //离线鉴权失败
    int ERROR_BF_DECODER_AUTH_ILLEGAL = 4008;

    //5:识别服务相关
    //无识别结果
    int ERROR_RECOGNITION_NO_MATCH = 5000;
    int Error_RECOGNITION_OTHER = 5009;
    //修改文字失败
    int ERROR_CORRECTING_FAILED = 5101;

    //6:唤醒相关
    //建网失败
    int ERROR_WAKE_UP_BUILD_NET_FAIL = 6000;
    //无效的垃圾词得分
    int ERROR_WAKE_UP_INVALID_PARAM = 6001;
    //无效的配置文件或者复制文件失败
    int ERROR_WAKE_UP_COPY_CONFIG = 6002;
    //设置配置文件路径失败
    int ERROR_WAKE_UP_SET_DATA_PATH = 6003;
    //配置模型文件失败
    int ERROR_WAKE_UP_SET_MODEL_PATH = 6004;
    //模型建网失败
    int ERROR_WAKE_UP_BUILD_NET = 6005;
    //关键词文件不存在
    int ERROR_WAKE_UP_INVALID_KEYWORD_PATH = 6006;
    //拷贝关键词文件失败
    int ERROR_WAKE_UP_COPY_KEYWORD = 6007;
    //唤醒未初始化
    int ERROR_WAKE_UP_KWS_UNPREPARED = 6008;
    //唤醒启动失败
    int ERROR_WAKE_UP_KWS_INVALID_HANDLE = 6009;




    //8：后端返回状态码
    //	there_is_no_decoded_result
    int ERROR_SERVER_NO_DECODED_RESULT = 8000;
    //decode_failed
    int ERROR_SERVER_DECODE_FAILED = 8001;
    //	server_is_busy
    int ERROR_SERVER_IS_BUSY = 8048;
    //voice_content_is_empty
    int ERROR_SERVER_VOICE_CONTENT_EMPTY = 8056;
    //socket_connection_failed
    int ERROR_SERVER_SOCKET_CONNECTION_FAILED = 8060;
    //	parsing_request_body_failed
    int ERROR_SERVER_PARSE_REQUEST_BODY_FAILED = 8062;
    //	request_body_is_empty
    int ERROR_SERVER_REQUEST_BODY_EMPTY = 8063;
    //request_method_is_illegal
    int ERROR_SERVER_REQUEST_METHOD_ILLEGAL = 8064;

    //9:其他
    //json解析异常
    int ERROR_ANALYZE_JSON = 9000;
    //数字格式化错误
    int ERROR_NUMBER_FORMAT = 9001;
}