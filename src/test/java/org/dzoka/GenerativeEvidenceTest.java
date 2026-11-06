package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.*;
import org.junit.jupiter.api.Test;
class GenerativeEvidenceTest {
    @Test void extendedTranslationBoundsRequireTheNewExplicitProfile(){
        var config=new HashMap<String,Object>(Map.of("sourceLanguage","sn","targetLanguage","en","representation","marian-budget-v2","maxInputTokens",256,"maxOutputTokens",128,"suppressUnknownToken",true));
        var manifest=Map.<String,Object>of("configuration",config);var e=translation("sn","en");e.put("representation","marian-budget-v2");e.put("inputTokenCount",256);e.put("outputTokenCount",128);e.put("outputTruncated",true);e.put("finishReason","OUTPUT_LIMIT");GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest);
        assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,Map.of("configuration",Map.of("sourceLanguage","sn","targetLanguage","en","representation","marian-source-target-v1"))));
        config.put("maxOutputTokens",129);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));config.put("maxOutputTokens",128);e.put("outputTokenCount",129);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
    }
    static Map<String,Object> draft(){return new HashMap<>(Map.of("text","A private draft.","inputTokenCount",40,"outputTokenCount",5,"inputTruncated",false,"instructionTruncated",false,"contextTruncated",false,"outputTruncated",false,"finishReason","EOS","trust","UNTRUSTED_DRAFT","representation","smollm2-draft-v1"));}
    static Map<String,Object> translation(String source,String target){var result=new HashMap<String,Object>(Map.of("text","A black bag.","sourceLanguage",source,"targetLanguage",target,"inputTokenCount",8,"inputUnknownTokenCount",0,"outputTokenCount",5,"inputTruncated",false,"outputTruncated",false,"finishReason","EOS","representation","m2m100-source-target-v1"));result.put("trust","UNVERIFIED_TRANSLATION");return result;}
    @Test void draftsCannotGainAuthorityOrHideTruncation(){
        var e=draft();GenerativeEvidence.validate("LLM",e,Map.of());e.put("releaseApproved",true);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("LLM",e,Map.of()));
        e.remove("releaseApproved");e.put("trust","VERIFIED");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("LLM",e,Map.of()));e.put("trust","UNTRUSTED_DRAFT");e.put("instructionTruncated",true);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("LLM",e,Map.of()));
        e.put("inputTruncated",true);e.put("outputTruncated",true);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("LLM",e,Map.of()));e.put("finishReason","OUTPUT_LIMIT");e.put("outputTokenCount",64);GenerativeEvidence.validate("LLM",e,Map.of());
    }
    @Test void explicitDirectionsAndRepresentedCoverageAreRequired(){
        var manifest=Map.<String,Object>of("configuration",Map.of("sourceLanguage","es","targetLanguage","en"));var e=translation("es","en");GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest);
        e.put("targetLanguage","zu");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));e.put("targetLanguage","en");e.put("inputUnknownTokenCount",8);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
    }
    @Test void marianRepresentationIsBoundToProfileAndItsOwnStructuralTokenCount(){
        var manifest=Map.<String,Object>of("configuration",Map.of("sourceLanguage","sn","targetLanguage","en","representation","marian-source-target-v1"));var e=translation("sn","en");
        assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));e.put("representation","marian-source-target-v1");e.put("inputTokenCount",128);e.put("inputUnknownTokenCount",126);GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest);
        e.put("inputUnknownTokenCount",127);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));e.put("inputUnknownTokenCount",0);e.put("inputTokenCount",1);assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
        e.put("inputTokenCount",2);e.put("trust","VERIFIED");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
    }
    @Test void ctranslateDirectionAndRepresentationStayUnverified(){
        var manifest=Map.<String,Object>of("configuration",Map.of("sourceLanguage","en","targetLanguage","sn","representation","ctranslate2-marian-v1"));var e=translation("en","sn");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
        e.put("representation","ctranslate2-marian-v1");GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest);e.put("targetLanguage","en");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));e.put("targetLanguage","sn");e.put("trust","VERIFIED");assertThrows(IllegalArgumentException.class,()->GenerativeEvidence.validate("MULTILINGUAL_TRANSLATION",e,manifest));
    }
    @Test void evenActiveLookingDraftsCannotBecomeIntrinsicFingerprints(){
        UUID report=UUID.randomUUID();var c=new Perception.Component(UUID.randomUUID(),report,1,"a".repeat(64),UUID.randomUUID(),"b".repeat(64),"LLM",java.time.Instant.now(),java.time.Instant.now(),Map.of(),draft(),"ACTIVE");
        assertThrows(IllegalArgumentException.class,()->new ItemFingerprint(1,UUID.randomUUID(),UUID.randomUUID(),report,1,List.of(),Map.of(),null,0,List.of(c)));
    }
}
