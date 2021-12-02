package argento.skywars;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import argento.skywars.configs.Config;
import argento.skywars.configs.Lang;
import net.milkbowl.vault.economy.Economy;

public class SkyWars implements Listener {
	private static boolean mysqlEnabled = false;
	private static boolean bungeeEnabled = false;
	private static String url, user, password;
	private static String status = "wait";
	private static int playersCount = 0;
	private static Inventory teamInv;
	private static List<String> colors;
	private static HashMap<Player, Integer> players = new HashMap<Player, Integer>();
	private static HashMap<String, Integer> kills = new HashMap<String, Integer>();
	private static HashMap<String, Location> playerCage = new HashMap<String, Location>();
	private static ArrayList<ArrayList<Player>> teams = new ArrayList<ArrayList<Player>>();
	private static HashSet<Location> chests = new HashSet<Location>();
	private static HashMap<String, Integer> isVotes = new HashMap<String, Integer>();
	private static HashMap<String, List<ItemStack>> playerKit = new HashMap<String, List<ItemStack>>();
	private static HashSet<Location> trappedChests = new HashSet<Location>();
    private static Economy econ;
	private static int startTime;
	private static int gameTime;
	private static int predGameStartTime;
	private static boolean timerStart = false;
	private static int playersInTeam;
	private static int maxTeams;
	private static int minTeams;
	private static int winMoney;
	private static int killMoney;
	private static String arenaName;
	private static int taskID = 0, taskID2 = 0;
	private static String top1;
	private static String top2;
	private static String top3;
	private static boolean ch = false, ch2 = false;
	private static boolean isImmortal = false;
	private static boolean allowedBlockPlace = false;
	private static ItemStack choose;
	private static ItemStack lobby;
	private static ItemStack teamItem;
	private static Inventory inv;
	private static boolean isCompass = false;
	private static int t1=0, t2=0, t3=0;
	
	private static Config config;
	private static Lang lang;
	private static Database db;
	
	public SkyWars() {
		init();
	}
	
	public static Logger getLogger() {
		return Main.instance.getLogger();
	}
	
	public static void init() {
		initObjects();
		if(!setupEconomy()) {
			getLogger().warning("You should install Vault to support Economy system");
		}
		initDatabase();
		initKits();
	}
	
	public static void initObjects() {
		config = new Config();
		lang = new Lang();
		mysqlEnabled = getConfig().getBoolean("MySQL.enabled");
		url = getConfig().getString("MySQL.url");
		user = getConfig().getString("MySQL.user");
		password = getConfig().getString("MySQL.password");
		bungeeEnabled = getConfig().getBoolean("bungee_enabled");
		arenaName = getConfig().getString("arena.name");
		status = getConfig().getString("arena.status");
		colors = new ArrayList<String>();
		colors.add("§a");
		colors.add("§b");
		colors.add("§3");
		colors.add("§1");
		colors.add("§e");
		colors.add("§9");
		colors.add("§8");
		colors.add("§7");
		colors.add("§6");
		colors.add("§5");
		colors.add("§4");
		colors.add("§2");
		colors.add("§0");
		colors.add("§d");
		colors.add("§f");
		startTime = getConfig().getInt("start_time");
		gameTime = getConfig().getInt("game_time");
		predGameStartTime = gameTime;
		playersInTeam = getConfig().getInt("arena.playersinteam");
		maxTeams = getConfig().getInt("arena.maxteams");
		minTeams = getConfig().getInt("arena.minteams");
		winMoney = getConfig().getInt("win_money");
		killMoney = getConfig().getInt("kill_money");
		
		teamInv = Bukkit.createInventory(null, 9*3, getLang("team_select"));
		for(int index = 0; index < maxTeams; ++index) {
			ArrayList<Player> pl = new ArrayList<Player>();
			teams.add(pl);

			ItemStack temp = new ItemStack(Material.WHITE_STAINED_GLASS);
			ItemMeta meta3 = temp.getItemMeta();
			meta3.setDisplayName("§"+colors.get(index)+getLang("team_inv")+String.valueOf(index+1));
			temp.setItemMeta(meta3);
			
			teamInv.addItem(temp);
		}
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		for(Location loc : locs) {
			addCage(loc);
		}
		choose = new ItemStack(Material.MAGMA_CREAM);
		ItemMeta meta = choose.getItemMeta();
		meta.setDisplayName(getLang("select_kit"));
		choose.setItemMeta(meta);

		lobby = new ItemStack(Material.RED_BED);
		ItemMeta meta2 = lobby.getItemMeta();
		meta2.setDisplayName(getLang("back_to_lobby"));
		lobby.setItemMeta(meta2);

		teamItem = new ItemStack(Material.TRAPPED_CHEST);
		ItemMeta meta3 = teamItem.getItemMeta();
		meta3.setDisplayName(getLang("team_select"));
		teamItem.setItemMeta(meta3);
		
		Location loc = getConfig().getLocation("arena.spec");
		if(loc != null) {
			WorldBorder wb = Bukkit.getWorld("world").getWorldBorder();
			wb.setCenter(loc.getBlockX(), loc.getBlockZ());
			wb.setSize(getConfig().getDouble("border_radius"));
		}
	}
	
