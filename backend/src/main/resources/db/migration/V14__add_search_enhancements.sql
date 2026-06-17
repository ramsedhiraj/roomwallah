-- Flyway Migration: V14__add_search_enhancements.sql
-- Add new fields to search_documents for advanced filtering
ALTER TABLE search_documents ADD COLUMN facing_direction VARCHAR(50);
ALTER TABLE search_documents ADD COLUMN availability_date DATE;

-- Add notification frequency configuration to saved_searches
ALTER TABLE saved_searches ADD COLUMN notification_frequency VARCHAR(50) DEFAULT 'INSTANT';
