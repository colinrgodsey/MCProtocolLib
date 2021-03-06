package ch.spacebase.mc.protocol.packet.ingame.server.world;

import java.io.IOException;

import ch.spacebase.packetlib.io.NetInput;
import ch.spacebase.packetlib.io.NetOutput;
import ch.spacebase.packetlib.packet.Packet;

public class ServerUpdateTimePacket implements Packet {
	
	private long age;
	private long time;
	
	public ServerUpdateTimePacket() {
	}
	
	public ServerUpdateTimePacket(long age, long time) {
		this.age = age;
		this.time = time;
	}
	
	public long getWorldAge() {
		return this.age;
	}
	
	public long getTime() {
		return this.time;
	}

	@Override
	public void read(NetInput in) throws IOException {
		this.age = in.readLong();
		this.time = in.readLong();
	}

	@Override
	public void write(NetOutput out) throws IOException {
		out.writeLong(this.age);
		out.writeLong(this.time);
	}
	
	@Override
	public boolean isPriority() {
		return false;
	}

}
