package org.dzoka.intake.ports;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.domain.*;
public interface Reports {
    ReportReceipt submit(VerifiedIdentity actor, UUID tenant, UUID report, Integer expectedRevision, ReportDraft draft, byte[] raw, String key);
    ReportView get(VerifiedIdentity actor, UUID tenant, UUID report, Integer revision);
    IntakeReference reference(VerifiedIdentity actor,UUID tenant,UUID id,boolean caseLookup);
    void delete(VerifiedIdentity actor, UUID tenant, UUID report);
    void hold(VerifiedIdentity actor, UUID tenant, UUID report, boolean held);
    int expire(VerifiedIdentity actor, UUID tenant);
}
