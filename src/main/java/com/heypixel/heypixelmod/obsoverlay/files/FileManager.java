package com.heypixel.heypixelmod.obsoverlay.files;

import com.heypixel.heypixelmod.obsoverlay.files.impl.CGuiFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ConfigFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.FriendFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.KillSaysFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ModuleFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ProxyFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.SpammerFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ValueFile;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileManager {
   public static final Logger logger = LogManager.getLogger(FileManager.class);
   public static final File clientFolder;
   public static final File configFolder;
   public static Object trash = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
   private final List<ClientFile> files = new ArrayList<>();

   public FileManager() {
      if (!clientFolder.exists() && clientFolder.mkdir()) {
         logger.info("Created client folder!");
      }
      if (!configFolder.exists() && configFolder.mkdir()) {
         logger.info("Created config folder!");
      }

      this.files.add(new KillSaysFile());
      this.files.add(new SpammerFile());
      this.files.add(new ModuleFile());
      this.files.add(new ValueFile());
      this.files.add(new CGuiFile());
      this.files.add(new ProxyFile());
      this.files.add(new FriendFile());
   }

   public void load() {
      for (ClientFile clientFile : this.files) {
         File file = clientFile.getFile();

         try {
            if (!file.exists() && file.createNewFile()) {
               logger.info("Created file " + file.getName() + "!");
               this.saveFile(clientFile);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            clientFile.read(reader);
            reader.close();
         } catch (IOException var5) {
            logger.error("Failed to load file " + file.getName() + "!", var5);
            this.saveFile(clientFile);
         }
      }
   }

   public void save() {
      for (ClientFile clientFile : this.files) {
         this.saveFile(clientFile);
      }

      logger.info("Saved all files!");
   }

   private void saveFile(ClientFile clientFile) {
      File file = clientFile.getFile();

      try {
         if (!file.exists() && file.createNewFile()) {
            logger.info("Created file " + file.getName() + "!");
         }

         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
         clientFile.save(writer);
         writer.flush();
         writer.close();
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }


   public void load(String fileName) {
      ConfigFile configFile = new ConfigFile(fileName + ".cfg");
      File file = configFile.getFile();

      if (!file.exists()) {
         ChatUtils.addChatMessage("§cConfig not found §6" + fileName + "§c!");
         logger.warn("Config file does not exist: {}", fileName);
         return;
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
         configFile.read(reader);
         ChatUtils.addChatMessage("§aConfig §6" + fileName + "§a loaded successfully!");
         logger.info("Successfully loaded config file: {}", fileName);
      } catch (IOException e) {
         ChatUtils.addChatMessage("§cAn error occurred while loading config §6" + fileName + "§c!");
         logger.error("Failed to load config file: {}", fileName, e);
      }
   }

   public void save(String fileName) {
      ConfigFile configFile = new ConfigFile(fileName + ".cfg");
      File file = configFile.getFile();

      try {
         if (!file.exists() && file.createNewFile()) {
            logger.info("Created new config file: {}", fileName);
         }
         try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            configFile.save(writer);
            writer.flush();
            ChatUtils.addChatMessage("§aConfig §6" + fileName + "§a saved successfully!");
            logger.info("Successfully saved config file: {}", fileName);
         }
      } catch (IOException e) {
         ChatUtils.addChatMessage("§cAn error occurred while saving config §6" + fileName + "§c!");
         logger.error("Failed to save config file: {}", fileName, e);
      }
   }

   static {
      File gameDir = FMLPaths.GAMEDIR.get().toFile();
      clientFolder = new File(gameDir, "Naven-XD");
      configFolder = new File(clientFolder, "config");
   }
}