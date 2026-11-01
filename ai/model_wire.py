"""Bounded binary framing; no URLs, executable commands or service identities."""
import json
import math
import struct
REQUEST_LIMIT=28*1024*1024
RESULT_LIMIT=1024*1024
def pairs(values):
    result={}
    for key,value in values:
        if key in result:raise ValueError('Duplicate pipe field')
        result[key]=value
    return result
def encode(value,limit):
    raw=json.dumps(value,sort_keys=True,separators=(',',':'),allow_nan=False,ensure_ascii=True).encode()
    if not 1<=len(raw)<=limit:raise ValueError('Pipe envelope exceeds bound')
    return struct.pack('<I',len(raw))+raw
def read(stream,limit):
    def exact(size):
        chunks=[]
        while size:
            chunk=stream.read(size)
            if not chunk:raise ValueError('Incomplete model pipe')
            chunks.append(chunk);size-=len(chunk)
        return b''.join(chunks)
    size=struct.unpack('<I',exact(4))[0]
    if not 1<=size<=limit:raise ValueError('Invalid model frame length')
    return json.loads(exact(size),object_pairs_hook=pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Nonfinite model JSON')))
def write(stream,value,limit):
    data=memoryview(encode(value,limit))
    while data:
        count=stream.write(data)
        if not count:raise OSError('Model pipe closed')
        data=data[count:]
    stream.flush()

def conversation(incoming,outgoing,request,native):
    write(outgoing,request,REQUEST_LIMIT);calls=0;ready=False
    while True:
        value=read(incoming,RESULT_LIMIT)
        if not isinstance(value,dict) or value.get('version')!=1 or value.get('id')!=request['id']:raise ValueError('Wrong model invocation')
        if value.get('type')=='ready':
            if ready or value!={'version':1,'id':request['id'],'type':'ready','boundary':'no-capabilities-egress-denied-v1'}:raise ValueError('Invalid model boundary proof')
            ready=True
        elif not ready:raise ValueError('Model boundary proof unavailable')
        elif value.get('type')=='native':
            if set(value)!={'version','id','type','pieces'} or calls:raise ValueError('Invalid native capability')
            calls+=1;tokens=native(value['pieces'])
            write(outgoing,{'version':1,'id':request['id'],'tokens':tokens},RESULT_LIMIT)
        elif value.get('type')=='result':
            if set(value)!={'version','id','type','inputSha256','profileKey','output'} or value['inputSha256']!=request['inputSha256']:raise ValueError('Invalid result envelope')
            if incoming.read(1):raise ValueError('Trailing model output')
            return value
        else:raise ValueError('Unsupported model operation')
