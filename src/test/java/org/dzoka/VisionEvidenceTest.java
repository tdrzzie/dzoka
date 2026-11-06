package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.PerceptionEvidence;
import org.junit.jupiter.api.Test;
class VisionEvidenceTest {
    @Test void alternativeOutputsCannotMasqueradeAsLegacyProfiles(){
        var e=new HashMap<String,Object>(Map.of("width",100,"height",80,"detections",List.of(Map.of("box",List.of(0,0,100,80),"classId",44,"modelScore",.9)),"truncated",false,"vocabulary","coco-90","representation","maskrcnn-coco-boxes-v2"));
        assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("DETECTION",e,100,80,0));
        var manifest=Map.<String,Object>of("configuration",Map.of("outputRepresentation","maskrcnn-coco-boxes-v2"));PerceptionEvidence.validate("DETECTION",e,100,80,0,manifest);
        e.put("representation","ssd-coco-boxes-v1");assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("DETECTION",e,100,80,0,manifest));
        var speech=Map.<String,Object>of("text","Seven.","samples",16000,"sampleRate",16000,"language","en","representation","whisper-en-greedy-v2");
        assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("SPEECH",speech,0,0,16000));PerceptionEvidence.validate("SPEECH",speech,0,0,16000,Map.of("configuration",Map.of("outputRepresentation","whisper-en-greedy-v2")));
    }
    @Test void typedBoxesAndCanonicalMasksRejectOverflowAndInvalidVocabulary(){
        var box=new HashMap<String,Object>(Map.of("width",100,"height",80,"detections",List.of(Map.of("box",List.of(0,0,100,80),"classId",1,"modelScore",.9)),"truncated",false,"vocabulary","coco-90","representation","ssd-coco-boxes-v1"));
        PerceptionEvidence.validate("DETECTION",box,100,80,0);box.put("detections",List.of(Map.of("box",List.of(0,0,101,80),"classId",1,"modelScore",.9)));assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("DETECTION",box,100,80,0));
        var mask=new HashMap<String,Object>(Map.of("width",100,"height",80,"maskWidth",64,"maskHeight",64,"runs",List.of(List.of(0,4096)),"vocabulary","pascal-voc-21","representation","semantic-mask-rle-v1"));
        PerceptionEvidence.validate("SEGMENTATION",mask,100,80,0);
        for(var bad:List.of(List.of(List.of(0,4095)),List.of(List.of(21,4096)),List.of(List.of(0,2000),List.of(0,2096)),List.of(List.of(0,0),List.of(1,4096)))){
            mask.put("runs",bad);assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("SEGMENTATION",mask,100,80,0));
        }
    }
}
