package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import org.dzoka.fingerprints.domain.*;
import org.junit.jupiter.api.Test;
class EmbeddingSpaceTest {
    private EmbeddingSpace space(String artifact){
        String key=EmbeddingSpace.key("onnxruntime-cpu","synthetic-model","reviewed-revision",artifact,"b".repeat(64),"v1",2,"COSINE","L2","c".repeat(64));
        return new EmbeddingSpace(UUID.randomUUID(),"onnxruntime-cpu","synthetic-model","reviewed-revision",artifact,"b".repeat(64),"v1",2,"COSINE","L2","c".repeat(64),key);
    }
    @Test void dimensionsDoNotEstablishCompatibilityAndResultRejectsFabricatedVectors(){
        assertFalse(space("a".repeat(64)).compatibleWith(space("d".repeat(64))));
        assertTrue(space("a".repeat(64)).compatibleWith(space("a".repeat(64))));
        for(double[] invalid:List.of(new double[]{0,0},new double[]{Double.NaN,1},new double[]{Double.POSITIVE_INFINITY,0},new double[]{2,0}))
            assertThrows(IllegalArgumentException.class,()->new EmbeddingResult(UUID.randomUUID(),"a".repeat(64),"b".repeat(64),"onnxruntime-cpu","text-embedding-v1",Instant.now(),invalid,3,false));
        var input=new double[]{1,0};var output=new EmbeddingResult(UUID.randomUUID(),"a".repeat(64),"b".repeat(64),"onnxruntime-cpu","text-embedding-v1",Instant.now(),input,3,false);
        input[0]=0;assertEquals(1,output.vector()[0]);var copy=output.vector();copy[0]=0;assertEquals(1,output.vector()[0]);
    }
}
