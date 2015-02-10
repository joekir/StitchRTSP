package com.skp.ps1.ykh;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.rtsp.RtspHeaders;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

public class ServerHandler extends SimpleChannelInboundHandler<DefaultHttpRequest>{

    private	static final String ROOT_DIRECTORY 			= "/Users/5001919/Contents";
    Thread rtpThread = null;
    
    @Override
	public void channelRead0(ChannelHandlerContext ctx, DefaultHttpRequest request)
			throws Exception {
		// TODO Auto-generated method stub
    	
    	if(request.getMethod().equals(RtspMethods.TEARDOWN))
    		handleRtspTEARDOWNMethod(ctx,request);
    	else if(request.getMethod().equals(RtspMethods.OPTIONS))
    		handleRtspOPTIONSMethod(ctx,request);
    	else if(request.getMethod().equals(RtspMethods.DESCRIBE))
    		handleRtspDESCRIBEMethod(ctx,request);
    	else if(request.getMethod().equals(RtspMethods.SETUP))
    		handleRtspSETUPMethod(ctx,request);
    	else if(request.getMethod().equals(RtspMethods.PLAY))
    		handleRtspPLAYMethod(ctx,request);
    	else if(request.getMethod().equals(RtspMethods.PAUSE))
    		handleRtspPAUSEMethod(ctx,request);
    	else
    		System.err.println("Exception in ServerHandler");
    }

	private void handleRtspTEARDOWNMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("TEARDOWN");
		rtpThread.interrupt();
		System.err.println("RTP 전송 중단!");
	}

	private void handleRtspPAUSEMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("PAUSE NOT YET");
	}

	private void handleRtspPLAYMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("PLAY");
		
		FullHttpResponse response = null;
		String sessionID = request.headers().get(RtspHeaders.Names.SESSION);
		
		String uri = request.getUri();
		String path = uri.substring(uri.indexOf("8554")+4);
		String filePath = changeUriToAbsolutePath(path);
		
		if (filePath.endsWith("/"))
			filePath = filePath.substring(0, filePath.length() - 1);

		File file = new File(filePath);
		
		if (file.isDirectory() || !file.exists()) {
			return;
		}
		
		long rtpTime = System.currentTimeMillis();
		int trackID = 1;
		String rtpInfo = "url="+uri+"/trackID="+trackID+";seq=10000;rtptime="+rtpTime;
		
		response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
		response.headers().set(RtspHeaders.Names.CSEQ,request.headers().get(RtspHeaders.Names.CSEQ));
		response.headers().set(RtspHeaders.Names.SESSION,sessionID);
		response.headers().set(RtspHeaders.Names.RTP_INFO,rtpInfo);
		response.headers().set(RtspHeaders.Names.RANGE,"npt=0.000-");
		
		writeResponseWithFuture(ctx, request, response);
        
		RtpPacketization rtpPacket = new RtpPacketization(ctx, filePath);
		rtpThread = new Thread(rtpPacket);
		rtpThread.start();
	}
	private void handleRtspSETUPMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("SETUP");
		
		FullHttpResponse response = null;
				
		URI uri = null;
		try {
			uri = new URI(request.getUri());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String path = uri.getPath();
        String filePath = changeUriToAbsolutePath(path).substring(0,changeUriToAbsolutePath(path).lastIndexOf("/"));
        
		if(filePath.endsWith("/"))
			filePath = filePath.substring(0,filePath.length()-1);
		
		File file = new File(filePath);
        if (file.isDirectory() || !file.exists()) {
                return;
        }
        
        String localAddress = ctx.channel().localAddress().toString();
        String remoteAddress = ctx.channel().remoteAddress().toString();
        String remotePort	= remoteAddress.substring(remoteAddress.indexOf(":")+1);
        String interleaved	= request.headers().get(RtspHeaders.Names.TRANSPORT).substring(request.headers().get(RtspHeaders.Names.TRANSPORT).indexOf("interleaved"));
        String serverTransport = "RTP/AVP/TCP;unicast;destination="+localAddress.substring(1,localAddress.indexOf(String.valueOf(ServerMain.PORT))-1) 
        					+ ";source="+remoteAddress.substring(1,remoteAddress.indexOf(remotePort)-1)
        					+ ";interleaved="+interleaved.substring(interleaved.indexOf("=")+1);
        
        response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        response.headers().set(RtspHeaders.Names.CSEQ, request.headers().get(RtspHeaders.Names.CSEQ));
        response.headers().set(RtspHeaders.Names.TRANSPORT, serverTransport);
        response.headers().set(RtspHeaders.Names.SESSION,"1");
        
		writeResponseWithFuture(ctx,request,response);

	}

	private void handleRtspDESCRIBEMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("DESCRIBE");

		String uri = request.getUri();
		String ipAddress = ctx.channel().localAddress().toString();
		ipAddress = ipAddress.substring(1,ipAddress.indexOf(":"));
		String path = uri.substring(uri.indexOf("8554")+4);
		String filePath = changeUriToAbsolutePath(path);
		generateSDP(filePath,ipAddress);
		
		String sdp = "";
		String s;
		try {
			FileReader fileReader = new FileReader(new File(filePath+".sdp"));
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while((s=bufferedReader.readLine())!=null){
				sdp=sdp+s+"\n";
			}
			bufferedReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{

		}
		
		ByteBuf buffer = Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8);
		
		FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
		response.headers().set(RtspHeaders.Names.CSEQ, request.headers().get(RtspHeaders.Names.CSEQ));
		response.headers().set(RtspHeaders.Names.CONTENT_BASE,uri);
		response.headers().set(RtspHeaders.Names.CONTENT_TYPE,"application/sdp");
		response.headers().set(RtspHeaders.Names.CONTENT_LENGTH,String.valueOf(sdp.length()));
		response.content().writeBytes(buffer);
        buffer.release();
        
		writeResponseWithFuture(ctx,request,response);
	}

	private void generateSDP(String filePath, String ipAddress) {
		// TODO Auto-generated method stub
		File file = new File(filePath);

		if(!file.exists()){
			System.err.println("File Not Exists");
			return;
		}
		
		StringBuilder sdpStr = new StringBuilder()
		.append("v=0\n")									/* 세션 버전 */
		.append("o=- "+System.currentTimeMillis()+" "		/* 세션 주인 */
		+(System.currentTimeMillis()+1)+" IN IP4 "+ipAddress+"\n") 
		.append("s=SKPlanet Netty Streaming Server\n")		/* 세션 이름 */
		.append("i="+filePath								/* 세션 정보 */
				.substring(filePath.lastIndexOf("/")+1)+"\n")							
		.append("t=0 0\n")									/* 세션 유효시간 */	
		.append("a=sendonly\n")								/* 송신만 가능 */
		.append("a=control:*\n")							/* 세션 제어 */
		
		.append("m=video 0 RTP/AVP 98\n")					/* 미디어 정보 */
		.append("a=rtpmap:98 H264/90000\n")					/* 미디어의 상세정보 */
		.append("a=ftmp:98 profile-level-id=42A01E;")
		.append("a=control:trackID=1\n")					/* 미디어 제어 접근 변수 명시 */
		.append("b=RS:0\n")									/* bandwidth RTCP Sender */
		.append("b=RR:0\n")									/* bandwidth RTCP Receiver */
		
		.append("m=audio 0 RTP/AVP 96\n")					/* 미디어 정보 */
		.append("a=rtpmap:96 MP4A-LATM/44100/2\n")			/* 미디어의 상세정보 */
		.append("a=ftmp:96 profile-level-id=1")
		.append("a=control:trackID=2\n")					/* 미디어 제어 접근 변수 명시 */
		.append("b=RS:0\n")									/* bandwidth RTCP Sender */
		.append("b=RR:0\n");								/* bandwidth RTCP Receiver */

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(filePath+".sdp"));
			bw.write(sdpStr.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleRtspOPTIONSMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
		// TODO Auto-generated method stub
		System.out.println("OPTIONS");
		String options = RtspMethods.OPTIONS.name()+", "+RtspMethods.DESCRIBE.name()+", "+RtspMethods.SETUP.name()
				+", "+RtspMethods.TEARDOWN.name()+", "+RtspMethods.PLAY.name()+", "+RtspMethods.PAUSE.name();
		
		FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);

		response.headers().set(RtspHeaders.Names.CSEQ,request.headers().get(RtspHeaders.Names.CSEQ));
		response.headers().set(RtspHeaders.Names.PUBLIC,options);
		
		writeResponseWithFuture(ctx,request,response);
		
	}

	private void writeResponseWithFuture(ChannelHandlerContext ctx,
			DefaultHttpRequest request, HttpResponse response) {
		// TODO Auto-generated method stub
		ChannelFuture responseFuture;
        ChannelFuture lastresponseFuture;
        
        responseFuture = ctx.write(response,ctx.newProgressivePromise());
        lastresponseFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        
        responseFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " "+future.cause()+" "+future.isCancelled()+" "+future.isDone()+" "+future.isSuccess()+" "/*+future.sync()*/);
            }

			@Override
			public void operationProgressed(ChannelProgressiveFuture paramF,
					long paramLong1, long paramLong2) throws Exception {
				// TODO Auto-generated method stub
				
			}
        });
        if (!HttpHeaders.isKeepAlive(request)) {
        	lastresponseFuture.addListener(ChannelFutureListener.CLOSE);
        }
		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}
	
	private static String changeUriToAbsolutePath(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        return ROOT_DIRECTORY+uri;
    }
}
