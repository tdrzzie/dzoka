package org.dzoka.matching.domain;

import java.util.List;
import java.util.UUID;

/** A reviewed score-to-frequency mapping for one active population and scoring policy. */
public final class Calibration {
    private Calibration() {}

    public record Bin(int fitCount, Double observedFrequency) {
        public Bin {
            if (fitCount<0 || (fitCount==0)!=(observedFrequency==null)
                    || (observedFrequency!=null && !probability(observedFrequency))) {
                throw new IllegalArgumentException("Invalid calibration bin");
            }
        }
    }

    public record Draft(UUID spaceId, String spaceKey, long activationVersion, String sourceKind,
                        String scoringVersion,String vectorBackend,String planSha256,String datasetSha256,
                        double minimumCoverage, int holdoutCount, int supportedHoldout,
                        double holdoutBrier, double rawScoreBrierOnSameRows, List<Bin> bins) {
        public Draft {
            if (spaceId==null || !digest(spaceKey) || activationVersion<1
                    || (!"LOST".equals(sourceKind) && !"FOUND".equals(sourceKind))
                    || !EvidenceScoring.VERSION.equals(scoringVersion) || !supportedBackend(vectorBackend)
                    || !digest(planSha256) || !digest(datasetSha256)
                    || !probability(minimumCoverage) || supportedHoldout<8 || holdoutCount<supportedHoldout
                    || !probability(holdoutBrier) || !probability(rawScoreBrierOnSameRows)
                    || bins==null || bins.size()!=5 || bins.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Invalid calibration draft");
            }
            bins=List.copyOf(bins);
            long fitCount=bins.stream().mapToLong(Bin::fitCount).sum();
            if (fitCount<8 || fitCount+holdoutCount>2048) throw new IllegalArgumentException("Invalid calibration support");
        }
    }

    public record Approval(String reviewSha256, double maximumBrier, int minimumBinSupport, int validHours) {
        public Approval {
            if (!digest(reviewSha256) || !probability(maximumBrier) || minimumBinSupport<2 || minimumBinSupport>2048
                    || validHours<1 || validHours>24*30) throw new IllegalArgumentException("Invalid calibration approval");
        }
        public boolean accepts(Draft draft) {
            return draft.supportedHoldout()==draft.holdoutCount() && draft.holdoutBrier()<=maximumBrier
                    && draft.holdoutBrier()<draft.rawScoreBrierOnSameRows()
                    && draft.bins().stream().anyMatch(bin -> bin.fitCount()>=minimumBinSupport);
        }
    }

    public record Estimate(UUID artifact, double probability) {}

    public static boolean digest(String value) { return value!=null && value.matches("[0-9a-f]{64}"); }
    private static boolean probability(double value) { return Double.isFinite(value) && value>=0 && value<=1; }
    private static boolean supportedBackend(String value) {
        return value!=null && java.util.Set.of("EXACT_ROUTE","PGVECTOR_EXACT","PGVECTOR_HNSW","QDRANT_EXACT").contains(value);
    }

    public static Estimate estimate(UUID artifact,Draft draft,int minimumSupport,
                                    EvidenceScoring.Score score,Matching.Evidence vector) {
        if (vector==null || !draft.spaceKey().equals(vector.spaceKey())
                || score.disposition()!=EvidenceScoring.Disposition.COMPARABLE
                || score.similarity()==null || score.coverage()<draft.minimumCoverage()) return null;
        Bin bin=draft.bins().get(Math.min(4,(int)(score.similarity()*5)));
        return bin.fitCount()<minimumSupport ? null : new Estimate(artifact,bin.observedFrequency());
    }
}
