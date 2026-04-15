-- THIS IS THE INIT SCRIPT FOR BUILDING OUR MYSQL DATABASE LAYOUT
-- IF WORKING ON THIS MAKE SURE TO DELETE VOLUMES ETC CUS OLD LAYOUT COULD STILL BE HERE FORCE DOCKER TO REBUILD IT
SET GLOBAL event_scheduler = ON;

CREATE TABLE users(
    discord_user_id BIGINT NOT NULL UNIQUE,

    PRIMARY KEY(discord_user_id)
);

CREATE TABLE messages(
    discord_message_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    time_created TIMESTAMP NOT NULL,
    message_content VARCHAR(2000) NOT NULL,
    -- 5000 chars should be plenti i think? these max would be 10 links in total including whitespaces between them
    attachments_links VARCHAR(5000) NOT NULL,

    PRIMARY KEY(discord_message_id),
    CONSTRAINT FK_users FOREIGN KEY (user_id)
        REFERENCES users(discord_user_id)
);

CREATE TABLE starred_messages(
    discord_message_id BIGINT NOT NULL UNIQUE,
    star_amount TINYINT NOT NULL,
    is_sent BOOLEAN NOT NULL,
    embed_message_id BIGINT NOT NULL,

    PRIMARY KEY(discord_message_id)
);

CREATE TABLE strikes_spam(
    discord_user_id BIGINT NOT NULL UNIQUE,
    amount TINYINT NOT NULL,
    most_recent_given TIMESTAMP NOT NULL,

    PRIMARY KEY(discord_user_id)
);

-- check every 12 hours if there are messages younger than 1 week delete those to clean DB.
-- We are checking these frequently cus could build up fast compared to other data.
CREATE EVENT clean_messages
    ON SCHEDULE
    EVERY 12 HOUR
    DO
    DELETE FROM messages WHERE DATE_ADD(time_created, INTERVAL 1 WEEK) < NOW();

-- clean starred messages that have discord_message_id that is NOT in messages table (message is not stored in DB)
CREATE EVENT clean_starred_messages
    ON SCHEDULE
    EVERY 12 HOUR
    DO
    DELETE FROM starred_messages WHERE discord_message_id NOT IN (
        SELECT discord_message_id
        FROM messages
    );

-- checks every day if a strike is a week old = delete
CREATE EVENT clean_strikes_spam
    ON SCHEDULE
    EVERY 1 DAY
    DO
    DELETE FROM strikes_spam WHERE DATE_ADD(most_recent_given, INTERVAL 1 WEEK) < NOW();
