-- Messaging reads a conversation in order and counts what has not been seen,
-- so both of those columns have to hold a value on every row.

-- Older rows may predate the defaults, and SET NOT NULL is refused while any
-- row violates it. These are no-ops on a database whose rows all have values.
UPDATE public.messages SET sent_at = CURRENT_TIMESTAMP WHERE sent_at IS NULL;
UPDATE public.messages SET is_read = FALSE WHERE is_read IS NULL;
ALTER TABLE public.messages ALTER COLUMN sent_at SET NOT NULL;
ALTER TABLE public.messages ALTER COLUMN is_read SET NOT NULL;

-- A conversation is a group of messages rather than a row of its own: there is
-- no conversations table, so a thread is identified by its listing and its two
-- participants. Every index below serves one of the queries that implies.

-- Reading one conversation, newest first, with message_id breaking ties so the
-- order cannot change between reads.
CREATE INDEX messages_thread_idx
    ON public.messages (listing_id, sent_at DESC, message_id DESC);

-- An inbox is the union of what a student received and what they sent, so each
-- direction needs its own entry point.
CREATE INDEX messages_received_idx
    ON public.messages (receiver_id, sent_at DESC, message_id DESC);
CREATE INDEX messages_sent_idx
    ON public.messages (sender_id, sent_at DESC, message_id DESC);

-- Unread badges only ever ask about unread messages, so the index stores only
-- those rows and stays small no matter how much history accumulates.
CREATE INDEX messages_unread_idx
    ON public.messages (receiver_id)
    WHERE is_read = FALSE;
