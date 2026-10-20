# reMbot
Remastered Mbot get it Mbot reMbot yeah my bad

## Reference Sheet

```text
[] = optional
Slash Commands (/)
    Admin Commands (privileged access)
        /ban username [reason]
        /kick username [reason]
        /mute username minutes [hours] [reason]
        /unban username [reason]
        /unmute username [reason]
Non-Slash Commands (&)
    &nuke - role play as Kim Jong-un and nuke a country
    &ping - returns "pong", time took in milliseconds (ms) and what version of reMbot is running.
Features
    Moderation
        Auto-delete messages that contain a banned word(s)
        Auto-mute users who spam (mute detection can be customized)
        Auto-delete messages that include a discord server/guild invite link
        Deleted and edited messages are logged in a log channel
    Other
        Message highlight based on the star emoji reaction
```

## Quickstart:

### Production/Hosting:

> [!WARNING]
> Do not use `latest` as reMbot version / IMAGE_TAG in [.env.example](.env.example) instead use a [release](https://github.com/MikuMikuSenpai/rembot/releases) for example `v1.0.0`.

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
    * Kick Members
    * Ban Members
    * Moderate Members
    * Send Messages
    * Manage Messages
2. Set `Install Link` to "None" in the [Installation](https://discord.com/developers/home) tab
3. Turn off `Public Bot`, turn on `Server Members Intent` and turn on `Message Content Intent` in the [Bot](https://discord.com/developers/home) tab
4. Clone the project: `git clone https://github.com/MikuMikuSenpai/rembot.git`
5. Change to the new `rembot` directory.
6. Create a new file called `.env` paste the contents of `.env.example` in `.env`
7. Edit the `.env` file with your information (e.g. BOT_TOKEN).
8. Start the bot by opening a terminal in the `rembot` directory and executing the command: `docker compose up`
9. To stop the bot, in the rembot directory use this command: `docker compose stop`

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
