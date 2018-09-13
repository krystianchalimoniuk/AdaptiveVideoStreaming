/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.majorkernelpanic.streaming.hw;

import java.nio.ByteBuffer;
import android.media.MediaCodecInfo;
import android.util.Log;

/**
 * Converts from NV21 to YUV420 semi planar or planar.
 */		
public class NV21Convertor {

	private int mSliceHeight, mHeight;
	private int mStride, mWidth;
	private int mSize;
	private boolean mPlanar, mPanesReversed = false;
	private int mYPadding;
	private byte[] mBuffer; 
	ByteBuffer mCopy;
	
	public void setSize(int width, int height) {
		mHeight = height;
		mWidth = width;
		mSliceHeight = height;
		mStride = width;
		mSize = mWidth*mHeight;
	}
	
	public void setStride(int width) {
		mStride = width;
	}
	
	public void setSliceHeigth(int height) {
		mSliceHeight = height;
	}
	
	public void setPlanar(boolean planar) {
		mPlanar = planar;
	}
	
	public void setYPadding(int padding) {
		mYPadding = padding;
	}
	
	public int getBufferSize() {
		return 3*mSize/2;
	}
	
	public void setEncoderColorFormat(int colorFormat) {
		switch (colorFormat) {
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			setPlanar(false);
			break;	
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
			setPlanar(true);
			break;
		}
	}
	
	public void setColorPanesReversed(boolean b) {
		mPanesReversed = b;
	}
	
	public int getStride() {
		return mStride;
	}

	public int getSliceHeigth() {
		return mSliceHeight;
	}

	public int getYPadding() {
		return mYPadding;
	}
	
	
	public boolean getPlanar() {
		return mPlanar;
	}
	
	public boolean getUVPanesReversed() {
		return mPanesReversed;
	}
	
	public void convert(byte[] data, ByteBuffer buffer) {
		byte[] result = convert(data);
		int min = buffer.capacity() < data.length?buffer.capacity() : data.length;
		buffer.put(result, 0, min);
	}
	
	public byte[] convert(byte[] data) {

		// A buffer large enough for every case
		if (mBuffer==null || mBuffer.length != 3*mSliceHeight*mStride/2+mYPadding) {
			mBuffer = new byte[3*mSliceHeight*mStride/2+mYPadding];
		}
		
		if (!mPlanar) {
			if (mSliceHeight==mHeight && mStride==mWidth) {
				// Swaps U and V
				if (!mPanesReversed) {
					for (int i = mSize; i < mSize+mSize/2; i += 2) {
						mBuffer[0] = data[i+1];
						data[i+1] = data[i];
						data[i] = mBuffer[0]; 
					}
				}
				if (mYPadding>0) {
					System.arraycopy(data, 0, mBuffer, 0, mSize);
					System.arraycopy(data, mSize, mBuffer, mSize+mYPadding, mSize/2);
					return mBuffer;
				}
				return data;
			}
		} else {
			if (mSliceHeight==mHeight && mStride==mWidth) {
				// De-interleave U and V
				if (!mPanesReversed) {
					for (int i = 0; i < mSize/4; i+=1) {
						mBuffer[i] = data[mSize+2*i+1];
						mBuffer[mSize/4+i] = data[mSize+2*i];
					}
				} else {
					for (int i = 0; i < mSize/4; i+=1) {
						mBuffer[i] = data[mSize+2*i];
						mBuffer[mSize/4+i] = data[mSize+2*i+1];
					}
				}
				if (mYPadding == 0) {
					System.arraycopy(mBuffer, 0, data, mSize, mSize/2);
				} else {
					System.arraycopy(data, 0, mBuffer, 0, mSize);
					System.arraycopy(mBuffer, 0, mBuffer, mSize+mYPadding, mSize/2);
					return mBuffer;
				}
				return data;
			}
		}
		
		return data;
	}	

	public static void rotateNV21(byte[] input, byte[] output, int width, int height, int rotation) {
		if (rotation==0){
			System.arraycopy(input, 0, output, 0, input.length);
			return;
		}
		boolean swap = (rotation == 90 || rotation == 270);
		boolean yflip = (rotation == 90 || rotation == 180);
		boolean xflip = (rotation == 270 || rotation == 180);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int xo = x, yo = y;
				int w = width, h = height;
				int xi = xo, yi = yo;
				if (swap) {
					xi = w * yo / h;
					yi = h * xo / w;
				}
				if (yflip) {
					yi = h - yi - 1;
				}
				if (xflip) {
					xi = w - xi - 1;
				}
				output[w * yo + xo] = input[w * yi + xi];
				int fs = w * h;
				int qs = (fs >> 2);
				xi = (xi >> 1);
				yi = (yi >> 1);
				xo = (xo >> 1);
				yo = (yo >> 1);
				w = (w >> 1);
				h = (h >> 1);
				// adjust for interleave here
				int ui = fs + (w * yi + xi) * 2;
				int uo = fs + (w * yo + xo) * 2;
				// and here
				int vi = ui + 1;
				int vo = uo + 1;
				output[uo] = input[ui];
				output[vo] = input[vi];
			}
		}
	}
}
