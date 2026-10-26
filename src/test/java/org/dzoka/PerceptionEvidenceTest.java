package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.PerceptionEvidence;
import org.junit.jupiter.api.Test;

class PerceptionEvidenceTest {
    @Test void finiteShapePixelBoundsAndEmptyEvidenceAreDifferentFromInvalidOutput(){
        PerceptionEvidence.validate("SPEECH",Map.of("text","","samples",8000,"sampleRate",16000,"language","en","representation","wav2vec2-ctc-greedy-v1"),0,0,8000);
        var empty=Map.<String,Object>of("width",100,"height",80,"points",List.of(),"representation","harris-patch4x4-v1");PerceptionEvidence.validate("LOCAL_FEATURES",empty,100,80,0);
        var bad=new HashMap<>(empty);bad.put("points",List.of(Map.of("box",List.of(0,0,101,80),"descriptor",Collections.nCopies(16,.25))));
        assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("LOCAL_FEATURES",bad,100,80,0));
        var vector=new ArrayList<Double>(Collections.nCopies(1000,0.));vector.set(0,Double.NaN);
        assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.validate("VISION",Map.of("width",100,"height",80,"vector",vector,"representation","resnet18-logits-l2-v1"),100,80,0));
        byte[] wav=PerceptionFixtures.audio(1);assertEquals(8000,PerceptionEvidence.wav(wav));wav[24]=0;assertThrows(IllegalArgumentException.class,()->PerceptionEvidence.wav(wav));
    }
}
