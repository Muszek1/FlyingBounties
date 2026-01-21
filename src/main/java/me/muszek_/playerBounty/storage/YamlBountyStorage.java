package me.muszek_.playerBounty.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import me.muszek_.playerBounty.PlayerBounty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class YamlBountyStorage implements BountyStorage {

  private final PlayerBounty plugin;
  private File file;
  private FileConfiguration config;

  public YamlBountyStorage(PlayerBounty plugin) {
    this.plugin = plugin;
  }

  @Override
  public void init() {
    file = new File(plugin.getDataFolder(), "bounties.yml");
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (Exception e) {
        plugin.getLogger().severe("Could not create bounties.yml file!");
        e.printStackTrace();
      }
    }
    config = YamlConfiguration.loadConfiguration(file);
  }

  /**
   * Zapisuje plik asynchronicznie, aby nie blokować głównego wątku serwera.
   * Metoda jest publiczna, aby spełnić wymagania interfejsu BountyStorage.
   */
  @Override
  public void save() {
    // 1. Konwersja konfiguracji do Stringa musi odbyć się na wątku, który edytował config (zazwyczaj główny).
    // Jest to operacja bardzo szybka (tylko w pamięci RAM).
    final String data = config.saveToString();

    // 2. Fizyczny zapis na dysk (operacja wolna) odbywa się w tle.
    CompletableFuture.runAsync(() -> {
      try {
        Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        plugin.getLogger().severe("Could not save bounties.yml file asynchronously!");
        e.printStackTrace();
      }
    });
  }

  @Override
  public CompletableFuture<Bounty> createBounty(Player issuer, String targetName, UUID targetUuid,
      double amount,
      ItemStack item, long durationSeconds) {

    int newId = config.getInt("lastId", 0) + 1;
    config.set("lastId", newId);

    String path = "bounties." + newId;
    Instant now = Instant.now();
    Instant expires = now.plusSeconds(durationSeconds);

    config.set(path + ".issuer", issuer.getName());
    config.set(path + ".target-name", targetName);
    config.set(path + ".target-uuid", targetUuid.toString());
    config.set(path + ".created", now.toString());
    config.set(path + ".expires", expires.toString());

    if (item != null) {
      config.set(path + ".reward-item", item);
      config.set(path + ".amount", 0);
    } else {
      config.set(path + ".amount", amount);
    }
    save(); // Wywołujemy publiczną metodę save (która teraz działa w tle)
    return CompletableFuture.completedFuture(
        new Bounty(newId, issuer.getName(), targetName, targetUuid, amount, item, now, expires));
  }

  @Override
  public CompletableFuture<Optional<Bounty>> getBounty(int id) {
    String path = "bounties." + id;
    if (!config.contains(path)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return CompletableFuture.completedFuture(
        Optional.of(loadBountyFromConfig(id, config.getConfigurationSection(path))));
  }

  @Override
  public CompletableFuture<Void> deleteBounty(int id) {
    config.set("bounties." + id, null);
    save();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<List<Bounty>> getAllBounties() {
    ConfigurationSection section = config.getConfigurationSection("bounties");
    if (section == null) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    List<Bounty> list = new ArrayList<>();
    for (String key : section.getKeys(false)) {
      try {
        int id = Integer.parseInt(key);
        list.add(loadBountyFromConfig(id, section.getConfigurationSection(key)));
      } catch (NumberFormatException ignored) {
      }
    }
    return CompletableFuture.completedFuture(list);
  }

  @Override
  public CompletableFuture<List<Bounty>> getBountiesByTarget(UUID targetUuid) {
    return getAllBounties().thenApply(list -> list.stream()
        .filter(b -> b.getTargetUuid().equals(targetUuid))
        .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<Bounty>> getBountiesByIssuer(String issuerName) {
    return getAllBounties().thenApply(list -> list.stream()
        .filter(b -> b.getIssuer().equals(issuerName))
        .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<Bounty>> getExpiredBounties(long currentTimeMillis) {
    return getAllBounties().thenApply(list -> list.stream()
        .filter(b -> b.getExpires().toEpochMilli() <= currentTimeMillis)
        .collect(java.util.stream.Collectors.toList()));
  }

  private Bounty loadBountyFromConfig(int id, ConfigurationSection sec) {
    String issuer = sec.getString("issuer");
    String targetName = sec.getString("target-name");
    String uuidStr = sec.getString("target-uuid");
    UUID targetUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
    double amount = sec.getDouble("amount", 0);
    ItemStack item = sec.getItemStack("reward-item");

    String createdStr = sec.getString("created");
    Instant created = createdStr != null ? Instant.parse(createdStr) : Instant.now();

    String expiresStr = sec.getString("expires");
    Instant expires = expiresStr != null ? Instant.parse(expiresStr) : Instant.now();

    return new Bounty(id, issuer, targetName, targetUuid, amount, item, created, expires);
  }
}