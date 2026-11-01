"""Bounded authenticated loopback IPC; no Docker commands or core credentials."""
import json
import os
import socket
import struct
import time

def read_frame(stream,limit,seconds):
    until=time.monotonic()+seconds
    def exact(size):
        parts=[]
        while size:
            remaining=until-time.monotonic()
            if remaining<=0:raise ValueError('IPC deadline')
            stream.settimeout(remaining);part=stream.recv(size)
            if not part:raise ValueError('IPC incomplete')
            parts.append(part);size-=len(part)
        return b''.join(parts)
    size=struct.unpack('!I',exact(4))[0]
    if size<2 or size>limit:raise ValueError('IPC bound')
    def unique(pairs):
        result={}
        for key,value in pairs:
            if key in result:raise ValueError('IPC duplicate key')
            result[key]=value
        return result
    def invalid(_):raise ValueError('IPC numeric constant')
    return json.loads(exact(size),object_pairs_hook=unique,parse_constant=invalid)

def send_frame(stream,value,limit):
    raw=json.dumps(value,ensure_ascii=False,allow_nan=False,separators=(',',':')).encode()
    if len(raw)>limit:raise ValueError('IPC bound')
    stream.settimeout(5);stream.sendall(struct.pack('!I',len(raw))+raw)

def call(operation,**values):
    port=os.environ.get('DZOKA_AI_DOCKER_PORT','');key=os.environ.get('DZOKA_AI_DOCKER_KEY','')
    if not port.isdecimal() or not 1<=int(port)<=65535 or len(key)!=64 or any(c not in '0123456789abcdef' for c in key):raise ValueError('Guardian unavailable')
    with socket.create_connection(('127.0.0.1',int(port)),timeout=5) as stream:
        send_frame(stream,{'key':key,'operation':operation,**values},300000)
        result=read_frame(stream,100000,180)
    if not isinstance(result,dict) or result.get('ok') is not True:raise ValueError('Guardian unavailable')
    return result
