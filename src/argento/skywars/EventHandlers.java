package argento.skywars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Arrow;
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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EventHandlers implements Listener {
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		Inventory in = e.getInventory();
		if(SkyWars.getStatus().equals("wait")) {
			if(in.equals(SkyWars.getTeamInv())) {
				ItemStack item = e.getCurrentItem();
				if(item != null && item.getType() == Material.WHITE_STAINED_GLASS) {
					int index = e.getRawSlot();
					ArrayList<Player> pl = SkyWars.getTeams().get(index);
					if(!pl.contains(p)) {
						if(pl.size() < SkyWars.getPlayersInTeam()) {
							SkyWars.changeTeam(p, index);
							SkyWars.send(p, SkyWars.getLang("team_selected"));
						}
						else {
							SkyWars.send(p, SkyWars.getLang("team_is_full"));
						}
					}
					else SkyWars.send(p, SkyWars.getLang("you_already_at_this_team"));
				}
				
				e.setCancelled(true);
			}
			else if(in.equals(SkyWars.getInv())) {
				ItemStack item = e.getCurrentItem();
				if(item != null) {
					ItemMeta meta = item.getItemMeta();
					String name = meta.getDisplayName().split(" ")[0].substring(2);
					if(SkyWars.getConfig().contains("kits."+name)) {
						String pex = SkyWars.getConfig().getString("kits."+name+".pex");
						Long price = SkyWars.getConfig().getLong("kits."+name+".price");
						if(!p.hasPermission(pex)) {
							SkyWars.send(p, SkyWars.getLang("no_perms"));
						}
						else if(SkyWars.getEcon() != null && SkyWars.getEcon().getBalance(p) < price) {
							SkyWars.send(p, SkyWars.getLang("no_money"));
						}
						else {
							List<ItemStack> items = (List<ItemStack>) SkyWars.getConfig().get("kits."+name+".extra");
							SkyWars.send(p, SkyWars.getLang("kit_selected")+name);
							SkyWars.getPlayerKit().put(p.getName(), items);
							if(SkyWars.getEcon() != null) SkyWars.getEcon().withdrawPlayer(p, price);
						}
						p.closeInventory();
					}
				}
				e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if(SkyWars.getStatus().equals("wait")) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.CHEST)) e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		if(SkyWars.getStatus().equals("wait")) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.CHEST)) e.setCancelled(true);
		if(e.getBlock().getType().equals(Material.TRAPPED_CHEST)) {
			SkyWars.getTrappedChests().add(e.getBlock().getLocation());
		}
		if(!SkyWars.isAllowedBlockPlace()) e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if(SkyWars.getStatus().equals("wait")) e.setCancelled(true);
	}
	
	@EventHandler
	public void damage(EntityDamageEvent e) {
		if(!SkyWars.getStatus().equals("play") && e.getEntity() instanceof Player) {
			e.setCancelled(true);
		}
		if(e.getEntity() instanceof Player && SkyWars.getStatus().equals("play")) {
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
	@EventHandler
	public void interact(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(SkyWars.getStatus().equals("wait")) {
			if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if(p.getItemInHand().isSimilar(SkyWars.getChoose())) {
					if(SkyWars.getPlayerKit().containsKey(p.getName())) {
						if(e.getHand() == EquipmentSlot.HAND) SkyWars.send(p, SkyWars.getLang("you_already_selected_kit"));
						e.setCancelled(true);
					}
					else p.openInventory(SkyWars.getInv());
				}
				else if(p.getItemInHand().isSimilar(SkyWars.getLobby())) {
					SkyWars.toLobby(p);
				}
				else if(p.getItemInHand().isSimilar(SkyWars.getTeamItem())) {
					p.openInventory(SkyWars.getTeamInv());
				}
			}
			e.setCancelled(true);
		}
	}
	@EventHandler
	public void bloccck(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		if(SkyWars.getStatus().equals("wait")) e.setCancelled(true);
	}
	@EventHandler
	public void death(EntityDeathEvent e) {
		Player p = (Player) e.getEntity();
		if(SkyWars.getPlayers().containsKey(p)) {
			Player killer = p.getKiller();
			long money = 0;
			if(SkyWars.getEcon() != null) {
				money = (long) SkyWars.getEcon().getBalance(p);
			}
			money = (long) (money*SkyWars.getKillMoney())/100;
			int w = 0;
			if(killer != null && killer instanceof Player) {
				if(!killer.getName().equals(p.getName())) {
					if(SkyWars.getEcon() != null) SkyWars.getEcon().depositPlayer(killer, money);
					if(SkyWars.isMysqlEnabled()) SkyWars.getDb().addSkyKill(killer);
					if(SkyWars.getKills().containsKey(killer.getName())) {
						w = SkyWars.getKills().get(killer.getName())+1;
						SkyWars.getKills().replace(killer.getName(), w);
					}
					else {
						w = 1;
						SkyWars.getKills().put(killer.getName(), 1);
					}
					for(Player pls : SkyWars.getPlayers().keySet()) {
						SkyWars.send(pls, SkyWars.getLang("kill").replaceAll("%killed%", p.getCustomName())
								.replaceAll("%killer%", killer.getCustomName()));
					}
				}
			}
			else {
				for(Player pls : SkyWars.getPlayers().keySet()) {
					SkyWars.send(pls, p.getCustomName()+SkyWars.getLang("died"));
				}
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
			SkyWars.setPlayersCount(SkyWars.getPlayersCount()-1);
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> p.spigot().respawn(), 1L);
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> SkyWars.toSpec(p), 1L);
			try {
				ArrayList<Player> pl = SkyWars.getTeams().get(SkyWars.getPlayers().get(p));
				for(Player pp : pl) {
					if(!p.getName().equals(pp.getName())) SkyWars.send(pp, SkyWars.getLang("teammate_died").replaceAll("%player%", p.getCustomName()));
				}
			} catch(Exception ee) {};
			SkyWars.removePlayerFromTeam(p);
			if(SkyWars.isEnd()) SkyWars.endGame();
		}
	}
	@EventHandler
	public void onPlayerCraft(CraftItemEvent e) {
		if(e.getRecipe().getResult().getType().equals(Material.CHEST)) e.setCancelled(true);
	}
	
	@EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof Chest) {
            Location loc = e.getInventory().getLocation();
            if(!SkyWars.getTrappedChests().contains(loc)) {
	            if(!SkyWars.getChests().contains(loc)) {
	            	e.getInventory().clear();
	            	List<ItemStack> list = (List<ItemStack>) SkyWars.getConfig().get("chests");
	            	for(int i = 0; i < list.size(); ++i) {
	            		ItemStack item = list.get(i);
	            		int chance = SkyWars.getChance(item);
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
	            	SkyWars.getChests().add(loc);
	            }
            }
        }
    }
	
	@EventHandler
	public void damage(EntityDamageByEntityEvent e) {
		if(SkyWars.getStatus().equals("wait")) e.setCancelled(true);
		if(e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
			if(SkyWars.getStatus().equals("play")) {
				Player damager = (Player) e.getDamager();
				Player p = (Player) e.getEntity();
				if(SkyWars.getPlayers().get(p) == SkyWars.getPlayers().get(damager)) e.setCancelled(true);
			}
		}
		else if(e.getEntity() instanceof Player && e.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
			Player p = (Player) e.getEntity();
			if(e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				Player damager = (Player) a.getShooter();
				Integer team = SkyWars.getPlayers().get(p);
				Integer team2 = SkyWars.getPlayers().get(damager);
				if(team == team2) e.setCancelled(true);
			}
			else if(e.getDamager() instanceof Trident) {
				Trident a = (Trident) e.getDamager();
				Player damager = (Player) a.getShooter();
				Integer team = SkyWars.getPlayers().get(p);
				Integer team2 = SkyWars.getPlayers().get(damager);
				if(team == team2) e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		e.setJoinMessage(null);
		if(SkyWars.isMysqlEnabled()) SkyWars.getDb().addToDB(p);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
			public void run() {
				SkyWars.scoreboardUpdate(p);
			}
		}, 0, 19*1);
		p.getInventory().clear();
		if(SkyWars.getStatus().equals("wait")) {
			int min_players = SkyWars.getPlayersInTeam()*SkyWars.getMinTeams();
			SkyWars.addToTeam(p);
			SkyWars.setPlayersCount(SkyWars.getPlayersCount()+1);
			p.getInventory().setItem(0, SkyWars.getTeamItem());
			p.getInventory().setItem(1, SkyWars.getChoose());
			p.getInventory().setItem(8, SkyWars.getLobby());
			SkyWars.sendAll(p.getCustomName()+SkyWars.getLang("player_join")+String.valueOf(SkyWars.getPlayersCount())+
					"&a/&c"+String.valueOf(SkyWars.getMaxTeams()*SkyWars.getPlayersInTeam()));
			if(!SkyWars.isTimerStart() && min_players <= SkyWars.getPlayersCount()) {
				SkyWars.startLobby();
			}
		}
		else if(SkyWars.getStatus().equals("play")) {
			SkyWars.toSpec(p);
			SkyWars.send(p, SkyWars.getLang("game_in_progress"));
			p.getInventory().setItem(8, SkyWars.getLobby());
			SkyWars.clearColor(p);
		}
		else {
			if(!p.hasPermission("SkyWars.admin")) SkyWars.toLobby(p);
		}
	}
	@EventHandler
	public void move(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if(SkyWars.getStatus().equals("play")) {
			if(SkyWars.isCompass()) {
				Location loc = p.getLocation();
				int min_dis = 100000000;
				for(ArrayList<Player> list : SkyWars.getTeams()) {
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
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title "+p.getName()+" actionbar \""+SkyWars.getLang("distance").
						replaceAll("%dis%", String.valueOf(min_dis))+" \"");
			}
		}
	}
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		e.setQuitMessage(null);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtabname");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player "+p.getName()+" customtagname");
		if(SkyWars.getStatus().equals("wait")) {
			SkyWars.setPlayersCount(SkyWars.getPlayersCount()-1);
			SkyWars.sendAll(p.getCustomName()+SkyWars.getLang("player_left")+String.valueOf(SkyWars.getPlayersCount())+
					"&a/&c"+String.valueOf(SkyWars.getMaxTeams()*SkyWars.getPlayersInTeam()));
			SkyWars.removePlayerFromTeam(p);
			if(SkyWars.isTimerStart() && SkyWars.getPlayersCount() < SkyWars.getMinTeams()*SkyWars.getPlayersCount()) {
				SkyWars.stopTimer();
			}
		}
		else if(SkyWars.getStatus().equals("play") && SkyWars.getPlayers().containsKey(p)) {
			SkyWars.setPlayersCount(SkyWars.getPlayersCount()-1);
			SkyWars.sendAll(p.getCustomName()+SkyWars.getLang("player_left"));
			ArrayList<Player> pl = SkyWars.getTeams().get(SkyWars.getPlayers().get(p));
			for(Player pp : pl) {
				if(!p.getName().equals(pp.getName())) SkyWars.send(pp, SkyWars.getLang("teammate_left").replaceAll("%player%", p.getCustomName()));
			}
			SkyWars.removePlayerFromTeam(p);
			if(SkyWars.isEnd()) {
				SkyWars.endGame();
			}
		}
	}
	@EventHandler
	public void pickup(PlayerPickupItemEvent e) {
		Player p = e.getPlayer();
		ItemStack item = e.getItem().getItemStack();
		if(item.getType() == Material.CHEST) {
			e.setCancelled(true);
		}
		else if(item.getType() == Material.COMPASS && !SkyWars.hasItem(p.getInventory(), Material.COMPASS, 1)) {
			e.setCancelled(true);
		}
	}
}
