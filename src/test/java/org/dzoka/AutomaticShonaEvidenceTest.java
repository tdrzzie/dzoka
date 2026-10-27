package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.AutomaticShonaEvidence;
import org.junit.jupiter.api.Test;
class AutomaticShonaEvidenceTest {
    static Map<String,Object> manifest(){return Map.of("configuration",Map.of("sourceLanguage","sn","targetLanguage","en","representation","ctranslate2-auto-v1","proposalPolicy","glotlid-english-window-v1"));}
    static Map<String,Object> evidence(String source,String output){
        var e=GenerativeEvidenceTest.translation("sn","en");e.put("text",output);e.put("representation","ctranslate2-auto-v1");e.put("scannedSourceUnits",source.length());e.put("proposalScanTruncated",false);e.put("proposalStatus","PROPOSED");
        var span=new HashMap<String,Object>();span.put("sourceStart",source.indexOf("around"));span.put("sourceEnd",source.indexOf("around")+6);span.put("outputStart",output.indexOf("around"));span.put("outputEnd",output.indexOf("around")+6);span.put("quote","around");span.put("language","en");span.put("modelScore",.99);span.put("modelMargin",.98);span.put("trust","AUTO_LANGUAGE_PROPOSAL_UNVERIFIED");e.put("proposedSpans",List.of(span));return e;
    }
    @Test void proposalsRequireExactSourceAndOutputAndUnverifiedTrust(){
        String source="Ndasiya 😀 around 2pm.",output="I left 😀 around 2pm.";var e=evidence(source,output);AutomaticShonaEvidence.validate(e,manifest(),source);
        for(var pair:Map.of("quote",(Object)"forged","trust","SOURCE_COPY_UNVERIFIED","modelScore",.84,"modelMargin",Double.NaN,"sourceStart",0,"outputEnd",0).entrySet()){
            var changed=evidence(source,output);var span=new HashMap<String,Object>((Map<String,Object>)((List<?>)changed.get("proposedSpans")).getFirst());span.put(pair.getKey(),pair.getValue());changed.put("proposedSpans",List.of(span));assertThrows(IllegalArgumentException.class,()->AutomaticShonaEvidence.validate(changed,manifest(),source));
        }
    }
    @Test void noProposalIsUnavailableLanguageCoverageAndScanBoundsAreEnforced(){
        String source="Ndarasa bhegi.";var e=evidence("around","around");e.put("text","I lost a bag.");e.put("proposedSpans",List.of());e.put("proposalStatus","NO_ACCEPTED_WINDOW");e.put("scannedSourceUnits",source.length());AutomaticShonaEvidence.validate(e,manifest(),source);
        e.put("proposalStatus","PROPOSED");assertThrows(IllegalArgumentException.class,()->AutomaticShonaEvidence.validate(e,manifest(),source));e.put("proposalStatus","NO_ACCEPTED_WINDOW");e.put("proposalScanTruncated",true);assertThrows(IllegalArgumentException.class,()->AutomaticShonaEvidence.validate(e,manifest(),source));
        for(String bad:List.of("1982","[[keep:around]]"))assertThrows(IllegalArgumentException.class,()->AutomaticShonaEvidence.validateSource(bad,manifest()));
    }
}
