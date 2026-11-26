package org.dzoka.passports;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.dzoka.fulfillment.Sql;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Service;

@Service
public final class Passports {
    private final TenantTransactions tx;
    private final ItemIdentityProvider identity;
    public Passports(TenantTransactions tx,ItemIdentityProvider identity) { this.tx=tx;this.identity=identity; }
    public record Item(UUID id,String description,boolean active) {}
    public record Tag(String kind,String token) { @Override public String toString() { return "PrivateItemTag"; } }
    public record Identity(String provider,String reference,String evidenceSha) {}
    public record StoredIdentity(UUID id,String provider,String version,String reference,String evidenceSha,boolean verified) {}
    public record TagView(UUID id,String kind,boolean active) {}
    public record Message(UUID id,boolean yours,String phrase) {}
    public record ThreadView(UUID id,boolean blocked,boolean flagged,List<Message> messages,UUID registeredItem,String privateDescription) {}
    public static String locator(UUID tenant,String token) {tokenSha(token);return "dzoka:tag:v1:"+tenant+":"+token;}
    public Item register(VerifiedIdentity actor,UUID tenant,UUID id,String description) {
        if(description==null || description.isBlank() || description.length()>160 || description.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);
            try(var query=Sql.q(db,"SELECT description,active FROM dzoka.registered_items WHERE id=?",id);var row=query.executeQuery()) {
                if(row.next()) { if(!description.equals(row.getString(1)))throw new Failures.Conflict();return new Item(id,description,row.getBoolean(2)); }
            }
            Sql.write(db,"INSERT INTO dzoka.registered_items(tenant_id,id,owner_ref,description) VALUES(?,?,?,?)",tenant,id,actor.reference(),description);
            return new Item(id,description,true);
        });
    }
    public List<Item> items(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"passports:use",db -> {
            var result=new ArrayList<Item>();
            try(var query=Sql.q(db,"SELECT id,description,active FROM dzoka.registered_items ORDER BY created_at DESC,id LIMIT 100");var row=query.executeQuery()) {
                while(row.next())result.add(new Item(row.getObject(1,UUID.class),row.getString(2),row.getBoolean(3)));
            }
            return List.copyOf(result);
        });
    }
    public void deactivate(VerifiedIdentity actor,UUID tenant,UUID id) {
        tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);
            try(var query=Sql.q(db,"SELECT 1 FROM dzoka.registered_items WHERE id=?",id);var row=query.executeQuery()){if(!row.next())throw new Failures.NotFound();}
            Sql.write(db,"UPDATE dzoka.registered_items SET active=false WHERE id=?",id);return null;
        });
    }
    public void tag(VerifiedIdentity actor,UUID tenant,UUID item,UUID id,Tag tag) {
        if(tag==null || !Set.of("QR","NFC").contains(tag.kind()))throw new IllegalArgumentException();
        String sha=tokenSha(tag.token());
        tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);item(db,item);
            try(var query=Sql.q(db,"SELECT item_id,kind,token_sha,active FROM dzoka.item_tags WHERE id=?",id);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!item.equals(row.getObject(1,UUID.class)) || !tag.kind().equals(row.getString(2)) || !sha.equals(row.getString(3)) || !row.getBoolean(4)) throw new Failures.Conflict();
                    return null;
                }
            }
            Sql.write(db,"INSERT INTO dzoka.item_tags(tenant_id,id,item_id,kind,token_sha) VALUES(?,?,?,?,?)",tenant,id,item,tag.kind(),sha);return null;
        });
    }
    public void revokeTag(VerifiedIdentity actor,UUID tenant,UUID item,UUID id) {
        tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);item(db,item);
            if(Sql.write(db,"UPDATE dzoka.item_tags SET active=false WHERE id=? AND item_id=?",id,item)!=1) throw new Failures.NotFound();return null;
        });
    }
    public ItemIdentityProvider.Assertion passport(VerifiedIdentity actor,UUID tenant,UUID item,UUID id,Identity request) {
        if(request==null)throw new IllegalArgumentException();Sql.sha(request.evidenceSha());
        var assertion=identity.identify(item,request.provider(),request.reference());
        return tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);item(db,item);
            if(Set.of("QR","NFC").contains(request.provider())) {
                try(var query=Sql.q(db,"SELECT 1 FROM dzoka.item_tags WHERE id=? AND item_id=? AND kind=? AND active",UUID.fromString(request.reference()),item,request.provider());var row=query.executeQuery()) {
                    if(!row.next())throw new Failures.NotFound();
                }
            }
            try(var query=Sql.q(db,"SELECT item_id,provider,reference,evidence_sha FROM dzoka.item_passports WHERE id=?",id);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!item.equals(row.getObject(1,UUID.class)) || !request.provider().equals(row.getString(2)) || !request.reference().equals(row.getString(3)) || !request.evidenceSha().equals(row.getString(4)))throw new Failures.Conflict();
                    return assertion;
                }
            }
            Sql.write(db,"INSERT INTO dzoka.item_passports(tenant_id,id,item_id,provider,reference,version,evidence_sha,verified) VALUES(?,?,?,?,?,?,?,?)",
                    tenant,id,item,assertion.provider(),assertion.reference(),assertion.version(),request.evidenceSha(),assertion.verified());return assertion;
        });
    }
    public List<StoredIdentity> identities(VerifiedIdentity actor,UUID tenant,UUID item) {
        return tx.execute(actor,tenant,"passports:use",db -> {
            item(db,item);var result=new ArrayList<StoredIdentity>();
            try(var query=Sql.q(db,"SELECT id,provider,version,reference,evidence_sha,verified FROM dzoka.item_passports WHERE item_id=? ORDER BY created_at,id LIMIT 100",item);var row=query.executeQuery()) {
                while(row.next())result.add(new StoredIdentity(row.getObject(1,UUID.class),row.getString(2),row.getString(3),row.getString(4),row.getString(5),row.getBoolean(6)));
            }
            return List.copyOf(result);
        });
    }
    public List<TagView> tags(VerifiedIdentity actor,UUID tenant,UUID item) {
        return tx.execute(actor,tenant,"passports:use",db -> {
            item(db,item);var result=new ArrayList<TagView>();
            try(var query=Sql.q(db,"SELECT id,kind,active FROM dzoka.item_tags WHERE item_id=? ORDER BY id LIMIT 100",item);var row=query.executeQuery()) {
                while(row.next())result.add(new TagView(row.getObject(1,UUID.class),row.getString(2),row.getBoolean(3)));
            }
            return List.copyOf(result);
        });
    }
    public ThreadView scan(VerifiedIdentity actor,UUID tenant,UUID thread,String token) {
        tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);
            if(Sql.write(db,"INSERT INTO dzoka.tag_scan_budgets(tenant_id,actor_ref,bucket,used) VALUES(?,?,date_trunc('hour',clock_timestamp()),1) ON CONFLICT(tenant_id,actor_ref,bucket) DO UPDATE SET used=tag_scan_budgets.used+1 WHERE tag_scan_budgets.used<60",tenant,actor.reference())!=1)throw new Failures.Conflict();
            return null;
        });
        if(token!=null && token.startsWith("dzoka:tag:v1:")) {
            String[] parts=token.split(":",-1);
            if(parts.length!=5 || !tenant.toString().equals(parts[3]))throw new Failures.NotFound();
            token=parts[4];
        }
        String sha=tokenSha(token);
        return tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);
            UUID item,tag;
            try(var query=Sql.q(db,"SELECT item,tag FROM dzoka.lookup_item_tag(?)",sha);var row=query.executeQuery()) {
                if(!row.next())throw new Failures.NotFound();item=row.getObject(1,UUID.class);tag=row.getObject(2,UUID.class);
            }
            try(var query=Sql.q(db,"SELECT item_id,tag_id,finder_ref FROM dzoka.tag_threads WHERE id=?",thread);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!item.equals(row.getObject(1,UUID.class)) || !tag.equals(row.getObject(2,UUID.class)) || !actor.reference().equals(row.getString(3)))throw new Failures.Conflict();return view(db,thread,actor);
                }
            }
            try(var query=Sql.q(db,"SELECT count(*) FROM dzoka.tag_threads WHERE finder_ref=? AND expires_at>clock_timestamp()-interval '1 day'",actor.reference());var row=query.executeQuery()) {
                row.next();if(row.getInt(1)>=20)throw new Failures.Conflict();
            }
            try(var query=Sql.q(db,"SELECT 1 FROM dzoka.tag_threads WHERE item_id=? AND finder_ref=? AND blocked AND expires_at>clock_timestamp()",item,actor.reference());var row=query.executeQuery()) {
                if(row.next())throw new Failures.Conflict();
            }
            Sql.write(db,"INSERT INTO dzoka.tag_threads(tenant_id,id,item_id,tag_id,finder_ref) VALUES(?,?,?,?,?)",tenant,thread,item,tag,actor.reference());return view(db,thread,actor);
        });
    }
    public ThreadView thread(VerifiedIdentity actor,UUID tenant,UUID id) { return tx.execute(actor,tenant,"passports:use",db -> view(db,id,actor)); }
    public List<ThreadView> threads(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"passports:use",db -> {
            var ids=new ArrayList<UUID>();
            try(var query=Sql.q(db,"SELECT id FROM dzoka.tag_threads WHERE expires_at>clock_timestamp() AND EXISTS(SELECT 1 FROM dzoka.tag_thread_live(id)) ORDER BY expires_at DESC,id LIMIT 100");var row=query.executeQuery()) {
                while(row.next())ids.add(row.getObject(1,UUID.class));
            }
            var result=new ArrayList<ThreadView>();for(UUID id:ids)result.add(view(db,id,actor));return List.copyOf(result);
        });
    }
    public ThreadView message(VerifiedIdentity actor,UUID tenant,UUID thread,UUID id,String phrase) {
        if(phrase==null || !Set.of("FOUND_ITEM","ARRANGE_STAFF_RETURN","THANK_YOU").contains(phrase))throw new IllegalArgumentException();
        return tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);var state=view(db,thread,actor);
            try(var query=Sql.q(db,"SELECT thread_id,actor_ref,phrase FROM dzoka.tag_messages WHERE id=?",id);var row=query.executeQuery()) {
                if(row.next()) {
                    if(!thread.equals(row.getObject(1,UUID.class)) || !actor.reference().equals(row.getString(2)) || !phrase.equals(row.getString(3)))throw new Failures.Conflict();return state;
                }
            }
            if(state.blocked() || state.messages().size()>=40)throw new Failures.Conflict();
            Sql.write(db,"INSERT INTO dzoka.tag_messages(tenant_id,thread_id,id,actor_ref,phrase) VALUES(?,?,?,?,?)",tenant,thread,id,actor.reference(),phrase);return view(db,thread,actor);
        });
    }
    public void moderate(VerifiedIdentity actor,UUID tenant,UUID thread,boolean flag) {
        tx.execute(actor,tenant,"passports:use",db -> {
            Sql.lock(db,tenant);view(db,thread,actor);
            Sql.write(db,flag?"UPDATE dzoka.tag_threads SET flagged=true,blocked=true WHERE id=?":"UPDATE dzoka.tag_threads SET blocked=true WHERE id=?",thread);return null;
        });
    }
    public int purge(VerifiedIdentity actor,UUID tenant) {
        return tx.execute(actor,tenant,"passports:maintain",db -> {
            Sql.write(db,"DELETE FROM dzoka.tag_scan_budgets WHERE (actor_ref,bucket) IN(SELECT actor_ref,bucket FROM dzoka.tag_scan_budgets WHERE bucket<clock_timestamp()-interval '2 days' ORDER BY bucket LIMIT 100)");
            return Sql.write(db,"DELETE FROM dzoka.tag_messages WHERE id IN(SELECT m.id FROM dzoka.tag_messages m JOIN dzoka.tag_threads t ON(t.tenant_id,t.id)=(m.tenant_id,m.thread_id) WHERE t.expires_at<=clock_timestamp() ORDER BY m.created_at LIMIT 100)");
        });
    }
    private static ThreadView view(Connection db,UUID id,VerifiedIdentity actor) throws SQLException {
        try(var query=Sql.q(db,"SELECT 1 FROM dzoka.tag_thread_live(?)",id);var row=query.executeQuery()) { if(!row.next())throw new Failures.NotFound(); }
        boolean blocked,flagged;
        try(var query=Sql.q(db,"SELECT blocked,flagged FROM dzoka.tag_threads WHERE id=?",id);var row=query.executeQuery()) {
            if(!row.next())throw new Failures.NotFound();blocked=row.getBoolean(1);flagged=row.getBoolean(2);
        }
        var messages=new ArrayList<Message>();
        try(var query=Sql.q(db,"SELECT id,actor_ref,phrase FROM dzoka.tag_messages WHERE thread_id=? ORDER BY created_at,id LIMIT 40",id);var row=query.executeQuery()) {
            while(row.next())messages.add(new Message(row.getObject(1,UUID.class),actor.reference().equals(row.getString(2)),row.getString(3)));
        }
        UUID item=null;String description=null;
        try(var query=Sql.q(db,"SELECT i.id,i.description FROM dzoka.registered_items i JOIN dzoka.tag_threads t ON(t.tenant_id,t.item_id)=(i.tenant_id,i.id) WHERE t.id=?",id);var row=query.executeQuery()) {
            if(row.next()){item=row.getObject(1,UUID.class);description=row.getString(2);}
        }
        return new ThreadView(id,blocked,flagged,List.copyOf(messages),item,description);
    }
    private static void item(Connection db,UUID id) throws SQLException {
        try(var query=Sql.q(db,"SELECT 1 FROM dzoka.registered_items WHERE id=? AND active",id);var row=query.executeQuery()) { if(!row.next())throw new Failures.NotFound(); }
    }
    private static String tokenSha(String token) {
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}") || token.chars().distinct().count()<8)throw new IllegalArgumentException("Random 32-byte tag token required");
        try { byte[] raw=Base64.getUrlDecoder().decode(token);if(raw.length!=32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(raw).equals(token))throw new IllegalArgumentException(); } catch(IllegalArgumentException bad) { throw new IllegalArgumentException("Invalid tag token"); }
        return ContentDigest.sha256(token.getBytes(StandardCharsets.US_ASCII));
    }
}
