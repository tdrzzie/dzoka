package org.dzoka.channels.adapters;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.channels.ports.MessagingProvider;
import org.dzoka.channels.ports.MessagingProvider.Template;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.dzoka.intake.domain.ReportReceipt;
import org.springframework.stereotype.Component;

@Component
public final class ChannelOperations {
    private static final String SERVICE="channels:service";
    private final TenantTransactions transactions;
    private final WhatsApp whatsapp;
    private final MessagingProvider provider;
    private final IntakeService intake;
    private final ChannelConversation conversation;
    private final ChannelMatching matching;
    private final org.dzoka.media.application.MediaService media;
    public ChannelOperations(TenantTransactions transactions,WhatsApp whatsapp,MessagingProvider provider,IntakeService intake,
                             ChannelConversation conversation,ChannelMatching matching,org.dzoka.media.application.MediaService media) {
        this.transactions=transactions;this.whatsapp=whatsapp;this.provider=provider;this.intake=intake;
        this.conversation=conversation;this.matching=matching;this.media=media;
    }

    public record Enrollment(String subject,String address,Boolean consentConfirmed) {
        @Override public String toString() { return "PrivateContactEnrollment"; }
    }
    public enum Phrase { REQUEST_UPDATE, READY_TO_ARRANGE_PICKUP, THANK_YOU }
    public record Message(UUID id,boolean yours,Phrase phrase,Instant at) {}
    public record ThreadView(boolean blocked,boolean flagged,List<Message> messages) {}
    private record Contact(UUID id,String member,String route,boolean enabled) {}
    private record Lease(UUID id,UUID contact,String route,Template template,int attempt,String protectedReply) {}
    private record Watch(Contact contact,UUID report) {}
    private record MediaJob(String event,Contact contact,String payload,Instant until,int attempt) {}
    private record ReportAnchor(UUID id,int revision) {}

    public void enroll(VerifiedIdentity actor,UUID tenant,UUID id,Enrollment enrollment) {
        verifyTenant(tenant);
        if(!WhatsApp.address(enrollment.address()) || !Boolean.TRUE.equals(enrollment.consentConfirmed()))
            throw new IllegalArgumentException("Verified consent is required");
        var target=new VerifiedIdentity(actor.issuer(),enrollment.subject());
        if(target.subject().indexOf('\n')>=0 || target.issuer().indexOf('\n')>=0) throw new IllegalArgumentException("Invalid contact");
        String routing=target.issuer()+"\n"+target.subject()+"\n"+enrollment.address();
        String addressKey=whatsapp.fingerprint("address",enrollment.address());
        transactions.execute(actor,tenant,"channels:admin",db -> {
            lock(db,tenant);
            try(var query=q(db,"SELECT address_key,member_ref FROM dzoka.channel_contacts WHERE tenant_id=? AND id=?",tenant,id);
                var rows=query.executeQuery()) {
                if(rows.next()) {
                    if(!addressKey.equals(rows.getString(1)) || !target.reference().equals(rows.getString(2))) throw new Failures.Conflict();
                    return null;
                }
            }
            if(write(db,"""
                    INSERT INTO dzoka.channel_contacts(tenant_id,id,member_ref,address_key,route,enabled) VALUES(?,?,?,?,?,true)
                    ON CONFLICT DO NOTHING
                    """,tenant,id,target.reference(),addressKey,whatsapp.protect(id,routing))!=1) throw new Failures.Conflict();
            return null;
        });
    }

    public void optOut(VerifiedIdentity actor,UUID tenant) {
        verifyTenant(tenant);
        transactions.execute(actor,tenant,"channels:use",db -> {
            lock(db,tenant);
            write(db,"UPDATE dzoka.channel_contacts SET enabled=false WHERE tenant_id=? AND member_ref=?",tenant,actor.reference());
            return null;
        });
    }

    public void consent(VerifiedIdentity actor,UUID tenant,UUID contact,boolean enabled) {
        verifyTenant(tenant);
        transactions.execute(actor,tenant,"channels:admin",db -> {
            lock(db,tenant);
            if(write(db,"UPDATE dzoka.channel_contacts SET enabled=? WHERE tenant_id=? AND id=?",enabled,tenant,contact)!=1)
                throw new Failures.NotFound();
            return null;
        });
    }

    public String notify(VerifiedIdentity actor,UUID tenant,UUID id,UUID contact) {
        verifyTenant(tenant);
        return transactions.execute(actor,tenant,"channels:admin",db -> {
            lock(db,tenant);
            return enqueue(db,tenant,id,contact,Template.RECOVERY_UPDATE);
        });
    }

    public String status(VerifiedIdentity actor,UUID tenant,UUID id) {
        verifyTenant(tenant);
        return transactions.execute(actor,tenant,"channels:use",db -> {
            try(var query=q(db,"SELECT state FROM dzoka.channel_operations WHERE tenant_id=? AND id=?",tenant,id);
                var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                return row.getString(1);
            }
        });
    }

    public record Notice(UUID id,Template template,String state) {}

