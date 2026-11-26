package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.learning.Evaluation;
import org.junit.jupiter.api.Test;

class LearningEvaluationTest {
    @Test void unavailableEvidenceDoesNotBecomeNegativeAndCoverageIsSeparate() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();var labels=Map.of(a,true,b,true,c,false);
        var m=Evaluation.compute(List.of(new Evaluation.Result(a,2,1,.9),new Evaluation.Result(b,null,null,null),new Evaluation.Result(c,0,0,.1)),labels,5);
        assertEquals(2.0/3,m.coverage());assertEquals(1.0,m.recallAtK());assertEquals(.5,m.recallLowerBound());assertEquals(.01,m.brier(),1e-10);assertEquals(0,m.falseMatchRate());
        assertThrows(IllegalArgumentException.class,()->Evaluation.compute(List.of(new Evaluation.Result(a,1,1,.9)),labels,5));
        assertThrows(IllegalArgumentException.class,()->new Evaluation.Result(a,1,null,.9));
        assertThrows(IllegalArgumentException.class,()->new Evaluation.Result(a,1,1,Double.NaN));
    }
}
