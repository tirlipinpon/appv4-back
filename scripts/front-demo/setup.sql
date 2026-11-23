-- Separate from Flyway: never applied automatically on application startup.
CREATE SCHEMA IF NOT EXISTS dev_front_demo;
CREATE TABLE IF NOT EXISTS dev_front_demo.roots (
    kind text NOT NULL, id bigint NOT NULL, PRIMARY KEY(kind,id)
);
CREATE TABLE IF NOT EXISTS dev_front_demo.installation (
    singleton boolean PRIMARY KEY DEFAULT true CHECK(singleton),
    installed_at timestamptz NOT NULL DEFAULT now(), version integer NOT NULL
);
