package org.dzoka.rewards;

import java.util.UUID;

public interface RewardEscrowProvider {
    String settlementState(org.dzoka.identity.domain.VerifiedIdentity actor,UUID tenant,UUID reward);
}