	public static void initKits() {
		inv = Bukkit.createInventory(null, 9*4, getLang("kits_shop"));
		ConfigurationSection cs = getConfig().getConfigurationSection("kits");
		int i = 0;
		for(String name : cs.getKeys(false)) {
			ItemStack item = getConfig().getItemStack("kits."+name+".disp");
			String pex = getConfig().getString("kits."+name+".pex");
			Long price = getConfig().getLong("kits."+name+".price");
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("§e"+name);
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			List<String> list = new ArrayList<String>();
			String lor = getLang("everyone");
			if(pex != null && pex != "" && pex.split(".").length >= 2 && !pex.contains("default") && !pex.contains("player")) {
				String[] spl = pex.split(".");
				lor = spl[spl.length-1];
				lor = lor.substring(0, 1).toUpperCase() + lor.substring(1);
				lor = getLang("available_for").replaceAll("%group%", lor);
			}
			list.add(lor);
			list.add(getLang("price_item").replaceAll("%price%", String.valueOf(price)));
			meta.setLore(list);
			item.setItemMeta(meta);
			
			inv.setItem(i, item);
			++i;
		}
	}
	
	public static void initDatabase() {
		if(mysqlEnabled) {
			db = new Database(url, user, password);
		}
		else {
			db = new Database();
		}
	}
	
	private static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Main.instance.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	public static FileConfiguration getConfig() {
		return config.getConfig();
	}
	
	public static Config getConfigFile() {
		return config;
	}
	
	public static String getLang(String path) {
		return getLang().getString(path).replaceAll("&", "§");
	}
	
	public static FileConfiguration getLang() {
		return lang.getConfig();
	}
	
	public static Lang getLangFile() {
		return lang;
	}
	
	public static boolean isMySQLEnabled() {
		return mysqlEnabled;
	}
	
	public static boolean isBungeeEnabled() {
		return bungeeEnabled;
	}
	
	public static boolean isMysqlEnabled() {
		return mysqlEnabled;
	}

	public static void setMysqlEnabled(boolean mysqlEnabled) {
		SkyWars.mysqlEnabled = mysqlEnabled;
	}

	public static int getPlayersCount() {
		return playersCount;
	}

	public static void setPlayersCount(int playersCount) {
		SkyWars.playersCount = playersCount;
	}

	public static Inventory getTeamInv() {
		return teamInv;
	}

	public static void setTeamInv(Inventory teamInv) {
		SkyWars.teamInv = teamInv;
	}

	public static List<String> getColors() {
		return colors;
	}

	public static void setColors(List<String> colors) {
		SkyWars.colors = colors;
	}

	public static HashMap<Player, Integer> getPlayers() {
		return players;
	}

	public static void setPlayers(HashMap<Player, Integer> players) {
		SkyWars.players = players;
	}

	public static HashMap<String, Integer> getKills() {
		return kills;
	}

	public static void setKills(HashMap<String, Integer> kills) {
		SkyWars.kills = kills;
	}

	public static ArrayList<ArrayList<Player>> getTeams() {
		return teams;
	}

	public static void setTeams(ArrayList<ArrayList<Player>> teams) {
		SkyWars.teams = teams;
	}

	public static HashSet<Location> getChests() {
		return chests;
	}

	public static void setChests(HashSet<Location> chests) {
		SkyWars.chests = chests;
	}

