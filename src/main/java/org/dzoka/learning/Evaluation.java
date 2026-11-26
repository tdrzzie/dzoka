package org.dzoka.learning;

import java.util.*;

/** Explicit known-case diagnostics; unavailable predictions remain separately counted. */
public final class Evaluation {
    public record Result(UUID feedback,Integer baselineRank,Integer candidateRank,Double probability) {
        public Result {
            Objects.requireNonNull(feedback);
            for(Integer rank:Arrays.asList(baselineRank,candidateRank))if(rank!=null && (rank<0 || rank>100))throw new IllegalArgumentException("Bounded rank required");
            if(probability!=null && (!Double.isFinite(probability) || probability<0 || probability>1 || candidateRank==null))throw new IllegalArgumentException("Available finite probability required");
        }
    }
    public record Metrics(int cases,int available,int positiveCases,int negativeCases,double coverage,
                          Double recallAtK,Double recallLowerBound,Double pairPrecisionAtK,Double mrr,Double singleRelevantNdcg,
                          Double falseMatchRate,Double falseNegativeRate,Double brier,int probabilityCases,Double baselineRecallAtK) {}
    public static Metrics compute(List<Result> results,Map<UUID,Boolean> labels,int k) {
        if(k<1 || k>20 || results.isEmpty() || results.size()>100 || results.size()!=labels.size())throw new IllegalArgumentException("Exact bounded test cohort required");
        var seen=new HashSet<UUID>();int available=0,positive=0,negative=0,knownPositive=0,knownNegative=0,tp=0,fp=0,fn=0,baseKnown=0,baseHits=0,probabilities=0;
        double reciprocal=0,discount=0,brier=0;
        for(var result:results) {
            if(!seen.add(result.feedback()) || !labels.containsKey(result.feedback()))throw new IllegalArgumentException("Test case mismatch");
            boolean truth=labels.get(result.feedback());if(truth)positive++;else negative++;
            if(truth && result.baselineRank()!=null){baseKnown++;if(result.baselineRank()>0 && result.baselineRank()<=k)baseHits++;}
            if(result.candidateRank()==null)continue;
            available++;boolean found=result.candidateRank()>0 && result.candidateRank()<=k;
            if(truth) {
                knownPositive++;
                if(found){tp++;reciprocal+=1.0/result.candidateRank();discount+=Math.log(2)/Math.log(result.candidateRank()+1);}else fn++;
            } else {knownNegative++;if(found)fp++;}
            if(result.probability()!=null){probabilities++;brier+=Math.pow(result.probability()-(truth?1:0),2);}
        }
        return new Metrics(results.size(),available,positive,negative,(double)available/results.size(),
                ratio(tp,knownPositive),ratio(tp,positive),ratio(tp,tp+fp),ratio(reciprocal,knownPositive),ratio(discount,knownPositive),
                ratio(fp,knownNegative),ratio(fn,knownPositive),ratio(brier,probabilities),probabilities,ratio(baseHits,baseKnown));
    }
    private static Double ratio(double value,int count){return count==0?null:value/count;}
    private Evaluation() {}
}
