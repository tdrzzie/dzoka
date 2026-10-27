package org.dzoka.fingerprints.domain;
import java.util.*;

/** AI language proposals are source-grounded copies, never confirmed language or ownership. */
public final class AutomaticShonaEvidence {
    private AutomaticShonaEvidence(){}
    private static void require(boolean ok){if(!ok)throw new IllegalArgumentException("Invalid automatic source proposals");}
    private static int offset(Object value,int max){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n)&&n==Math.rint(n)&&n>=0&&n<=max);return (int)n;}
    private static double score(Object value,double min){require(value instanceof Number);double n=((Number)value).doubleValue();require(Double.isFinite(n)&&n>=min&&n<=1);return n;}
    private static boolean boundary(String text,int index){return index==0||index==text.length()||!(Character.isHighSurrogate(text.charAt(index-1))&&Character.isLowSurrogate(text.charAt(index)));}
    public static void validateSource(String source,Map<String,Object> manifest){
        require(manifest.get("configuration") instanceof Map<?,?>);var c=(Map<?,?>)manifest.get("configuration");
        require("glotlid-english-window-v1".equals(c.get("proposalPolicy"))&&"ctranslate2-auto-v1".equals(c.get("representation"))&&"sn".equals(c.get("sourceLanguage"))&&"en".equals(c.get("targetLanguage")));
        require(!source.contains("982")&&!source.contains("[[")&&!source.contains("]]"));
    }
    public static void validate(Map<String,Object> evidence,Map<String,Object> manifest,String source){
        validateSource(source,manifest);var base=new HashMap<>(evidence);
        for(String key:List.of("proposedSpans","proposalStatus","scannedSourceUnits","proposalScanTruncated"))base.remove(key);
        GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",base,manifest);
        require(evidence.size()==base.size()+4&&evidence.get("proposedSpans") instanceof List<?>);
        var spans=(List<?>)evidence.get("proposedSpans");require(spans.size()<=8);
        require((spans.isEmpty()?"NO_ACCEPTED_WINDOW":"PROPOSED").equals(evidence.get("proposalStatus")));
        int scanned=offset(evidence.get("scannedSourceUnits"),Math.min(512,source.length()));require(boundary(source,scanned));
        require(evidence.get("proposalScanTruncated") instanceof Boolean&&((Boolean)evidence.get("proposalScanTruncated"))==(scanned<source.length()));
        String output=(String)evidence.get("text");int lastSource=0,lastOutput=0;
        for(Object item:spans){
            require(item instanceof Map<?,?>);var span=(Map<?,?>)item;
            require(span.keySet().equals(Set.of("sourceStart","sourceEnd","outputStart","outputEnd","quote","trust","language","modelScore","modelMargin")));
            require("AUTO_LANGUAGE_PROPOSAL_UNVERIFIED".equals(span.get("trust"))&&"en".equals(span.get("language")));score(span.get("modelScore"),.85);score(span.get("modelMargin"),.2);
            int start=offset(span.get("sourceStart"),scanned),end=offset(span.get("sourceEnd"),scanned),outStart=offset(span.get("outputStart"),output.length()),outEnd=offset(span.get("outputEnd"),output.length());
            require(start>=lastSource&&end>start&&end-start<=256&&outStart>=lastOutput&&outEnd>outStart&&boundary(source,start)&&boundary(source,end)&&boundary(output,outStart)&&boundary(output,outEnd));
            String quote=source.substring(start,end);require(!quote.isBlank()&&quote.equals(span.get("quote"))&&quote.equals(output.substring(outStart,outEnd)));lastSource=end;lastOutput=outEnd;
        }
    }
}
