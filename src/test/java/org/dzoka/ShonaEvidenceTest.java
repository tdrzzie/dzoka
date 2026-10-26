package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.ShonaEvidence;
import org.junit.jupiter.api.Test;
class ShonaEvidenceTest {
    static Map<String,Object> manifest(){return Map.of("configuration",Map.of("sourceLanguage","sn","targetLanguage","en","representation","ctranslate2-marked-v1","protectionPolicy","explicit-keep-utf16-v1","reservedPlaceholderPrefix","982"));}
    static Map<String,Object> evidence(String source,String quote,String output){var e=GenerativeEvidenceTest.translation("sn","en");e.put("text",output);e.put("representation","ctranslate2-marked-v1");e.put("preservedSpans",List.of(Map.of("sourceStart",source.indexOf(quote),"sourceEnd",source.indexOf(quote)+quote.length(),"outputStart",output.indexOf(quote),"outputEnd",output.indexOf(quote)+quote.length(),"quote",quote,"trust","SOURCE_COPY_UNVERIFIED")));return e;}
    @Test void exactUtf16SourceAndOutputQuotesAreRequired(){
        String source="Ndasiya [[keep:bag 😀]]",quote="bag 😀",output="I left bag 😀";var e=evidence(source,quote,output);ShonaEvidence.validate(e,manifest(),source);
        var changed=new HashMap<String,Object>((Map<String,Object>)((List<?>)e.get("preservedSpans")).getFirst());changed.put("sourceEnd",source.indexOf(quote)+quote.length()-1);e.put("preservedSpans",List.of(changed));assertThrows(IllegalArgumentException.class,()->ShonaEvidence.validate(e,manifest(),source));
        e.put("preservedSpans",List.of());assertThrows(IllegalArgumentException.class,()->ShonaEvidence.validate(e,manifest(),source));
    }
    @Test void malformedCollidingNestedAndUnmarkedInputsFailClosed(){
        for(String source:List.of("Plain source","[[keep:bag]","[[keep:outer [[keep:inner]]]]","[[keep: ]]","1982 [[keep:bag]]","[[keep:"+"a".repeat(257)+"]]"))assertThrows(IllegalArgumentException.class,()->ShonaEvidence.validateSource(source,manifest()));
        String source="[[keep:bag]]";var e=evidence(source,"bag","bag");e.put("text","cat");assertThrows(IllegalArgumentException.class,()->ShonaEvidence.validate(e,manifest(),source));
    }
}