	public static HashMap<String, Integer> getIsVotes() {
		return isVotes;
	}

	public static void setIsVotes(HashMap<String, Integer> isVotes) {
		SkyWars.isVotes = isVotes;
	}

	public static HashMap<String, List<ItemStack>> getPlayerKit() {
		return playerKit;
	}

	public static void setPlayerKit(HashMap<String, List<ItemStack>> playerKit) {
		SkyWars.playerKit = playerKit;
	}

	public static HashSet<Location> getTrappedChests() {
		return trappedChests;
	}

	public static void setTrappedChests(HashSet<Location> trappedChests) {
		SkyWars.trappedChests = trappedChests;
	}

	public static Economy getEcon() {
		return econ;
	}

	public static void setEcon(Economy econ) {
		SkyWars.econ = econ;
	}

	public static int getStartTime() {
		return startTime;
	}

	public static void setStartTime(int startTime) {
		SkyWars.startTime = startTime;
	}

	public static int getGameTime() {
		return gameTime;
	}

	public static void setGameTime(int gameTime) {
		SkyWars.gameTime = gameTime;
	}

	public static boolean isTimerStart() {
		return timerStart;
	}

	public static void setTimerStart(boolean timerStart) {
		SkyWars.timerStart = timerStart;
	}

	public static int getPlayersInTeam() {
		return playersInTeam;
	}

	public static void setPlayersInTeam(int playersInTeam) {
		SkyWars.playersInTeam = playersInTeam;
	}

	public static int getMaxTeams() {
		return maxTeams;
	}

	public static void setMaxTeams(int maxTeams) {
		SkyWars.maxTeams = maxTeams;
	}

	public static int getMinTeams() {
		return minTeams;
	}

	public static void setMinTeams(int minTeams) {
		SkyWars.minTeams = minTeams;
	}

	public static int getWinMoney() {
		return winMoney;
	}

	public static void setWinMoney(int winMoney) {
		SkyWars.winMoney = winMoney;
	}

	public static int getKillMoney() {
		return killMoney;
	}

	public static void setKillMoney(int killMoney) {
		SkyWars.killMoney = killMoney;
	}

	public static String getArenaName() {
		return arenaName;
	}

	public static void setArenaName(String arenaName) {
		SkyWars.arenaName = arenaName;
	}

	public static String getTop1() {
		return top1;
	}

	public static void setTop1(String top1) {
		SkyWars.top1 = top1;
	}

	public static String getTop2() {
		return top2;
	}

	public static void setTop2(String top2) {
		SkyWars.top2 = top2;
	}

	public static String getTop3() {
		return top3;
	}

	public static void setTop3(String top3) {
		SkyWars.top3 = top3;
	}

	public static boolean isImmortal() {
		return isImmortal;
	}

	public static void setImmortal(boolean isImmortal) {
		SkyWars.isImmortal = isImmortal;
	}

	public static boolean isAllowedBlockPlace() {
		return allowedBlockPlace;
	}

	public static void setAllowedBlockPlace(boolean allowedBlockPlace) {
		SkyWars.allowedBlockPlace = allowedBlockPlace;
	}

	public static ItemStack getChoose() {
		return choose;
	}

	public static void setChoose(ItemStack choose) {
		SkyWars.choose = choose;
	}

	public static ItemStack getLobby() {
		return lobby;
	}

	public static void setLobby(ItemStack lobby) {
		SkyWars.lobby = lobby;
	}

	public static Inventory getInv() {
		return inv;
	}

	public static void setInv(Inventory inv) {
		SkyWars.inv = inv;
	}

	public static Database getDb() {
		return db;
	}

	public static void setDb(Database db) {
		SkyWars.db = db;
	}

	public static String getStatus() {
		return status;
	}

	public static HashMap<String, Location> getPlayerCage() {
		return playerCage;
	}

	public static void setPlayerCage(HashMap<String, Location> playerCage) {
		SkyWars.playerCage = playerCage;
	}

	public static ItemStack getTeamItem() {
		return teamItem;
	}

	public static void setTeamItem(ItemStack teamItem) {
		SkyWars.teamItem = teamItem;
	}

	public static boolean isCompass() {
		return isCompass;
	}

