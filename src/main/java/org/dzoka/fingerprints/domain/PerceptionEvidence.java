package org.dzoka.fingerprints.domain;
import java.nio.*;
import java.util.*;

/** Fixed profile schemas; coordinates/scores are evidence, never release policy. */
public final class PerceptionEvidence {
    private PerceptionEvidence(){}
    private static void require(boolean value){if(!value)throw new IllegalArgumentException("Invalid perception evidence");}
    private static double number(Object value){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n));return n;}
    private static int integer(Object value){double n=number(value);require(n==Math.rint(n)&&n>=0&&n<=4_000_000);return (int)n;}
    private static List<?> list(Object value,int max){require(value instanceof List<?>);var list=(List<?>)value;require(list.size()<=max);return list;}
    private static Map<?,?> map(Object value){require(value instanceof Map<?,?>);return (Map<?,?>)value;}
    private static void keys(Map<?,?> value,String... keys){require(value.keySet().equals(Set.of(keys)));}
    private static void text(Object value,int max){require(value instanceof String);String s=(String)value;require(s.codePointCount(0,s.length())<=max&&s.indexOf('\0')<0);}
    private static void vector(Object value,int size){var v=list(value,size);require(v.size()==size);double norm=0;for(var x:v){double n=number(x);norm+=n*n;}require(Math.abs(norm-1)<.001);}
    private static void box(Object value,int width,int height){var b=list(value,4);require(b.size()==4);int x=integer(b.get(0)),y=integer(b.get(1)),x1=integer(b.get(2)),y1=integer(b.get(3));require(x<x1&&y<y1&&x1<=width&&y1<=height);}
    public static int wav(byte[] bytes){
        require(bytes.length>=844&&bytes.length<=320044);var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        require(b.getInt(0)==0x46464952&&b.getInt(4)==bytes.length-8&&b.getInt(8)==0x45564157&&b.getInt(12)==0x20746d66);
        require(b.getInt(16)==16&&b.getShort(20)==1&&b.getShort(22)==1&&b.getInt(24)==16000&&b.getInt(28)==32000&&b.getShort(32)==2&&b.getShort(34)==16);
        require(b.getInt(36)==0x61746164&&b.getInt(40)==bytes.length-44&&(bytes.length-44)%2==0);return (bytes.length-44)/2;
    }
    public static void validate(String task,Map<String,Object> e,int width,int height,int samples){
        validate(task,e,width,height,samples,Map.of());
    }
    public static void validate(String task,Map<String,Object> e,int width,int height,int samples,Map<String,Object> manifest){
        if(task.startsWith("JOINT_")){
            var shared=map(manifest.get("sharedSpace"));var config=map(map(shared.get("definition")).get("configuration"));
            require("joint-embedding-v1".equals(e.get("representation"))&&Objects.equals(shared.get("key"),e.get("spaceKey")));
            require(integer(config.get("dimensions"))==768&&"COSINE".equals(config.get("metric"))&&"L2".equals(config.get("normalization")));
            vector(e.get("vector"),768);
            if(task.equals("JOINT_TEXT")){
                keys(e,"vector","spaceKey","representation","tokenCount","truncated");require(integer(e.get("tokenCount"))>=1&&integer(e.get("tokenCount"))<=512&&e.get("truncated") instanceof Boolean);return;
            }
        }
        if(task.equals("SPEECH")){
            keys(e,"text","samples","sampleRate","language","representation");text(e.get("text"),2048);require(integer(e.get("samples"))==samples&&integer(e.get("sampleRate"))==16000);
            require("en".equals(e.get("language"))&&"wav2vec2-ctc-greedy-v1".equals(e.get("representation")));return;
        }
        require(integer(e.get("width"))==width&&integer(e.get("height"))==height);
        switch(task){
            case "JOINT_IMAGE" -> keys(e,"width","height","vector","spaceKey","representation");
            case "DETECTION" -> {
                keys(e,"width","height","detections","truncated","vocabulary","representation");
                require("coco-90".equals(e.get("vocabulary"))&&"ssd-coco-boxes-v1".equals(e.get("representation"))&&e.get("truncated") instanceof Boolean);
                for(var value:list(e.get("detections"),20)){var p=map(value);keys(p,"box","classId","modelScore");box(p.get("box"),width,height);
                    int label=integer(p.get("classId"));double score=number(p.get("modelScore"));require(label>=1&&label<=90&&score>=.5&&score<=1);}
            }
            case "SEGMENTATION" -> {
                keys(e,"width","height","maskWidth","maskHeight","runs","vocabulary","representation");
                require("pascal-voc-21".equals(e.get("vocabulary"))&&"semantic-mask-rle-v1".equals(e.get("representation"))&&integer(e.get("maskWidth"))==64&&integer(e.get("maskHeight"))==64);
                int total=0,previous=-1;for(var value:list(e.get("runs"),4096)){var run=list(value,2);require(run.size()==2);int label=integer(run.get(0)),count=integer(run.get(1));require(label<=20&&label!=previous&&count>0&&count<=4096);total+=count;previous=label;}require(total==4096);
            }
            case "VISION" -> {keys(e,"width","height","vector","representation");require("resnet18-logits-l2-v1".equals(e.get("representation")));vector(e.get("vector"),1000);}
            case "LOCAL_FEATURES" -> {keys(e,"width","height","points","representation");require("harris-patch4x4-v1".equals(e.get("representation")));
                for(var value:list(e.get("points"),16)){var p=map(value);keys(p,"box","descriptor");box(p.get("box"),width,height);vector(p.get("descriptor"),16);}}
            case "OCR" -> {keys(e,"width","height","regions","text","representation");require("ppocr-v6-axis-ctc-v1".equals(e.get("representation")));text(e.get("text"),2048);
                for(var value:list(e.get("regions"),16)){var p=map(value);keys(p,"box","text","modelScore");box(p.get("box"),width,height);text(p.get("text"),256);double score=number(p.get("modelScore"));require(score>=0&&score<=1);}}
            default -> throw new IllegalArgumentException("Unsupported task");
        }
    }
}
