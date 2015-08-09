
# --- !Ups
CREATE TABLE additional_queries (
  project VARCHAR(255) NOT NULL,
  when VARCHAR(255) NOT NULL,
  cypher VARCHAR(4096) NOT NULL,

  PRIMARY KEY (project, cypher)
)

# --- !Downs
DROP TABLE additional_queries