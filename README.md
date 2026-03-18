# rembot
Remastered mbot get it mbot rembot yeah my bad

## Quickstart:

> [!NOTE]  
> Note to devs: you are constantly rebuilding the rembot image, 
> make sure to clean your system at regular periods
> ```docker system prune -a```. 

> [!NOTE]  
> Note to normal users: we will provide a pre-built docker image that runs rembot in the future.

1. Invite bot with these permissions in [OAuth2](https://discord.com/developers/home) tab:
   * Kick Members
   * Ban Members
   * Moderate Members
   * Send Messages
   * Manage Messages
2. Copy [.env.example](.env.example) and create your own .env variant
3. ```docker compose up --build```

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
  - [ ] Banned words filter
  - [ ] Anti-spam filter
- [ ] Docker
  - [x] Dockerfile
    - [ ] Being automatically build by GH Actions?
  - [x] docker-compose (for development)
  - [ ] docker-compose (for production)
