-- Add ward_display_name to guardian_pairing_codes so it can be persisted
-- at code generation time and copied to guardian_pairings on claim.
ALTER TABLE guardian_pairing_codes ADD COLUMN ward_display_name TEXT DEFAULT '';
