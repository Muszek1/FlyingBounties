package me.muszek_.playerBounty.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SqlBountyStorage implements BountyStorage {

  private final PlayerBounty plugin;
  private final DatabaseManager dbManager;
  private final String tableName;

  public SqlBountyStorage(PlayerBounty plugin) {
    this.plugin = plugin;
    this.dbManager = new DatabaseManager(plugin);
    this.tableName = plugin.getConfig().getString("database.table-prefix", "fb_") + "bounties";
  }

  @Override
  public void init() {
    try {
      dbManager.init();
      createTable();
    } catch (Exception e) {
      plugin.getLogger().severe("Could not initialize database!");
      e.printStackTrace();
    }
  }

  public void close() {
    dbManager.close();
  }

  private void createTable() {
    String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
        "id INT AUTO_INCREMENT PRIMARY KEY, " +
        "issuer VARCHAR(36) NOT NULL, " +
        "target_name VARCHAR(16) NOT NULL, " +
        "target_uuid VARCHAR(36) NOT NULL, " +
        "amount DOUBLE NOT NULL, " +
        "reward_item TEXT, " +
        "created BIGINT NOT NULL, " +
        "expires BIGINT NOT NULL" +
        ");";

    try (Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    } catch (SQLException e) {
      plugin.getLogger().severe("Could not create database table!");
      e.printStackTrace();
    }
  }

  @Override
  public void save() {
  }

  @Override
  public CompletableFuture<Bounty> createBounty(Player issuer, String targetName, UUID targetUuid,
      double amount,
      ItemStack item, long durationSeconds) {

    String issuerName = issuer.getName();
    String itemBase64 = ItemSerializer.toBase64(item);
    Instant now = Instant.now();
    Instant expires = now.plusSeconds(durationSeconds);

    return CompletableFuture.supplyAsync(() -> {
      String sql = "INSERT INTO " + tableName
          + " (issuer, target_name, target_uuid, amount, reward_item, created, expires) VALUES (?, ?, ?, ?, ?, ?, ?)";

      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, issuerName);
        ps.setString(2, targetName);
        ps.setString(3, targetUuid.toString());
        ps.setDouble(4, amount);
        ps.setString(5, itemBase64);
        ps.setLong(6, now.toEpochMilli());
        ps.setLong(7, expires.toEpochMilli());

        ps.executeUpdate();

        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            int id = rs.getInt(1);
            return new Bounty(id, issuerName, targetName, targetUuid, amount, item, now, expires);
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return null;
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<Optional<Bounty>> getBounty(int id) {
    return CompletableFuture.supplyAsync(() -> {
      String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapResultSetToBounty(rs));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return Optional.empty();
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<Void> deleteBounty(int id) {
    return CompletableFuture.runAsync(() -> {
      String sql = "DELETE FROM " + tableName + " WHERE id = ?";
      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, id);
        ps.executeUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<List<Bounty>> getAllBounties() {
    return CompletableFuture.supplyAsync(() -> {
      List<Bounty> bounties = new ArrayList<>();
      String sql = "SELECT * FROM " + tableName;
      try (Connection conn = dbManager.getConnection();
          Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
          bounties.add(mapResultSetToBounty(rs));
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return bounties;
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<List<Bounty>> getBountiesByTarget(UUID targetUuid) {
    return CompletableFuture.supplyAsync(() -> {
      List<Bounty> bounties = new ArrayList<>();
      String sql = "SELECT * FROM " + tableName + " WHERE target_uuid = ?";
      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, targetUuid.toString());
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            bounties.add(mapResultSetToBounty(rs));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return bounties;
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<List<Bounty>> getBountiesByIssuer(String issuerName) {
    return CompletableFuture.supplyAsync(() -> {
      List<Bounty> bounties = new ArrayList<>();
      String sql = "SELECT * FROM " + tableName + " WHERE issuer = ?";
      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, issuerName);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            bounties.add(mapResultSetToBounty(rs));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return bounties;
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  @Override
  public CompletableFuture<List<Bounty>> getExpiredBounties(long currentTimeMillis) {
    return CompletableFuture.supplyAsync(() -> {
      List<Bounty> bounties = new ArrayList<>();
      String sql = "SELECT * FROM " + tableName + " WHERE expires <= ?";
      try (Connection conn = dbManager.getConnection();
          PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, currentTimeMillis);

        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            bounties.add(mapResultSetToBounty(rs));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return bounties;
    }, command -> Bukkit.getScheduler().runTaskAsynchronously(plugin, command));
  }

  private Bounty mapResultSetToBounty(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    String issuer = rs.getString("issuer");
    String targetName = rs.getString("target_name");
    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
    double amount = rs.getDouble("amount");
    String itemBase64 = rs.getString("reward_item");
    ItemStack item = ItemSerializer.fromBase64(itemBase64);
    Instant created = Instant.ofEpochMilli(rs.getLong("created"));
    Instant expires = Instant.ofEpochMilli(rs.getLong("expires"));

    return new Bounty(id, issuer, targetName, targetUuid, amount, item, created, expires);
  }
}