
# --- !Ups
CREATE TABLE snapshots (
  id UUID NOT NULL PRIMARY KEY,
  project VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  size INTEGER NOT NULL
)

# --- !Downs
DROP TABLE snapshots