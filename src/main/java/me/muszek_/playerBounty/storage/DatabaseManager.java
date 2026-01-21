package me.muszek_.playerBounty.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.muszek_.playerBounty.PlayerBounty;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

  private final PlayerBounty plugin;
  private HikariDataSource dataSource;

  public DatabaseManager(PlayerBounty plugin) {
    this.plugin = plugin;
  }

  public void init() {
    FileConfiguration config = plugin.getConfig();
    String host = config.getString("database.host");
    int port = config.getInt("database.port");
    String database = config.getString("database.database");
    String username = config.getString("database.username");
    String password = config.getString("database.password");
    boolean useSSL = config.getBoolean("database.use-ssl");

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);

    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    dataSource = new HikariDataSource(hikariConfig);
  }

  public Connection getConnection() throws SQLException {
    if (dataSource == null) {
      throw new SQLException("DataSource not initialized");
    }
    return dataSource.getConnection();
  }

  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}