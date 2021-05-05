package argento.skywars;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

public class Main extends JavaPlugin implements Listener {
	private static final String url2 = "jdbc:mysql://localhost:3306/minigames?useSSL=false&autoReConnect=true";
	private static final String user = "root";
    private static final String password = "";
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static Connection con2;
    private static Statement stmt2;
    private static ResultSet rs2;
    
	File file = null;
	FileConfiguration config = null;
	private String STATUS = "wait";
	private int players_count = 0;
    List<String> colors;
    HashMap<Player, Integer> players = new HashMap<Player, Integer>();
    HashMap<String, Integer> kills = new HashMap<String, Integer>();
    HashMap<String, Inventory> ch_invs = new HashMap<String, Inventory>();
    HashMap<String, ArrayList<Integer>> player_ch = new HashMap<String, ArrayList<Integer>>();
    HashMap<String, Location> player_cage = new HashMap<String, Location>();
    ArrayList<ArrayList<Player>> teams = new ArrayList<ArrayList<Player>>();
    HashSet<Location> chests = new HashSet<Location>();
    HashMap<String, Integer> isVotes = new HashMap<String, Integer>();
    HashMap<String, List<ItemStack>> player_kit = new HashMap<String, List<ItemStack>>();
    HashSet<Location> trapped_chests = new HashSet<Location>();
    private Economy econ;
	private int START_TIME;
	private int GAME_TIME;
	private int PRED_GAME_TIME;
	private boolean TIMER_START = false;
	int playersinteam;
	int maxteams;
	int minteams;
	int win_money;
	int kill_money;
	private String arena_name;
	private int taskID = 0;
	private int taskID2 = 0;
	String top1;
	String top2;
	String top3;
	int t1=0,t2=0,t3=0;
	private boolean ch = false;
	private boolean ch2 = false;
	private boolean GOD = false;
	private boolean BlockPlace = false;
	ItemStack choose;
	ItemStack lobby;
	ItemStack challenge;
	ItemStack team_item;
	Inventory inv;
	Inventory team_inv;
	boolean is_compass = false;
	
	public void addToDB() {
		String query = "INSERT INTO skywars_arenas (arena, status, online, max_online, type, index1) VALUES ('"+arena_name+"', 'wait', '"+0+"', '"+maxteams*playersinteam+"', '"+playersinteam+"', '"+getMax()+"')";
		try {
			stmt2.executeUpdate(query);
		} catch(SQLException e) {
			updateDB("wait", 0);
		}
	}
	
