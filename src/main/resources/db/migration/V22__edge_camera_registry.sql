CREATE TABLE dzoka.edge_cameras (
 tenant_id uuid NOT NULL REFERENCES dzoka.tenants(id), id uuid NOT NULL,
 archive_sha text NOT NULL CHECK(archive_sha ~ '^[0-9a-f]{64}$'),
 active boolean NOT NULL, registered_by text NOT NULL,
 PRIMARY KEY(tenant_id,id)
);
ALTER TABLE dzoka.edge_cameras ENABLE ROW LEVEL SECURITY;
ALTER TABLE dzoka.edge_cameras FORCE ROW LEVEL SECURITY;
CREATE POLICY camera_tenant ON dzoka.edge_cameras AS RESTRICTIVE FOR ALL TO dzoka_app
 USING(tenant_id=dzoka.current_tenant()) WITH CHECK(tenant_id=dzoka.current_tenant());
CREATE POLICY camera_read ON dzoka.edge_cameras FOR SELECT TO dzoka_app
 USING(dzoka.permitted('edge:review') OR dzoka.permitted('edge:process') OR dzoka.permitted('edge:investigate'));
CREATE POLICY camera_write ON dzoka.edge_cameras FOR ALL TO dzoka_app USING(dzoka.permitted('edge:review'));
GRANT SELECT,INSERT,UPDATE(active) ON dzoka.edge_cameras TO dzoka_app;
