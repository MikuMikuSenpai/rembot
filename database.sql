-- THIS IS THE INIT SCRIPT FOR BUILDING OUR MYSQL DATABASE LAYOUT
-- IF WORKING ON THIS MAKE SURE TO DELETE VOLUMES ETC CUS OLD LAYOUT COULD STILL BE HERE FORCE DOCKER TO REBUILD IT

-- TODO ADD AUTO DELETE THING:
--  to enable it: https://dev.mysql.com/doc/refman/8.4/en/events-configuration.html
--  syntax to create event: https://dev.mysql.com/doc/refman/8.4/en/create-event.html

CREATE TABLE users(
    discord_user_id BIGINT,

    PRIMARY KEY(discord_user_id)
);

CREATE TABLE messages_spam(
    discord_message_id BIGINT,
    user_id BIGINT,
    time_created TIMESTAMP NOT NULL,

    PRIMARY KEY(discord_message_id),
    CONSTRAINT FK_users FOREIGN KEY (user_id)
        REFERENCES users(discord_user_id)
);