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