	public void updateDB(String status, int online) {
		String query = "UPDATE skywars_arenas SET status = '"+status+"', online = '"+online+"' WHERE arena = '"+arena_name+"'";
		try {
			stmt2.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void saveDefaultConfig() {
		if(file == null) {
			file = new File(getDataFolder(), "config.yml");
		}
		if(!file.exists()) {
			saveResource("config.yml", false);
		}
	}
	
	public void toLobby(Player p ) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");
        p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
	}
	
	public void saveConfig() {
		if(config == null || file == null) return;
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	List<Long> toCoins(long l) {
		ArrayList<Long> list = new ArrayList<Long>();
		long med_money = l%1000;
		long ost = (l-med_money)/1000;
		long gold_money = ost%1000;
		long plat_money = (ost-gold_money)/1000;
		list.add(med_money);
		list.add(gold_money);
		list.add(plat_money);
		return list;
	}
	
	public void reloadConfig() {
		if(file == null) {
			file = new File(getDataFolder(), "config.yml");
		}
		config = YamlConfiguration.loadConfiguration(file);
		Reader defConfigStream = new InputStreamReader(getResource("config.yml"));
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
	}
	
	public FileConfiguration getConfig() {
		if(config == null) {
			reloadConfig();
		}
		return config;
	}
	
	int getSkyGames(Player p) {
		String name = p.getName();
		int games = 0;
		String query = "SELECT games FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs2 = stmt2.executeQuery(query);
			rs2.next();
			games = rs2.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return games;
	}
	
	public void addSkyGame(Player p) {
		String name = p.getName();
		int games = getSkyGames(p);
		String query = "UPDATE sw_stats SET games = '"+(games+1)+"' WHERE name = '"+name+"'";
		try {
			stmt2.executeUpdate(query);
		} catch (SQLException e) {}
	}
	
	public void addSkyWin(Player p) {
		String name = p.getName();
		int wins = getSkyWins(p);
		String query = "UPDATE sw_stats SET wins = '"+(wins+1)+"' WHERE name = '"+name+"'";
		try {
			stmt2.executeUpdate(query);
		} catch (SQLException e) {}
	}

	
	public void addSkyKill(Player p) {
		String name = p.getName();
		int kills = getSkyKills(p);
		String query = "UPDATE sw_stats SET kills = '"+(kills+1)+"' WHERE name = '"+name+"'";
		try {
			stmt2.executeUpdate(query);
		} catch (SQLException e) {}
	}
	
	int getSkyKit(Player p, int type) {
		String name = p.getName();
		int count = 0;
		String t = "t"+String.valueOf(type);
		String query = "SELECT "+t+" FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs2 = stmt2.executeQuery(query);
			rs2.next();
			count = rs2.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
	
	public void addSkyKit(Player p, int type) {
		String name = p.getName();
		int count = getSkyKit(p, type);
		String t = "t"+String.valueOf(type);
		String query = "UPDATE sw_stats SET "+t+" = '"+(count+1)+"' WHERE name = '"+name+"'";
		try {
			stmt2.executeUpdate(query);
		} catch (SQLException e) {}
	}
	
	int getSkyWins(Player p) {
		String name = p.getName();
		int wins = 0;
		String query = "SELECT wins FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs2 = stmt2.executeQuery(query);
			rs2.next();
			wins = rs2.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return wins;
	}
	
	int getMax() {
		int index = 0;
		String query = "select MAX(index1) from skywars_arenas;";
		try {
			rs2 = stmt2.executeQuery(query);
			rs2.next();
			index = rs2.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return index;
	}
	
	int getSkyKills(Player p) {
		String name = p.getName();
		int kills = 0;
		String query = "SELECT kills FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs2 = stmt2.executeQuery(query);
			rs2.next();
			kills = rs2.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return kills;
	}
	
	public void connect() {
		getLogger().info("Connecting");
		
		try {
			con2 = DriverManager.getConnection(url2, user, password);;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt2 = con2.createStatement();
		} catch (SQLException e) {}
	}
	
	public void onEnable() {
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		this.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		arena_name = getConfig().getString("arena.name");
		connect();
		file = new File(getDataFolder(), "config.yml");
		if(!file.exists()) {
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
		}
		if (!setupEconomy()) {
			Bukkit.getPluginManager().disablePlugin(this);
			return;
	    }
		reloadConfig();
		STATUS = getConfig().getString("arena.status");
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
		START_TIME = getConfig().getInt("start_time");
		GAME_TIME = getConfig().getInt("game_time");
		PRED_GAME_TIME = GAME_TIME;
		playersinteam = getConfig().getInt("arena.playersinteam");
		maxteams = getConfig().getInt("arena.maxteams");
		minteams = getConfig().getInt("arena.minteams");
		win_money = getConfig().getInt("win_money");
		kill_money = getConfig().getInt("kill_money");
		if(arena_name != null) {
			addToDB();
		}
		
		team_inv = Bukkit.createInventory(null, 9*3, "§aВыбор команды");
		for(int index = 0; index < maxteams; ++index) {
			ArrayList<Player> pl = new ArrayList<Player>();
			teams.add(pl);

			ItemStack temp = new ItemStack(Material.WHITE_STAINED_GLASS);
			ItemMeta meta3 = temp.getItemMeta();
			meta3.setDisplayName("§"+colors.get(index)+"Команда "+String.valueOf(index+1));
			temp.setItemMeta(meta3);
			
			team_inv.addItem(temp);
		}
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		for(Location loc : locs) {
			addCage(loc);
		}
		choose = new ItemStack(Material.MAGMA_CREAM);
		ItemMeta meta = choose.getItemMeta();
		meta.setDisplayName("§cВыбор набора §7(одноразовый)");
		choose.setItemMeta(meta);
		
		challenge = new ItemStack(Material.NETHER_STAR);
		meta = challenge.getItemMeta();
		meta.setDisplayName("§cЧеленджы");
		challenge.setItemMeta(meta);

		lobby = new ItemStack(Material.RED_BED);
		ItemMeta meta2 = lobby.getItemMeta();
		meta2.setDisplayName("§cНазад в лобби §7(ПКМ)");
		lobby.setItemMeta(meta2);

		team_item = new ItemStack(Material.TRAPPED_CHEST);
		ItemMeta meta3 = team_item.getItemMeta();
		meta3.setDisplayName("§cВыбрать команду");
		team_item.setItemMeta(meta3);
		
		Location loc = getConfig().getLocation("arena.spec");
		WorldBorder wb = Bukkit.getWorld("world").getWorldBorder();
		wb.setCenter(loc.getBlockX(), loc.getBlockZ());
		wb.setSize(400);
		
		initKits();
		
		Bukkit.getWorld("world").setGameRuleValue("sendCommandFeedback", "false");
	}
	
	public void disablePlugin() {
		Plugin ocm = Bukkit.getPluginManager().getPlugin("OldCombatMechanics");
		Bukkit.getPluginManager().disablePlugin(ocm);
	}
	
	Inventory getChallenges() {
		Inventory chel = Bukkit.createInventory(null, 9*4, "§cЧеленджы");
		
		ItemStack o = new ItemStack(Material.CROSSBOW);
		ItemMeta meta = o.getItemMeta();
		meta.setDisplayName("§eБез лука и арбалета");
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы не сможете использовать луки и арбалеты");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(10, o);
		
		o = new ItemStack(Material.LAVA_BUCKET);
		meta = o.getItemMeta();
		meta.setDisplayName("§eБез лавы и воды");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы не сможете использовать вёдра воды и лавы");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(12, o);
		
		o = new ItemStack(Material.SHIELD);
		meta = o.getItemMeta();
		meta.setDisplayName("§eБез щита");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы не сможете использовать щиты");
		lore.add("§f(работает только на версии PvP §c1.16)");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(14, o);
		
		o = new ItemStack(Material.OAK_PLANKS);
		meta = o.getItemMeta();
		meta.setDisplayName("§eБез блоков");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы не сможете ставить блоки");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(16, o);
		
		o = new ItemStack(Material.GOLDEN_APPLE);
		meta = o.getItemMeta();
		meta.setDisplayName("§eБез регенерации");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВаше здоровье не регенерируется");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(19, o);
		
		o = new ItemStack(Material.NETHERITE_AXE);
		meta = o.getItemMeta();
		meta.setDisplayName("§eДвойной урон");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВам будет наносится двойной урон");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(21, o);
		
		o = new ItemStack(Material.WOODEN_SWORD);
		meta = o.getItemMeta();
		meta.setDisplayName("§eСлабак");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы будете наносить нулевой урон");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(23, o);
		
		o = new ItemStack(Material.DIAMOND_CHESTPLATE);
		meta = o.getItemMeta();
		meta.setDisplayName("§eБез брони");
		lore = new ArrayList<String>();
		lore.add("§7Нажмите, чтобы активировать челендж");
		lore.add("§7При победе вы получите на §c10% §7монет больше");
		lore.add("§fВы не сможете надеть броню");
		meta.setLore(lore);
		o.setItemMeta(meta);
		chel.setItem(25, o);
		
		return chel;
	}
	
	@EventHandler
	public void onPlayerRegainHealth(EntityRegainHealthEvent e) {
		if(e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			if(STATUS.equals("play")) {
				if(player_ch.get(p.getName()).contains(19) && e.getRegainReason() == RegainReason.SATIATED) {
					e.setCancelled(true);
				}
			}
		}
	}
	
	public void initKits() {
		inv = Bukkit.createInventory(null, 9*4, "§3Покупка одноразовых наборов");
		ConfigurationSection cs = getConfig().getConfigurationSection("kits");
		int i = 0;
		for(String name : cs.getKeys(false)) {
			ItemStack item = getConfig().getItemStack("kits."+name+".disp");
			String pex = getConfig().getString("kits."+name+".pex");
			Long price = getConfig().getLong("kits."+name+".price");
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("§e"+name+" §c(одноразовый)");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			List<String> list = new ArrayList<String>();
			String lor = "§7Доступно всем";
			if(pex.equals("API.premium")) lor = "§7Доступно с [§9Premium§7] и выше";
			else if(pex.equals("API.mvp+")) lor = "§7Доступно с [§bMVP+§7] и выше";
			else if(pex.equals("API.mvp")) lor = "§7Доступно с [§bMVP§7] и выше";
			else if(pex.equals("API.vip+")) lor = "§7Доступно с [§6VIP+§7] и выше";
			else if(pex.equals("API.vip")) lor = "§7Доступно с [§6VIP§7] и выше";
			list.add(lor);
			list.add("§7Цена: §c"+String.valueOf(price)+" медных §7монет");
			meta.setLore(list);
			item.setItemMeta(meta);
			
			inv.setItem(i, item);
			++i;
		}
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		Inventory in = e.getInventory();
		if(STATUS.equals("play")) {
			ItemStack item = e.getCurrentItem();
			if(player_ch.get(p.getName()).contains(14) && item.getType() == Material.SHIELD) {
				send(p, "&cВы не можете использовать щиты из-за челенджа!");
				e.setCancelled(true);
			}
			if(player_ch.get(p.getName()).contains(25) && (item.getType().toString().contains("CHESTPLATE") || item.getType().toString().contains("LEGGINGS") || item.getType().toString().contains("BOOTS") || item.getType().toString().contains("HELMET"))) {
				e.setCancelled(true);
			}
		}
		else {
			if(ch_invs.containsKey(p.getName()) && in.equals(ch_invs.get(p.getName()))) {
				ItemStack item = e.getCurrentItem();
				if(item != null) {
					int raw = e.getRawSlot();
					if(raw <= 25) {
						if(player_ch.containsKey(p.getName())) {
							ArrayList<Integer> arr = player_ch.get(p.getName());
							if(!arr.contains(raw)) {
								arr.add(raw);
								player_ch.replace(p.getName(), arr);
								Inventory inv = ch_invs.get(p.getName());
								
								ItemStack st = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
								ItemMeta meta = st.getItemMeta();
								meta.setDisplayName("§aУже активен!");
								st.setItemMeta(meta);
								
								inv.setItem(raw, st);
								ch_invs.replace(p.getName(), inv);
								p.updateInventory();
								p.closeInventory();
							}
							else {
								p.sendMessage("§сЧеллендж уже выбран");
							}
						}
						else {
							ArrayList<Integer> arr = new ArrayList<Integer>();
							arr.add(raw);
							player_ch.put(p.getName(), arr);
							Inventory inv = ch_invs.get(p.getName());
							
							ItemStack st = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
							ItemMeta meta = st.getItemMeta();
							meta.setDisplayName("§aУже активен!");
							st.setItemMeta(meta);
							
							inv.setItem(raw, st);
							ch_invs.replace(p.getName(), inv);
							p.updateInventory();
							p.closeInventory();
						}
					}
					
					e.setCancelled(true);
				}
			}
			else if(in.equals(team_inv)) {
				ItemStack item = e.getCurrentItem();
				if(item != null && item.getType() == Material.WHITE_STAINED_GLASS) {
					int index = e.getRawSlot();
					ArrayList<Player> pl = teams.get(index);
					if(!pl.contains(p)) {
						if(pl.size() < playersinteam) {
							changeTeam(p, index);
							send(p, "&aВыбрана команда &c"+String.valueOf(index+1));
						}
						else {
							send(p, "&cКоманда заполнена");
						}
					}
					else send(p, "&cВы уже в этой команде");
				}
				
				e.setCancelled(true);
			}
			else if(in.equals(inv)) {
				ItemStack item = e.getCurrentItem();
				if(item != null) {
					ItemMeta meta = item.getItemMeta();
					String name = meta.getDisplayName().split(" ")[0].substring(2);
					if(getConfig().contains("kits."+name)) {
						String pex = getConfig().getString("kits."+name+".pex");
						Long price = getConfig().getLong("kits."+name+".price");
						if(!p.hasPermission(pex)) {
							send(p, "Нет прав");
						}
						else if(econ.getBalance(p) < price) {
							send(p, "Не хватает монет");
						}
						else {
							List<ItemStack> items = (List<ItemStack>) getConfig().get("kits."+name+".extra");
							send(p, "§7Вы выбрали набор §e"+name);
							player_kit.put(p.getName(), items);
							if(name.contains("Боевой")) {
								addSkyKit(p, 1);
							}
							else if(name.contains("Лучник")) {
								addSkyKit(p, 2);
							}
							else if(name.contains("Танк")) {
								addSkyKit(p, 3);
							}
							else if(name.contains("Строитель")) {
								addSkyKit(p, 4);
							}
							else if(name.contains("Подрывник")) {
								addSkyKit(p, 5);
							}
							else if(name.contains("Механик")) {
								addSkyKit(p, 6);
							}
							else if(name.contains("Профи")) {
								addSkyKit(p, 7);
							}
							else if(name.contains("Ассасин")) {
								addSkyKit(p, 8);
							}
							else if(name.contains("Зельевар")) {
								addSkyKit(p, 9);
							}
							else if(name.contains("Музыкант")) {
								addSkyKit(p, 10);
							}
							else if(name.contains("Арбалетчик")) {
								addSkyKit(p, 11);
							}
							else if(name.contains("Священник")) {
								addSkyKit(p, 12);
							}
							else {
								addSkyKit(p, 13);
							}
							econ.withdrawPlayer(p, price);
						}
						p.closeInventory();
					}
				}
				e.setCancelled(true);
			}
		}
	}
	
	public void addKit(Player p) {
		if(player_kit.containsKey(p.getName())) {
			List<ItemStack> items = player_kit.get(p.getName());
			p.updateInventory();
			if(!player_ch.get(p.getName()).contains(25)) {
				if(items.get(39) != null) p.getInventory().setHelmet(items.get(39));
				if(items.get(38) != null) p.getInventory().setChestplate(items.get(38));
				if(items.get(37) != null) p.getInventory().setLeggings(items.get(37));
				if(items.get(36) != null) p.getInventory().setBoots(items.get(36));
			}
			for(int i = 0; i < 36; ++i) {
				ItemStack it = items.get(i);
				if(it != null) {
					if(it.getType() == Material.SHIELD) {
						if(!player_ch.get(p.getName()).contains(25)) {
							p.getInventory().addItem(it);
						}
					}
					else p.getInventory().addItem(it);
				}
			}
		}
	}
	
	@EventHandler
	public void interact(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(STATUS.equals("wait")) {
			if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if(p.getItemInHand().isSimilar(choose)) {
					if(player_kit.containsKey(p.getName())) {
						if(e.getHand() == EquipmentSlot.HAND) send(p, "&cВы уже выбрали набор");
						e.setCancelled(true);
					}
					else p.openInventory(inv);
				}
				else if(p.getItemInHand().isSimilar(lobby)) {
					toLobby(p);
				}
				else if(p.getItemInHand().isSimilar(challenge)) {
					p.openInventory(ch_invs.get(p.getName()));
				}
				else if(p.getItemInHand().isSimilar(team_item)) {
					p.openInventory(team_inv);
				}
			}
			e.setCancelled(true);
		}
		else if(STATUS.equals("play")) {
			if(e.getItem() != null) {
				if(e.getItem().getType() == Material.BOW || e.getItem().getType() == Material.CROSSBOW) {
					if(player_ch.get(p.getName()).contains(10)) {
						send(p, "&cВы не можете использовать луки и арбалеты из-за челенджа!");
						e.setCancelled(true);
					}
				}
				else if(e.getItem().getType() == Material.LAVA_BUCKET || e.getItem().getType() == Material.WATER_BUCKET) {
					if(player_ch.get(p.getName()).contains(12)) {
						send(p, "&cВы не можете использовать вёдра воды и лавы из-за челенджа!");
						e.setCancelled(true);
					}
				}
			}
		}
	}
	
	public boolean isFull(Inventory inv) {
		for(ItemStack it : inv.getContents()) {
			if(it == null) {
				return false;
			}
		}
		return true;
	}
	
	@EventHandler
	public void bloccck(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		if(STATUS.equals("play")) {
			ItemStack item = e.getOffHandItem();
			if(item.getType() == Material.SHIELD && player_ch.get(p.getName()).contains(14)) {
				e.setCancelled(true);
			}
			if(player_ch.get(p.getName()).contains(25) && (item.getType().toString().contains("CHESTPLATE") || item.getType().toString().contains("LEGGINGS") || item.getType().toString().contains("BOOTS") || item.getType().toString().contains("HELMET"))) {
				e.setCancelled(true);
			}
		}
		if(STATUS.equals("wait")) e.setCancelled(true);
	}
	
	public void addPlayerToTeam(Player p, int index) {
		if(players.containsKey(p)) {
			players.replace(p, index);
		}
		else players.put(p, index);
		ArrayList<Player> pl = teams.get(index);
		pl.add(p);
		teams.set(index, pl);
	}
	
	public void removePlayerFromTeam(Player p) {
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
	}
	
	public void send(Player p, String msg) {
		p.sendMessage(msg.replaceAll("&", "§"));
	}
	
	ItemStack getCage(Player p) {
		if(p.hasPermission("API.premium")) return new ItemStack(Material.BLACK_STAINED_GLASS);
		if(p.hasPermission("API.mvp+")) return new ItemStack(Material.RED_STAINED_GLASS);
		if(p.hasPermission("API.mvp")) return new ItemStack(Material.GREEN_STAINED_GLASS);
		if(p.hasPermission("API.vip+")) return new ItemStack(Material.YELLOW_STAINED_GLASS);
		if(p.hasPermission("API.vip")) return new ItemStack(Material.BLUE_STAINED_GLASS);
		return new ItemStack(Material.WHITE_STAINED_GLASS);
	}
	
	Integer getGroup(Player p) {
		if(p.hasPermission("API.glav")) return 10;
		if(p.hasPermission("API.youtube")) return 9;
		if(p.hasPermission("API.admin")) return 8;
		if(p.hasPermission("API.moder")) return 7;
		if(p.hasPermission("API.helper")) return 6;
		if(p.hasPermission("API.premium")) return 5;
		if(p.hasPermission("API.mvp+")) return 4;
		if(p.hasPermission("API.mvp")) return 3;
		if(p.hasPermission("API.vip+")) return 2;
		if(p.hasPermission("API.vip")) return 1;
		return 0;
	}
	
	public void defCage(Location loc) {
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
	
	public void delCages() {
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
	
	public void setCage(Player p) {
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
	
	public void addCage(Location loc) {
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
	
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if(STATUS.equals("wait")) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.CHEST)) e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		if(STATUS.equals("wait")) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.CHEST)) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.TRAPPED_CHEST)) {
			trapped_chests.add(e.getBlock().getLocation());
		}
		if(!BlockPlace) e.setCancelled(true);
		if(STATUS.equals("play")) {
			if(player_ch.get(p.getName()).contains(16)) {
				send(p, "&cВы не можете ставить блоки из-за челенджа!");
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if(STATUS.equals("wait")) e.setCancelled(true);
	}
	
	@EventHandler
	public void damage(EntityDamageEvent e) {
		if(!STATUS.equals("play") && e.getEntity() instanceof Player) {
			e.setCancelled(true);
		}
		if(e.getEntity() instanceof Player && STATUS.equals("play")) {
			Player p = (Player) e.getEntity();
			if(p.getLastDamageCause() != null) {
				DamageCause deathCause = p.getLastDamageCause().getCause();
				if(deathCause != null) {
					if(deathCause == DamageCause.VOID) {
						p.setHealth(1);
					}
				}
			}
		}
	}
	
	public void reward(Player p, long money) {
		ArrayList<Long> list = (ArrayList<Long>) toCoins(money);
		p.sendMessage("§7Получено §a"+String.valueOf(list.get(2))+" §fплатиновых, §a"+String.valueOf(list.get(1))+" §6золотых§7 и §a"+String.valueOf(list.get(0))+" §cмедных §7монет");
	}
	
	public void loose(Player p, long money) {
		ArrayList<Long> list = (ArrayList<Long>) toCoins(money);
		p.sendMessage("§7Потеряно §a"+String.valueOf(list.get(2))+" §fплатиновых, §a"+String.valueOf(list.get(1))+" §6золотых§7 и §a"+String.valueOf(list.get(0))+" §cмедных §7монет");
	}
	
	public void toSpec(Player p) {
		Location loc = getConfig().getLocation("arena.spec");
		p.teleport(loc);
		p.setGameMode(GameMode.SPECTATOR);
		clearColor(p);
		send(p, "&6Вы зритель!");
	}
	
	@EventHandler
	public void death(EntityDeathEvent e) {
		Player p = (Player) e.getEntity();
		if(players.containsKey(p)) {
			Player killer = p.getKiller();
			long money = (long) econ.getBalance(p);
			money = (long) (money*kill_money)/100;
			int w = 0;
			if(killer != null && killer instanceof Player) {
				if(!killer.getName().equals(p.getName())) {
					econ.depositPlayer(killer, money);
					send(killer, "&7Вы убили игрока!");
					addSkyKill(killer);
					reward(killer, money);
					if(kills.containsKey(killer.getName())) {
						w = kills.get(killer.getName())+1;
						kills.replace(killer.getName(), w);
					}
					else {
						w = 1;
						kills.put(killer.getName(), 1);
					}
					for(Player pls : players.keySet()) {
						send(pls, p.getDisplayName()+" &7был убит "+killer.getDisplayName());
					}
				}
			}
			else {
				for(Player pls : players.keySet()) {
					send(pls, p.getDisplayName()+" &7погиб");
				}
			}
			econ.withdrawPlayer(p, money);
			loose(p, money);
			send(p, "&cВы погибли и потеряли 2% своих монет");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
			players_count--;
			updateDB("play", players_count);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> p.spigot().respawn(), 1L);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> toSpec(p), 1L);
			try {
				ArrayList<Player> pl = teams.get(players.get(p));
				for(Player pp : pl) {
					if(!p.getName().equals(pp.getName())) send(pp, "&cВаш союзник "+p.getDisplayName()+" &cпогиб");
				}
			} catch(Exception ee) {};
			removePlayerFromTeam(p);
			if(isEnd()) endGame();
		}
	}
	
