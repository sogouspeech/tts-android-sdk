// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.tts;

public class SynthesizerData {
	public byte[] wavData;
	public short[] sylLen;
	public String[] curChars;
	public float[] sylLenf;
	public SynthesizerData(byte[] wavData, short[] sylLen, String curChars[]){
		this.wavData = wavData;
		this.sylLen = sylLen;
		this.curChars = curChars;
	}
	public SynthesizerData(){
	    
	}
}