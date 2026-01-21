package me.muszek_.playerBounty.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class ItemSerializer {

  public static String toBase64(ItemStack item) {
    if (item == null) {
      return null;
    }
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
      dataOutput.writeObject(item);
      dataOutput.close();
      return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ItemStack fromBase64(String data) {
    if (data == null || data.isEmpty()) {
      return null;
    }
    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
      BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
      ItemStack item = (ItemStack) dataInput.readObject();
      dataInput.close();
      return item;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}