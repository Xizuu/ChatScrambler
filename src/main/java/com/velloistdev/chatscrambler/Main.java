package com.velloistdev.chatscrambler;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private final List<String> words = new ArrayList<>();
    private String word = null;
    private int price;
    private int time;
    private long wordScrambleInterval;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault is not installed or no economy plugin was found.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        initWords();
        this.time = getConfig().getInt("guess_time_limit");
        this.wordScrambleInterval = getConfig().getLong("word_scramble_interval") * 20; // Convert to ticks
        startWordScrambleTask();
        startCountdownTask();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    private void startWordScrambleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (word == null && price == 0) {
                    Random random = new Random();
                    int key = random.nextInt(words.size());
                    word = words.get(key);
                    price = random.nextInt(getConfig().getInt("max_prize") - getConfig().getInt("min_prize") + 1) + getConfig().getInt("min_prize");
                    String text = ChatColor.GRAY + "===============\n" +
                            " \n" +
                            ChatColor.GREEN + "Kata: " + ChatColor.AQUA + scrambleWord(word) + "\n" +
                            " \n" +
                            ChatColor.GREEN + "Orang pertama yang dapat mengurutkan kata tersebut akan mendapatkan " + ChatColor.AQUA + "Rp." +
                            String.format("%,d", price) + "\n" +
                            " \n" +
                            ChatColor.GRAY + "===============";
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(text);
                    }
                }
            }
        }.runTaskTimer(this, wordScrambleInterval, wordScrambleInterval);
    }

    private void startCountdownTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (word != null && price != 0) {
                    time--;
                    if (time == 0) {
                        word = null;
                        price = 0;
                        time = getConfig().getInt("guess_time_limit");
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(ChatColor.GREEN + "Waktu menebak telah habis");
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private String scrambleWord(String word) {
        List<Character> characters = new ArrayList<>();
        for (char c : word.toCharArray()) {
            characters.add(c);
        }
        Collections.shuffle(characters);
        StringBuilder scrambled = new StringBuilder();
        for (char c : characters) {
            scrambled.append(c);
        }
        return scrambled.toString();
    }

    private void initWords() {
        FileConfiguration config = getConfig();
        words.addAll(config.getStringList("words"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (message.endsWith(".")) {
            message = message.substring(0, message.length() - 1);
        }

        message = message.toLowerCase();

        String lowerCaseWord = word != null ? word.toLowerCase() : null;

        if (price != 0 && message.equalsIgnoreCase(lowerCaseWord)) {
            event.setCancelled(true);
            econ.depositPlayer(player, price);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.GREEN + " telah berhasil menebak kata dan mendapatkan " + ChatColor.AQUA + "Rp." + String.format("%,d", price));
            }
            word = null;
            price = 0;
            time = getConfig().getInt("guess_time_limit");
        }
    }
}
