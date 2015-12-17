package org.mcsg.double0negative.tabapi;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.base.Charsets;
import com.mojang.authlib.properties.Property;

public class TabAPI extends JavaPlugin implements Listener, CommandExecutor {

	private static HashMap<String, TabObject> playerTab = new HashMap<String, TabObject>();
	private static HashMap<String, TabHolder> playerTabLast = new HashMap<String, TabHolder>();
	private static HashMap<String, TabObject47> playerTab47 = new HashMap<String, TabObject47>();
	private static HashMap<String, TabHolder47> playerTabLast47 = new HashMap<String, TabHolder47>();
	private static HashMap<Player, ArrayList<PacketContainer>> cachedPackets = new HashMap<Player, ArrayList<PacketContainer>>();
	private static HashMap<Player, Integer> updateSchedules = new HashMap<Player, Integer>();
	private static int horizTabSize = 3;
	private static int vertTabSize = 20;
	private static int horizTabSize47 = 4;
	private static int vertTabSize47 = 20;
	private static String[] colors = { "1", "2", "3", "4", "5", "6", "7", "8",
			"9", "0", "a", "b", "c", "d", "f", "g", "h", "i", "j", "k", "l",
			"m", "n", "o", "p", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
	private static int e = 0;
	private static int r = 0;
	private static long flickerPrevention = 5L;
	public static ProtocolManager protocolManager;
	private static boolean shuttingdown = true;
	private static TabAPI plugin;

	public void onEnable() {
		plugin = this;
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);

		MemoryConfiguration defaultConfig = new MemoryConfiguration();
		defaultConfig.set("flickerPrevention", Long.valueOf(flickerPrevention));
		config.setDefaults(defaultConfig);
		saveConfig();

		reloadConfiguration();
		getCommand("tabapi").setExecutor(new TabCommand(this));
		protocolManager = ProtocolLibrary.getProtocolManager();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		for (Player p : Bukkit.getOnlinePlayers()) {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("TabAPI");
			setPriority(plugin, p, 2);
			resetTabList(p);
			setPriority(plugin, p, -2);
		}
		protocolManager.addPacketListener(new PacketAdapter(this,
				ListenerPriority.NORMAL,
				new PacketType[] { PacketType.Play.Server.PLAYER_INFO }) {
			public void onPacketSending(PacketEvent event) {
				PacketContainer p = event.getPacket();
				List<PlayerInfoData> pinfodata = p.getPlayerInfoDataLists()
						.read(0);
				String s = ((PlayerInfoData) pinfodata.get(0)).getProfile()
						.getName();
				if (s.startsWith("$")) {
					List<PlayerInfoData> pinfodataReSend = new ArrayList<PlayerInfoData>();
					PlayerInfoData pinfod = (PlayerInfoData) pinfodata.get(0);
					pinfodataReSend.add(new PlayerInfoData(pinfod.getProfile()
							.withName(s.substring(1)), pinfod.getPing(), pinfod
							.getGameMode(), WrappedChatComponent
							.fromText(pinfod.getProfile().getName()
									.substring(1))));

					p.getPlayerInfoDataLists().write(0, pinfodataReSend);
					event.setPacket(p);
				} else if (TabAPI.protocolManager.getProtocolVersion(event
						.getPlayer()) < 47) {
					event.setCancelled(true);
				}
			}
		});
	}

	public void reloadConfiguration() {
		reloadConfig();
		flickerPrevention = getConfig().getLong("flickerPrevention");
	}

