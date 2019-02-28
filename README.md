# tts-android-sdk
搜狗语音合成库

集成方式在工程的build.gradle里面加入以下代码：
```
allprojects {
  repositories {
    ... 
    { url 'https://jitpack.io' }
  }
}
```
增加以下依赖：
```
implementation 'com.github.sogouspeech:common-android-sdk:1.0.1'
```
```
implementation 'com.github.sogouspeech:tts-android-sdk:1.0.2'
```

使用方法请参考[搜狗知音文档](https://docs.zhiyin.sogou.com/docs/tts/sdk)
