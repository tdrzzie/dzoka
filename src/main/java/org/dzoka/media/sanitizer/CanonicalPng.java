package org.dzoka.media.sanitizer;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;

/** Non-decoding validation of the narrowly allowed sandbox output. */
public final class CanonicalPng {
    private CanonicalPng(){}
    public static int[] inspect(byte[] bytes){
        if(bytes.length<45 || bytes.length>20*1024*1024 || !Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))throw new IllegalArgumentException();
        int pos=8,width=0,height=0,chunks=0;boolean data=false,end=false;
        while(pos<bytes.length){
            if(++chunks>4096 || bytes.length-pos<12)throw new IllegalArgumentException();
            int length=ByteBuffer.wrap(bytes,pos,4).getInt();if(length<0 || length>bytes.length-pos-12)throw new IllegalArgumentException();
            String type=new String(bytes,pos+4,4,StandardCharsets.US_ASCII);
            if(!Set.of("IHDR","IDAT","IEND").contains(type))throw new IllegalArgumentException();
            if(chunks==1){
                if(!type.equals("IHDR") || length!=13)throw new IllegalArgumentException();
                width=ByteBuffer.wrap(bytes,pos+8,4).getInt();height=ByteBuffer.wrap(bytes,pos+12,4).getInt();
                if(width<1 || height<1 || width>8192 || height>8192 || (long)width*height>4_000_000)throw new IllegalArgumentException();
            }else if(type.equals("IHDR"))throw new IllegalArgumentException();
            CRC32 crc=new CRC32();crc.update(bytes,pos+4,length+4);
            if((int)crc.getValue()!=ByteBuffer.wrap(bytes,pos+8+length,4).getInt())throw new IllegalArgumentException();
            pos+=12+length;if(type.equals("IDAT"))data=true;
            if(type.equals("IEND")){if(length!=0 || pos!=bytes.length)throw new IllegalArgumentException();end=true;break;}
        }
        if(!data || !end)throw new IllegalArgumentException();return new int[]{width,height};
    }
}
