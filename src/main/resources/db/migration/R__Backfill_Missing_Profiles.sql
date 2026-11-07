/*
 * Create the missing profile of every user that has none.
 *
 * Between commit 5782dfc and its fix, NewProfileEventListener wrote the profile inside an already
 * committed transaction, so users who registered (email or Google) in that window have no profile
 * and get "no profile was found" on profile requests. Google profile pictures from that window
 * cannot be recovered; those users can upload one again.
 *
 * Repeatable and idempotent: it only inserts rows that do not exist yet.
 */
INSERT INTO profiles (id, creator_id, updater_id)
SELECT u.id, 'SYSTEM', 'SYSTEM'
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM profiles p WHERE p.id = u.id);
