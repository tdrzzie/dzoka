package org.dzoka.fingerprints.domain;
import java.util.*;

/** Generated fields remain private proposals; literal support is not semantic certainty. */
public final class LanguageEvidence {
    private LanguageEvidence(){}
    public static final Set<String> FIELDS=Set.of("category","brand","colour","location","time");
    private static final Set<String> COLOURS=Set.of("black","white","blue","green","red","yellow","orange","purple","pink","brown","grey","gray","silver","gold","beige","navy","cream");
    private static final Set<String> MODIFIERS=Set.of("light","dark","pale","bright","metallic","off");
    private static void require(boolean ok){if(!ok)throw new IllegalArgumentException("Invalid language evidence");}
    private static Map<?,?> map(Object v){require(v instanceof Map<?,?>);return (Map<?,?>)v;}
    private static int integer(Object v,int max){require(v instanceof Number);double n=((Number)v).doubleValue();require(Double.isFinite(n)&&n==Math.rint(n)&&n>=0&&n<=max);return (int)n;}
    private static String text(Object v,int max){require(v instanceof String);String s=(String)v;require(s.length()<=max&&s.indexOf('\0')<0);return s;}
    private static void keys(Map<?,?> m,String... names){require(m.keySet().equals(Set.of(names)));}
    private static void tokens(Map<?,?> m){require(integer(m.get("inputTokenCount"),512)>0&&m.get("inputTruncated") instanceof Boolean&&m.get("outputTruncated") instanceof Boolean);}
    public static void validate(String task,Map<String,Object> e,String source,Set<String> candidateIds){
        if(task.equals("ATTRIBUTES")){
            keys(e,"fields","inputTokenCount","inputTruncated","representation");require("qa-source-spans-v1".equals(e.get("representation")));
            require(integer(e.get("inputTokenCount"),256)>0&&e.get("inputTruncated") instanceof Boolean);
            var fields=map(e.get("fields"));require(fields.keySet().equals(FIELDS));
            for(var entry:fields.entrySet()){
                var f=map(entry.getValue());require(f.get("outputTruncated") instanceof Boolean);
                if("UNAVAILABLE".equals(f.get("state"))){keys(f,"state","reason","outputTruncated");require("NO_GROUNDED_SPAN".equals(f.get("reason")));continue;}
                if(entry.getKey().equals("time")){keys(f,"state","quote","start","end","outputTruncated","uncertainty");require("UNINTERPRETED".equals(f.get("uncertainty")));}
                else keys(f,"state","quote","start","end","outputTruncated");
                require("AVAILABLE".equals(f.get("state"))&&Boolean.FALSE.equals(f.get("outputTruncated")));
                String quote=text(f.get("quote"),256);int start=integer(f.get("start"),source.length()),end=integer(f.get("end"),source.length());
                if(entry.getKey().equals("time"))require(quote.codePoints().anyMatch(Character::isDigit)||java.util.regex.Pattern.compile("(?iu)\\b(morning|afternoon|evening|night|noon|midnight|today|yesterday|tomorrow|week|month|year|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").matcher(quote).find());
                if(entry.getKey().equals("colour")){var words=List.of(quote.toLowerCase(Locale.ROOT).split("[\\s/-]+"));require(words.stream().anyMatch(COLOURS::contains)&&words.stream().allMatch(word->COLOURS.contains(word)||MODIFIERS.contains(word)));}
                require(start<end&&!quote.isBlank()&&source.substring(start,end).equals(quote)&&!Character.isLowSurrogate(source.charAt(start))&&(end==source.length()||!Character.isLowSurrogate(source.charAt(end))));
                if(entry.getKey().equals("time"))require(!java.util.regex.Pattern.compile("(?iu)\\b(around|about|approximately|roughly|before|after|between)\\s+$").matcher(source.substring(0,start)).find());
                if(entry.getKey().equals("location")){
                    String cue="(?iu)\\b(at|in|near|inside|outside|by|from|around)\\s+(?:(the|a|an)\\s+)?";
                    require(java.util.regex.Pattern.compile(cue+"$").matcher(source.substring(0,start)).find()||java.util.regex.Pattern.compile("^"+cue).matcher(quote).find());
                }
                var match=java.util.regex.Pattern.compile("(?<!\\w)"+java.util.regex.Pattern.quote(quote)+"(?!\\w)",java.util.regex.Pattern.CASE_INSENSITIVE|java.util.regex.Pattern.UNICODE_CASE|java.util.regex.Pattern.UNICODE_CHARACTER_CLASS).matcher(source);
                require(match.find()&&match.start()==start&&match.end()==end&&!match.find());
            }return;
        }
        if(task.equals("TRANSLATION")){
            keys(e,"text","sourceLanguage","targetLanguage","inputTokenCount","inputTruncated","outputTruncated","representation");tokens(e);require(integer(e.get("inputTokenCount"),256)>0);
            require(!text(e.get("text"),2048).isBlank()&&"fr".equals(e.get("sourceLanguage"))&&"en".equals(e.get("targetLanguage"))&&"opus-fr-en-greedy-v1".equals(e.get("representation")));return;
        }
        require(task.equals("RERANK"));keys(e,"candidates","representation");require("msmarco-raw-logit-v1".equals(e.get("representation"))&&e.get("candidates") instanceof List<?>);
        var candidates=(List<?>)e.get("candidates");require(candidates.size()==candidateIds.size()&&candidates.size()>=1&&candidates.size()<=8);var seen=new HashSet<String>();double prior=Double.POSITIVE_INFINITY;String priorId="";
        for(var value:candidates){var f=map(value);keys(f,"id","rawScore","inputTokenCount","inputTruncated");String id=text(f.get("id"),36);require(candidateIds.contains(id)&&seen.add(id));
            require(f.get("rawScore") instanceof Number);double score=((Number)f.get("rawScore")).doubleValue();require(Double.isFinite(score)&&Math.abs(score)<=1000&&score<=prior&&(score!=prior||id.compareTo(priorId)>0));
            require(integer(f.get("inputTokenCount"),512)>0&&f.get("inputTruncated") instanceof Boolean);prior=score;priorId=id;
        }
    }
}
