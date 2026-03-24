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

> [!NOTE]  
> Note to devs: you are constantly rebuilding the rembot image, 
> make sure to clean your system at regular periods
> ```docker system prune -a```. 

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
   * Kick Members
   * Ban Members
   * Moderate Members
   * Send Messages
   * Manage Messages
2. Copy [.env.example](.env.example) and create your own .env variant
3. ```docker compose up --build```
   1. Each time you make a change to rembot you will need to rebuild it using the --build option.

## TODO:

Only for core features other stuff will be worked on after v1.0.0 release of rembot.
This will be removed when all done, here for organization.

- [ ] Admin Features:
  - [ ] Ban
    - [x] backend
    - [ ] frontend
  - [ ] Unban
    - [x] backend
    - [ ] frontend
  - [ ] Kick
    - [x] backend
    - [ ] frontend
  - [ ] Mute
    - [x] backend
    - [ ] frontend
- [ ] Moderation:
  - [x] Banned words filter
  - [ ] Anti-spam filter
- [x] Docker
  - [x] Dockerfile
    - [x] Being automatically build by GH Actions?
  - [x] docker-compose (for development)
  - [x] docker-compose (for production)
