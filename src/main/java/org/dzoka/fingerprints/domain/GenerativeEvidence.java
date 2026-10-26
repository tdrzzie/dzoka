package org.dzoka.fingerprints.domain;
import java.util.*;

/** Text generation is untrusted data; this contract conveys no executable action or authority. */
public final class GenerativeEvidence {
    private GenerativeEvidence(){}
    private static void require(boolean ok){if(!ok)throw new IllegalArgumentException("Invalid generative evidence");}
    private static int integer(Object value,int max){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n)&&n==Math.rint(n)&&n>=1&&n<=max);return (int)n;}
    private static int nonnegative(Object value,int max){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n)&&n==Math.rint(n)&&n>=0&&n<=max);return (int)n;}
    private static boolean flag(Object value){require(value instanceof Boolean);return (Boolean)value;}
    public static void validate(String task,Map<String,Object> e,Map<String,Object> manifest){
        boolean llm=task.equals("LLM");require(llm||task.equals("MULTILINGUAL_TRANSLATION"));
        require(e.get("text") instanceof String);String text=(String)e.get("text");require(!text.isBlank()&&text.length()<=(llm?4096:2048)&&text.indexOf('\0')<0);
        integer(e.get("inputTokenCount"),llm?512:128);int tokens=integer(e.get("outputTokenCount"),llm?64:32);
        boolean cut=flag(e.get("outputTruncated"));require(cut?"OUTPUT_LIMIT".equals(e.get("finishReason"))&&tokens==(llm?64:32):"EOS".equals(e.get("finishReason")));
        flag(e.get("inputTruncated"));
        if(llm){
            require(e.keySet().equals(Set.of("text","inputTokenCount","outputTokenCount","inputTruncated","outputTruncated","finishReason","instructionTruncated","contextTruncated","trust","representation")));
            require("UNTRUSTED_DRAFT".equals(e.get("trust"))&&"smollm2-draft-v1".equals(e.get("representation")));
            require(flag(e.get("inputTruncated"))==(flag(e.get("instructionTruncated"))|flag(e.get("contextTruncated"))));
        }else{
            require(e.keySet().equals(Set.of("text","sourceLanguage","targetLanguage","inputTokenCount","inputUnknownTokenCount","outputTokenCount","inputTruncated","outputTruncated","finishReason","trust","representation")));
            require("UNVERIFIED_TRANSLATION".equals(e.get("trust"))&&e.get("sourceLanguage") instanceof String&&e.get("targetLanguage") instanceof String);
            require(nonnegative(e.get("inputUnknownTokenCount"),126)<=integer(e.get("inputTokenCount"),128)-2);
            require("m2m100-source-target-v1".equals(e.get("representation"))&&manifest.get("configuration") instanceof Map<?,?>);
            var config=(Map<?,?>)manifest.get("configuration");require(Objects.equals(config.get("sourceLanguage"),e.get("sourceLanguage"))&&Objects.equals(config.get("targetLanguage"),e.get("targetLanguage"))&&!Objects.equals(e.get("sourceLanguage"),e.get("targetLanguage")));
        }
    }
}
