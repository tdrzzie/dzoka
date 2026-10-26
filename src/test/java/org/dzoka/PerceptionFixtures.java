package org.dzoka;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import tools.jackson.databind.ObjectMapper;

final class PerceptionFixtures {
    static UUID id(String task){return UUID.nameUUIDFromBytes(("synthetic-profile:"+task).getBytes(StandardCharsets.UTF_8));}
    static void profiles(Connection c,UUID tenant)throws Exception{
        var root=new ObjectMapper().readTree(Files.readString(Path.of("ai/perception-profiles.json")));
        for(String task:List.of("VISION","LOCAL_FEATURES","OCR","SPEECH"))try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
            s.setObject(1,tenant);s.setObject(2,id(task));s.setString(3,task);s.setString(4,root.get(task).get("key").asString());s.setString(5,root.get(task).get("manifest").toString());s.executeUpdate();
        }
    }
    static byte[] audio(int variant){
        var b=ByteBuffer.allocate(16044).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(16036).put("WAVEfmt ".getBytes()).putInt(16)
            .putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(16000);
        for(int i=0;i<8000;i++)b.putShort((short)(Math.sin(i*(.03+variant*.001))*1000));return b.array();
    }
}
