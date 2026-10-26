package org.dzoka.foundation.persistence;

import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy(false)
public final class RuntimeDatabaseGuard implements InitializingBean {
    private final DataSource source;
    public RuntimeDatabaseGuard(DataSource source) { this.source = source; }
    @Override public void afterPropertiesSet() throws Exception {
        try (var c = source.getConnection(); var s = c.createStatement(); var r = s.executeQuery("""
                SELECT r.rolsuper OR r.rolbypassrls OR r.rolcreaterole OR r.rolcreatedb
                    OR EXISTS (SELECT 1 FROM pg_tables t WHERE t.schemaname='dzoka'
                        AND pg_has_role(current_user,t.tableowner,'MEMBER'))
                    OR EXISTS (SELECT 1 FROM pg_namespace n WHERE n.nspname='dzoka'
                        AND pg_has_role(current_user,n.nspowner,'MEMBER'))
                    OR EXISTS (SELECT 1 FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace
                        WHERE n.nspname='dzoka' AND t.relkind='r' AND (NOT t.relrowsecurity OR NOT t.relforcerowsecurity))
                    OR to_regclass('dzoka.organizations') IS NULL
                    OR current_user <> 'dzoka_app'
                FROM pg_roles r WHERE r.rolname=current_user
                """)) {
            if (!r.next() || r.getBoolean(1)) throw new IllegalStateException("Unsafe runtime database role");
        }
    }
}
