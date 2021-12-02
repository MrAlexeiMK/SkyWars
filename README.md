# SkyWars
 Minecraft plugin on SkyWars game on spigot-1.16+  
 SpigotMC: https://www.spigotmc.org/resources/skywarsmeow.98062/
  
<b>Plugin support only 1 arena with 1 server</b>

<b>Features:</b>
- MySQL support to collect players stats
- BungeeCord support to leave from game into lobby
- Cages with permissions
- Kits with permissions
- Configurable loot into chests
- Configurable kits
- Configurable messages

<b>Requirements:</b>
- Spigot/Paperspigot 1.16+
- Vault and any economy plugin
- Optionally, TAB plugin

<b>Installation:</b>
1) Put this plugin into your 'plugins' folder.
2) Duplicate your 'world' folder as 'map' folder and write in your 'start.sh/.bat' script to delete 'world' folder and duplicate 'map' folder as 'world' folder (server example at github). It is needed for map regeneration.
3) Start the server and edit config.yml in plugin folder, then restart.

Commands:
1) /sw create [name] - create arena
2) /sw setminteams [count]
3) /sw setmaxteams [count]
4) /sw setplayersinteam [count]
5) /sw addspawn - add player's spawn (above island)
6) /sw finish - finish setup
7) /sw addkit [name] [permission] [price] - add items in your inventory in config as kit
8) /sw setspec - set spectator's spawn
9) /sw start - force start the game
10) /sw chestadd [chance] - add item in your hand into chests loot with some chance (integer number from 1 to 100)
