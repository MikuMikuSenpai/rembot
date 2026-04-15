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
2. Copy [.env.example](.env.example) and create your own .env variant
3. ```docker compose -f docker-compose-prod.yml up```
   1. Keep in mind that you will need to delete old rembot containers and images and redo this command to host the most recent version of rembot (build on each push to [main](https://github.com/MikuMikuSenpai/rembot/tree/main) or check [releases](https://github.com/MikuMikuSenpai/rembot/releases)).

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
