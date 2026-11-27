-- Existing receipts retain their original digests and are classified as legacy.
ALTER TABLE dzoka.place_commands ADD COLUMN request_hash_version smallint NOT NULL DEFAULT 1
 CHECK(request_hash_version IN (1,2));
