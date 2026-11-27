package org.dzoka.foundation.persistence;

import java.net.URI;
import java.sql.*;
import java.util.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import tools.jackson.databind.json.JsonMapper;

/** Privileged operator entry point; never registered in the HTTP application. */
public final class FoundationAdmin {
    private FoundationAdmin() {}
    private static final JsonMapper JSON=JsonMapper.builder()
            .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    public record Command(String kind,UUID tenant,String name,String issuer,String subject,List<String> permissions,
            Boolean active,Long expectedVersion,String idempotencyKey) {}
    public record Receipt(UUID operation,long version,boolean active) {}

    public static void main(String[] args) {
        try {
            if(args.length!=0)throw new IllegalArgumentException();
            byte[] input=System.in.readNBytes(16385);
            if(input.length>16384)throw new IllegalArgumentException();
            var command=JSON.readValue(input,Command.class);
            try(var c=DriverManager.getConnection(MigrateDatabase.required("DZOKA_MIGRATION_URL"),
                    MigrateDatabase.required("DZOKA_MIGRATION_USER"),MigrateDatabase.required("DZOKA_MIGRATION_PASSWORD"))) {
                System.out.println(JSON.writeValueAsString(execute(c,command,Boolean.parseBoolean(System.getenv("DZOKA_ADMIN_ALLOW_INSECURE_LOOPBACK")))));
            }
        } catch(Exception failure) {
            // JDBC/parser messages can include input, identity values or credentials.
            System.err.println("Foundation administration failed; check configuration, permissions, input and expected version.");
            System.exit(2);
        }
    }

    public static Receipt execute(Connection c,Command command,boolean testLoopback) throws SQLException {
        if(command==null || command.tenant()==null || command.active()==null || command.expectedVersion()==null
                || command.expectedVersion()<0 || command.idempotencyKey()==null || !command.idempotencyKey().matches("[A-Za-z0-9._:-]{8,128}"))throw new IllegalArgumentException();
        boolean tenant="TENANT".equals(command.kind());
        List<String> permissions=List.of();
        String target;
        if(tenant) {
            new Organization(command.tenant(),command.name(),1);
            if(command.issuer()!=null || command.subject()!=null || command.permissions()!=null)throw new IllegalArgumentException();
            target=VerifiedIdentity.digest("tenant:"+command.tenant());
        } else {
            if(!"MEMBERSHIP".equals(command.kind()) || command.name()!=null || command.permissions()==null || command.permissions().size()>64)throw new IllegalArgumentException();
            var identity=new VerifiedIdentity(command.issuer(),command.subject());
            URI uri=URI.create(identity.issuer());
            boolean local=testLoopback && "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost());
            if((!"https".equals(uri.getScheme()) && !local) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null)throw new IllegalArgumentException();
            if(command.permissions().stream().anyMatch(p->p==null || !p.matches("[a-z][a-z0-9-]{0,39}:[a-z][a-z0-9-]{0,39}")))throw new IllegalArgumentException();
            permissions=command.permissions().stream().distinct().sorted().toList();
            if(permissions.size()!=command.permissions().size())throw new IllegalArgumentException();
            target=identity.reference();
        }
        String key=VerifiedIdentity.digest(command.idempotencyKey());
        String hash=VerifiedIdentity.digest(JSON.writeValueAsString(new Command(command.kind(),command.tenant(),command.name(),command.issuer(),command.subject(),tenant?null:permissions,command.active(),command.expectedVersion(),null)));
        if(!c.getAutoCommit())throw new IllegalArgumentException("Dedicated connection required");
        c.setAutoCommit(false);
        try {
            String actor;
            try(var s=c.createStatement();var r=s.executeQuery("SELECT current_user,rolsuper,rolbypassrls,pg_has_role(current_user,(SELECT nspowner FROM pg_namespace WHERE nspname='dzoka'),'MEMBER') FROM pg_roles WHERE rolname=current_user")) {
                if(!r.next() || !"dzoka_migrator".equals(r.getString(1)) || r.getBoolean(2) || r.getBoolean(3) || !r.getBoolean(4))throw new Failures.Forbidden();
                actor=r.getString(1);
            }
            try(var s=c.createStatement()){s.execute("SET LOCAL statement_timeout='10000'; SET LOCAL lock_timeout='5000'");}
            // One tenant lock serializes lifecycle and membership changes, including absent rows.
            try(var s=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,66))")){s.setString(1,command.tenant().toString());s.execute();}
            try(var s=c.prepareStatement("SELECT request_hash,id,result_version,result_active FROM dzoka.foundation_admin_operations WHERE tenant_id=? AND key_hash=?")) {
                s.setObject(1,command.tenant());s.setString(2,key);
                try(var r=s.executeQuery()){if(r.next()) {
                    if(!hash.equals(r.getString(1)))throw new Failures.Conflict();
                    var receipt=new Receipt(r.getObject(2,UUID.class),r.getLong(3),r.getBoolean(4));c.commit();return receipt;
                }}
            }
            long version;
            if(tenant) {
                try(var s=c.prepareStatement("SELECT version FROM dzoka.tenants WHERE id=? FOR UPDATE")) {
                    s.setObject(1,command.tenant());try(var r=s.executeQuery()){version=r.next()?r.getLong(1):0;}
                }
                if(version!=command.expectedVersion())throw new Failures.Conflict();
                version=Math.addExact(version,1);
                try(var s=c.prepareStatement("INSERT INTO dzoka.tenants(id,name,active,version) VALUES (?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,active=excluded.active,version=excluded.version")) {
                    s.setObject(1,command.tenant());s.setString(2,command.name());s.setBoolean(3,command.active());s.setLong(4,version);s.executeUpdate();
                }
            } else {
                try(var s=c.prepareStatement("SELECT 1 FROM dzoka.tenants WHERE id=?")){s.setObject(1,command.tenant());try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();}}
                try(var s=c.prepareStatement("SELECT version FROM dzoka.memberships WHERE tenant_id=? AND issuer=? AND subject=? FOR UPDATE")) {
                    s.setObject(1,command.tenant());s.setString(2,command.issuer());s.setString(3,command.subject());try(var r=s.executeQuery()){version=r.next()?r.getLong(1):0;}
                }
                if(version!=command.expectedVersion())throw new Failures.Conflict();
                version=Math.addExact(version,1);
                try(var s=c.prepareStatement("INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions,active,version) VALUES (?,?,?,?,?,?) ON CONFLICT(tenant_id,issuer,subject) DO UPDATE SET permissions=excluded.permissions,active=excluded.active,version=excluded.version")) {
                    s.setObject(1,command.tenant());s.setString(2,command.issuer());s.setString(3,command.subject());s.setArray(4,c.createArrayOf("text",permissions.toArray(String[]::new)));s.setBoolean(5,command.active());s.setLong(6,version);s.executeUpdate();
                }
            }
            UUID operation=UUID.randomUUID();
            try(var s=c.prepareStatement("INSERT INTO dzoka.foundation_admin_operations(tenant_id,id,key_hash,request_hash,db_actor,target_ref,kind,result_version,result_active) VALUES (?,?,?,?,?,?,?,?,?)")) {
                s.setObject(1,command.tenant());s.setObject(2,operation);s.setString(3,key);s.setString(4,hash);s.setString(5,actor);s.setString(6,target);s.setString(7,command.kind());s.setLong(8,version);s.setBoolean(9,command.active());s.executeUpdate();
            }
            c.commit();return new Receipt(operation,version,command.active());
        } catch(SQLException | RuntimeException | Error failure) {
            try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;
        } finally {c.setAutoCommit(true);}
    }
}
