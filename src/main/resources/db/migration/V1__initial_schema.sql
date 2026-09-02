-- ==============================================================================
-- CCE Protocol Service — Initial Database Schema
-- ==============================================================================
-- Flyway Migration: V1
-- Database: PostgreSQL 16
--
-- The definitional plane. This service is the sole writer of every table below; the Matcher and
-- Compliance services read protocol_definition, action_definition and trigger_index directly and
-- never write to them.
--
-- Shares the `ccedb` database with the other CCE services and keeps its own Flyway ledger
-- (`spring.flyway.table = flyway_schema_history_protocol`), so its migrations are tracked
-- independently.
--
-- REPLICA IDENTITY FULL is set on every CDC-replicated table. Publication membership and CDC-user
-- grants live in data-pipeline/cdc/01-configure-replication.sql, not here.
-- ==============================================================================

-- =============================================
-- 1. protocol_definition
-- =============================================
CREATE TABLE protocol_definition (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    url                 VARCHAR         NOT NULL,
    version             VARCHAR         NOT NULL,
    status              VARCHAR         NOT NULL,
    definition          JSONB           NOT NULL,
    loaded_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT protocol_definition_pkey PRIMARY KEY (id),
    CONSTRAINT protocol_definition_url_version_key UNIQUE (url, version),
    CONSTRAINT protocol_definition_status_check CHECK (status IN ('ACTIVE', 'RETIRED'))
);

-- No index on definition. The 1.x schema carried a GIN index over it (jsonb_path_ops) for the trigger
-- extraction that queried the JSON directly; 2.0.0 extracts triggers into trigger_index at load time
-- and parses the definition in process, so nothing reaches into the JSONB from SQL. Every read of this
-- table is by id, (url, version), url or status, which the primary key and the unique constraint serve.
ALTER TABLE protocol_definition REPLICA IDENTITY FULL;

-- =============================================
-- 2. action_definition
-- =============================================
-- FHIR ActivityDefinition resources referenced by intelligence actions via definitionCanonical.
-- Written here; resolved by canonical at runtime by the Compliance Service.
-- =============================================
CREATE TABLE action_definition (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    canonical_url       VARCHAR         NOT NULL,
    version             VARCHAR         NOT NULL,
    name                VARCHAR,
    title               VARCHAR,
    status              VARCHAR         NOT NULL,
    action_type         VARCHAR         NOT NULL,
    definition          JSONB           NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT action_definition_pkey PRIMARY KEY (id),
    CONSTRAINT action_definition_url_version_key UNIQUE (canonical_url, version),
    CONSTRAINT action_definition_status_check CHECK (status IN ('ACTIVE', 'RETIRED'))
);

-- No secondary indexes. Lookups are by canonical_url, or by canonical_url and version, and
-- action_definition_url_version_key answers both — canonical_url is its leading column, so a separate
-- index on it could only duplicate work the unique constraint already does. Nor is one on status worth
-- having: the table holds tens of rows, so PostgreSQL reads it in a page or two and an index scan never
-- wins.
ALTER TABLE action_definition REPLICA IDENTITY FULL;

-- =============================================
-- 3. trigger_index
-- =============================================
-- Tier 1 structural match: one row per decomposed trigger data[].codeFilter[] coding.
-- Built here from each PlanDefinition on load; read by the Matcher Service on every inbound event.
-- Nested sub-step triggers are indexed under their own action_id (flat step model).
-- =============================================
CREATE TABLE trigger_index (
    resource_type           VARCHAR         NOT NULL,
    path                    VARCHAR         NOT NULL,
    code_system             VARCHAR         NOT NULL DEFAULT '',
    code_value              VARCHAR         NOT NULL DEFAULT '',
    protocol_definition_id  UUID            NOT NULL,
    action_id               VARCHAR         NOT NULL,

    CONSTRAINT trigger_index_pkey PRIMARY KEY (resource_type, path, code_system, code_value, protocol_definition_id, action_id),
    CONSTRAINT trigger_index_protocol_definition_id_fkey
        FOREIGN KEY (protocol_definition_id) REFERENCES protocol_definition(id)
);

-- No secondary indexes: the primary key is (resource_type, path, code_system, code_value,
-- protocol_definition_id, action_id), so its index already serves both the resource-type-only probe
-- and the (resource_type, path, code_system, code_value) probe the Tier 1 query performs.

ALTER TABLE trigger_index REPLICA IDENTITY FULL;

