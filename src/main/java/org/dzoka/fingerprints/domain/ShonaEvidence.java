package org.dzoka.fingerprints.domain;
import java.util.*;
import java.util.regex.Pattern;

/** Exact caller-marked source copying is unverified translation, never ownership authority. */
public final class ShonaEvidence {
    private ShonaEvidence(){}
    private static final Pattern KEEP=Pattern.compile("\\[\\[keep:([^\\[\\]\\r\\n]+)\\]\\]");
    private record Span(int start,int end,String quote){}
    private static void require(boolean ok){if(!ok)throw new IllegalArgumentException("Invalid preserved source spans");}
    private static int offset(Object value){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n)&&n==Math.rint(n)&&n>=0&&n<=32000);return (int)n;}
    private static List<Span> sourceSpans(String source,Map<String,Object> manifest){
        require(manifest.get("configuration") instanceof Map<?,?>);var config=(Map<?,?>)manifest.get("configuration");require("explicit-keep-utf16-v1".equals(config.get("protectionPolicy"))&&"ctranslate2-marked-v1".equals(config.get("representation"))&&"982".equals(config.get("reservedPlaceholderPrefix")));
        require(!source.contains("982"));var spans=new ArrayList<Span>();var matcher=KEEP.matcher(source);
        while(matcher.find()){String quote=matcher.group(1);require(!quote.isBlank()&&quote.length()<=256);spans.add(new Span(matcher.start(1),matcher.end(1),quote));}
        require(!spans.isEmpty()&&spans.size()<=8);String remainder=KEEP.matcher(source).replaceAll("");require(!remainder.contains("[[")&&!remainder.contains("]]"));return spans;
    }
    public static void validateSource(String source,Map<String,Object> manifest){sourceSpans(source,manifest);}
    public static void validate(Map<String,Object> evidence,Map<String,Object> manifest,String source){
        var spans=sourceSpans(source,manifest);require(evidence.get("preservedSpans") instanceof List<?>);var copies=(List<?>)evidence.get("preservedSpans");require(copies.size()==spans.size());
        var base=new HashMap<>(evidence);base.remove("preservedSpans");GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",base,manifest);String output=(String)evidence.get("text");int previous=0;
        for(int i=0;i<spans.size();i++){
            var span=spans.get(i);require(copies.get(i) instanceof Map<?,?>);var copy=(Map<?,?>)copies.get(i);require(copy.keySet().equals(Set.of("sourceStart","sourceEnd","outputStart","outputEnd","quote","trust")));
            require("SOURCE_COPY_UNVERIFIED".equals(copy.get("trust"))&&span.quote().equals(copy.get("quote")));require(offset(copy.get("sourceStart"))==span.start()&&offset(copy.get("sourceEnd"))==span.end());
            int start=offset(copy.get("outputStart")),end=offset(copy.get("outputEnd"));require(start>=previous&&end>start&&end<=output.length()&&span.quote().equals(output.substring(start,end)));previous=end;
        }
    }
}
