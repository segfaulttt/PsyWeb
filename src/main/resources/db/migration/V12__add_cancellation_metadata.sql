ALTER TABLE slots
ADD COLUMN cancelled_at TIMESTAMP,
ADD COLUMN cancellation_initiator VARCHAR(50),
ADD COLUMN cancellation_reason VARCHAR(50),
ADD CONSTRAINT chk_slots_cancellation_initiator
CHECK (cancellation_initiator in ('CLIENT', 'SPECIALIST', 'ADMIN', 'SYSTEM')),
ADD CONSTRAINT chk_slots_cancellation_reason 
CHECK (cancellation_reason in ('CLIENT_REQUEST', 'SPECIALIST_REMOVED_AVAILABILITY', 
	'SPECIALIST_SUSPENDED', 'ADMINISTRATIVE_CANCELLATION')),
ADD CONSTRAINT chk_slots_cancellation_consistency
CHECK (
    (
        cancelled_at IS NULL
        AND cancellation_initiator IS NULL
        AND cancellation_reason IS NULL
    )
    OR
    (
        cancelled_at IS NOT NULL
        AND cancellation_initiator IS NOT NULL
        AND cancellation_reason IS NOT NULL
    )
);

ALTER TABLE bookings
ADD COLUMN cancellation_initiator VARCHAR(50),
ADD COLUMN cancellation_reason VARCHAR(50),
ADD CONSTRAINT chk_bookings_cancellation_initiator 
CHECK (cancellation_initiator in ('CLIENT', 'SPECIALIST', 'ADMIN', 'SYSTEM')),
ADD CONSTRAINT chk_bookings_cancellation_reason 
CHECK (cancellation_reason in ('CLIENT_REQUEST', 'SPECIALIST_REMOVED_AVAILABILITY', 
	'SPECIALIST_SUSPENDED', 'ADMINISTRATIVE_CANCELLATION')),
ADD CONSTRAINT chk_bookings_cancellation_consistency
CHECK (
    (
        cancelled_at IS NULL
        AND cancellation_initiator IS NULL
        AND cancellation_reason IS NULL
    )
    OR
    (
        cancelled_at IS NOT NULL
        AND cancellation_initiator IS NOT NULL
        AND cancellation_reason IS NOT NULL
    )
);

ALTER TABLE reservations
ADD COLUMN cancelled_at TIMESTAMP,
ADD COLUMN cancellation_initiator VARCHAR(50),
ADD COLUMN cancellation_reason VARCHAR(50),
ADD CONSTRAINT chk_reservations_cancellation_initiator 
CHECK (cancellation_initiator in ('CLIENT', 'SPECIALIST', 'ADMIN', 'SYSTEM')),
ADD CONSTRAINT chk_reservations_cancellation_reason 
CHECK (cancellation_reason in ('CLIENT_REQUEST', 'SPECIALIST_REMOVED_AVAILABILITY', 
	'SPECIALIST_SUSPENDED', 'ADMINISTRATIVE_CANCELLATION')),
ADD CONSTRAINT chk_reservations_cancellation_consistency
CHECK (
    (
        cancelled_at IS NULL
        AND cancellation_initiator IS NULL
        AND cancellation_reason IS NULL
    )
    OR
    (
        cancelled_at IS NOT NULL
        AND cancellation_initiator IS NOT NULL
        AND cancellation_reason IS NOT NULL
    )
);