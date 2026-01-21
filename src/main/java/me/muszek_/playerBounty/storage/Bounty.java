package me.muszek_.playerBounty.storage;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class Bounty {
  private final int id;
  private final String issuer;
  private final String targetName;
  private final UUID targetUuid;
  private final double amount;
  private final ItemStack rewardItem;
  private final Instant created;
  private final Instant expires;

  public int getId() {
    return id;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getTargetName() {
    return targetName;
  }

  public UUID getTargetUuid() {
    return targetUuid;
  }

  public double getAmount() {
    return amount;
  }

  public ItemStack getRewardItem() {
    return rewardItem;
  }

  public boolean isItemReward() {
    return rewardItem != null;
  }

  public Instant getCreated() {
    return created;
  }

  public Instant getExpires() {
    return expires;
  }

  public Bounty(int id, String issuer, String targetName, UUID targetUuid, double amount,
      ItemStack rewardItem, Instant created, Instant expires) {
    this.id = id;
    this.issuer = issuer;
    this.targetName = targetName;
    this.targetUuid = targetUuid;
    this.amount = amount;
    this.rewardItem = rewardItem;
    this.created = created;
    this.expires = expires;
  }
}
