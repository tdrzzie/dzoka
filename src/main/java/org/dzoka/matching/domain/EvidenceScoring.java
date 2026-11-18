package org.dzoka.matching.domain;

import java.util.EnumMap;
import java.util.List;

/** Scores observed evidence and tracks the policy budget independently. */
public final class EvidenceScoring {
    public static final String VERSION = "bounded-evidence-v1";
    private EvidenceScoring() {}

    public enum Group {
        DESCRIPTION(0.45), CONTEXT(0.15), VERIFIED_IDENTIFIER(0.40);
        private final double weight;
        Group(double weight) { this.weight = weight; }
    }

    public enum State { AVAILABLE, UNAVAILABLE, FAILED }
    public enum Disposition { CONFLICT, COMPARABLE, INSUFFICIENT_EVIDENCE }

    public record Signal(String kind, Group group, State state, Double similarity,
                         double reliability, String reason) {
        public Signal {
            if (kind == null || group == null || state == null || reason == null ||
                    !Double.isFinite(reliability) || reliability < 0 || reliability > 1 ||
                    (state == State.AVAILABLE) != (similarity != null) ||
                    (similarity != null && (!Double.isFinite(similarity) || similarity < 0 || similarity > 1)) ||
                    (state != State.AVAILABLE && reliability != 0)) {
                throw new IllegalArgumentException("Invalid match evidence");
            }
        }
    }

    public record Score(Double similarity, double coverage, Disposition disposition, String calibration) {}

    public static Score score(List<Signal> signals, boolean verifiedConflict) {
        var weights = new EnumMap<Group, Double>(Group.class);
        var weightedScores = new EnumMap<Group, Double>(Group.class);
        var limits = new EnumMap<Group, Double>(Group.class);
        for (Signal signal : signals) {
            if (signal.state() != State.AVAILABLE || signal.reliability() == 0) continue;
            weights.merge(signal.group(), signal.reliability(), Double::sum);
            weightedScores.merge(signal.group(), signal.reliability() * signal.similarity(), Double::sum);
            limits.merge(signal.group(), signal.reliability(), Math::max);
        }
        double covered = 0;
        double observed = 0;
        for (Group group : limits.keySet()) {
            double budget = group.weight * limits.get(group);
            covered += budget;
            observed += budget * weightedScores.get(group) / weights.get(group);
        }
        Disposition disposition = verifiedConflict ? Disposition.CONFLICT : covered > 0
                ? Disposition.COMPARABLE : Disposition.INSUFFICIENT_EVIDENCE;
        return new Score(covered == 0 ? null : clamp(observed / covered), clamp(covered),
                disposition, "UNCALIBRATED");
    }

    private static double clamp(double value) { return Math.min(1, Math.max(0, value)); }
}
