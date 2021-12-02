package argento.skywars;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.entity.Player;

public class Database {
    private Connection con;
    private Statement stmt;
    private ResultSet rs;
    private String url = "", user = "", password = "";
    
    public Database() {
    	
    }
    
    public Database(String url, String user, String password) {
    	this.url = url;
    	this.user = user;
    	this.password = password;
    	connect();
    	createTable();
    }
    
    public void addToDB(Player p) {
    	String name = p.getName();
		String query = "INSERT INTO sw_stats (name, games, kills, wins) VALUES ('"+name+"', 0, 0, 0)";
		try {
			stmt.executeUpdate(query);
		} catch (SQLException e) {};
	}
	
	public void createTable() {
		String query = "CREATE TABLE sw_stats (name VARCHAR(512) UNIQUE, games INTEGER, kills INTEGER, wins INTEGER)";
		try {
			stmt.execute(query);
		}
		catch(SQLException e) {};
	}
	
	int getSkyGames(Player p) {
		String name = p.getName();
		int games = 0;
		String query = "SELECT games FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			games = rs.getInt(1);
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
			stmt.executeUpdate(query);
		} catch (SQLException e) {}
	}
	
	public void addSkyWin(Player p) {
		String name = p.getName();
		int wins = getSkyWins(p);
		String query = "UPDATE sw_stats SET wins = '"+(wins+1)+"' WHERE name = '"+name+"'";
		try {
			stmt.executeUpdate(query);
		} catch (SQLException e) {}
	}

	
	public void addSkyKill(Player p) {
		String name = p.getName();
		int kills = getSkyKills(p);
		String query = "UPDATE sw_stats SET kills = '"+(kills+1)+"' WHERE name = '"+name+"'";
		try {
			stmt.executeUpdate(query);
		} catch (SQLException e) {}
	}
	
	int getSkyKit(Player p, int type) {
		String name = p.getName();
		int count = 0;
		String t = "t"+String.valueOf(type);
		String query = "SELECT "+t+" FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			count = rs.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
	
	int getSkyWins(Player p) {
		String name = p.getName();
		int wins = 0;
		String query = "SELECT wins FROM sw_stats WHERE name = '"+name+"'";
		try {
			rs = stmt.executeQuery(query);
			rs.next();
			wins = rs.getInt(1);
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
			rs = stmt.executeQuery(query);
			rs.next();
			index = rs.getInt(1);
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
			rs = stmt.executeQuery(query);
			rs.next();
			kills = rs.getInt(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return kills;
	}
	
	public void connect() {
		if(SkyWars.isMySQLEnabled()) {
			Main.instance.getLogger().info("Connecting");
			
			try {
				con = DriverManager.getConnection(url, user, password);;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				stmt = con.createStatement();
			} catch (SQLException e) {}
		}
	}
}