	public void onDisable() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			clearTab(p);
		}
		// flushPackets();
		shuttingdown = true;
		playerTab = null;
		playerTabLast = null;
		playerTab47 = null;
		playerTabLast47 = null;
	}

	private static String toColorString(int slotId) {
		/*
		 * StringBuilder sb = new StringBuilder();
		 * 
		 * for (char c : String.valueOf(slotId).toCharArray()) {
		 * sb.append(String.valueOf(ChatColor.COLOR_CHAR) + c); }
		 * 
		 * return sb.toString();
		 */
		return ChatColor.COLOR_CHAR + String.valueOf((char) slotId);
	}

	private static void addPacket(Player p, String msg, int slotId,
			WrappedGameProfile gameProfile, boolean b, int ping) {
		PacketContainer message = protocolManager
				.createPacket(PacketType.Play.Server.PLAYER_INFO);
		String nameToShow = (!shuttingdown ? "$" : "") + msg;
		if (protocolManager.getProtocolVersion(p) >= 47) {
			nameToShow = (!shuttingdown ? "$" : "") + toColorString(slotId)
					+ msg.substring(0, Math.min(msg.length(), 14));
		}
		EnumWrappers.PlayerInfoAction action;
		if (b) {
			action = EnumWrappers.PlayerInfoAction.ADD_PLAYER;
		} else {
			action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER;
		}
		message.getPlayerInfoAction().write(0, action);
		List<PlayerInfoData> pInfoData = new ArrayList<PlayerInfoData>();
		if (gameProfile != null) {
			pInfoData.add(new PlayerInfoData(gameProfile.withName(
					nameToShow.substring(1)).withId(
					UUID.nameUUIDFromBytes(
							("OfflinePlayer:" + nameToShow.substring(1))
									.getBytes(Charsets.UTF_8)).toString()),
					ping, EnumWrappers.NativeGameMode.SURVIVAL,
					WrappedChatComponent.fromText(nameToShow)));
		} else {
			pInfoData.add(new PlayerInfoData(new WrappedGameProfile(UUID
					.nameUUIDFromBytes(("OfflinePlayer:" + nameToShow
							.substring(1)).getBytes(Charsets.UTF_8)),
					nameToShow.substring(1)), ping,
					EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent
							.fromText(nameToShow)));
		}
		message.getPlayerInfoDataLists().write(0, pInfoData);
		ArrayList<PacketContainer> packetList = cachedPackets.get(p);
		if (packetList == null) {
			packetList = new ArrayList<PacketContainer>();
			cachedPackets.put(p, packetList);
		}
		packetList.add(message);
	}

	@SuppressWarnings("unused")
	private static void flushPackets() {
		Player[] packetPlayers = (Player[]) cachedPackets.keySet().toArray(
				new Player[0]);
		Player[] arrayOfPlayer1 = packetPlayers;
		int j = packetPlayers.length;
		for (int i = 0; i < j; i++) {
			Player p = arrayOfPlayer1[i];

			flushPackets(p, null);
		}
	}

	private static void flushPackets(final Player p, final Object tabCopy) {
		final PacketContainer[] packets = (PacketContainer[]) cachedPackets
				.get(p).toArray(new PacketContainer[0]);

		Integer taskID = (Integer) updateSchedules.get(p);
		if (taskID != null) {
			Bukkit.getScheduler().cancelTask(taskID.intValue());
		}
		taskID = Integer.valueOf(Bukkit.getScheduler().scheduleSyncDelayedTask(
				plugin, new Runnable() {
					public void run() {
						if (p.isOnline()) {
							for (PacketContainer packet : packets) {
								try {
									protocolManager.sendServerPacket(p, packet);
								} catch (InvocationTargetException e) {
									e.printStackTrace();
									System.out
											.println("[TabAPI] Error sending packet to client");
								}
							}
						}
						if (tabCopy != null) {
							if (tabCopy instanceof TabHolder47) {
								playerTabLast47.put(p.getName(),
										(TabHolder47) tabCopy);
							} else if ((tabCopy instanceof TabHolder)) {
								playerTabLast.put(p.getName(),
										(TabHolder) tabCopy);
							}
						}
						TabAPI.updateSchedules.remove(p);
					}
				}, flickerPrevention));

		updateSchedules.put(p, taskID);
		cachedPackets.remove(p);
	}

	private static TabObject getTab(Player p) {
		TabObject tabo = (TabObject) playerTab.get(p.getName());
		if (tabo == null) {
			tabo = new TabObject();
			playerTab.put(p.getName(), tabo);
		}
		return tabo;
	}

	private static TabObject47 getTab47(Player p) {
		TabObject47 tabo = (TabObject47) playerTab47.get(p.getName());
		if (tabo == null) {
			tabo = new TabObject47();
			playerTab47.put(p.getName(), tabo);
		}
		return tabo;
	}

	public static void setPriority(Plugin plugin, Player player, int pri) {
		getTab(player).setPriority(plugin, pri);
	}

	public static void disableTabForPlayer(Player p) {
		playerTab.put(p.getName(), null);
		playerTab47.put(p.getName(), null);
		resetTabList(p);
	}

	public static void resetTabList(Player p) {
		int a = 0;
		int b = 0;
		for (Player pl : Bukkit.getOnlinePlayers()) {
			setTabString(Bukkit.getPluginManager().getPlugin("TabAPI"), p, a,
					b, pl.getPlayerListName());
			b++;
			if (b > getHorizSize(protocolManager.getProtocolVersion(pl))) {
				b = 0;
				a++;
			}
		}
	}

	public static void setTabString(Plugin plugin, Player p, int x, int y,
			String msg) {
		setTabString(plugin, p, x, y, msg, 0, null);
	}

	public static void setTabString(Plugin plugin, Player p, int x, int y,
			String msg, int ping) {
		setTabString(plugin, p, x, y, msg, ping, null);
	}

	public static void setTabString(Plugin plugin, Player p, int x, int y) {
		setTabString(plugin, p, x, y);
	}

	public static void setTabString(Plugin plugin, Player p, int x, int y,
			String msg, int ping, WrappedGameProfile gameProfile) {
		try {
			if (p != null) {
				if (protocolManager.getProtocolVersion(p) >= 47) {
					TabObject47 tabo = getTab47(p);
					if (gameProfile != null) {
						String ip = "http://textures.minecraft.net/texture/5163dafac1d91a8c91db576caac784336791a6e18d8f7f62778fc47bf146b6";
						gameProfile.getProperties()
								.put("textures",
										new WrappedSignedProperty("textures",
												buildBase64For(
														gameProfile.getId(),
														gameProfile.getName(),
														ip), ""));
					}
					tabo.setTab(plugin, x, y, msg, ping, gameProfile);
					playerTab47.put(p.getName(), tabo);
				} else {
					TabObject tabo = getTab(p);
					tabo.setTab(plugin, x, y, msg, ping);
					playerTab.put(p.getName(), tabo);
				}
			} else {
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String buildBase64For(String uuid, String name, String url) {
		StringBuilder builder = new StringBuilder();

		builder.append("{\"timestamp\":" + System.currentTimeMillis() + ",");
		builder.append("\"profileId\":\"" + uuid + "\",");
		builder.append("\"profileName\":\"" + name + "\",");
		builder.append("\"isPublic\":true,");
		builder.append("\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}");

		return encodeBase64(builder.toString().getBytes());
	}

	public static String encodeBase64(byte[] bytes) {
		return Base64.encodeBase64String(bytes);
	}

	public static void updatePlayer(Player p) {
		if (!p.isOnline()) {
			return;
		}
		r = 0;
		e = 0;
		if (protocolManager.getProtocolVersion(p) >= 47) {
			TabObject47 tabo = (TabObject47) playerTab47.get(p.getName());
			TabHolder47 tab = tabo.getTab();
			if (tab == null) {
				return;
			}
			clearTab(p);
			for (int b = 0; b < tab.maxv; b++) {
				for (int a = 0; a < tab.maxh; a++) {
					if (tab.tabs[a][b] == null) {
						tab.tabs[a][b] = nextNull();
					}
					String msg = tab.tabs[a][b];
					int ping = tab.tabPings[a][b];
					WrappedGameProfile gameProfile = tab.tabGameProfiles[a][b];
					if (gameProfile != null) {
						String ip = "http://textures.minecraft.net/texture/5163dafac1d91a8c91db576caac784336791a6e18d8f7f62778fc47bf146b6";
						gameProfile.getProperties()
								.put("textures",
										new WrappedSignedProperty("textures",
												buildBase64For(
														gameProfile.getId(),
														gameProfile.getName(),
														ip), ""));
					}
					addPacket(
							p,
							msg == null ? " " : msg.substring(0,
									Math.min(msg.length(), 16)),
							getSlotId(b, a), gameProfile, true, ping);
				}
			}
			flushPackets(p, tab.getCopy());
		} else {
			TabObject tabo = (TabObject) playerTab.get(p.getName());
			TabHolder tab = tabo.getTab();
			if (tab == null) {
				return;
			}
			clearTab(p);
			for (int b = 0; b < tab.maxv; b++) {
				for (int a = 0; a < tab.maxh; a++) {
					if (tab.tabs[a][b] == null) {
						tab.tabs[a][b] = nextNull();
					}
					String msg = tab.tabs[a][b];
					int ping = tab.tabPings[a][b];
					addPacket(
							p,
							msg == null ? " " : msg.substring(0,
									Math.min(msg.length(), 16)), 0, null, true,
							ping);
				}
			}
			flushPackets(p, tab.getCopy());
		}
	}

	public static void clearTab(Player p) {
		if (!p.isOnline()) {
			return;
		}
		int a;
		String msg;
		WrappedGameProfile gameProfile;
		if (protocolManager.getProtocolVersion(p) >= 47) {
			TabHolder47 tabold = (TabHolder47) playerTabLast47.get(p.getName());
			if (tabold != null) {
				for (int b = 0; b < tabold.maxv; b++) {
					for (a = 0; a < tabold.maxh; a++) {
						msg = tabold.tabs[a][b];
						gameProfile = tabold.tabGameProfiles[a][b];
						if (gameProfile != null) {
							String ip = "http://textures.minecraft.net/texture/5163dafac1d91a8c91db576caac784336791a6e18d8f7f62778fc47bf146b6";
							Property prop = (Property) gameProfile.getProperties().get("signature");
							gameProfile.getProperties()
									.put("textures",
											new WrappedSignedProperty("textures",
													buildBase64For(
															gameProfile.getId(),
															gameProfile.getName(),
															ip), prop.toString()));
							
						}
						addPacket(p,
								msg.substring(0, Math.min(msg.length(), 16)),
								getSlotId(b, a), gameProfile, false, 0);
					}
				}
			}
		} else {
			TabHolder tabold = (TabHolder) playerTabLast.get(p.getName());
			if (tabold != null) {
				for (String[] s : tabold.tabs) {
					for (String message : s) {
						if (message != null) {
							addPacket(
									p,
									message.substring(0,
											Math.min(message.length(), 16)), 0,
									null, false, 0);
						}
					}
				}
			}
		}
	}

	public static void updateAll() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			updatePlayer(p);
		}
	}

	public static String nextNull() {
		String s = "";
		for (int a = 0; a < r; a++) {
			s = " " + s;
		}
		s = s + "§" + colors[e];
		e += 1;
		if (e > 14) {
			e = 0;
			r += 1;
		}
		return s;
	}

	@EventHandler
	public void PlayerLeave(PlayerQuitEvent e) {
		playerTab.remove(e.getPlayer().getName());
		playerTabLast.remove(e.getPlayer().getName());
		playerTab47.remove(e.getPlayer().getName());
		playerTabLast47.remove(e.getPlayer().getName());
	}

	@EventHandler
	public void PlayerKick(PlayerKickEvent e) {
		playerTab.remove(e.getPlayer().getName());
		playerTabLast.remove(e.getPlayer().getName());
		playerTab47.remove(e.getPlayer().getName());
		playerTabLast47.remove(e.getPlayer().getName());
	}

	@Deprecated
	public static int getVertSize() {
		return vertTabSize;
	}

	@Deprecated
	public static int getHorizSize() {
		return horizTabSize;
	}

	public static int getVertSize(int protocol) {
		if (protocol >= 47) {
			return vertTabSize47;
		}
		return vertTabSize;
	}

	public static int getHorizSize(int protocol) {
		if (protocol >= 47) {
			return horizTabSize47;
		}
		return horizTabSize;
	}

	public static int getSlotId(int x, int y) {
		if (y == 0) {
			return 11 + x;
		}
		if (y == 1) {
			return 31 + x;
		}
		if (y == 2) {
			return 51 + x;
		}
		if (y == 3) {
			return 71 + x;
		}
		return 0;
	}
}