	public boolean isEnd() {
		int count = 0;
		for(ArrayList<Player> arr : teams) {
			if(!arr.isEmpty()) count++;
		}
		if(count == 2 && !is_compass) is_compass = true;
		if(count <= 1) return true;
		return false;
	}
	
	int getChance(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		String value = meta.getLore().get(0);
		return Integer.valueOf(value.substring(0, value.length()-1));
	}
	
	@EventHandler
	public void onPlayerCraft(CraftItemEvent e) {
		if(e.getRecipe().getResult().getType().equals(Material.CHEST)) e.setCancelled(true);
	}
	
	@EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof Chest) {
            Location loc = e.getInventory().getLocation();
            if(!trapped_chests.contains(loc)) {
	            if(!chests.contains(loc)) {
	            	e.getInventory().clear();
	            	List<ItemStack> list = (List<ItemStack>) getConfig().get("chests");
	            	for(int i = 0; i < list.size(); ++i) {
	            		ItemStack item = list.get(i);
	            		int chance = getChance(item);
	            		int rand = (int)(Math.random() * (100));
	            		if(rand <= chance) {
	            			ItemStack item2 = item.clone();
	                		int index = (int)(Math.random() * (26));
	                		ItemMeta meta = item2.getItemMeta();
	                		meta.setLore(null);
	                		item2.setItemMeta(meta);
	            			e.getInventory().setItem(index, item2);
	            		}
	            	}
	            	chests.add(loc);
	            }
            }
        }
    }
	
	@EventHandler
	public void damage(EntityDamageByEntityEvent e) {
		if(STATUS.equals("wait")) e.setCancelled(true);
		if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
			if(STATUS.equals("play")) {
				Player damager = (Player) e.getDamager();
				Player p = (Player) e.getEntity();
				if(players.get(p) == players.get(damager)) e.setCancelled(true);
				if(player_ch.get(p.getName()).contains(21)) {
					e.setDamage(2*e.getDamage());
				}
				if(player_ch.get(damager.getName()).contains(23)) {
					e.setDamage(0.0);
				}
			}
		}
		else if(e.getEntity() instanceof Player && e.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
			Player p = (Player) e.getEntity();
			if(e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				Player damager = (Player) a.getShooter();
				Integer team = players.get(p);
				Integer team2 = players.get(damager);
				if(team == team2) e.setCancelled(true);
			}
			else if(e.getDamager() instanceof Trident) {
				Trident a = (Trident) e.getDamager();
				Player damager = (Player) a.getShooter();
				Integer team = players.get(p);
				Integer team2 = players.get(damager);
				if(team == team2) e.setCancelled(true);
			}
		}
	}
	
	public void setColor(Player p, int index) {
		String name = p.getDisplayName();
		String[] split = name.split(":");
		String nn = colors.get(index)+p.getName();
		p.setDisplayName(split[0]+": "+nn+"§f");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname "+nn);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname "+nn);
			}
		}, 20);
	}
	
	public void clearColor(Player p) {
		String name = p.getDisplayName();
		String[] split = name.split(":");
		String nn = "§f§m"+p.getName();
		p.setDisplayName(split[0]+": "+nn+"§f");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
			}
		}, 20);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				scoreboardUpdate(p);
			}
		}, 0, 19*1);
		p.getInventory().clear();
		if(STATUS.equals("wait")) {
			if(!ch_invs.containsKey(p.getName())) {
				ch_invs.put(p.getName(), getChallenges());
			}
			if(!player_ch.containsKey(p.getName())) {
				ArrayList<Integer> arr = new ArrayList<Integer>();
				player_ch.put(p.getName(), arr);
			}
			int min_players = playersinteam*minteams;
			addToTeam(p);
			players_count++;
			updateDB("wait", players_count);
			p.getInventory().setItem(0, team_item);
			p.getInventory().setItem(1, choose);
			p.getInventory().setItem(4, challenge);
			p.getInventory().setItem(8, lobby);
			if(player_ch.containsKey(p.getName()) && !player_ch.get(p.getName()).isEmpty()) {
				ArrayList<Integer> list = new ArrayList<Integer>();
				player_ch.replace(p.getName(), list);
				if(ch_invs.containsKey(p.getName()) && ch_invs.get(p.getName()) != null) ch_invs.replace(p.getName(), getChallenges());
			}
			sendAll(p.getDisplayName()+" &eприсоединился к игре &c"+String.valueOf(players_count)+"&a/&c"+String.valueOf(maxteams*playersinteam));
			if(!TIMER_START && min_players <= players_count) {
				startGame();
			}
		}
		else if(STATUS.equals("play")) {
			toSpec(p);
			send(p, "&7Идёт игра...");
			p.getInventory().setItem(8, lobby);
			clearColor(p);
		}
		else {
			if(!p.hasPermission("API.glav")) toLobby(p);
		}
	}
	
	public void onDisable() {
		updateDB("reload", 0);
		try {
			if(con2 != null && !con2.isClosed()) {
				try {
					con2.close();
					stmt2.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void spawnFireworks(Location location, int amount){
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
	
	public void endGame() {
		boolean isEnd = isEnd();
		stopTimer();
		STATUS = "edit";
		updateDB("reload", 0);
		int count_wins = players.keySet().size();
		if(count_wins != 0) {
			int each_money = (int) (win_money/count_wins);
			String winners = "";
			for(Player p : players.keySet()) {
				Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
					public void run() {
						spawnFireworks(p.getLocation(), 3);
					}
				}, 0, 20);
				if(!isEnd) {
					send(p, "&7Так как остались выжившие команды, ваша статистика побед не изменилась");
				}
				else {
					addSkyWin(p);
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title "+p.getName()+" title {\"text\":\"ПОБЕДА!\", \"bold\":true, \"color\":\"gold\"}");
				winners += p.getDisplayName()+", ";
				int chel = 0;
				if(player_ch.containsKey(p.getName())) {
					chel = player_ch.get(p.getName()).size();
					if(chel != 0) send(p, "&7Выполнено &c"+String.valueOf(chel) + " &7челендж(-а,-ов). Вы получите на &c"+String.valueOf(chel*10)+" &7процентов больше монет!");
				}
				send(p, "&aВы победили! Ваша награда: &c"+String.valueOf(each_money + (int)(chel*10*each_money)/100)+" медных &aмонет");
				econ.depositPlayer(p, each_money + (int)(chel*10*each_money)/100);
			}
			winners = winners.substring(0, winners.length()-3);
			sendAll("&6&lПобедители: "+winners);
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
			sendAll("&b&lТоп по убийствам&f:");
			if(t1 != 0) sendAll("&6&l1 место &7("+String.valueOf(t1)+") &f- "+top1);
			if(t2 != 0) sendAll("&6&l2 место &7("+String.valueOf(t2)+") &f- "+top2);
			if(t3 != 0) sendAll("&6&l3 место &7("+String.valueOf(t3)+") &f- "+top3);
			
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					if(ch2) {
						for(Player p : Bukkit.getOnlinePlayers()) {
							toLobby(p);
						}
					}
					ch2 = true;
				}
			}, 0, 20*8);
			
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					if(ch) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
					}
					ch = true;
				}
			}, 0, 20*12);
		}
	}
	
	public void stopTimer2() {
		Bukkit.getServer().getScheduler().cancelTask(taskID2);
	}
	
	public void startGame2() {
		TIMER_START = false;
		for(Player p: Bukkit.getOnlinePlayers()) {
			p.closeInventory();
		}
		if(isEnd()) endGame();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule fallDamage false");
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				BlockPlace = true;
			}
		}, 20);
		taskID2 = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if(GOD) {
					GOD = false;
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule fallDamage true");
					stopTimer2();
				}
				GOD = true;
			}
		}, 0, 20*10);
		stopTimer();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskID = scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if(GAME_TIME == 100 || GAME_TIME == 50 || GAME_TIME == 20 || GAME_TIME <= 10) {
					sendAll("&7Игра закончится через &c"+String.valueOf(GAME_TIME)+" &7секунд");
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				if(GAME_TIME == 300) {
					if(!is_compass) is_compass = true;
				}
				if(GAME_TIME == (int) (PRED_GAME_TIME/3) || GAME_TIME == (int) (2*PRED_GAME_TIME/3)) {
					chests.clear();
					sendAll("&eСундуки перезаполнены!");
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				GAME_TIME--;
				if(GAME_TIME <= 0) {
					sendAll("&cИгра закончилась! Призовые деньги были распределены выжившим!");
					endGame();
				}
			}
		}, 0, 20*1);
	}
	
	public void startGame() {
		TIMER_START = true;
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskID = scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if(START_TIME%5 == 0 || START_TIME <= 5) {
					sendAll("&7Игра начнётся через &c"+String.valueOf(START_TIME)+" &7секунд");
					for(Player pl : Bukkit.getOnlinePlayers()) {
						pl.playSound(pl.getLocation(), Sound.UI_LOOM_SELECT_PATTERN, 1, 1);
					}
				}
				START_TIME--;
				if(START_TIME <= 0) {
					STATUS = "play";
					updateDB("play", players_count);
					delCages();
					sendAll("&aИгра началась! Удачи!");
					for(Player pl : players.keySet()) {
						addSkyGame(pl);
					}
					for(Player pl : Bukkit.getOnlinePlayers()) {
						try {
							pl.getInventory().remove(lobby);
							pl.getInventory().remove(challenge);
							pl.getInventory().remove(choose);
							pl.getInventory().remove(team_item);
							addKit(pl);
						} catch(Exception e) {};
					}
					startGame2();
				}
			}
		}, 0, 20*1);
	}
	
	public void stopTimer() {
		TIMER_START = false;
		START_TIME = getConfig().getInt("start_time");
		Bukkit.getScheduler().cancelTask(taskID);
	}
	
	public void addToTeam(Player p) {
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		int index = 0;
		for(int i = 0; i < maxteams; ++i) {
			if(teams.get(i).size() < playersinteam) {
				index = i;
				break;
			}
		}
		setColor(p, index);
		addPlayerToTeam(p, index);
		Location loc = locs.get(index);
		p.teleport(loc);
		if(player_cage.containsKey(p.getName())) {
			player_cage.replace(p.getName(), loc);
		}
		else player_cage.put(p.getName(), loc);
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
	
	public void changeTeam(Player p, int index) {
		List<Location> locs = (List<Location>) getConfig().get("arena.spawns");
		if(player_cage.containsKey(p.getName())) {
			if(p.hasPermission("API.vip")) {
				defCage(player_cage.get(p.getName()));
			}
		}
		removePlayerFromTeam(p);
		
		setColor(p, index);
		addPlayerToTeam(p, index);
		Location loc = locs.get(index);
		p.teleport(loc);
		if(player_cage.containsKey(p.getName())) {
			player_cage.replace(p.getName(), loc);
		}
		else player_cage.put(p.getName(), loc);
		
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
	
	@EventHandler
	public void move(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if(STATUS.equals("play")) {
			if(is_compass) {
				Location loc = p.getLocation();
				int min_dis = 100000000;
				for(ArrayList<Player> list : teams) {
					if(list.contains(p)) {
						continue;
					}
					for(Player pl : list) {
						if(!pl.getName().equals(p.getName())) {
							Location lc = pl.getLocation();
							int dis = (int) Math.sqrt(Math.pow(loc.getBlockX()-lc.getBlockX(), 2) + Math.pow(loc.getBlockY()-lc.getBlockY(), 2) + Math.pow(loc.getBlockZ()-lc.getBlockZ(), 2));
							if(dis < min_dis) {
								min_dis = dis;
							}
						}
					}
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title "+p.getName()+" actionbar \"§eДо ближайшего игрока §c"+String.valueOf(min_dis)+" §eблоков \"");
			}
		}
	}
	
	public void scoreboardUpdate(Player p) {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
        final Scoreboard board = manager.getNewScoreboard();
        final Objective objective = board.registerNewObjective("score", "dummy");        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName("§b§lSkyWars");
        ArrayList<Long> list = (ArrayList<Long>) toCoins((long)econ.getBalance(p));
        long med = list.get(0);
        long gold = list.get(1);
        long plat = list.get(2);
		int rr = maxteams*playersinteam;
        if(STATUS.equals("wait")) {
        	Score score = objective.getScore("§7Баланс: §a"+plat+"§fп §a"+gold+"§6з §a"+med+"§cм");
            score.setScore(5);
            Score score2 = objective.getScore("§7Состояние: §cОжидание...");
            score2.setScore(4);
            Score score3 = objective.getScore("§7Игроков: §c"+String.valueOf(players_count)+"§e/§c"+String.valueOf(rr));
            score3.setScore(3);
            Score score8 = objective.getScore("§7До начала игры: §c"+String.valueOf(START_TIME));
            score8.setScore(2);
            Score score4 = objective.getScore("§7Донат: §cVaultCommunity.ru");
            score4.setScore(1);
            p.setScoreboard(board);
        }
        else if(STATUS.equals("play")) {
        	Score score = objective.getScore("§7Баланс: §a"+plat+"§fп §a"+gold+"§6з §a"+med+"§cм");
            score.setScore(5);
            Score score2 = objective.getScore("§7Состояние: §aИдёт игра");
            score2.setScore(4);
            Score score3 = objective.getScore("§7Игроков: §c"+String.valueOf(players_count)+"§e/§c"+String.valueOf(rr));
            score3.setScore(3);
            Score score8 = objective.getScore("§7До окончания: §c"+String.valueOf(GAME_TIME));
            score8.setScore(2);
            Score score4 = objective.getScore("§7Донат: §cVaultCommunity.ru");
            score4.setScore(1);
            p.setScoreboard(board);
        }
        else {
        	Score score = objective.getScore("§7Баланс: §a"+plat+"§fп §a"+gold+"§6з §a"+med+"§cм");
            score.setScore(3);
            Score score2 = objective.getScore("§7Состояние: §fРед.");
            score2.setScore(2);
            Score score4 = objective.getScore("§7Донат: §cVaultCommunity.ru");
            score4.setScore(1);
            p.setScoreboard(board);
        }
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
		if(STATUS.equals("wait")) {
			players_count--;
			updateDB("wait", players_count);
			sendAll(p.getDisplayName()+" &eпокинул игру &c"+String.valueOf(players_count)+"&a/&c"+String.valueOf(maxteams*playersinteam));
			if(player_cage.containsKey(p.getName()) && p.hasPermission("API.vip")) {
				defCage(player_cage.get(p.getName()));
			}
			removePlayerFromTeam(p);
			if(TIMER_START && players_count < minteams*playersinteam) {
				stopTimer();
			}
		}
		else if(STATUS.equals("play") && players.containsKey(p)) {
			players_count--;
			updateDB("play", players_count);
			sendAll(p.getDisplayName()+" &7покинул игру");
			ArrayList<Player> pl = teams.get(players.get(p));
			for(Player pp : pl) {
				if(!p.getName().equals(pp.getName())) send(pp, "&cВаш союзник "+p.getDisplayName()+" &cпокинул игру");
			}
			removePlayerFromTeam(p);
			if(isEnd()) {
				endGame();
			}
		}
	}
	
	public void sendAll(String msg) {
		for(Player pl : Bukkit.getOnlinePlayers()) {
			send(pl, msg);
		}
	}
	
	public boolean hasItem(Inventory inv, Material mat, int amount) {
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
	
	@EventHandler
	public void pickup(PlayerPickupItemEvent e) {
		Player p = e.getPlayer();
		ItemStack item = e.getItem().getItemStack();
		if(item.getType() == Material.CHEST) {
			e.setCancelled(true);
		}
		else if(item.getType() == Material.COMPASS && !hasItem(p.getInventory(), Material.COMPASS, 1)) {
			e.setCancelled(true);
		}
		if(STATUS.equals("play")) {
			if(item.getType() == Material.SHIELD && player_ch.get(p.getName()).contains(14)) {
				e.setCancelled(true);
			}
			if(player_ch.get(p.getName()).contains(25) && (item.getType().toString().contains("CHESTPLATE") || item.getType().toString().contains("LEGGINGS") || item.getType().toString().contains("BOOTS") || item.getType().toString().contains("HELMET"))) {
				e.setCancelled(true);
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equals("skywars")) {
				if(p.hasPermission("API.glav")) {
					if(args.length == 0) {
						send(p, "/sw create [name]");
						send(p, "/sw setminteams [count]");
						send(p, "/sw setmaxteams [count]");
						send(p, "/sw setplayersinteam [count]");
						send(p, "/sw addspawn");
						send(p, "/sw finish");
						send(p, "/sw addkit [name] [permission] [price]");
						send(p, "/sw setspec");
						send(p, "/sw start");
						send(p, "/sw chestadd [chance]");
					}
					else {
						if(args[0].equals("create")) {
							if(args.length == 2) {
								String name = args[1];
								getConfig().set("arena.name", name);
							}
						}
						else if(args[0].equals("setminteams")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								getConfig().set("arena.minteams", count);
							}
						}
						else if(args[0].equals("addkit")) {
							if(args.length == 4) {
								String name = args[1];
								String pex = args[2];
								long price = Integer.valueOf(args[3]);
								getConfig().set("kits."+name+".pex", pex);
								getConfig().set("kits."+name+".price", price);
								getConfig().set("kits."+name+".disp", p.getItemInHand());
								ItemStack[] extra = p.getInventory().getContents();
								getConfig().set("kits."+name+".extra", extra);
								saveConfig();
								reloadConfig();
							}
						}
						else if(args[0].equals("setmaxteams")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								getConfig().set("arena.maxteams", count);
							}
						}
						else if(args[0].equals("setplayersinteam")) {
							if(args.length == 2) {
								int count = Integer.valueOf(args[1]);
								getConfig().set("arena.playersinteam", count);
							}
						}
						else if(args[0].equals("addspawn")) {
							List<Location> list = (List<Location>) getConfig().get("arena.spawns");
							list.add(p.getLocation());
							getConfig().set("arena.spawns", list);
						}
						else if(args[0].equals("setspec")) {
							getConfig().set("arena.spec", p.getLocation());
						}
						else if(args[0].equals("finish")) {
							toLobby(p);
							getConfig().set("arena.status", "wait");
							STATUS = "wait";
							updateDB("wait", players_count);
							saveConfig();
							reloadConfig();
						}
						else if(args[0].equals("start")) {
							startGame();
						}
						else if(args[0].equals("addcage")) {
							if(args.length == 1) {
								String name = args[1];
								ItemStack item = p.getItemInHand();
								getConfig().set("cages."+name, item);
							}
						}
						else if(args[0].equals("chestadd")) {
							if(args.length == 2) {
								int chance = Integer.valueOf(args[1]);
								List<ItemStack> list = new ArrayList<ItemStack>();
								list = (List<ItemStack>) getConfig().get("chests");
								ItemStack item = p.getItemInHand();
								ItemMeta meta = item.getItemMeta();
								List<String> lore = new ArrayList<String>();
								lore.add(String.valueOf(chance)+"%");
								meta.setLore(lore);
								item.setItemMeta(meta);
								list.add(item);
								getConfig().set("chests", list);
								saveConfig();
								reloadConfig();
							}
						}
					}
				}
			}
		}
		return true;
	}
}
