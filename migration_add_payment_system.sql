-- Migration: Add Payment System for Appointment Deposits
-- Purpose: Add payment system to require 50,000 VND deposit for appointments (anti-abuse measure)
-- Date: 2025-01-XX

-- Step 1: Add deposit_amount column to appointments table
ALTER TABLE appointments 
ADD COLUMN IF NOT EXISTS deposit_amount DOUBLE PRECISION;

-- Step 2: Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT UNIQUE,
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_gateway VARCHAR(50),
    paid_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

-- Step 3: Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_payments_patient_id ON payments(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_appointment_id ON payments(appointment_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- Step 4: Add comment for documentation
COMMENT ON TABLE payments IS 'Payment records for appointment deposits (50,000 VND) to prevent abuse';
COMMENT ON COLUMN payments.amount IS 'Deposit amount in VND (default: 50,000)';
COMMENT ON COLUMN payments.status IS 'Payment status: PENDING, PAID, FAILED, REFUNDED, CANCELLED';
COMMENT ON COLUMN payments.payment_method IS 'Payment method: BANK_TRANSFER, CREDIT_CARD, E_WALLET, CASH';
COMMENT ON COLUMN payments.payment_gateway IS 'Payment gateway: VNPAY, MOMO, ZALOPAY, MANUAL';

-- Step 5: Update existing appointments (optional - set deposit_amount for reference)
UPDATE appointments 
SET deposit_amount = 50000.0 
WHERE deposit_amount IS NULL AND status != 'CANCELLED';
