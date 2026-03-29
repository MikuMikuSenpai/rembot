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
    most_recent_given TIMESTAMP NOT NULL
);

-- check every 12 hours if there are messages younger than 1 week (could extend test this for now) delete those to clean DB
CREATE EVENT clean_messages
       ON SCHEDULE
       EVERY 12 HOUR
       DO
       DELETE FROM messages WHERE DATE_ADD(time_created, INTERVAL 1 WEEK) < NOW();
