package com.skp.ps1.ykh.rtcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RtcpCheckInboundHandler extends ChannelInboundHandlerAdapter{

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// TODO Auto-generated method stub
		
		ByteBuf message = ((ByteBuf)msg).copy();
		
		byte magicNum = message.readByte();
		byte channelNum = message.readByte();
		
		if(magicNum==0x54){
			System.err.println("TEARDOWN");
		}
		if(magicNum==0x24){
			ByteBuf rtcpResponse = Unpooled.buffer();
			//short length = rtcpPacket.;
			
			rtcpResponse.writeByte(magicNum);	// RTCP over TCP magic
			rtcpResponse.writeByte(channelNum);	// channelNumber Video=0x01 Audio=0x03
			rtcpResponse.writeShort(60);		// RTCP packet size 60으로 고정
			rtcpResponse.writeByte(0x80);		// RTCP V P RC
			rtcpResponse.writeByte(0xc8);		// RTCP PT
			rtcpResponse.writeShort(6);			// RTCP Header + SenderInfo length
			rtcpResponse.writeInt(0);			// RTCP SSRC identifier
			rtcpResponse.writeInt(0);			// RTCP NTP Timestamp Most Significant Word
			rtcpResponse.writeInt(0);			// RTCP NTP Timestamp Least Significant Word
			rtcpResponse.writeInt(0);			// RTP Timestamp
			rtcpResponse.writeInt(0);			// sender's Packet Count
			rtcpResponse.writeInt(0);			// sender's Octet Count
			
			rtcpResponse.writeByte(0x81);		// RTCP SDES header
			rtcpResponse.writeByte(0xca);		// RTCP SDES PT
			rtcpResponse.writeShort(7);			// RTCP SDES length
			rtcpResponse.writeInt(0);			// RTCP SSRC/CSRC Identifier - RTP에서 SSRC의 identifier값이 들어간다!
			rtcpResponse.writeByte(0x01);		// RTCP CNAME Type
			rtcpResponse.writeByte(0x15);		// RTCP CNAME Length
			
			String text = "SKP5001919MN001.local";
			byte[] bytes = text.getBytes();
			rtcpResponse.writeBytes(bytes);
			
			rtcpResponse.writeByte(0x00);
			
			System.out.println("RTCP Packet OUT = "+ByteBufUtil.hexDump(rtcpResponse));
			//ctx.writeAndFlush(rtcpResponse);
		}else{
			System.out.println("RTSP Packet IN = "+ByteBufUtil.hexDump((ByteBuf)msg));
			super.channelRead(ctx, msg);
		}
	}
}