    public void publishMatchOffer(VerifiedIdentity actor,UUID tenant,UUID report,int revision,String summary,boolean enabled) {
        verifyTenant(tenant);
        matching.publish(actor,tenant,report,revision,summary,enabled);
    }
    public List<Notice> notices(VerifiedIdentity actor,UUID tenant) {
        verifyTenant(tenant);
        return transactions.execute(actor,tenant,"channels:use",db -> {
            var result=new ArrayList<Notice>();
            try(var query=q(db,"""
                    SELECT o.id,o.template,o.state FROM dzoka.channel_operations o
                    JOIN dzoka.channel_contacts c ON (c.tenant_id,c.id)=(o.tenant_id,o.contact_id)
                    WHERE o.tenant_id=? AND c.member_ref=? ORDER BY o.created_at DESC,o.id DESC LIMIT 100
                    """,tenant,actor.reference());var rows=query.executeQuery()) {
                while(rows.next()) result.add(new Notice(rows.getObject(1,UUID.class),Template.valueOf(rows.getString(2)),rows.getString(3)));
            }
            return List.copyOf(result);
        });
    }

    public void observe(WhatsApp.Batch batch) {
        UUID tenant=whatsapp.tenant();
        for(var message:batch.messages()) inbound(tenant,message);
        for(var delivery:batch.deliveries()) delivery(tenant,delivery);
    }

