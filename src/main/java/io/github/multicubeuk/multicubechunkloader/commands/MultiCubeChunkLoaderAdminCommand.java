package io.github.multicubeuk.multicubechunkloader.commands;

import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import io.github.multicubeuk.multicubechunkloader.util.StringUtils;
import io.github.multicubeuk.multicubechunkloader.util.UUIDUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MultiCubeChunkLoaderAdminCommand implements CommandExecutor
{
    private MultiCubeChunkLoader plugin;
    private List<Command> commands;


    public MultiCubeChunkLoaderAdminCommand(MultiCubeChunkLoader plugin)
    {
        this.plugin = plugin;

        commands = new ArrayList<>();
        commands.add(new Command("add", "/mcla add <player> <personal/world> <# chunks> - Adds available chunks of the given type to players balance. This can be a negative number.", "multicubechunkloader.admin.add"));
        commands.add(new Command("balance", "/mcla balance <player> - Lists the balance of the specified player.", "multicubechunkloader.admin.balance"));
        commands.add(new Command("list", "/mcla list [player:<player>] [world:<world>] [type:<personal/world/creative>] [page #] - Lists all chunk loaders.", "multicubechunkloader.admin.list"));
        commands.add(new Command("delete", "/mcla delete [id:<id>] [player:<player>] [world:<world>] [all] - Deletes specified or all chunk loaders", "multicubechunkloader.admin.delete"));
        commands.add(new Command("reload", "/mcla reload - Reloads the plugin, including configuration and loaded chunks", "multicubechunkloader.admin.reload"));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) 
    {

        /**
         * HELP
         */
        if (args.length == 0 || args[0].equalsIgnoreCase("?")) 
        {
            commands
                    .stream()
                    .filter(c -> sender.hasPermission(c.permissions) || sender.isOp())
                    .forEach(c -> sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, c.commandText)));
            return true;
        }

        /**
         * ADD
         */
        if (args[0].equalsIgnoreCase("add"))
        {
            if (args.length < 4)
            {
                sender.sendMessage(String.format(commands.stream().filter(c -> c.command.equalsIgnoreCase("add")).findFirst().get().commandText, ChatColor.RED));
                return false;
            }

            try
            {
                UUID player = null;
                if (args[1].length() == 36)
                {
                    try
                    {
                        player = UUID.fromString(args[1]);
                    } 
                    catch (Exception ex)
                    {
                        // Not a uuid
                    }
                }

                if (player == null)
                    player = UUIDUtils.getUUID(args[1]);

                ChunkLoader.ChunkType type = ChunkLoader.ChunkType.valueOf(args[2].toUpperCase());
                int chunks = Integer.parseInt(args[3]);

                PlayerInfo pi = plugin.getPlayerInfo(player);

                if (type == ChunkLoader.ChunkType.PERSONAL)
                {
                    pi.addPersonalChunks(chunks);
                    sender.sendMessage(ChatColor.GREEN + "Successfully added " + chunks + " personal chunkloaders to " + pi.getName() + "'s balance!");
                }
                else
                {
                    pi.addWorldChunks(chunks);
                    sender.sendMessage(ChatColor.GREEN + "Successfully added " + chunks + " world chunkloaders to " + pi.getName() + "'s balance!");
                }

                plugin.updatePlayerInfo(pi);


                return true;
            } 
            catch (Exception ex)
            {
                sender.sendMessage(ChatColor.RED + "Error while adjusting balance! See console for more information.");
                plugin.getLogger().log(Level.SEVERE, "Error while updating player chunk balance!", ex);
                return false;
            }
        }

        /**
         * BALANCE
         */
        if (args[0].equalsIgnoreCase("balance")) 
        {
            if (args.length != 2) 
            {
                sender.sendMessage(ChatColor.RED + "Invalid arguments to balance command. Format must be /mcl balance <uuid|player name>");
                return true;
            }

            UUID uuid = null;
            if (args[1].length() == 36) 
            {
                try 
                {
                    uuid = UUID.fromString(args[1]);
                } 
                catch (Exception ex) 
                {
                    // No a uuid
                }
            }

            if (uuid == null) 
            {
                uuid = UUIDUtils.getUUID(args[1]);
            }

            if (uuid == null) 
            {
                sender.sendMessage(ChatColor.RED + "The specified player can not be found! Specify a valid UUID or player name.");
                return true;
            }

            PlayerInfo pi = plugin.getPlayerInfo(uuid);
            int usedPersonal = 0;
            int usedWorld = 0;

            for (ChunkLoader c : plugin.getChunkLoaders()
                    .stream()
                    .filter(cl -> cl.getOwner().equals(pi.getUuid()))
                    .collect(Collectors.toList())) 
                    {
                if (c.getChunkType() == ChunkLoader.ChunkType.WORLD)
                    usedWorld += c.getSize();
                else if (c.getChunkType() == ChunkLoader.ChunkType.PERSONAL)
                    usedPersonal += c.getSize();
            }

            if (!UUIDUtils.getPlayer(uuid).isOnline()) 
            {
                List<ChunkLoader> chunks = plugin.getPersonalChunks(uuid);

                for (ChunkLoader c : chunks)
                    usedPersonal += c.getSize();
            }

            sender.sendMessage(String.format("%sBalance of %s is:", ChatColor.GREEN, pi.getName()));
            sender.sendMessage(String.format("%s%s of %s free personal chunks", ChatColor.GREEN, pi.getPersonalChunks() - usedPersonal, pi.getPersonalChunks()));
            sender.sendMessage(String.format("%s%s of %s free world chunks", ChatColor.GREEN, pi.getWorldChunks() - usedWorld, pi.getWorldChunks()));

            return true;
        }

        /**
         * LIST
         */
        if (args[0].equalsIgnoreCase("list")) 
        {

            boolean filterOwner = false;
            String owner = "";
            boolean filterType = false;
            ChunkLoader.ChunkType type = null;
            boolean filterWorld = false;
            String world = "";

            int page = 0;

            if (args.length > 1) 
            {
                for (int i = 1; i < args.length; i++) 
                {
                    //if (args[i] = )
                    if (args[i].startsWith("owner:")) 
                    {
                        String[] o = args[i].split(":");
                        if (o.length != 2) 
                        {
                            sender.sendMessage(ChatColor.RED + "Invalid owner filter. Filter must have the form of \"owner:<uuid|name>\"");
                            return true;
                        }

                        owner = o[1];
                        filterOwner = true;
                    } 
                    else if (args[i].startsWith("type:")) 
                    {
                        String[] t = args[i].split(":");
                        if (t.length != 2) 
                        {
                            sender.sendMessage(ChatColor.RED + "Invalid type filter. Filter must have the form of \"type:<personal|world|creative>\"");
                            return true;
                        }

                        try 
                        {
                            type = ChunkLoader.ChunkType.valueOf(t[1].toUpperCase());
                        } 
                        catch (Exception ex) 
                        {
                            sender.sendMessage(ChatColor.RED + "Invalid type filter. Filter must have the form of \"type:<personal|world|creative>\"");
                            return true;
                        }

                        filterType = true;
                    } 
                    else if (args[i].startsWith("world:")) 
                    {
                        String[] w = args[i].split(":");
                        if (w.length != 2) 
                        {
                            sender.sendMessage(ChatColor.RED + "Invalid world filter. Filter must have the form of \"world:worldname\"");
                            return true;
                        }

                        World w1 = plugin.getServer().getWorld(w[1]);

                        if (w1 == null) 
                        {
                            sender.sendMessage(ChatColor.RED + "Invalid world filter. Specified world does not exist!");
                            return true;
                        }

                        world = w[1];
                        filterWorld = true;
                    }
                    else if (StringUtils.isInteger(args[i])) 
                    {
                        page = Integer.parseInt(args[i]);
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid arguement (" + args[i] + ")");
                        return false;
                    }
                }
            }

            page = page < 0 ? 0 : page * 10;
            int totalPages = (int) Math.ceil(plugin.getChunkLoaders().size() / 10.0);

            sender.sendMessage(String.format("%sListing chunk loaders (page %s/%s)", ChatColor.YELLOW, page + 1, totalPages));

            for (ChunkLoader c : plugin.getChunkLoaders().stream().skip(page).limit(10).collect(Collectors.toList())) {

                if (filterOwner) 
                {
                    if (owner.length() == 36) 
                    {
                        try 
                        {
                            if (!c.getOwner().equals(UUID.fromString(owner)))
                                continue;
                        } 
                        catch (Exception ex) 
                        {
                            // Not a UUID
                        }
                    }
                    else 
                    {
                        if (!c.getOwnerName().equalsIgnoreCase(owner))
                            continue;
                    }
                }

                if (filterType) 
                {
                    if (type != null) 
                    {
                        if (c.getChunkType() != type)
                            continue;
                    }
                }

                if (filterWorld) 
                {
                    if (!c.getWorld().equalsIgnoreCase(world))
                        continue;
                }

                sender.sendMessage(
                        String.format(
                                "%s#%s, %s, %s(%s) %s(%s,%s,%s)",
                                ChatColor.GREEN,
                                c.getId(),
                                c.getOwnerName(),
                                c.getChunkType(),
                                c.getSize(),
                                c.getWorld(),
                                c.getX(),
                                c.getY(),
                                c.getZ()
                        )
                );
            }

            return true;

        }

        /**
         * DELETE
         */

        if (args[0].equalsIgnoreCase("delete")) 
        {
            if (!sender.hasPermission("multicubechunkloader.admin.delete") || !sender.isOp()) 
            {
                sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
            }

            if (args.length < 2) 
            {
                sender.sendMessage(ChatColor.RED + "Invalid arguments. At least one filter must be specified");
                sender.sendMessage(ChatColor.YELLOW + commands.stream().filter(c -> c.command.equalsIgnoreCase("delete")).findFirst().orElse(null).commandText);
            }

            boolean filterId = false;
            boolean filterOwner = false;
            boolean filterWorld = false;
            boolean filterType = false;
            boolean deleteAll = false;

            int id = -1;
            UUID owner = null;
            String world = null;
            ChunkLoader.ChunkType type = null;

            for (int i = 1; i < args.length; i++) 
            {
                String arg = args[i];
                if (arg.startsWith("id:"))
                {
                    String[] i2 = arg.split(":");

                    if (i2.length != 2 || !StringUtils.isInteger(i2[1]))
                    {
                        sender.sendMessage("Invalid ID argument for the delete command! Format must be \"id:<#>\"");
                        return true;
                    }

                    id = Integer.parseInt(i2[1]);
                    filterId = true;
                }
                else if (arg.startsWith("owner:"))
                {
                    String[] o = arg.split(":");

                    if (o.length != 2) 
                    {
                        sender.sendMessage("Invalid owner argument for the delete command! Format must be \"owner:<UUID|Player name>\"");
                        return true;
                    }

                    if (o[1].length() == 36) 
                    {
                        try 
                        {
                            owner = UUID.fromString(o[1]);
                        }
                        catch (Exception ex) 
                        {
                            sender.sendMessage("Invalid UUID argument for the delete command! Format must be \"owner:<UUID|Player name>\"");
                            return true;
                        }
                    }
                    else 
                    {
                        owner = UUIDUtils.getUUID(o[1]);
                    }

                    if (owner == null) 
                    {
                        sender.sendMessage("Invalid Player Name argument for the delete command! Format must be \"owner:<UUID|Player name>\"");
                        return true;
                    }

                    filterOwner = true;
                } 
                else if (arg.startsWith("type:")) 
                {
                    String[] t = arg.split(":");

                    if (t.length != 2) 
                    {
                        sender.sendMessage("Invalid type argument for the delete command! Format must be \"type:<personal|world|creative>\"");
                        return true;
                    }

                    try 
                    {
                        type = ChunkLoader.ChunkType.valueOf(t[1].toUpperCase());
                    }
                    catch (Exception ex) 
                    {
                        sender.sendMessage("Invalid type argument for the delete command! Format must be \"type:<personal|world|creative>\"");
                        return true;
                    }

                    filterType = true;

                }
                else if (arg.startsWith("world:")) 
                {
                    String[] w = arg.split(":");

                    if (w.length != 2) 
                    {
                        sender.sendMessage("Invalid world argument for the delete command! Format must be \"world:<world name>\"");
                        return true;
                    }

                    World w1 = plugin.getServer().getWorld(w[1]);

                    if (w1 == null)
                    {
                        sender.sendMessage("Specified World Name does not exist! Format must be \"world:<world name>\"");
                        return true;
                    }

                    filterWorld = true;
                    world = w1.getName();

                } 
                else if (arg.equalsIgnoreCase("all")) 
                {
                    deleteAll = true;
                } 
                else 
                {
                    sender.sendMessage(String.format("%sInvalid argument (%s) used with the delete command!", ChatColor.RED, arg));
                    sender.sendMessage(ChatColor.YELLOW + commands.stream().filter(c -> c.command.equalsIgnoreCase("delete")).findFirst().orElse(null).commandText);
                    return true;
                }
            }

            if (deleteAll && (filterId || filterOwner || filterWorld)) 
            {
                sender.sendMessage(ChatColor.RED + "Delete ALL only works with TYPE as an optional extra argument. Command aborted!");
                return true;
            } 
            else if (deleteAll) 
            {
                int loaders = 0;
                int chunks = 0;
                if (filterType) 
                {
                    if (type == ChunkLoader.ChunkType.PERSONAL) 
                    {
                        List<ChunkLoader> cl = plugin.getPersonalChunks();

                        for (ChunkLoader c : cl) 
                        {
                            loaders++;
                            chunks += c.getSize();
                            ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cls -> cls.getId() == c.getId()).findFirst().orElse(null);
                            if (lpc != null)
                                lpc.delete();
                            else
                                c.delete();
                        }
                    }
                    else
                    {
                        ChunkLoader.ChunkType ct = type;
                        for (ChunkLoader c : plugin.getChunkLoaders()
                                .stream()
                                .filter(cl -> cl.getChunkType() == ct)
                                .collect(Collectors.toList())) 
                        {
                            loaders++;
                            chunks += c.getSize();
                            c.delete();
                        }
                    }
                }
                else 
                {
                    List<ChunkLoader> pChunks = plugin.getPersonalChunks();

                    for (ChunkLoader c : pChunks) 
                    {
                        loaders++;
                        chunks += c.getSize();
                        // Check if it is already loaded
                        ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cl -> c.getId() == c.getId()).findFirst().orElse(null);
                        if (lpc != null)
                            lpc.delete();
                        else
                            c.delete();
                    }

                    for (ChunkLoader c : plugin.getChunkLoaders()
                            .stream()
                            .filter(cl -> cl.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                            .collect(Collectors.toList())) 
                    {
                        loaders++;
                        chunks += c.getSize();
                    }

                }
                sender.sendMessage(String.format("%sSuccessfully deleted %s chunk loaders, totalling %s chunks", ChatColor.GREEN, loaders, chunks));
                return true;
            } 
            else if (filterId && (filterOwner || filterType || filterWorld)) 
            {
                sender.sendMessage(ChatColor.RED + "Delete ID does not work with any other filter arguments. Command aborted!");
                return true;
            }
            else if (filterId) 
            {
                int cid = id;
                ChunkLoader c = plugin.getChunkLoaders().stream().filter(cl -> cl.getId() == cid).findFirst().orElse(null);

                if (c == null) 
                {
                    c = plugin.getChunkLoader(id);

                    if (c == null) 
                    {
                        sender.sendMessage(ChatColor.RED + "The specified ID is invalid. Command aborted!");
                        return true;
                    }

                    int chunks = c.getSize();
                    c.delete();

                    sender.sendMessage(String.format("%sSuccessfully deleted 1 chunk loader, totalling %s chunks", ChatColor.GREEN, chunks));
                    return true;
                }
            } 
            else if (filterOwner || filterType || filterWorld)
            {
                List<ChunkLoader> clsType = null;
                List<ChunkLoader> clsOwner = null;
                List<ChunkLoader> clsWorld = null;

                if (filterType) 
                {
                    clsType = new ArrayList<>();
                    if (type == ChunkLoader.ChunkType.PERSONAL) 
                    {
                        List<ChunkLoader> personal = plugin.getPersonalChunks();

                        for (ChunkLoader c : personal)
                        {
                            ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cl -> cl.getId() == c.getId()).findFirst().orElse(null);

                            clsType.add(lpc == null ? c : lpc);
                        }
                    }
                    else
                    {
                        List<ChunkLoader> tType = new ArrayList<>();
                        plugin.getChunkLoaders()
                                .stream()
                                .filter(cl -> cl.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                .forEach(cl -> tType.add(cl));

                        clsType.addAll(tType);
                    }
                }
                if (filterOwner) 
                {
                    clsOwner = new ArrayList<>();
                    UUID uuid = owner;
                    List<ChunkLoader> tOwner = new ArrayList<>();

                    if (clsType != null) 
                    {
                        clsType.stream()
                                .filter(c -> c.getOwner().equals(uuid))
                                .forEach(c -> tOwner.add(c));
                        clsOwner.addAll(tOwner);
                    } 
                    else 
                    {
                        List<ChunkLoader> personal = plugin.getPersonalChunks(owner);

                        for (ChunkLoader c : personal) 
                        {
                            ChunkLoader lpc = plugin.getChunkLoaders()
                                    .stream()
                                    .filter(cl -> cl.getId() == c.getId())
                                    .findFirst().orElse(null);

                            clsOwner.add(lpc == null ? c : lpc);
                        }

                        plugin.getChunkLoaders()
                                .stream()
                                .filter(c -> c.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                .forEach(c -> tOwner.add(c));

                        clsOwner.addAll(tOwner);
                    }
                }
                if (filterWorld) 
                {
                    clsWorld = new ArrayList<>();
                    String world2 = world;
                    List<ChunkLoader> tworld = new ArrayList<>();

                    if (clsOwner != null) 
                    {
                        clsOwner.stream()
                                .filter(c -> c.getWorld().equalsIgnoreCase(world2))
                                .forEach(c -> tworld.add(c));
                        clsWorld.addAll(tworld);
                    } 
                    else 
                    {
                        List<ChunkLoader> personal = plugin.getPersonalChunks();

                        for (ChunkLoader c : personal) 
                        {
                            if (!c.getWorld().equalsIgnoreCase(world2))
                                continue;

                            ChunkLoader lpc = plugin.getChunkLoaders()
                                    .stream()
                                    .filter(cl -> cl.getWorld().equalsIgnoreCase(c.getWorld()))
                                    .findFirst().orElse(null);

                            clsWorld.add(lpc == null ? c : lpc);
                        }

                        plugin.getChunkLoaders()
                                .stream()
                                .filter(c -> c.getWorld().equalsIgnoreCase(world2))
                                .forEach(c -> tworld.add(c));

                        clsWorld.addAll(tworld);
                    }
                }

                List<ChunkLoader> deleteList = clsWorld == null ? clsOwner == null ? clsType : clsOwner : clsWorld;
                int loaders = 0;
                int chunks = 0;

                for (ChunkLoader c : deleteList) 
                {
                    loaders++;
                    chunks += c.getSize();

                    c.delete();
                }

                sender.sendMessage(String.format("%sSuccessfully deleted %s chunk loaders, totalling %s chunks", ChatColor.GREEN, loaders, chunks));
                return true;
            }

        }


        /**
         * RELOAD
         */
        if (args[0].equalsIgnoreCase("reload")) 
        {
            if (!sender.hasPermission("multicubechunkloader.admin.reload") || !sender.isOp()) 
            {
                sender.sendMessage(ChatColor.RED + "You do not have access to this command");
                return true;
            }

            plugin.onDisable();
            plugin.onEnable();
            return true;
        }
        return false;
    }

    private class Command 
    {
        public String command;
        public String commandText;
        public String permissions;

        public Command() 
        {
            
        }

        public Command(String command, String commandText, String permissions) 
        {
            this.command = command;
            this.commandText = commandText;
            this.permissions = permissions;
        }
    }
}
