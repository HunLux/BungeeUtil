package dev.wolveringer.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants.Direction;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import dev.wolveringer.BungeeUtil.ClientVersion;
import dev.wolveringer.BungeeUtil.PacketHandleEvent;
import dev.wolveringer.BungeeUtil.PacketLib;
import dev.wolveringer.BungeeUtil.packets.Packet;
import dev.wolveringer.packet.ByteBuffCreator;
import dev.wolveringer.packet.PacketHandle;
import dev.wolveringer.profiler.Profiler;
import dev.wolveringer.strings.Messages;

public class Encoder extends MinecraftEncoder {

	@Getter
	@Setter
	private IInitialHandler initHandler;
	@Getter
	private Protocol protocoll = Protocol.HANDSHAKE;
	@Getter
	private int version;
	@Getter
	private ClientVersion clientVersion = ClientVersion.UnderknownVersion;
	@Getter
	private	boolean server;

	public Encoder(Protocol protocol, boolean server, int protocolVersion, IInitialHandler i) {
		super(protocol, server, protocolVersion);
		this.initHandler = i;
		this.server = server;
		this.clientVersion = ClientVersion.fromProtocoll(protocolVersion);
	}
	@Override
	protected void encode(ChannelHandlerContext ctx, DefinedPacket msg, ByteBuf out) throws Exception {
		Profiler.encoder_timings.start(Messages.getString("network.timings.encoder.handle"));
		if(msg instanceof LoginSuccess)
			initHandler.isConnected = true;
		ByteBuf in;
		super.encode(ctx, msg, in = Unpooled.buffer());

		Profiler.encoder_timings.start(Messages.getString("network.timings.encoder.create.packet"));
		Packet packet = Packet.getPacket(clientVersion.getProtocollVersion(),protocoll, Direction.TO_CLIENT, in, initHandler.getPlayer());
		Profiler.encoder_timings.stop(Messages.getString("network.timings.encoder.create.packet"));
		if(packet == null){
			ByteBuffCreator.copy(in, out);
			in.release();
			return;
		}
		in.release();
		PacketHandleEvent e = new PacketHandleEvent(packet, initHandler.getPlayer());
		Profiler.encoder_timings.start(Messages.getString("network.timings.encoder.handle.intern"));
		boolean intern = PacketHandle.handlePacket(e);
		Profiler.encoder_timings.stop(Messages.getString("network.timings.encoder.handle.intern"));
		if(!intern){
			Profiler.encoder_timings.start(Messages.getString("network.timings.encoder.handle.extern"));
			PacketLib.handle(e);
			Profiler.encoder_timings.stop(Messages.getString("network.timings.encoder.handle.extern"));
			if(!e.isCancelled()){
				Profiler.encoder_timings.start(Messages.getString("network.timings.encoder.write.writeNewbyteBuff"));

				e.getPacket().writeToByteBuff(out,ClientVersion.fromProtocoll(initHandler.getVersion())); //write direct to out
				//ByteBuffCreator.copy(buf, out);
				//buf.release();
				
				Profiler.encoder_timings.stop(Messages.getString("network.timings.encoder.write.writeNewbyteBuff"));
			}
			else
			{
				
			}
		}
		Profiler.encoder_timings.stop(Messages.getString("network.timings.encoder.handle"));
	}
	
	public void setProtocol(Protocol protocol) {
		super.setProtocol(protocol);
		this.protocoll = protocol;
	}

	public void setProtocolVersion(int protocol) {
		super.setProtocolVersion(protocol);
		this.version = protocol;
		this.clientVersion = ClientVersion.fromProtocoll(protocol);
	}
}
