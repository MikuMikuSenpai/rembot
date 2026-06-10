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
2. Clone the project: `git clone https://github.com/MikuMikuSenpai/rembot.git`
3. Change to the new `rembot` directory.
4. Create a new file called `.env` paste the contents of `.env.example` in `.env`
   1. Read `.env` carefully and add information where needed (e.g. BOT_TOKEN).
5. Start the bot by opening a terminal in the `rembot` directory and executing the command: `docker compose up` 
(Starting with the `-d` "detached" option is not recommended on first startup as important error messages
would not be shown).
   1. To close the rembot logs in terminal force close your terminal (this can be avoided by doing "docker compose up **-d**").
6. To stop the bot: `docker compose stop`

For more information regarding docker consult their documentation.

### Development:

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
   * Kick Members
   * Ban Members
   * Moderate Members
   * Send Messages
   * Manage Messages
2. Copy [.env.example](.env.example) and create your own .env variant
3. ```docker compose -f ./docker-compose-dev.yml up --build```
