package ch.spacebase.mc.protocol.test;

import java.util.Arrays;

import ch.spacebase.mc.auth.GameProfile;
import ch.spacebase.mc.auth.exceptions.AuthenticationException;
import ch.spacebase.mc.protocol.MinecraftProtocol;
import ch.spacebase.mc.protocol.ProtocolConstants;
import ch.spacebase.mc.protocol.ProtocolMode;
import ch.spacebase.mc.protocol.ServerLoginHandler;
import ch.spacebase.mc.protocol.data.status.PlayerInfo;
import ch.spacebase.mc.protocol.data.status.ServerStatusInfo;
import ch.spacebase.mc.protocol.data.status.VersionInfo;
import ch.spacebase.mc.protocol.data.status.handler.ServerInfoBuilder;
import ch.spacebase.mc.protocol.data.status.handler.ServerInfoHandler;
import ch.spacebase.mc.protocol.data.status.handler.ServerPingTimeHandler;
import ch.spacebase.mc.protocol.packet.ingame.client.ClientChatPacket;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerChatPacket;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerJoinGamePacket.Difficulty;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerJoinGamePacket.GameMode;
import ch.spacebase.mc.protocol.packet.ingame.server.ServerJoinGamePacket.WorldType;
import ch.spacebase.mc.util.message.ChatColor;
import ch.spacebase.mc.util.message.ChatFormat;
import ch.spacebase.mc.util.message.Message;
import ch.spacebase.packetlib.Client;
import ch.spacebase.packetlib.Server;
import ch.spacebase.packetlib.Session;
import ch.spacebase.packetlib.event.server.ServerAdapter;
import ch.spacebase.packetlib.event.server.SessionAddedEvent;
import ch.spacebase.packetlib.event.session.DisconnectedEvent;
import ch.spacebase.packetlib.event.session.PacketReceivedEvent;
import ch.spacebase.packetlib.event.session.SessionAdapter;
import ch.spacebase.packetlib.tcp.TcpSessionFactory;

public class Test {
	
	private static final boolean SPAWN_SERVER = true;
	private static final String HOST = "127.0.0.1";
	private static final int PORT = 25565;
	private static final String USERNAME = "Username";
	private static final String PASSWORD = "Password";

	public static void main(String[] args) {
		if(SPAWN_SERVER) {
			Server server = new Server(HOST, PORT, MinecraftProtocol.class, new TcpSessionFactory());
			server.setGlobalFlag(ProtocolConstants.VERIFY_USERS_KEY, true);
			server.setGlobalFlag(ProtocolConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
				@Override
				public ServerStatusInfo buildInfo() {
					return new ServerStatusInfo(new VersionInfo("1.7.2", 4), new PlayerInfo(100, 0, new GameProfile[0]), new Message("Hello world!"), null);
				}
			});
			
			server.setGlobalFlag(ProtocolConstants.SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
				@Override
				public void loggedIn(Session session) {
					session.send(new ServerJoinGamePacket(0, false, GameMode.SURVIVAL, 0, Difficulty.PEACEFUL, 10, WorldType.DEFAULT));
				}
			});
			
			server.addListener(new ServerAdapter() {
				@Override
				public void sessionAdded(SessionAddedEvent event) {
					event.getSession().addListener(new SessionAdapter() {
						@Override
						public void packetReceived(PacketReceivedEvent event) {
							if(event.getPacket() instanceof ClientChatPacket) {
								ClientChatPacket packet = event.getPacket();
								GameProfile profile = event.getSession().getFlag(ProtocolConstants.PROFILE_KEY);
								System.out.println(profile.getName() + ": " + packet.getMessage());
								Message msg = new Message("Hello, ");
								msg.setColor(ChatColor.GREEN);
								Message name = new Message(profile.getName());
								name.setColor(ChatColor.AQUA);
								name.setFormat(ChatFormat.UNDERLINED, true);
								Message end = new Message("!");
								msg.addSubMessage(name);
								msg.addSubMessage(end);
								event.getSession().send(new ServerChatPacket(msg));
							}
						}
					});
				}
			});
			
			server.bind();
		}
		
		status();
		login();
	}
	
	private static void status() {
		MinecraftProtocol protocol = new MinecraftProtocol(ProtocolMode.STATUS);
		Client client = new Client(HOST, PORT, protocol, new TcpSessionFactory());
		client.getSession().setFlag(ProtocolConstants.SERVER_INFO_HANDLER_KEY, new ServerInfoHandler() {
			@Override
			public void handle(ServerStatusInfo info) {
				System.out.println("Version: " + info.getVersionInfo().getVersionName() + ", " + info.getVersionInfo().getProtocolVersion());
				System.out.println("Player Count: " + info.getPlayerInfo().getOnlinePlayers() + " / " + info.getPlayerInfo().getMaxPlayers());
				System.out.println("Players: " + Arrays.toString(info.getPlayerInfo().getPlayers()));
				System.out.println("Description: " + info.getDescription().getRawText());
				System.out.println("Icon: " + info.getIcon());
			}
		});
		
		client.getSession().setFlag(ProtocolConstants.SERVER_PING_TIME_HANDLER_KEY, new ServerPingTimeHandler() {
			@Override
			public void handle(long pingTime) {
				System.out.println("Server ping took " + pingTime + "ms");
			}
		});
		
		client.getSession().connect();
		while(client.getSession().isConnected()) {
			try {
				Thread.sleep(5);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void login() {
		MinecraftProtocol protocol = null;
		try {
			protocol = new MinecraftProtocol(USERNAME, PASSWORD);
		} catch(AuthenticationException e) {
			e.printStackTrace();
			return;
		}
		
		Client client = new Client(HOST, PORT, protocol, new TcpSessionFactory());
		client.getSession().addListener(new SessionAdapter() {
			@Override
			public void packetReceived(PacketReceivedEvent event) {
				if(event.getPacket() instanceof ServerJoinGamePacket) {
					event.getSession().send(new ClientChatPacket("Hello, this is a test of MCProtocolLib."));
				} else if(event.getPacket() instanceof ServerChatPacket) {
					System.out.println(event.<ServerChatPacket>getPacket().getRawMessage());
					event.getSession().disconnect("Finished");
				}
			}
			
			@Override
			public void disconnected(DisconnectedEvent event) {
				System.out.println("Disconnected: " + new Message(event.getReason(), true).getRawText());
			}
		});
		
		client.getSession().connect();
	}

}
