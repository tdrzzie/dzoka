package org.dzoka.fingerprints.ports;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.fingerprints.domain.Perception.*;
public interface PerceptionJobs {
    Job enqueue(VerifiedIdentity actor,UUID tenant,UUID report,UUID profile,UUID media,byte[] audio);
    Job rerank(VerifiedIdentity actor,UUID tenant,UUID report,byte[] rawRequest);
    Job read(VerifiedIdentity actor,UUID tenant,UUID id);
    Optional<Lease> claim(VerifiedIdentity actor,UUID tenant,UUID profile);
    byte[] input(VerifiedIdentity actor,UUID tenant,UUID id,UUID lease);
    boolean complete(VerifiedIdentity actor,UUID tenant,UUID id,Result result);
    boolean fail(VerifiedIdentity actor,UUID tenant,UUID id,UUID lease,String reason);
    List<Component> current(VerifiedIdentity actor,UUID tenant,UUID report);
    Optional<Component> result(VerifiedIdentity actor,UUID tenant,UUID id);
    default List<Component> selected(VerifiedIdentity actor,UUID tenant,UUID report){return current(actor,tenant,report).stream().filter(c->!c.task().equals("RERANK")&&c.selection().equals("ACTIVE")).toList();}
    int purge(VerifiedIdentity actor,UUID tenant);
}
