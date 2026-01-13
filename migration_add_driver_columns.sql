-- Migration script to add driver_name and driver_phone columns to ambulances table
-- Run this script on your PostgreSQL database

-- Step 1: Add driver_name column (nullable)
ALTER TABLE ambulances 
ADD COLUMN IF NOT EXISTS driver_name VARCHAR(255);

-- Step 2: Add driver_phone column (nullable)
ALTER TABLE ambulances 
ADD COLUMN IF NOT EXISTS driver_phone VARCHAR(20);

-- Note: These columns are nullable to allow existing ambulances without drivers
-- You can update existing records manually if needed:
-- UPDATE ambulances SET driver_name = 'Driver Name', driver_phone = '0123456789' WHERE id = <ambulance_id>;
