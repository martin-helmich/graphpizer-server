
# --- !Ups
CREATE TABLE projects (
  slug VARCHAR(255) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL
)

# --- !Downs
DROP TABLE projects