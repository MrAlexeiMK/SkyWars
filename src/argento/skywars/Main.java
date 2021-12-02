package argento.skywars;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	public static Main instance;
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	
	public void onEnable() {
		instance = this;
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		SkyWars.init();
		this.getServer().getPluginManager().registerEvents(new EventHandlers(), this);
		if(SkyWars.isBungeeEnabled()) {
			Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		}
		
		Bukkit.getWorld("world").setGameRuleValue("sendCommandFeedback", "false");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equals("skywars")) {
				if(p.hasPermission("SkyWars.glav")) {
					if(args.length == 0) {
						SkyWars.send(p, "/sw create [name]");
						SkyWars.send(p, "/sw setminteams [count]");
						SkyWars.send(p, "/sw setmaxteams [count]");
						SkyWars.send(p, "/sw setplayersinteam [count]");
						SkyWars.send(p, "/sw addspawn");
						SkyWars.send(p, "/sw finish");
						SkyWars.send(p, "/sw addkit [name] [permission] [price]");
						SkyWars.send(p, "/sw setspec");
						SkyWars.send(p, "/sw start");
						SkyWars.send(p, "/sw chestadd [chance]");
					}
					else {
						if(args[0].equals("create")) {
							if(args.length == 2) {
								String name = args[1];
								SkyWars.getConfig().set("arena.name", name);
							}
						}
						else if(args[0].equals("setminteams")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								SkyWars.getConfig().set("arena.minteams", count);
							}
						}
						else if(args[0].equals("addkit")) {
							if(args.length == 4) {
								String name = args[1];
								String pex = args[2];
								long price = Integer.valueOf(args[3]);
								SkyWars.getConfig().set("kits."+name+".pex", pex);
								SkyWars.getConfig().set("kits."+name+".price", price);
								SkyWars.getConfig().set("kits."+name+".disp", p.getItemInHand());
								ItemStack[] extra = p.getInventory().getContents();
								SkyWars.getConfig().set("kits."+name+".extra", extra);
								SkyWars.getConfigFile().accept();
								
							}
						}
						else if(args[0].equals("setmaxteams")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								SkyWars.getConfig().set("arena.maxteams", count);
							}
						}
						else if(args[0].equals("setplayersinteam")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								SkyWars.getConfig().set("arena.playersinteam", count);
							}
						}
						else if(args[0].equals("addspawn")) {
							List<Location> list = (List<Location>) SkyWars.getConfig().get("arena.spawns");
							list.add(p.getLocation());
							SkyWars.getConfig().set("arena.spawns", list);
						}
						else if(args[0].equals("setspec")) {
							SkyWars.getConfig().set("arena.spec", p.getLocation());
						}
						else if(args[0].equals("finish")) {
							SkyWars.toLobby(p);
							SkyWars.getConfig().set("arena.status", "wait");
							SkyWars.setStatus("wait");
							SkyWars.getConfigFile().accept();
							
						}
						else if(args[0].equals("start")) {
							SkyWars.startLobby();
						}
						else if(args[0].equals("addcage")) {
							if(args.length == 1) {
								String name = args[1];
								ItemStack item = p.getItemInHand();
								SkyWars.getConfig().set("cages."+name, item);
							}
						}
						else if(args[0].equals("chestadd")) {
							if(args.length == 2) {
								int chance = Integer.valueOf(args[1]);
								List<ItemStack> list = new ArrayList<ItemStack>();
								list = (List<ItemStack>) SkyWars.getConfig().get("chests");
								ItemStack item = p.getItemInHand();
								ItemMeta meta = item.getItemMeta();
								List<String> lore = new ArrayList<String>();
								lore.add(String.valueOf(chance)+"%");
								meta.setLore(lore);
								item.setItemMeta(meta);
								list.add(item);
								SkyWars.getConfig().set("chests", list);
								SkyWars.getConfigFile().accept();
								
							}
						}
					}
				}
			}
		}
		return true;
	}
}
