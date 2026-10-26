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
    public TenantTransactions(DataSource dataSource) { this.dataSource = dataSource; }
    @FunctionalInterface public interface Work<T> { T run(Connection connection) throws SQLException; }

    public <T> T execute(VerifiedIdentity actor, UUID tenant, String permission, Work<T> work) {
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
