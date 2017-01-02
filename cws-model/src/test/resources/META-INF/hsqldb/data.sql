-- =============================================================================
-- Initialization Script for the CWS Database
-- =============================================================================

-- Initial Database Version is 1, initial Production CWS release is 1.0.0
INSERT INTO versions(schema_version, cws_version) VALUES (1, '1.0.0');

-- Default, we have 1 Object Datatype, which is the folder. The rest is left to
-- the initial setup to create
INSERT INTO datatypes (type_name, type_value) VALUES ('folder', 'Folder');
