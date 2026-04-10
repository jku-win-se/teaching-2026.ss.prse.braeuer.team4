CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (email, username, password_hash, role, status)
VALUES (
  'owner@smarthome.com',
  'owner',
  crypt('SAMPLE', gen_salt('bf')),
  'Owner',
  'Active'
);