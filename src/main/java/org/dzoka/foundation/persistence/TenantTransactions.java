package org.dzoka.foundation.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Component;

/** Infrastructure boundary. No thread-local tenant or pooled session-level context. */
@Component
public final class TenantTransactions {
    private final DataSource dataSource;
    private final org.dzoka.operations.Telemetry telemetry;
    public TenantTransactions(DataSource dataSource) { this(dataSource,null); }
    @org.springframework.beans.factory.annotation.Autowired
    public TenantTransactions(DataSource dataSource,org.dzoka.operations.Telemetry telemetry) { this.dataSource = dataSource;this.telemetry=telemetry; }
    @FunctionalInterface public interface Work<T> { T run(Connection connection) throws SQLException; }

    public <T> T execute(VerifiedIdentity actor, UUID tenant, String permission, Work<T> work) {
        if(telemetry==null)return perform(actor,tenant,permission,work);
        var context=telemetry.context(org.dzoka.operations.TraceContext.current());var start=java.time.Instant.now();long nanos=System.nanoTime();int status=200;
        try(var scope=new org.dzoka.operations.TraceContext("00-"+context.trace()+"-"+context.span()+"-01")) {return perform(actor,tenant,permission,work);}
        catch(RuntimeException failure){status=failure instanceof Failures.Forbidden?403:failure instanceof Failures.NotFound?404:failure instanceof Failures.Conflict?409:failure instanceof Failures.RateLimited?429:503;throw failure;}
        finally{telemetry.record(context,"INTERNAL","/transactions/"+(permission==null?"UNAVAILABLE":permission.replace(':','-')),status,start,System.nanoTime()-nanos);}
    }
    private <T> T perform(VerifiedIdentity actor, UUID tenant, String permission, Work<T> work) {
        if (actor == null || tenant == null || permission == null || permission.isBlank())
            throw new Failures.Forbidden();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                try (var statement = connection.prepareStatement("""
                        SELECT set_config('dzoka.tenant_id', ?, true),
                               set_config('dzoka.issuer', ?, true),
                               set_config('dzoka.subject', ?, true),
                               set_config('statement_timeout', '10000', true),
                               set_config('lock_timeout', '5000', true)
                        """)) {
                    statement.setString(1, tenant.toString());
                    statement.setString(2, actor.issuer()); statement.setString(3, actor.subject());
                    statement.execute();
                }
                try(var diagnostic=connection.prepareStatement("SELECT set_config('dzoka.traceparent',?,true)")){diagnostic.setString(1,java.util.Objects.toString(org.dzoka.operations.TraceContext.current(),""));diagnostic.execute();}
                try (var statement = connection.prepareStatement("""
                        SELECT 1 FROM dzoka.memberships m JOIN dzoka.tenants t ON t.id=m.tenant_id
                        WHERE m.tenant_id = ? AND m.issuer = ? AND m.subject = ? AND m.active AND t.active
                          AND ? = ANY(m.permissions)
                        """)) {
                    statement.setObject(1, tenant); statement.setString(2, actor.issuer());
                    statement.setString(3, actor.subject()); statement.setString(4, permission);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) throw new Failures.Forbidden();
                    }
                }
                // A close waits for in-flight work; no request can pass the barrier check while it closes.
                String barrierLock=permission.equals("retention:recover")?"pg_advisory_xact_lock":"pg_advisory_xact_lock_shared";
                try(var statement=connection.prepareStatement("SELECT "+barrierLock+"(hashtextextended('restore:'||?::text,25))")){statement.setObject(1,tenant);statement.execute();}
                try(var statement=connection.prepareStatement("SELECT closed FROM dzoka.restore_barriers WHERE tenant_id=?")) {
                    statement.setObject(1,tenant);
                    try(var rows=statement.executeQuery()) {
                        if(rows.next() && rows.getBoolean(1) && !permission.equals("retention:recover") && !permission.equals("operations:read")) {
                            boolean maintenance=java.util.Set.of("reports:purge","media:purge","learning:maintain","matches:maintain-history").contains(permission);
                            if(!maintenance)throw new Failures.Conflict();
                            try(var check=connection.prepareStatement("SELECT dzoka.permitted('retention:recover')");var result=check.executeQuery()){
                                result.next();if(!result.getBoolean(1))throw new Failures.Conflict();
                            }
                        }
                    }
                }
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error error) {
                try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        } catch (SQLException error) { throw new Failures.Unavailable(error); }
    }
}