    private void inbound(UUID tenant,WhatsApp.Inbound message) {
        String event=whatsapp.fingerprint("message",message.id());
        String input=whatsapp.fingerprint("input",message.attachment()!=null
                ? "3"+message.attachment()+"\n"+message.replyTo()
                : message.place()!=null
                ? "2"+message.place().replyTo()+"\n"+message.place().source()
                : message.text()==null?"0":"1"+message.text()+"\n"+message.replyTo());
        Contact contact=transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            lock(db,tenant);
            Contact candidate=contact(db,tenant,whatsapp.fingerprint("address",message.address()),timestamp(Instant.ofEpochSecond(message.timestamp())));
            if(candidate==null || !candidate.enabled()) return null;
            try(var query=q(db,"SELECT input_key,processed,contact_id FROM dzoka.channel_events WHERE tenant_id=? AND event_key=?",tenant,event);
                var row=query.executeQuery()) {
                if(row.next()) return !row.getBoolean(2) && input.equals(row.getString(1))
                        && candidate.id().equals(row.getObject(3,UUID.class))?candidate:null;
            }
            try(var query=q(db,"""
                    SELECT count(*)>=30 FROM (SELECT 1 FROM dzoka.channel_events WHERE tenant_id=? AND contact_id=?
                        AND received_at>clock_timestamp()-interval '1 hour' LIMIT 30) recent
                    """,tenant,candidate.id());var row=query.executeQuery()) {
                row.next();
                if(row.getBoolean(1) && !"STOP".equalsIgnoreCase(message.text())) return null;
            }
            write(db,"""
                    INSERT INTO dzoka.channel_events(tenant_id,event_key,contact_id,kind,input_key,processed)
                    VALUES(?,?,?,'INBOUND',?,false)
                    """,tenant,event,candidate.id(),input);
            return candidate;
        });
        if(contact==null) return;
        if(message.attachment()!=null) {
            queueAttachment(tenant,event,contact,message);
            return;
        }
        String text=message.text();
        Template response=Template.HELP;
        String sessionReply=null;
        ReportReceipt report=null;
        if(message.place()!=null) {
            try { report=locate(tenant,event,contact,message.place()); }
            catch(Failures.Forbidden | Failures.NotFound | Failures.Conflict | IllegalArgumentException denied) { }
            response=report==null?Template.UNSUPPORTED:Template.REPORT_SAVED;
        }
        else if(text!=null && text.length()<=4000 && text.indexOf('\0')<0 && conversation.handles(text)) {
            if(message.timestamp()>Instant.now().minusSeconds(23*3600).getEpochSecond()) {
                try {
                    var anchor=anchor(tenant,contact,message.replyTo());
                    String quoteKey=anchor==null?null:whatsapp.fingerprint("message",message.replyTo());
                    sessionReply=conversation.reply(verified(contact),tenant,contact.id(),anchor==null?null:anchor.id(),
                            operation(event),quoteKey,text);
                } catch(Failures.Forbidden | Failures.NotFound | Failures.Conflict | IllegalArgumentException denied) {
                    sessionReply="That action is unavailable. Use SEARCH as a reply to your report or contact the recovery desk.";
                }
            } else response=Template.UNSUPPORTED;
        }
        else if(text==null || text.length()>4000 || text.indexOf('\0')>=0) response=Template.UNSUPPORTED;
        else if(text.startsWith("LOST ") || text.startsWith("FOUND ")) {
            int separator=text.indexOf(' ');
            String description=text.substring(separator+1);
            if(description.isBlank()) response=Template.HELP;
            else {
                try {
                    report=intake.submit(verified(contact),tenant,null,null,new ReportDraft(ReportDraft.Kind.valueOf(text.substring(0,separator)),
                            description,"en","UTC",null,null,null,null,null,null,null),
                            text.getBytes(StandardCharsets.UTF_8),"whatsapp:"+event);
                    response=Template.REPORT_SAVED;
                } catch(Failures.Forbidden | IllegalArgumentException denied) { response=Template.UNSUPPORTED; }
            }
        }
        Template reply=response;
        String body=sessionReply;
        ReportReceipt receipt=report;
        transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            lock(db,tenant);
            int completed=write(db,"""
                    UPDATE dzoka.channel_events SET processed=true,report_id=?,report_revision=?
                    WHERE tenant_id=? AND event_key=? AND NOT processed
                    """,receipt==null?null:receipt.reportId(),receipt==null?null:receipt.revision(),tenant,event);
            if(completed==0) return null;
            if(receipt!=null && receipt.revision()==1) write(db,"""
                    INSERT INTO dzoka.channel_match_watches(tenant_id,contact_id,report_id,original_event,due_at)
                    VALUES(?,?,?,?,CASE WHEN ? THEN clock_timestamp()+interval '5 minutes' ELSE 'infinity'::timestamptz END)
                    ON CONFLICT DO NOTHING
                    """,tenant,contact.id(),receipt.reportId(),event,text!=null && text.startsWith("LOST "));
            if("STOP".equalsIgnoreCase(text)) write(db,"UPDATE dzoka.channel_contacts SET enabled=false WHERE tenant_id=? AND id=?",
                    tenant,contact.id());
            else if(text!=null && text.startsWith("RELAY ")) relayInbound(db,tenant,contact,text,operation(event));
            else if(body==null) enqueue(db,tenant,operation(event),contact.id(),reply);
            else if(message.timestamp()>Instant.now().minusSeconds(23*3600).getEpochSecond())
                enqueue(db,tenant,operation(event),contact.id(),Template.SESSION_REPLY,
                        whatsapp.protect(operation(event),body),Instant.ofEpochSecond(message.timestamp()+23*3600));
            return null;
        });
    }

    private void queueAttachment(UUID tenant,String event,Contact contact,WhatsApp.Inbound message) {
        var attachment=message.attachment();
        var anchor=anchor(tenant,contact,message.replyTo());
        var until=Instant.ofEpochSecond(message.timestamp()+23*3600);
        transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            lock(db,tenant);
            if(anchor==null || !until.isAfter(Instant.now())) {
                int changed=write(db,"UPDATE dzoka.channel_events SET processed=true WHERE tenant_id=? AND event_key=? AND NOT processed",
                        tenant,event);
                if(changed!=0) enqueue(db,tenant,operation(event),contact.id(),Template.UNSUPPORTED);
            } else write(db,"""
                    UPDATE dzoka.channel_events SET attachment_payload=?,attachment_until=?,attachment_due=clock_timestamp()
                    WHERE tenant_id=? AND event_key=? AND NOT processed AND attachment_payload IS NULL
                    """,whatsapp.protect(operation(event),String.join("\n",anchor.id().toString(),attachment.id(),
                            attachment.type(),attachment.sha256())),timestamp(until),tenant,event);
            return null;
        });
    }

    private void processAttachment(VerifiedIdentity dispatcher,UUID tenant) {
        MediaJob job=transactions.execute(dispatcher,tenant,SERVICE,db -> {
            lock(db,tenant);
            write(db,"""
                    UPDATE dzoka.channel_events e SET processed=true,attachment_payload=NULL
                    FROM dzoka.channel_contacts c
                    WHERE e.tenant_id=? AND c.tenant_id=e.tenant_id AND c.id=e.contact_id AND e.attachment_payload IS NOT NULL
                    AND (NOT c.enabled OR c.consented_at>e.received_at OR e.attachment_until<=clock_timestamp())
                    """,tenant);
            try(var query=q(db,"""
                    SELECT e.event_key,c.id,c.member_ref,c.route,e.attachment_payload,e.attachment_until,e.attachment_attempts
                    FROM dzoka.channel_events e JOIN dzoka.channel_contacts c ON c.tenant_id=e.tenant_id AND c.id=e.contact_id
                    WHERE e.tenant_id=? AND e.attachment_payload IS NOT NULL AND e.attachment_due<=clock_timestamp()
                    ORDER BY e.attachment_due,e.event_key LIMIT 1
                    """,tenant);var row=query.executeQuery()) {
                if(!row.next()) return null;
                var jobLease=new MediaJob(row.getString(1),new Contact(row.getObject(2,UUID.class),row.getString(3),row.getString(4),true),
                        row.getString(5),row.getObject(6,java.time.OffsetDateTime.class).toInstant(),Math.min(3,row.getInt(7)+1));
                write(db,"""
                        UPDATE dzoka.channel_events SET attachment_attempts=?,attachment_due=clock_timestamp()+interval '2 minutes'
                        WHERE tenant_id=? AND event_key=?
                        """,jobLease.attempt(),tenant,jobLease.event());
                return jobLease;
            }
        });
        if(job==null) return;
        String[] fields=whatsapp.reveal(operation(job.event()),job.payload()).split("\n",-1);
        String reply=null;
        if(job.attempt()<3) {
            try {
                UUID report=UUID.fromString(fields[0]);
                var identity=verified(job.contact());
                String key="whatsapp:"+job.event();
                media.authorizeUpload(identity,tenant,report,key);
                if(intake.get(identity,tenant,report,null).legalHold()) throw new Failures.Conflict();
                media.upload(identity,tenant,report,key,provider.download(fields[1],fields[2],fields[3]),fields[2]);
                reply=fields[2].startsWith("image/")?"Photo received. It will be checked before it can be viewed."
                        :"Attachment received in private quarantine. Playback and transcription are unavailable.";
            } catch(Failures.Unavailable retryable) { return; }
            catch(Failures.Forbidden | Failures.NotFound | Failures.Conflict | IllegalArgumentException denied) { }
        }
        String body=reply;
        transactions.execute(dispatcher,tenant,SERVICE,db -> {
            lock(db,tenant);
            int done=write(db,"""
                    UPDATE dzoka.channel_events e SET processed=true,attachment_payload=NULL FROM dzoka.channel_contacts c
                    WHERE e.tenant_id=? AND e.event_key=? AND NOT e.processed AND e.attachment_attempts=?
                     AND c.tenant_id=e.tenant_id AND c.id=e.contact_id AND c.enabled AND c.consented_at<=e.received_at
                    """,tenant,job.event(),job.attempt());
            if(done!=0 && job.until().isAfter(Instant.now())) {
                UUID id=operation(job.event());
                if(body==null) enqueue(db,tenant,id,job.contact().id(),Template.UNSUPPORTED);
                else enqueue(db,tenant,id,job.contact().id(),Template.SESSION_REPLY,whatsapp.protect(id,body),job.until());
            }
            return null;
        });
    }

    private ReportReceipt locate(UUID tenant,String event,Contact contact,WhatsApp.Place place) {
        ReportAnchor anchor=anchor(tenant,contact,place.replyTo());
        if(anchor==null) return null;
        var actor=verified(contact);
        var previous=intake.get(actor,tenant,anchor.id(),anchor.revision());
        if(previous.legalHold()) throw new Failures.Conflict();
        // WhatsApp points provide no horizontal accuracy. Preserve that uncertainty instead of assuming a precise observation.
        var draft=new ReportDraft(previous.kind(),previous.rawText(),previous.language(),previous.timezone(),
                previous.rawLocation(),previous.rawTime(),place.latitude(),place.longitude(),200000.0,
                previous.occurredAfter(),previous.occurredBefore(),previous.category());
        return intake.submit(actor,tenant,anchor.id(),anchor.revision(),draft,place.source().getBytes(StandardCharsets.UTF_8),"whatsapp:"+event);
    }

    private ReportAnchor anchor(UUID tenant,Contact contact,String quoted) {
        if(quoted==null) return null;
        return transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            String key=whatsapp.fingerprint("message",quoted);
            try(var q=q(db,"""
                    SELECT report_id,report_revision FROM dzoka.channel_events
                    WHERE tenant_id=? AND contact_id=? AND event_key=? AND processed AND report_id IS NOT NULL
                    UNION ALL
                    SELECT report_id,1 FROM dzoka.channel_match_watches WHERE tenant_id=? AND contact_id=? AND original_event=?
                    LIMIT 1
                    """,tenant,contact.id(),key,tenant,contact.id(),key);var row=q.executeQuery()) {
                return row.next()?new ReportAnchor(row.getObject(1,UUID.class),row.getInt(2)):null;
            }
        });
    }

    private VerifiedIdentity verified(Contact contact) {
        String[] route=whatsapp.reveal(contact.id(),contact.route()).split("\n",-1);
        return new VerifiedIdentity(route[0],route[1]);
    }

    private void relayInbound(Connection db,UUID tenant,Contact contact,String text,UUID event) throws SQLException {
        try {
            String[] fields=text.split(" ",-1);
            if(fields.length!=3) return;
            UUID thread=UUID.fromString(fields[1]);
            Phrase phrase=Phrase.valueOf(fields[2]);
            relay(db,tenant,thread,event,contact.member(),phrase);
        } catch(IllegalArgumentException | Failures.NotFound | Failures.Conflict ignored) { }
    }

    public void openRelay(VerifiedIdentity actor,UUID tenant,UUID thread,UUID claim) {
        transactions.execute(actor,tenant,"channels:moderate",db -> {
            lock(db,tenant);
            try(var query=q(db,"""
                    INSERT INTO dzoka.relay_threads(tenant_id,id,intake_id,claim_id,claimant_ref,finder_ref)
                    SELECT c.tenant_id,?,i.id,c.id,c.actor_ref,r.owner_ref FROM dzoka.ownership_claims c
                    JOIN dzoka.recovery_intakes i ON (i.tenant_id,i.id)=(c.tenant_id,c.intake_id)
                    JOIN dzoka.reports r ON (r.tenant_id,r.id)=(i.tenant_id,i.report_id)
                    WHERE c.tenant_id=? AND c.id=? AND c.state IN ('PENDING','APPROVED') AND c.actor_ref<>r.owner_ref
                    AND NOT i.blocked AND NOT i.returned AND r.deleted_at IS NULL AND r.expires_at>clock_timestamp()
                    AND dzoka.permitted('recovery:review') AND dzoka.permitted('reports:read:any')
                    ON CONFLICT DO NOTHING
                    """,thread,tenant,claim)) {
                if(query.executeUpdate()!=1) try(var existing=q(db,"""
                        SELECT 1 FROM dzoka.relay_threads WHERE tenant_id=? AND id=? AND claim_id=?
                          AND dzoka.permitted('recovery:review') AND dzoka.permitted('reports:read:any')
                        """,tenant,thread,claim);var rows=existing.executeQuery()) {
                    if(!rows.next()) throw new Failures.Conflict();
                }
            }
            return null;
        });
    }

    public void relay(VerifiedIdentity actor,UUID tenant,UUID thread,UUID id,Phrase phrase) {
        if(phrase==null) throw new IllegalArgumentException("Required relay phrase");
        transactions.execute(actor,tenant,"channels:use",db -> {
            lock(db,tenant);
            relay(db,tenant,thread,id,actor.reference(),phrase);
            return null;
        });
    }

    private static void relay(Connection db,UUID tenant,UUID thread,UUID id,String sender,Phrase phrase) throws SQLException {
        try(var query=q(db,"""
                SELECT 1 FROM dzoka.relay_threads
                WHERE tenant_id=? AND id=? AND ? IN (claimant_ref,finder_ref) AND dzoka.relay_available(id)
                """,tenant,thread,sender);var row=query.executeQuery()) {
            if(!row.next()) throw new Failures.NotFound();
        }
        try(var query=q(db,"""
                SELECT sender_ref,phrase FROM dzoka.relay_entries WHERE tenant_id=? AND thread_id=? AND id=?
                """,tenant,thread,id);var row=query.executeQuery()) {
            if(row.next()) {
                if(!sender.equals(row.getString(1)) || !phrase.name().equals(row.getString(2))) throw new Failures.Conflict();
                return;
            }
        }
        try(var query=q(db,"""
                SELECT count(*)>=30 FROM (SELECT 1 FROM dzoka.relay_entries WHERE tenant_id=? AND thread_id=?
                    AND created_at>clock_timestamp()-interval '1 hour' LIMIT 30) recent
                """,tenant,thread);var row=query.executeQuery()) {
            row.next();if(row.getBoolean(1)) throw new Failures.Conflict();
        }
        write(db,"INSERT INTO dzoka.relay_entries(tenant_id,thread_id,id,sender_ref,phrase) VALUES(?,?,?,?,?)",
                tenant,thread,id,sender,phrase.name());
    }

    public ThreadView thread(VerifiedIdentity actor,UUID tenant,UUID thread) {
        return thread(actor,tenant,thread,false);
    }

    public ThreadView moderate(VerifiedIdentity actor,UUID tenant,UUID thread) {
        return thread(actor,tenant,thread,true);
    }

    private ThreadView thread(VerifiedIdentity actor,UUID tenant,UUID thread,boolean moderator) {
        return transactions.execute(actor,tenant,moderator?"channels:moderate":"channels:use",db -> {
            try(var query=q(db,"""
                    SELECT blocked,flagged FROM dzoka.relay_threads WHERE tenant_id=? AND id=?
                    AND (? OR (? IN (claimant_ref,finder_ref) AND expires_at>clock_timestamp()))
                    """,tenant,thread,moderator,actor.reference());var row=query.executeQuery()) {
                if(!row.next()) throw new Failures.NotFound();
                var messages=new ArrayList<Message>();
                try(var list=q(db,"""
                        SELECT id,sender_ref=?,phrase,created_at FROM dzoka.relay_entries
                        WHERE tenant_id=? AND thread_id=? ORDER BY created_at DESC,id DESC LIMIT 100
                        """,actor.reference(),tenant,thread);var entries=list.executeQuery()) {
                    while(entries.next()) messages.add(new Message(entries.getObject(1,UUID.class),entries.getBoolean(2),
                            Phrase.valueOf(entries.getString(3)),entries.getTimestamp(4).toInstant()));
                }
                return new ThreadView(row.getBoolean(1),row.getBoolean(2),List.copyOf(messages));
            }
        });
    }

    public int pruneRelays(VerifiedIdentity actor,UUID tenant) {
        return transactions.execute(actor,tenant,"channels:moderate",db -> {
            lock(db,tenant);
            return write(db,"""
                    DELETE FROM dzoka.relay_threads WHERE tenant_id=? AND id IN (
                        SELECT id FROM dzoka.relay_threads WHERE tenant_id=?
                          AND expires_at+CASE WHEN flagged THEN interval '30 days' ELSE interval '0 days' END<clock_timestamp()
                        ORDER BY expires_at,id LIMIT 100)
                    """,tenant,tenant);
        });
    }

    public void block(VerifiedIdentity actor,UUID tenant,UUID thread,boolean abuse) {
        transactions.execute(actor,tenant,"channels:use",db -> {
            lock(db,tenant);
            if(write(db,"""
                    UPDATE dzoka.relay_threads SET blocked=true,flagged=flagged OR ?
                    WHERE tenant_id=? AND id=? AND ? IN (claimant_ref,finder_ref) AND expires_at>clock_timestamp()
                    """,abuse,tenant,thread,actor.reference())!=1) throw new Failures.NotFound();
            return null;
        });
    }

    public String dispatch(VerifiedIdentity actor,UUID tenant) {
        verifyTenant(tenant);
        processAttachment(actor,tenant);
        scanWatch(actor,tenant);
        Lease lease=transactions.execute(actor,tenant,SERVICE,db -> {
            lock(db,tenant);
            collectAlerts(db,tenant);
            write(db,"""
                    DELETE FROM dzoka.channel_match_runs WHERE tenant_id=? AND (contact_id,report_id,id) IN (
                      SELECT contact_id,report_id,id FROM dzoka.channel_match_runs WHERE tenant_id=?
                        AND created_at<clock_timestamp()-interval '24 hours' ORDER BY created_at,id LIMIT 100)
                    """,tenant,tenant);
            write(db,"""
                    DELETE FROM dzoka.channel_alerts WHERE tenant_id=? AND id IN (
                      SELECT id FROM dzoka.channel_alerts WHERE tenant_id=? AND considered
                        AND created_at<clock_timestamp()-interval '30 days' ORDER BY created_at,id LIMIT 100)
                    """,tenant,tenant);
            write(db,"""
                    DELETE FROM dzoka.channel_events WHERE tenant_id=? AND event_key IN (
                       SELECT event_key FROM dzoka.channel_events WHERE tenant_id=?
                        AND received_at<clock_timestamp()-interval '8 days' ORDER BY received_at LIMIT 100)
                    """,tenant,tenant);
            write(db,"UPDATE dzoka.channel_operations SET state='UNKNOWN' WHERE tenant_id=? AND state='SENDING' AND sending_at<clock_timestamp()-interval '2 minutes'",tenant);
            write(db,"""
                    UPDATE dzoka.channel_operations o SET state='CANCELLED',protected_reply=NULL FROM dzoka.channel_contacts c
                    WHERE (o.tenant_id,o.contact_id)=(c.tenant_id,c.id) AND o.tenant_id=? AND o.state='QUEUED'
                    AND (NOT c.enabled OR o.created_at<clock_timestamp()-interval '1 day' OR o.reply_until<=clock_timestamp()
                      OR (o.relay_thread IS NOT NULL AND NOT dzoka.relay_available(o.relay_thread)))
                    """,tenant);
            write(db,"""
                    UPDATE dzoka.channel_operations SET protected_reply=NULL
                    WHERE tenant_id=? AND protected_reply IS NOT NULL AND state NOT IN ('QUEUED','SENDING')
                    """,tenant);
            try(var query=q(db,"""
                    SELECT o.id,c.id,c.route,o.template,o.attempts,o.protected_reply FROM dzoka.channel_operations o
                    JOIN dzoka.channel_contacts c ON (c.tenant_id,c.id)=(o.tenant_id,o.contact_id)
                    WHERE o.tenant_id=? AND o.state='QUEUED' AND o.ready_at<=clock_timestamp() AND c.enabled
                    ORDER BY o.created_at,o.id LIMIT 1
                    """,tenant);var rows=query.executeQuery()) {
                if(!rows.next()) return null;
                var job=new Lease(rows.getObject(1,UUID.class),rows.getObject(2,UUID.class),rows.getString(3),
                        Template.valueOf(rows.getString(4)),rows.getInt(5)+1,rows.getString(6));
                write(db,"UPDATE dzoka.channel_operations SET state='SENDING',sending_at=clock_timestamp(),attempts=? WHERE tenant_id=? AND id=?",
                        job.attempt(),tenant,job.id());
                return job;
            }
        });
        if(lease==null) return "IDLE";
        var result=provider.submit(lease.id(),whatsapp.reveal(lease.contact(),lease.route()).split("\n",-1)[2],
                lease.template(),lease.protectedReply()==null?null:whatsapp.reveal(lease.id(),lease.protectedReply()));
        return transactions.execute(actor,tenant,SERVICE,db -> {
            lock(db,tenant);
            String state=result.result()==MessagingProvider.Result.RETRY?(lease.attempt()<3?"QUEUED":"FAILED"):result.result().name();
            write(db,"""
                    UPDATE dzoka.channel_operations SET state=?,provider_key=?,ready_at=clock_timestamp()+interval '1 minute',
                       protected_reply=CASE WHEN ?='QUEUED' THEN protected_reply ELSE NULL END
                    WHERE tenant_id=? AND id=? AND state='SENDING'
                    """,state,result.providerId()==null?null:whatsapp.fingerprint("provider",result.providerId()),state,tenant,lease.id());
            try(var query=q(db,"SELECT state FROM dzoka.channel_operations WHERE tenant_id=? AND id=?",tenant,lease.id());
                var rows=query.executeQuery()) { rows.next();return rows.getString(1); }
        });
    }

    private void scanWatch(VerifiedIdentity dispatcher,UUID tenant) {
        Watch watch=transactions.execute(dispatcher,tenant,SERVICE,db -> {
            lock(db,tenant);
            try(var query=q(db,"""
                    SELECT c.id,c.member_ref,c.route,w.report_id FROM dzoka.channel_match_watches w
                    JOIN dzoka.channel_contacts c ON c.tenant_id=w.tenant_id AND c.id=w.contact_id
                    WHERE w.tenant_id=? AND c.enabled AND w.due_at<=clock_timestamp()
                    ORDER BY w.due_at,w.contact_id,w.report_id LIMIT 1
                    """,tenant);var row=query.executeQuery()) {
                if(!row.next()) return null;
                var result=new Watch(new Contact(row.getObject(1,UUID.class),row.getString(2),row.getString(3),true),
                        row.getObject(4,UUID.class));
                write(db,"""
                        UPDATE dzoka.channel_match_watches SET due_at=clock_timestamp()+interval '5 minutes'
                        WHERE tenant_id=? AND contact_id=? AND report_id=?
                        """,tenant,result.contact().id(),result.report());
                return result;
            }
        });
        if(watch==null) return;
        var member=verified(watch.contact());
        List<ChannelMatching.Candidate> candidates;
        try { candidates=matching.scan(member,tenant,watch.report()).candidates(); }
        catch(Failures.NotFound missing) {
            transactions.execute(dispatcher,tenant,SERVICE,db -> {
                write(db,"DELETE FROM dzoka.channel_match_watches WHERE tenant_id=? AND contact_id=? AND report_id=?",
                        tenant,watch.contact().id(),watch.report());
                return null;
            });
            return;
        } catch(Failures.Forbidden | Failures.Unavailable | Failures.Conflict retryable) { return; }
        transactions.execute(dispatcher,tenant,SERVICE,db -> {
            lock(db,tenant);
            for(var candidate:candidates) {
                int inserted=write(db,"""
                        INSERT INTO dzoka.channel_match_notifications(tenant_id,contact_id,report_id,candidate_id)
                        SELECT w.tenant_id,w.contact_id,w.report_id,? FROM dzoka.channel_match_watches w
                        JOIN dzoka.channel_contacts c ON c.tenant_id=w.tenant_id AND c.id=w.contact_id
                        WHERE w.tenant_id=? AND w.contact_id=? AND w.report_id=? AND c.enabled
                        ON CONFLICT DO NOTHING
                        """,candidate.report(),tenant,watch.contact().id(),watch.report());
                if(inserted!=0) enqueue(db,tenant,operation("match:"+watch.contact().id()+":"+watch.report()+":"+candidate.report()),
                        watch.contact().id(),Template.MATCH_ALERT);
            }
            return null;
        });
    }

    private static void collectAlerts(Connection db,UUID tenant) throws SQLException {
        try(var query=q(db,"""
                SELECT a.id,a.relay_thread,c.id,a.created_at>clock_timestamp()-interval '1 day'
                  AND (a.relay_thread IS NULL OR dzoka.relay_available(a.relay_thread))
                FROM dzoka.channel_alerts a LEFT JOIN dzoka.channel_contacts c
                  ON (c.tenant_id,c.member_ref)=(a.tenant_id,a.member_ref) AND c.enabled AND c.consented_at<=a.created_at
                WHERE a.tenant_id=? AND NOT a.considered ORDER BY a.created_at,a.id LIMIT 20
                """,tenant);var rows=query.executeQuery()) {
            while(rows.next()) {
                UUID id=rows.getObject(1,UUID.class);
                UUID relay=rows.getObject(2,UUID.class);
                UUID contact=rows.getObject(3,UUID.class);
                if(contact!=null && rows.getBoolean(4)) {
                    enqueue(db,tenant,id,contact,relay==null?Template.RECOVERY_UPDATE:Template.RELAY_UPDATE);
                    if(relay!=null) write(db,"""
                            UPDATE dzoka.channel_operations SET relay_thread=? WHERE tenant_id=? AND id=? AND state='QUEUED'
                            """,relay,tenant,id);
                }
                write(db,"UPDATE dzoka.channel_alerts SET considered=true WHERE tenant_id=? AND id=?",tenant,id);
            }
        }
    }

    private void delivery(UUID tenant,WhatsApp.Delivery delivery) {
        transactions.execute(WhatsApp.SERVICE,tenant,SERVICE,db -> {
            lock(db,tenant);
            String address=whatsapp.fingerprint("address",delivery.address());
            String providerKey=whatsapp.fingerprint("provider",delivery.id());
            try(var query=q(db,"""
                    SELECT o.id,o.state,c.id FROM dzoka.channel_operations o
                    JOIN dzoka.channel_contacts c ON (c.tenant_id,c.id)=(o.tenant_id,o.contact_id)
                    WHERE o.tenant_id=? AND c.address_key=? AND o.state NOT IN ('QUEUED','CANCELLED') AND
                        (o.provider_key=? OR (o.provider_key IS NULL AND o.id=?::uuid))
                    """,tenant,address,providerKey,delivery.operation());var row=query.executeQuery()) {
                if(!row.next()) return null;
                UUID operation=row.getObject(1,UUID.class);
                String key=whatsapp.fingerprint("callback",delivery.id()+"\n"+delivery.state()+"\n"+delivery.timestamp());
                if(write(db,"""
                        INSERT INTO dzoka.channel_events(tenant_id,event_key,contact_id,operation_id,kind)
                        VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING
                        """,tenant,key,row.getObject(3,UUID.class),operation,delivery.state())==1
                        && rank(delivery.state())>rank(row.getString(2))) {
                    write(db,"UPDATE dzoka.channel_operations SET state=?,provider_key=? WHERE tenant_id=? AND id=?",
                            delivery.state(),providerKey,tenant,operation);
                }
            }
            return null;
        });
    }

    private static int rank(String state) {
        return switch(state) { case "SENT" -> 1; case "FAILED" -> 2; case "DELIVERED" -> 3; case "READ" -> 4; default -> 0; };
    }
    private static String enqueue(Connection db,UUID tenant,UUID id,UUID contact,Template template) throws SQLException {
        return enqueue(db,tenant,id,contact,template,null,null);
    }

    private static String enqueue(Connection db,UUID tenant,UUID id,UUID contact,Template template,
                                  String protectedReply,Instant replyUntil) throws SQLException {
        try(var query=q(db,"""
                SELECT state,contact_id,template FROM dzoka.channel_operations WHERE tenant_id=? AND id=?
                """,tenant,id);var row=query.executeQuery()) {
            if(row.next()) {
                if(!contact.equals(row.getObject(2,UUID.class)) || !template.name().equals(row.getString(3))) throw new Failures.Conflict();
                return row.getString(1);
            }
        }
        try(var query=q(db,"""
                SELECT enabled FROM dzoka.channel_contacts WHERE tenant_id=? AND id=?
                """,tenant,contact);var row=query.executeQuery()) {
            if(!row.next()) return "CANCELLED";
            if(!row.getBoolean(1)) {
                write(db,"""
                        INSERT INTO dzoka.channel_operations(tenant_id,id,contact_id,template,state,reply_until)
                        VALUES(?,?,?,?,'CANCELLED',?)
                        """,tenant,id,contact,template.name(),timestamp(replyUntil));
                return "CANCELLED";
            }
        }
        try(var query=q(db,"""
                SELECT count(*)>=60 FROM (SELECT 1 FROM dzoka.channel_operations WHERE tenant_id=? AND contact_id=?
                 AND created_at>clock_timestamp()-interval '1 day' LIMIT 60) recent
                """,tenant,contact);var row=query.executeQuery()) {
            row.next();if(row.getBoolean(1)) return "CANCELLED";
        }
        write(db,"""
                INSERT INTO dzoka.channel_operations(tenant_id,id,contact_id,template,protected_reply,reply_until) VALUES(?,?,?,?,?,?)
                """,tenant,id,contact,template.name(),protectedReply,timestamp(replyUntil));
        return "QUEUED";
    }
    private static java.time.OffsetDateTime timestamp(Instant value) {
        return value==null?null:value.atOffset(java.time.ZoneOffset.UTC);
    }
    private static Contact contact(Connection db,UUID tenant,String key) throws SQLException {
        return contact(db,tenant,key,null);
    }
    private static Contact contact(Connection db,UUID tenant,String key,java.time.OffsetDateTime inboundAt) throws SQLException {
        try(var query=q(db,"""
                SELECT id,member_ref,route,enabled FROM dzoka.channel_contacts WHERE tenant_id=? AND address_key=?
                AND (?::timestamptz IS NULL OR date_trunc('second',consented_at)<=?)
                """,tenant,key,inboundAt,inboundAt);
            var rows=query.executeQuery()) {
            return rows.next()?new Contact(rows.getObject(1,UUID.class),rows.getString(2),rows.getString(3),rows.getBoolean(4)):null;
        }
    }
    private void verifyTenant(UUID tenant) { if(!whatsapp.tenant().equals(tenant)) throw new Failures.NotFound(); }
    private static UUID operation(String key) { return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.US_ASCII)); }
    private static void lock(Connection db,UUID tenant) throws SQLException {
        try(var sql=q(db,"SELECT pg_advisory_xact_lock(hashtextextended('channel-tenant:'||?::text,18))",tenant)) { sql.execute(); }
    }
    private static PreparedStatement q(Connection db,String sql,Object... args) throws SQLException {
        var statement=db.prepareStatement(sql);
        try { for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]); return statement; }
        catch(SQLException failure) { statement.close(); throw failure; }
    }
    private static int write(Connection db,String sql,Object... args) throws SQLException {
        try(var statement=q(db,sql,args)) { return statement.executeUpdate(); }
    }
}
