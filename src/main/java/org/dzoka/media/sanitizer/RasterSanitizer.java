package org.dzoka.media.sanitizer;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import javax.imageio.*;

/** Standalone JDK-only entry point; invoke exclusively inside the restricted decoder sandbox. */
public final class RasterSanitizer {
    public static final int SOURCE_LIMIT=5*1024*1024, OUTPUT_LIMIT=20*1024*1024, PIXEL_LIMIT=4_000_000;
    private static final byte[] PNG={(byte)137,80,78,71,13,10,26,10};
    private RasterSanitizer(){}
    public static void main(String[] args){
        try{
            if(args.length!=2)throw new IllegalArgumentException();
            byte[] source=Files.readAllBytes(Path.of(args[0]));
            if(source.length<12 || source.length>SOURCE_LIMIT)throw new IllegalArgumentException();
            boolean png=Arrays.equals(Arrays.copyOf(source,8),PNG);
            if(png)validatePng(source);
            else if((source[0]&255)!=255 || (source[1]&255)!=216
                    || (source[source.length-2]&255)!=255 || (source[source.length-1]&255)!=217)throw new IllegalArgumentException();
            ImageIO.setUseCache(false);
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(source))){
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext())throw new IllegalArgumentException();
                var reader=readers.next();
                try{
                    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!(png?format.equals("png"):format.equals("jpeg")))throw new IllegalArgumentException();
                    reader.setInput(input,true,true);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<1 || height<1 || width>8192 || height>8192 || (long)width*height>PIXEL_LIMIT)throw new IllegalArgumentException();
                    BufferedImage decoded=reader.read(0);
                    BufferedImage pixels=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<height;y++)pixels.setRGB(0,y,width,1,decoded.getRGB(0,y,width,1,null,0,width),0,width);
                    var output=new ByteArrayOutputStream();
                    if(!ImageIO.write(pixels,"png",output) || output.size()>OUTPUT_LIMIT)throw new IllegalArgumentException();
                    Files.write(Path.of(args[1]),output.toByteArray(),StandardOpenOption.CREATE_NEW);
                    System.out.print(width+" "+height+" "+(png?"image/png":"image/jpeg"));
                }finally{reader.dispose();}
            }
        }catch(IllegalArgumentException|javax.imageio.IIOException malformed){System.exit(2);}
        catch(Throwable failure){System.exit(3);}
    }
    private static void validatePng(byte[] bytes){
        int pos=8;boolean first=true,end=false;
        while(pos<bytes.length){
            if(bytes.length-pos<12)throw new IllegalArgumentException();
            int length=ByteBuffer.wrap(bytes,pos,4).getInt();
            if(length<0 || length>bytes.length-pos-12)throw new IllegalArgumentException();
            String type=new String(bytes,pos+4,4,java.nio.charset.StandardCharsets.US_ASCII);
            if(first && (!type.equals("IHDR") || length!=13))throw new IllegalArgumentException();
            if(Set.of("acTL","fcTL","fdAT").contains(type))throw new IllegalArgumentException();
            CRC32 crc=new CRC32();crc.update(bytes,pos+4,length+4);
            if((int)crc.getValue()!=ByteBuffer.wrap(bytes,pos+8+length,4).getInt())throw new IllegalArgumentException();
            pos+=12+length;first=false;
            if(type.equals("IEND")){if(length!=0 || pos!=bytes.length)throw new IllegalArgumentException();end=true;break;}
        }
        if(!end)throw new IllegalArgumentException();
    }
}
