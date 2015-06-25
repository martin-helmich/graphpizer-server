
# --- !Ups
CREATE TABLE queries (
  id UUID NOT NULL PRIMARY KEY,
  cypher TEXT NOT NULL
)

# --- !Downs
DROP TABLE queries