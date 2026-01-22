/*
 * New Role Admin
 */
INSERT INTO roles(name, description, creator_id, updater_id)
VALUES ('ROLE_ADMIN', 'Act as admin / super user', 'SYSTEM', 'SYSTEM');


/*
 * New Default Users
 */
INSERT INTO users(email, password, creator_id, updater_id)
VALUES ('mondijewellery@gmail.com',
        '$2a$10$6aS/efMFc5eAZ7F9EKsDT.fwH7hL6welQ24ASQdsFerNvjjBCkp6W', /* Admin@2026 */
        'SYSTEM',
        'SYSTEM');

/*
 * New User Roles
 */
INSERT INTO users_roles(user_id, role_id)
VALUES (1, 1);
