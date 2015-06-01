
# --- !Ups
ALTER TABLE snapshots ALTER COLUMN timestamp LONG NOT NULL;

# --- !Downs
ALTER TABLE snapshots ALTER COLUMN timestamp TIMESTAMP NOT NULL;