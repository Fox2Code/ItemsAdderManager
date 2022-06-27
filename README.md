# ItemsAdderManager

Setup and manage 
[ItemsAdder](https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/) 
 the easy way. (Made for SMPs between friends.)

Having just to drop inside and outside `iapacks` is easier.

This pack is made for peoples that don't want to bother hosting their packs.  
But you still need to follow [Resourcepack hosting](https://itemsadder.devs.beer/plugin-usage/resourcepack-hosting) tutorial.

Self-hosting is recommended, just put your server IP inside `ItemsAdder/config.yml` self-hosting feature.

## Dependencies

- [LoneLibs](https://www.spigotmc.org/resources/lonelibs.75974/)
- [ItemsAdder](https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/)
- [LuckPerms](https://www.spigotmc.org/resources/luckperms.28140/) (Optional)  
  LuckPerms may be replaced with a Vault compatible permission manager, but support may not be guaranteed.

## Usage notes

This module creates a `iapacks` folder, where you can drag existing item adder addons zip files, 
just drop the modules you want there and restart the server.  
(You may want to reset the map for ItemsAdder packs that change generation)

You should extract resource ItemsAdderManager can't read in `iapacks/data` instead of `plugins/ItemsAdder/data` directly.

For fast initial setup for survival multi players (aka. SMP) you can type `/iasetup force` or `/iamanager setup force`.

By default, the plugin prevent itself from touching `ItemsAdder/data` folder if it already exists, 
please do a backup of it before using this plugin.

Windows is not fully supported due to how it manages its file system, 
it's a kernel limitation that has nothing to do with security.

## Easy setup for new servers

Step 1: Setup hosting method (Try self-hosting if possible)
Step 2: Run `/iasetup` or `/iasetup force`
Step 3: Put all your packs in the `iapacks` folder.
Step 4: Run `/iamanager reload`
Step 5: Have fun

Note: You may want to delete the world between step 4 and 5 to get new generation.

## Commands

Note: Commands with `(Unsafe)` at the end require the `ia.admin.manager.unsafe` perm.

- `/iamanager help` -> Show plugin help
- `/iamanager allowModifications` -> Unlock modification access (Unsafe)
- `/iamanager setup` -> Allow to set-up ItemsAdder for SMP, and download default packs
- `/iamanager setup force` -> Set-up but ignore warnings. (Unsafe)
- `/iamanager setup permissions` -> Set-up permissions for SMP without touching anything else. 
  (Useful if you want to change permission plugin)
- `/iamanager list` -> List loaded packs
- `/iamanager reload` -> Reload packs in `iapacks`
- `/iamanager delete` -> Delete `ItemsAdder/data`. (Console only)

- `/iasetup` is an alias for `/iamanager setup`
- `/iareloadpacks` is an alias for `/iamanager reload`

## Permissions

- `ia.admin.manager` -> Allow to use the plugin commands.
- `ia.admin.manager.unsafe` -> Allow to perform unsafe operations, 
this permission is required to force allow modifications.

# Usages for developers

Put your custom entries in `iapacks/data` and execute 
`/iareloadpacks`/`/iamanager reload` each time you want to apply your changes.

(On Windows you may need to restart the server instead, 
so it's recommended you run your server on Linux instead)

# Usages for networks

If you are a big server, it is recommended to remove this plugin on production server after all packs are loaded.
The role of this plugin is to manage packs, so it should not be needed after it has applied all of them.

# Example of supported packs

- [ItemsAdder Slimefun Addon](https://www.spigotmc.org/resources/addon-itemsadder-slimefun-addon.98439/)
- [PathwayAddons](https://www.spigotmc.org/resources/deco-pathwayaddons-itemsadder-addon.91702/)
- [NewTrees](https://www.spigotmc.org/resources/trees-newtrees-itemsadder-addon.84604/)
- [More Cookies](https://www.spigotmc.org/threads/more-cookies-for-itemsadder.559657/)
- [Custom resource-pack logo and background](https://www.spigotmc.org/resources/addon-custom-resourcepack-logo-and-background.95384/)

The loaded also support loading resources packs from the `/iapacks` folder. 
