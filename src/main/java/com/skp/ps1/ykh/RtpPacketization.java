package com.skp.ps1.ykh;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.mp4.impl.MP4Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtpPacketization implements Runnable {
	
	private static Logger log = LoggerFactory.getLogger(RtpPacketization.class);
	
	ChannelHandlerContext ctx;
	String filePath;
	
	public RtpPacketization(ChannelHandlerContext ctx, String filePath) {
		// TODO Auto-generated constructor stub
		this.ctx = ctx;
		this.filePath = filePath;
	}
    
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("SendRtpPacket");
		try {
			MP4Reader mp4Reader = new MP4Reader(new File(filePath));
			ITag tag = null;
			int mp4FrameSize = mp4Reader.getFramesTotalSize();	// 직접 만든 getFramesTotalSize()로 비디오+오디오 프레임 수를 리턴한다.
			Short sequenceNumber = 10000;
			
			for (int t = 0; t < mp4FrameSize; t++) {
				tag = mp4Reader.readTag();
				log.debug("Tag\n{}",tag);
				IoBuffer body = tag.getBody();
				
				/***
				 * 
				 * RTP over TCP
				 * 
				 */
				//int bodyLength = tag.getBodySize();
				//short rtpLength = (short)(bodyLength+12);			// short(2byte) header(12)+payload(getBodySize())
				short rtpLength = (short)(body.limit()+12);
				ByteBuf buffer = Unpooled.buffer(); 

				byte magicNumber = (byte)0x24;					// 1byte magic number 0x24
				byte channelNumber = (byte)0x00;				// 1byte channel number 0x00(RTP)
				buffer.writeByte(magicNumber);
				System.err.println(ByteBufUtil.hexDump(buffer));
				
				buffer.writeByte(channelNumber);
				System.err.println(ByteBufUtil.hexDump(buffer));

				buffer.writeShort(rtpLength);
				System.err.println(ByteBufUtil.hexDump(buffer));

				
				/***
				 * Version/Padding/Extension/CSRC Count
				 */
				byte VnPnXnCC	= (byte)0x80;	// V=2 P=0 X=0 CC=0
				buffer.writeByte(VnPnXnCC);
				System.err.println(ByteBufUtil.hexDump(buffer));

				
				/***
				 * Marker/PayloadType
				 */
				byte MnPT= (byte)0x80;		// frame단위로 전송하기 때문에 M=0 PT=96(audio)/98(video)
				byte dataType = tag.getDataType();
				if(dataType==MP4Reader.TYPE_AUDIO)				// 8
					MnPT = (byte) (MnPT|0x60);					// 96
				else if(dataType==MP4Reader.TYPE_VIDEO)			// 9
					MnPT = (byte)(MnPT|0x62);					// 98
				else if(dataType==MP4Reader.TYPE_METADATA)		// 18
					//MnPT = (byte)0x12;						// 18
					continue;
				else{
					System.err.println("Exception in buffer");
				}
				buffer.writeByte(MnPT);
				System.err.println(ByteBufUtil.hexDump(buffer));
				
				
				/***
				 * sequenceNumber/timestamp/SSRC
				 */
				buffer.writeShort(sequenceNumber); 
				System.err.println(ByteBufUtil.hexDump(buffer));
				sequenceNumber++;
				
				int timestamp = tag.getTimestamp();
				buffer.writeInt(timestamp);			// timestamp int 그대로 전송! header size가 32bit니까
				System.err.println(ByteBufUtil.hexDump(buffer));
				
				//Todo 
				int ssrc = 0;		// ssrc가 뭔지 도저히 모르겠다...
				buffer.writeInt(ssrc);
				System.err.println(ByteBufUtil.hexDump(buffer));

				
				/***
				 *  제일 중요한 payload
				 */
				buffer.writeBytes(body.buf());
				System.err.println(ByteBufUtil.hexDump(buffer));
				
				/***
				 * Channel에 write
				 */
				//ctx.writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE);
				ctx.writeAndFlush(buffer);
				Thread.sleep(50);
				 // Now we are sure the future is completed.
				//buffer.release();
//				FullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.CONTINUE);
//				res.content().writeBytes(buffer);
//				ctx.write(res);
//				ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
				
			}			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
