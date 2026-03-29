# DiscordSRV-LinkChannel

A Paper/Spigot plugin that integrates with DiscordSRV account linking and allows linking through a dedicated Discord channel.

## Features

- Listens for Discord messages in one configured channel.
- Processes only numeric linking codes (for example `8727`).
- Ignores non-numeric messages for linking.
- Optional cleanup mode that deletes:
	- user messages in the linking channel
	- plugin reply messages after sending
- Console command to reload config without restart.

## Compatibility

- Minecraft API: `1.21.x` (built against Spigot API `1.21.11-R0.2-SNAPSHOT`)
- Server software: Paper/Spigot compatible
- Java: 21
- DiscordSRV dependency: `1.28.0` (provided)

## Installation

1. Install DiscordSRV and complete its setup.
2. Place `DiscordSRV-LinkChannel-1.3.jar` in your server `plugins/` folder.
3. Start the server once to generate config.
4. Set the Discord channel ID in `plugins/DiscordSRV-LinkChannel/config.yml`.
5. Reload with `/lcreload` from console (or restart).

## Configuration

`config.yml`:

```yml
# DiscordSRV LinkChannel settings
LinkingDiscordChannel: "000000000000000000"
RemoveMessages: true
```

- `LinkingDiscordChannel`: Discord channel ID used for linking.
- `RemoveMessages`:
	- `true`: delete user messages and plugin responses after a short delay.
	- `false`: keep messages visible.

## Commands

- `lcreload`:
	- Reloads plugin config.
	- Console only.

## How Linking Works

1. Player attempts to join and is asked by DiscordSRV for a code.
2. Player sends that numeric code in the configured Discord channel.
3. Plugin passes the code to DiscordSRV account link manager.
4. Plugin sends DiscordSRV response text back to channel.
5. If cleanup is enabled, the messages are deleted after delay.

## Release Notes (1.3)

- Updated for Minecraft 1.21.11 API level.
- Updated DiscordSRV dependency to 1.28.0.
- Improved compatibility with DiscordSRV/JDA runtime variants.
- Enforced numeric-only link code processing.
- Added optional cleanup for plugin response messages.

## Development

Build with Maven:

```bash
mvn -DskipTests clean package
```

Output jar:

- `target/DiscordSRV-LinkChannel-1.3.jar`
