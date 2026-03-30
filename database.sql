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

    PRIMARY KEY(discord_message_id),
    CONSTRAINT FK_users FOREIGN KEY (user_id)
        REFERENCES users(discord_user_id)
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

-- checks every day if a strike is a week old = delete
CREATE EVENT clean_strikes_spam
    ON SCHEDULE
    EVERY 1 DAY
    DO
    DELETE FROM strikes_spam WHERE DATE_ADD(most_recent_given, INTERVAL 1 WEEK) < NOW();
