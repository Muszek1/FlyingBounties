package me.muszek_.playerBounty.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface BountyStorage {

  void init();

  void save();

  CompletableFuture<Bounty> createBounty(Player issuer, String targetName, UUID targetUuid,
      double amount, ItemStack item, long durationSeconds);

  CompletableFuture<Optional<Bounty>> getBounty(int id);

  CompletableFuture<Void> deleteBounty(int id);

  CompletableFuture<List<Bounty>> getAllBounties();

  CompletableFuture<List<Bounty>> getBountiesByTarget(UUID targetUuid);

  CompletableFuture<List<Bounty>> getBountiesByIssuer(String issuerName);

  CompletableFuture<List<Bounty>> getExpiredBounties(long currentTimeMillis);
}