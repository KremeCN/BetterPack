# BetterPack

**BetterPack** is a powerful Geyser extension designed to enhance resource pack management for Bedrock players. It allows players to manage their own resource pack lists, and provides admin tools for managing default packs.

## Features

*   **Per-Player Pack Management**: Players can select and reorder their own resource packs via a GUI menu.
*   **Auto-Fixer (Experimental)**: Automatically detects and fixes resource packs with file paths exceeding the 80-character limit (a common issue on Bedrock).
    *   **Smart Migration**: Moves long paths to a safe directory (`textures/_s/`) while preserving file extensions.
    *   **Global Reference Update**: Updates all references in JSON, `.material`, `.lang`, and other text files.
*   **Admin Tools**:
    *   **Default Packs**: Admins can set default packs that are automatically applied to new players.
    *   **Push Config**: Push default pack configurations to specific players or all players.
*   **Localization**: Supports English (`en_US`), Simplified Chinese (`zh_CN`), and Traditional Chinese (`zh_TW`).

## Installation

1.  Download the latest `BetterPack.jar` release.
2.  Place the jar file into the `extensions` folder of your Geyser standalone or Geyser-Spigot/Velocity/BungeeCord installation.
3.  Restart Geyser.

## Usage

### Commands

*   `/pack menu` - Open the resource pack management menu.
*   `/pack fix <pack_name> [threshold] [-fast]` - Fix a resource pack with long paths.
    *   `pack_name`: The name of the pack file (partial match supported).
    *   `threshold`: (Optional) The path length threshold (default: 80).
    *   `-fast`: (Optional) Enable fast mode (may cause lag on main thread).
*   `/pack admin` - Open the admin menu.
*   `/pack reload` - Reload configuration and language files.
*   `/pack push <player>` - Push default packs to a specific player.
*   `/pack pushall` - Push default packs to ALL players.

### Configuration

The configuration file is located at `extensions/betterpack/config.yml`.

```yaml
# BetterPack Configuration

# Default locale for the extension (en_US, zh_CN, zh_TW)
locale: en_US

# Transfer server settings (for applying packs)
address: 127.0.0.1
port: 19132

# Path Fixer settings
fix-threshold: 80
```

## Building

To build the project from source:

```bash
./gradlew build shadowJar
```

The output jar will be located in `build/libs/BetterPack.jar`.

## Acknowledgements

*   **Special Thanks** to [PickPack](https://github.com/onebeastchris/PickPack) by onebeastchris. This project provided significant inspiration and served as a key reference for the design and logic of BetterPack.

## License

This project is licensed under the MIT License.
