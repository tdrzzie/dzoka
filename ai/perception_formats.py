"""Narrow, bounded input readers. No general image/audio codec or remote resources."""
import struct
import zlib


def png_rgba(data):
    if len(data)>20*1024*1024 or data[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Unsupported PNG')
    pos=8;chunks=[];size=None;ended=False
    while pos<len(data):
        if pos+12>len(data):raise ValueError('Truncated PNG')
        length=struct.unpack_from('>I',data,pos)[0];kind=data[pos+4:pos+8]
        if length>len(data)-pos-12:raise ValueError('Invalid chunk')
        payload=data[pos+8:pos+8+length]
        if zlib.crc32(kind+payload)&0xffffffff!=struct.unpack_from('>I',data,pos+8+length)[0]:raise ValueError('CRC mismatch')
        if size is None:
            if kind!=b'IHDR' or length!=13:raise ValueError('Missing header')
            w,h,depth,color,compression,filtering,interlace=struct.unpack('>IIBBBBB',payload)
            if not(1<=w<=8192 and 1<=h<=8192 and w*h<=4_000_000) or (depth,color,compression,filtering,interlace)!=(8,6,0,0,0):raise ValueError('Unsupported raster profile')
            size=(w,h)
        elif kind==b'IDAT':chunks.append(payload)
        elif kind==b'IEND':
            if length or pos+12!=len(data):raise ValueError('Invalid end')
            ended=True;break
        else:raise ValueError('Noncanonical raster')
        pos+=length+12
        if len(chunks)>4096:raise ValueError('Too many chunks')
    if not ended or not chunks:raise ValueError('Incomplete PNG')
    w,h=size;stride=w*4;expected=(stride+1)*h;decoder=zlib.decompressobj()
    raw=decoder.decompress(b''.join(chunks),expected+1)
    if len(raw)!=expected or not decoder.eof or decoder.unused_data or decoder.unconsumed_tail:raise ValueError('Invalid bounded raster stream')
    output=bytearray(stride*h);previous=bytearray(stride)
    for y in range(h):
        offset=y*(stride+1);kind=raw[offset];row=bytearray(raw[offset+1:offset+1+stride])
        if kind>4:raise ValueError('Invalid PNG filter')
        if kind:
            for x in range(stride):
                a=row[x-4] if x>=4 else 0;b=previous[x];c=previous[x-4] if x>=4 else 0
                if kind==1:value=a
                elif kind==2:value=b
                elif kind==3:value=(a+b)//2
                else:
                    p=a+b-c;da,db,dc=abs(p-a),abs(p-b),abs(p-c)
                    value=a if da<=db and da<=dc else b if db<=dc else c
                row[x]=(row[x]+value)&255
        output[y*stride:(y+1)*stride]=row;previous=row
    return w,h,bytes(output)


def pcm16(data):
    if len(data)<844 or len(data)>320044:raise ValueError('PCM duration/size unsupported')
    if data[:4]!=b'RIFF' or data[8:16]!=b'WAVEfmt ' or data[36:40]!=b'data':raise ValueError('Canonical PCM WAV required')
    riff,fmt,kind,channels,rate,byte_rate,align,bits=struct.unpack_from('<I4x4xIHHIIHH',data,4)
    if riff!=len(data)-8 or (fmt,kind,channels,rate,byte_rate,align,bits)!=(16,1,1,16000,32000,2,16):raise ValueError('Unsupported PCM format')
    length=struct.unpack_from('<I',data,40)[0]
    if length!=len(data)-44 or length%2:raise ValueError('Invalid PCM length')
    return data[44:]
