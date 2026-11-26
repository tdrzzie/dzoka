package org.dzoka.recovery.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Recovery {
    private static final Pattern SEPARATORS=Pattern.compile("[^\\p{L}\\p{N}]+");
    private Recovery() {}

    public enum Capacity { OWNER, REPRESENTATIVE }
    public enum Decision { APPROVE, REJECT }
    public enum Reason { INDEPENDENT_EVIDENCE, INSUFFICIENT_EVIDENCE, CONFLICT, FRAUD }
    public enum FactWithdrawal { DISCLOSED, WITHDRAWN }
    public enum FactKind {
        HIDDEN_MARK("Describe a distinctive mark on the item that is absent from its public description."),
        PRIVATE_ACCESSORY("Describe a private accessory attached to or kept with the item."),
        PRIVATE_CONTENT("Describe distinctive non-sensitive contents kept inside the item.");
        private final String prompt;
        FactKind(String prompt) { this.prompt=prompt; }
        public String prompt() { return prompt; }
    }

    public record Policy(int version,boolean dualControl,boolean allowRepresentatives) {}
    public record Claim(UUID id,UUID intake,Capacity capacity,String state) {}
    public record Challenge(UUID id,String status,String prompt,Instant expiresAt) {}
    public record Assessment(String outcome) {}
    public record ClaimRiskScore(int score,int actorFailures,int itemFailures,int competingClaims,boolean blocked) {
        public static ClaimRiskScore assess(int actorFailures,int itemFailures,int competingClaims,boolean blocked) {
            int score=Math.min(100,actorFailures*15+itemFailures*3+competingClaims*15+(blocked?30:0));
            return new ClaimRiskScore(score,actorFailures,itemFailures,competingClaims,blocked);
        }
    }
    public record ReviewContext(Claim claim,String claimantRef,ClaimRiskScore risk) {}
    public record Review(UUID session,Decision decision,Reason reason,String evidenceSha,String entitlementSha) {}
    public record Grant(UUID id,String token,Instant expiresAt) {
        @Override public String toString() { return "Grant["+id+","+expiresAt+"]"; }
    }
    public record Receipt(UUID id,Instant returnedAt) {}
    public record Redemption(boolean accepted,Receipt receipt) {}
    public record PickupContext(UUID site,String recipientRef,Capacity capacity) {}
    public record PickupMessage(UUID claim,String kind,Instant at) {}

    public static boolean digest(String value) { return value!=null && value.matches("[0-9a-f]{64}"); }
    public static String normalize(String value) {
        if (value==null) return "";
        return SEPARATORS.matcher(Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)).replaceAll("");
    }

    public static String answer(String value) {
        if (value==null || value.length()>160 || value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid challenge response");
        String normalized=normalize(value);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Invalid challenge response");
        return normalized;
    }

    public static boolean privateEnough(String normalized) {
        return normalized.length()>=10 && normalized.codePoints().distinct().count()>=6
                && normalized.codePoints().anyMatch(Character::isLetter);
    }
}
