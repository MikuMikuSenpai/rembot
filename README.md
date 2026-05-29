# rembot
Remastered mbot get it mbot rembot yeah my bad

## Quickstart:

### Production/Hosting:

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
    * Kick Members
    * Ban Members
    * Moderate Members
    * Send Messages
    * Manage Messages
2. Create a directory for example: `rembot-directory` and create a file called `.env` with this information:
   ```sh
   ### REMBOT CONFIGURATION FILE ###
   # Read thoroughly to avoid issues with running bot.
   # If defaults provided = recommended settings.
   # All NUMBERS should be POSITIVE unless stated otherwise.
   # Your Discord Bot token which can be claimed under: https://discord.com/developers/applications -> Click on bot
   # -> Left side at the tabs "Bot" -> Token: "Reset Token"
   BOT_TOKEN=

   # Change log level here. (default=info)
   # It is possible to change this level to debug if the bot is not running like expected,
   # however output can become overwhelming.
   ROOT_LOG_LEVEL=info
   # Change which docker image tag to use (default=latest) [intended for host/production]
   # For example: v2 is buggy, its possible to only run v1.9 by editing below.
   # /!\ latest = LATEST version of rembot and could be buggy, it's recommended to run a tagged version e.g. "v1.0.0"
   # as these would be more reliable.
   # For releases check: https://github.com/MikuMikuSenpai/rembot/releases
   IMAGE_TAG=latest
   # rembot can only be connected to ONE server at a time (this is a deliberate decision)
   GUILD_ID=

   # Database password: choose a strong password. With this you are able to query data locally and rembot needs this.
   MYSQL_ROOT_PASSWORD=
   # You can choose any database name. (default=rembot)
   MYSQL_DATABASE=rembot
   # Change to your timezone (MySQL needs this for correct time): https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
   TZ=Etc/GMT
   # Some interactions with the bot are either only for mods (e.g. admin commands) or mods are excluded (e.g. spam filter).
   MOD_ROLE_ID=
   # For logging in private channel what only mods should see (e.g. logging deleted messages).
   LOG_CHANNEL_ID=
   # For public things (e.g. user being banned for misbehaving).
   DARWIN_CHANNEL_ID=
   # The amount of star emoji reactions a message needs to be highlighted (default=2)
   HIGHLIGHT_STAR_THRESHOLD=2

   # Example for below if ANTI_SPAM_WORDS_AMOUNT=5 and ANTI_SPAM_TIME_AMOUNT=2
   # this translates to "if 5 words are send within a time frame of 2 seconds = spam".
   # The sample size of determining spam amount. (default=5)
   ANTI_SPAM_WORDS_AMOUNT=5
   # The amount of time in which a certain amount of words (env ANTI_SPAM_WORDS_AMOUNT) are considered spam. (default=2)
   ANTI_SPAM_TIME_AMOUNT=2
   # The amount of minutes for mute if someone spams. (default=5)
   ANTI_SPAM_MUTE_AMOUNT=5
   # The amount of strikes a user is allowed to get before being banned for spamming. (default=3)
   ANTI_SPAM_STRIKE_AMOUNT=3

   # Words that will be auto-deleted they are split up using "," (COMMA!) (e.g. "badword,badword2,badword3")
   # WHITESPACES are counted as a character keep that in mind to avoid unwanted behaviour.
   BANNED_WORDS=
   # Words that will be EXCLUDED from being auto-deleted they are split up using "," (COMMA!) (e.g. "badword,badword2,badword3")
   # WHITESPACES are counted as a character keep that in mind to avoid unwanted behaviour.
   WHITELISTED_WORDS=
   # Choose an amount banned words will be replicated for (e.g. ["badword", "badwordbadword"]), if unsure keep default.
   # (default=10)
   REPLICATE_AMOUNT=10
   # This operation is heavy on the CPU/RAM if you are unsure keep default value (default=2)
   # /!\ Using a big number has the potential to use too much heap memory making the bot freeze.
   # How many substituted words need to be checked, if someone sends a message with a word count higher than you set it will be logged in the log channel.
   SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT=2
   # How many substitute characters are allowed to be in a message for it to still be checked via substitute() (default=3)
   # /!\ Again, using a big number has the potential to freeze rembot use default value if unsure.
   ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE=3
   ```
3. Create a file called `docker-compose.yml` with this inside:
   ```yml
   services:
     rembot:
       image: ghcr.io/mikumikusenpai/rembot:${IMAGE_TAG}
       container_name: rembot
       depends_on:
         db:
           condition: service_healthy
       env_file:
         - .env

     db:
       image: mysql
       container_name: db
       healthcheck:
         test: ["CMD", "mysqladmin" ,"ping", "-h", "localhost"]
         interval: 15s
         timeout: 20s
         retries: 10
       restart: on-failure
       env_file:
         - .env
       volumes:
         - ./database.sql:/docker-entrypoint-initdb.d/database.sql
   ```
4. Start a terminal in this `rembot-directory` with the two files you just created.
5. Execute this command: `docker compose up` (-d / detached is not recommended as important error messages would be
missed on first startup). To close the terminal either press `d` or force close the terminal.
6. To stop the bot: open a terminal in the `rembot-directory` and execute: `docker compose stop`

For more information regarding docker consult their documentation.

### Development:

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
   * Kick Members
   * Ban Members
   * Moderate Members
   * Send Messages
   * Manage Messages
2. Copy [.env.example](.env.example) and create your own .env variant
3. ```docker compose up --build```
   1. Each time you make a change to rembot, you will need to rebuild it using the --build option.