	public static void setCompass(boolean isCompass) {
		SkyWars.isCompass = isCompass;
	}

	public static void setBungeeEnabled(boolean bungeeEnabled) {
		SkyWars.bungeeEnabled = bungeeEnabled;
	}

	public static void toLobby(Player p ) {
		if(bungeeEnabled) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
	        out.writeUTF("Connect");
	        out.writeUTF("lobby");
	        p.sendPluginMessage(Main.instance, "BungeeCord", out.toByteArray());
		}
	}
	
	public static void addCage(Location loc) {
		ItemStack cage = new ItemStack(Material.WHITE_STAINED_GLASS);
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		for(int xx = x-1; xx <= x+1; ++xx) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				Bukkit.getWorld("world").getBlockAt(xx, y-1, zz).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(xx, y+3, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				Bukkit.getWorld("world").getBlockAt(x-2, yy, zz).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(x+2, yy, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int xx = x-1; xx <= x+1; ++xx) {
				Bukkit.getWorld("world").getBlockAt(xx, yy, z-2).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(xx, yy, z+2).setType(cage.getType());
			}
		}
	}

	public static ItemStack getCage(Player p) {
		if(p.hasPermission("SkyWars.premium")) return new ItemStack(Material.getMaterial(getConfig().getString("glasses.premium")));
		if(p.hasPermission("SkyWars.mvp+")) return new ItemStack(Material.getMaterial(getConfig().getString("glasses.mvp+")));
		if(p.hasPermission("SkyWars.mvp")) return new ItemStack(Material.getMaterial(getConfig().getString("glasses.mvp")));
		if(p.hasPermission("SkyWars.vip+")) return new ItemStack(Material.getMaterial(getConfig().getString("glasses.vip+")));
		if(p.hasPermission("SkyWars.vip")) return new ItemStack(Material.getMaterial(getConfig().getString("glasses.vip")));
		return new ItemStack(Material.getMaterial(getConfig().getString("glasses.default")));
	}
	
	public static void defCage(Location loc) {
		ItemStack cage = new ItemStack(Material.WHITE_STAINED_GLASS);
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		for(int xx = x-1; xx <= x+1; ++xx) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				Bukkit.getWorld("world").getBlockAt(xx, y-1, zz).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(xx, y+3, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				Bukkit.getWorld("world").getBlockAt(x-2, yy, zz).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(x+2, yy, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int xx = x-1; xx <= x+1; ++xx) {
				Bukkit.getWorld("world").getBlockAt(xx, yy, z-2).setType(cage.getType());	
				Bukkit.getWorld("world").getBlockAt(xx, yy, z+2).setType(cage.getType());
			}
		}
	}
	
	public static void delCages() {
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		for(Location loc : locs) {
			ItemStack cage = new ItemStack(Material.AIR);
			int x = loc.getBlockX();
			int y = loc.getBlockY();
			int z = loc.getBlockZ();
			for(int xx = x-1; xx <= x+1; ++xx) {
				for(int zz = z-1; zz <= z+1; ++zz) {
					Bukkit.getWorld("world").getBlockAt(xx, y-1, zz).setType(cage.getType());	
					Bukkit.getWorld("world").getBlockAt(xx, y+3, zz).setType(cage.getType());
				}
			}
			for(int yy = y-1; yy <= y+3; ++yy) {
				for(int zz = z-1; zz <= z+1; ++zz) {
					Bukkit.getWorld("world").getBlockAt(x-2, yy, zz).setType(cage.getType());	
					Bukkit.getWorld("world").getBlockAt(x+2, yy, zz).setType(cage.getType());
				}
			}
			for(int yy = y-1; yy <= y+3; ++yy) {
				for(int xx = x-1; xx <= x+1; ++xx) {
					Bukkit.getWorld("world").getBlockAt(xx, yy, z-2).setType(cage.getType());	
					Bukkit.getWorld("world").getBlockAt(xx, yy, z+2).setType(cage.getType());
				}
			}
		}
	}
	
	public static void setCage(Player p) {
		ItemStack cage = getCage(p);
		Location loc = p.getLocation();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		for(int xx = x-1; xx <= x+1; ++xx) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				p.getWorld().getBlockAt(xx, y-1, zz).setType(cage.getType());	
				p.getWorld().getBlockAt(xx, y+3, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int zz = z-1; zz <= z+1; ++zz) {
				p.getWorld().getBlockAt(x-2, yy, zz).setType(cage.getType());	
				p.getWorld().getBlockAt(x+2, yy, zz).setType(cage.getType());
			}
		}
		for(int yy = y-1; yy <= y+3; ++yy) {
			for(int xx = x-1; xx <= x+1; ++xx) {
				p.getWorld().getBlockAt(xx, yy, z-2).setType(cage.getType());	
				p.getWorld().getBlockAt(xx, yy, z+2).setType(cage.getType());
			}
		}
	}
	
	public static Integer getGroup(Player p) {
		if(p.hasPermission("SkyWars.admin")) return 6;
		if(p.hasPermission("SkyWars.premium")) return 5;
		if(p.hasPermission("SkyWars.mvp+")) return 4;
		if(p.hasPermission("SkyWars.mvp")) return 3;
		if(p.hasPermission("SkyWars.vip+")) return 2;
		if(p.hasPermission("SkyWars.vip")) return 1;
		return 0;
	}
	
	public static void send(Player p, String msg) {
		p.sendMessage(msg.replaceAll("&", "§"));
	}
	
	public static void addKit(Player p) {
		if(playerKit.containsKey(p.getName())) {
			List<ItemStack> items = playerKit.get(p.getName());
			p.updateInventory();
			for(int i = 0; i < 36; ++i) {
				ItemStack it = items.get(i);
				if(it != null) {
					p.getInventory().addItem(it);
				}
			}
		}
	}
	
	public static void addPlayerToTeam(Player p, int index) {
		if(players.containsKey(p)) {
			players.replace(p, index);
		}
		else players.put(p, index);
		ArrayList<Player> pl = teams.get(index);
		pl.add(p);
		teams.set(index, pl);
	}
	
	public static void removePlayerFromTeam(Player p) {
		try {
			int index = players.get(p);
			players.remove(p);
			ArrayList<Player> pl = teams.get(index);
			for(int i = 0; i < pl.size(); ++i) {
				if(pl.get(i).equals(p)) {
					pl.remove(i);
					break;
				}
			}
			teams.set(index, pl);
		} catch(Exception e) {}
	}
	
	public static boolean isFull(Inventory inv) {
		for(ItemStack it : inv.getContents()) {
			if(it == null) {
				return false;
			}
		}
		return true;
	}
	
	public static void clearColor(Player p) {
		String name = p.getDisplayName();
		String[] split = name.split(":");
		String nn = "§f§m"+p.getName();
		p.setDisplayName(split[0]+": "+nn+"§f");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
			public void run() {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
			}
		}, 20);
	}
	
	public static void toSpec(Player p) {
		Location loc = getConfig().getLocation("arena.spec");
		p.teleport(loc);
		p.setGameMode(GameMode.SPECTATOR);
		clearColor(p);
		send(p, getLang("spec"));
	}
	
	public static boolean isEnd() {
		int count = 0;
		for(ArrayList<Player> arr : teams) {
			if(!arr.isEmpty()) count++;
		}
		if(count == 2 && !isCompass) isCompass = true;
		if(count <= 1) return true;
		return false;
	}
	
	public static int getChance(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		String value = meta.getLore().get(0);
		return Integer.valueOf(value.substring(0, value.length()-1));
	}
	
	public static void setColor(Player p, int index) {
		String name = p.getDisplayName();
		String[] split = name.split(":");
		String nn = colors.get(index)+p.getName();
		p.setDisplayName(split[0]+": "+nn+"§f");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
			public void run() {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname "+nn);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname "+nn);
			}
		}, 20);
	}
	
	public static void spawnFireworks(Location location, int amount) {
        Location loc = location;
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();
       
        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());
       
        fw.setFireworkMeta(fwm);
        fw.detonate();
       
        for(int i = 0; i < amount; i++){
            Firework fw2 = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
            fw2.setFireworkMeta(fwm);
        }
    }
	
	public static void endGame() {
		boolean isEnd = isEnd();
		stopTimer();
		status = "edit";
		int count_wins = players.keySet().size();
		if(count_wins != 0) {
			int each_money = (int) (winMoney/count_wins);
			String winners = "";
			for(Player p : players.keySet()) {
				Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
					public void run() {
						spawnFireworks(p.getLocation(), 3);
					}
				}, 0, 20);
				if(!isEnd) {
					send(p, getLang("end_game1"));
				}
				else {
					if(mysqlEnabled) db.addSkyWin(p);
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title "+p.getName()+" title {\"text\":\""+getLang("win")+"\", \"bold\":true, \"color\":\"gold\"}");
				winners += p.getCustomName()+", ";
				send(p, getLang("end_game2").replaceAll("%money%", String.valueOf(each_money)));
				if(econ != null) econ.depositPlayer(p, each_money);
			}
			winners = winners.substring(0, winners.length()-3);
			sendAll(getLang("end_game3"));
			for(String p_name : kills.keySet()) {
				if(kills.get(p_name) > t1) {
					t2 = t1;
					top2 = top1;
					t1 = kills.get(p_name);
					top1 = p_name;
				}
				else if(kills.get(p_name) > t2) {
					t3 = t2;
					top3 = top2;
					t2 = kills.get(p_name);
					top2 = p_name;
				}
				else {
					t3 = kills.get(p_name);
					top3 = p_name;
				}
			}
			sendAll(getLang("kills_top1"));
			if(t1 != 0) sendAll("&6&l1 "+getLang("kills_top2")+" &7("+String.valueOf(t1)+") &f- "+top1);
			if(t2 != 0) sendAll("&6&l2 "+getLang("kills_top2")+" &7("+String.valueOf(t2)+") &f- "+top2);
			if(t3 != 0) sendAll("&6&l3 "+getLang("kills_top2")+" &7("+String.valueOf(t3)+") &f- "+top3);
			
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
				public void run() {
					for(Player p : Bukkit.getOnlinePlayers()) {
						toLobby(p);
					}
				}
			}, 20*8);
			
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
				}
			}, 20*12);
		}
	}
	
	public static void stopLobbyTimer() {
		Bukkit.getServer().getScheduler().cancelTask(taskID2);
	}
	
	public static void startGame() {
		timerStart = false;
		for(Player p: Bukkit.getOnlinePlayers()) {
			p.closeInventory();
		}
		if(isEnd()) endGame();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule fallDamage false");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
			public void run() {
				allowedBlockPlace = true;
			}
		}, 20);
		taskID2 = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
			public void run() {
				if(isImmortal) {
					isImmortal = false;
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule fallDamage true");
					stopLobbyTimer();
				}
				isImmortal = true;
			}
		}, 0, 20*10);
		stopTimer();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskID = scheduler.scheduleSyncRepeatingTask(Main.instance, new Runnable() {
			public void run() {
				if(gameTime == 100 || gameTime == 50 || gameTime == 20 || gameTime <= 10) {
					sendAll(getLang("end_game4").replaceAll("%time%", String.valueOf(gameTime)));
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				if(gameTime == 300) {
					if(!isCompass) isCompass = true;
				}
				if(gameTime == (int) (predGameStartTime/3) || gameTime == (int) (2*predGameStartTime/3)) {
					chests.clear();
					sendAll(getLang("chests_refill"));
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				gameTime--;
				if(gameTime <= 0) {
					sendAll(getLang("end_game5"));
					endGame();
				}
			}
		}, 0, 20*1);
	}
	
	public static void startLobby() {
		timerStart = true;
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskID = scheduler.scheduleSyncRepeatingTask(Main.instance, new Runnable() {
			public void run() {
				if(startTime%5 == 0 || startTime <= 5) {
					sendAll(getLang("start_game1").replaceAll("%time%", String.valueOf(startTime)));
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				startTime--;
				if(startTime <= 0) {
					status = "play";
					delCages();
					sendAll(getLang("start_game2"));
					for(Player pl : players.keySet()) {
						if(mysqlEnabled) db.addSkyGame(pl);
					}
					for(Player pl : Bukkit.getOnlinePlayers()) {
						try {
							pl.getInventory().remove(lobby);;
							pl.getInventory().remove(choose);
							pl.getInventory().remove(teamItem);
							addKit(pl);
						} catch(Exception e) {};
					}
					startGame();
				}
			}
		}, 0, 20*1);
	}
	
	public static void stopTimer() {
		timerStart = false;
		startTime = getConfig().getInt("start_time");
		Bukkit.getScheduler().cancelTask(taskID);
	}
	
	public static void setStatus(String status) {
		SkyWars.status = status;
	}
	
	public static void addToTeam(Player p) {
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		int index = 0;
		for(int i = 0; i < maxTeams; ++i) {
			if(teams.get(i).size() < playersInTeam) {
				index = i;
				break;
			}
		}
		setColor(p, index);
		addPlayerToTeam(p, index);
		Location loc = locs.get(index);
		p.teleport(loc);
		if(playerCage.containsKey(p.getName())) {
			playerCage.replace(p.getName(), loc);
		}
		else playerCage.put(p.getName(), loc);
		ArrayList<Player> f = teams.get(index);
		boolean check = true;
		for(Player pl : f) {
			if(getGroup(pl) > getGroup(p)) {
				check = false;
				break;
			}
		}
		if(check) setCage(p);
	}
	
	public static void changeTeam(Player p, int index) {
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		if(playerCage.containsKey(p.getName())) {
			if(p.hasPermission("API.vip")) {
				defCage(playerCage.get(p.getName()));
			}
		}
		removePlayerFromTeam(p);
		
		setColor(p, index);
		addPlayerToTeam(p, index);
		Location loc = locs.get(index);
		p.teleport(loc);
		if(playerCage.containsKey(p.getName())) {
			playerCage.replace(p.getName(), loc);
		}
		else playerCage.put(p.getName(), loc);
		
		ArrayList<Player> f = teams.get(index);
		boolean check = true;
		for(Player pl : f) {
			if(getGroup(pl) > getGroup(p)) {
				check = false;
				break;
			}
		}
		if(check) setCage(p);
	}
	
	public static void scoreboardUpdate(Player p) {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
        final Scoreboard board = manager.getNewScoreboard();
        final Objective objective = board.registerNewObjective("score", "dummy");        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName("§b§lSkyWars");
        double bal = 0;
        if(econ != null) bal = econ.getBalance(p);
        String val = String.valueOf(bal);
		int rr = maxTeams*playersInTeam;
        if(status.equals("wait")) {
        	Score score = objective.getScore(getLang("scoreboard.balance").replaceAll("%money%", val));
            score.setScore(5);
            score = objective.getScore(getLang("scoreboard.status1"));
            score.setScore(4);
            score = objective.getScore(getLang("scoreboard.players").replaceAll("%current%", String.valueOf(playersCount))
            		.replaceAll("%max%", String.valueOf(rr)));
            score.setScore(3);
            score = objective.getScore(getLang("scoreboard.time").replaceAll("%time%", String.valueOf(gameTime)));
            score.setScore(2);
            score = objective.getScore(getLang("scoreboard.info"));
            score.setScore(1);
            p.setScoreboard(board);
        }
        else if(status.equals("play")) {
        	Score score = objective.getScore(getLang("scoreboard.balance").replaceAll("%money%", val));
            score.setScore(5);
            score = objective.getScore(getLang("scoreboard.status2"));
            score.setScore(4);
            score = objective.getScore(getLang("scoreboard.players").replaceAll("%current%", String.valueOf(playersCount))
            		.replaceAll("%max%", String.valueOf(rr)));
            score.setScore(3);
            score = objective.getScore(getLang("scoreboard.time").replaceAll("%time%", String.valueOf(gameTime)));
            score.setScore(2);
            score = objective.getScore(getLang("scoreboard.info"));
            score.setScore(1);
            p.setScoreboard(board);
        }
        else {
        	Score score = objective.getScore(getLang("scoreboard.balance").replaceAll("%money%", val));
            score.setScore(3);
            score = objective.getScore(getLang("scoreboard.status3"));
            score.setScore(2);
            score = objective.getScore(getLang("scoreboard.info"));
            score.setScore(1);
            p.setScoreboard(board);
        }
	}
	
	public static void sendAll(String msg) {
		for(Player pl : Bukkit.getOnlinePlayers()) {
			send(pl, msg);
		}
	}
	
	public static boolean hasItem(Inventory inv, Material mat, int amount) {
		int sum = 0;
		for(ItemStack it : inv.getContents()) {
			if(it != null) {
				if(it.getType() == mat) {
					sum += it.getAmount();
					if(sum >= amount) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
