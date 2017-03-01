package com.example.deocder;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import android.annotation.SuppressLint;

import android.app.Activity;

import android.content.Context;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;

import android.graphics.Canvas;

import android.graphics.Color;

import android.graphics.ImageFormat;

import android.graphics.Paint;

import android.graphics.Rect;

import android.graphics.YuvImage;

import android.media.MediaCodec;

import android.media.MediaFormat;

import android.media.MediaCodec.BufferInfo;

import android.os.Bundle;

import android.util.Log;

import android.view.Surface;

import android.view.SurfaceHolder;

import android.view.SurfaceView;

import android.view.View;

import android.view.WindowManager;

import android.widget.ImageView;

public class DecodeActivity extends Activity implements SurfaceHolder.Callback {

	private final int width = 640;

	private final int height = 480;

	private FileOutputStream file = null;//接收数据文件

	private String filename = "/sdcard/receive.h264";//接收数据文件


	private PlayerThread mPlayer = null;//解码线程

	private ReceiveThread mReceive=null;//接收线程

	private RenderThread mRender = null;//渲染线程

	private SurfaceHolder holder = null;

	private ImageView imageView = null;

	private MediaCodec decoder = null;

	//socket
	private RDUDPServer server=new RDUDPServer();//socket套接字
	
	private List<DatagramPacket> rcvqueue = new LinkedList<DatagramPacket>();//接收数据队列
	
	private ReadWriteLock mLock = new ReentrantReadWriteLock();//接收数据所
	

	private List<Integer> mOutList = new LinkedList<Integer>();//解码输出缓冲区索引队列
	
	private ReadWriteLock mOutLock = new ReentrantReadWriteLock();

	private static final int REDIRECTED_SERVERPORT = 8999;//端口


	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// SurfaceView sv = new SurfaceView(this);

		// sv.getHolder().addCallback(this);

		// Set keep screen on

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_decode);

		SurfaceView sfv_video = (SurfaceView) findViewById(R.id.sfv_video);

		imageView = (ImageView) findViewById(R.id.image_view);

		if (null == imageView) {

			Log.d("Fuck002", "can not find imageView");

		}

		holder = sfv_video.getHolder();

		holder.addCallback(this);


		server.InitSocket();



		// setContentView(new CustomView(this));
	}

	protected void onDestroy() {

		super.onDestroy();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {

			file = new FileOutputStream(filename);
			//Log.e("surfaceCreated", "File open ");
		} catch (FileNotFoundException e) {

			Log.e("surfaceCreated", "File open error"+ e.toString());

			e.printStackTrace();

		}
		
		if(mRender == null)
		{
			mRender = new RenderThread();

			mRender.start();
		}

		if (mPlayer == null) {


			mPlayer = new PlayerThread(imageView, holder.getSurface(), holder);


			mPlayer.start();

		}

		if (mReceive == null) {


			mReceive = new ReceiveThread(server);

			mReceive.start();

		}


	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		if (mPlayer != null) {

			mPlayer.interrupt();

		}
		if (mReceive != null) {

			mReceive.interrupt();

		}
		if (mRender != null) {

			mRender.interrupt();

		}
		try {

			file.flush();

			file.close();


		} catch (IOException e) {

			Log.d("File", "File close error");

			e.printStackTrace();

		}
		decoder.stop();//lxj+
		decoder.release();//lxj+
	}
	private class ReceiveThread extends Thread{

		private RDUDPServer server=null;

		public ReceiveThread(RDUDPServer server){

			this.server=server;		

		}
		
		public int bytesToInt(byte[] src, int offset) {

			int value;

			value = (int) ((src[offset] & 0xFF)

					| ((src[offset + 1] & 0xFF) << 8)

					| ((src[offset + 2] & 0xFF) << 16)

					| ((src[offset + 3] & 0xFF) << 24));

			return value;

		}
		
		public void run() {
			
			List<DatagramPacket> framelist = new ArrayList<DatagramPacket>();
//			byte[] h264frame = new byte[1024*1024];

			while(true){
				
				try {
					
					byte[] h264=new byte[1024];

					if(h264!=null)
					{
						DatagramPacket packet = new DatagramPacket(h264, h264.length);
						
						if(packet!=null){
							
							server.mServer.receive(packet);
							//file.write(h264,8,packet.getLength()-8);//原始
							file.write(h264,0,packet.getLength());//接收长度
							//Log.e("ReceiveTh","recv_packet size:"+packet.getLength());
							int len = bytesToInt(packet.getData(), 4);
							Log.e("ReceiveTh","...rcvqueue_addr:"+len+"rcvqueue size:"+rcvqueue.size()+", flag"+packet.getData()[0]);
							if(packet.getData()!=null)
							{
								if(packet.getData()[0] == 0) //不拆包一帧
								{
									byte[] h264frame = new byte[len];
									System.arraycopy(packet.getData(), 8, h264frame, 0, len);//lxj
									DatagramPacket frmpkt = new DatagramPacket(h264frame, len);
									frmpkt.setLength(len);
								
									mLock.writeLock().lock();
									rcvqueue.add(frmpkt);
									mLock.writeLock().unlock();
								}
								else
								{							
									framelist.add(packet);//ly
								
									if(packet.getData()[0]==3)
									{
										
										int pos = 0;
										int frmlen = 0;
										for(int i=0; i<framelist.size(); i++)
										{
											frmlen += (framelist.get(i).getLength()-8);//ly
											
											
										}
										byte[] h264frame = new byte[frmlen];
										
										while(framelist.size() > 0)
										{
											DatagramPacket pkt = framelist.remove(0);//ly
										
											System.arraycopy(pkt.getData(), 8, h264frame, pos, pkt.getLength()-8);//ly
											
											pos += (pkt.getLength()-8);//ly
											
										}
										DatagramPacket frmpkt = new DatagramPacket(h264frame, frmlen);
										frmpkt.setLength(pos);
										
										if(frmpkt.getData()[0]!=0||frmpkt.getData()[1]!=0||frmpkt.getData()[2]!=0||frmpkt.getData()[3]!=1)
											Log.e("defile:","write file:"+Arrays.toString(frmpkt.getData()));
										mLock.writeLock().lock();
										rcvqueue.add(frmpkt);
										mLock.writeLock().unlock();
									}
								}
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e("ReceiveTh", "receive error: "+e.toString());
					e.printStackTrace();

				}finally{

				}
			}
		}
	}

	private class RenderThread extends Thread
	{
		@SuppressLint("NewApi")
		@Override
		public void run() 
		{
			while(true)
			{
				int idx = -1;				
				
				if(mOutList.size() > 0)
				{
					mOutLock.writeLock().lock();
					idx = mOutList.remove(0);
					mOutLock.writeLock().unlock();
				}
				
				//lxj end
				
				if(idx >= 0)
				{
					decoder.releaseOutputBuffer(idx, true);
					Log.e("Render","渲染");
					try {
						Thread.sleep(32);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
					continue;



			}
		}
	}

	private class PlayerThread extends Thread {


		private ImageView imageView = null;

		private Surface surface = null;

		private SurfaceHolder surfaceHolder = null;

		public PlayerThread(ImageView imageView2, Surface surface,
				SurfaceHolder surfaceHolder)  {

			this.imageView = imageView2;

			this.surface = surface;

			this.surfaceHolder = surfaceHolder;


		}

		@SuppressLint("NewApi")
		@Override
		public void run() {
			
			Log.e("Fuck", "start run");

			MediaFormat mediaFormat = MediaFormat.createVideoFormat(
					"video/avc", width, height);

			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);

			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);

			decoder = MediaCodec.createDecoderByType("video/avc");

			decoder.configure(mediaFormat, surface, null, 0);

			// decoder.configure(mediaFormat, null, null, 0);

			decoder.start();
			
			Log.e("PlayTh", "decoder start: ");

			// new BufferInfo();

			ByteBuffer[] inputBuffers = decoder.getInputBuffers();

			ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

			if (null == inputBuffers) {

				Log.d("PlayTh", "null == inputBuffers");

			}

			if (null == outputBuffers) {

				Log.d("PlayTh", "null == outbputBuffers 111");

			}

			int mCount = 0;


			List<Integer> inputidxarr = new LinkedList<Integer>();

			for (;;) {
				
				try{	
					//ly
					while(true)
					{
						int idx = decoder.dequeueInputBuffer(0);

						if(idx >= 0)
						{
							
							inputidxarr.add(idx);
							
						}

						else
							break;
					}
//ly end
					
					while(inputidxarr.size()>0 && rcvqueue.size()>0)
					
					{

						Log.e("Player","Do decode");
						
						mLock.writeLock().lock();
						
						DatagramPacket dp = rcvqueue.remove(0);//rcvqueue.take();
						
						mLock.writeLock().unlock();
						
						int idx = inputidxarr.remove(0);//ly
								
						byte[] h264data = dp.getData();
						if(h264data[0]!=0||h264data[1]!=0||h264data[2]!=0||h264data[3]!=1)
							Log.e("player:","packet:"+h264data);

						
						
						ByteBuffer inputBuffer = decoder.getInputBuffers()[idx];//inputBuffers[idx];
						
						inputBuffer.put(h264data);//socket接收数据h264data
						
						decoder.queueInputBuffer(idx, 0, h264data.length,
								mCount * 1000000 / 20, 0);

						
					}


					while(true)
					{
						BufferInfo info = new BufferInfo();
						int outidx = decoder.dequeueOutputBuffer(info, 0);

						if(outidx >= 0)
						{
							mOutLock.writeLock().lock();
							
							mOutList.add(outidx);
							
							mOutLock.writeLock().unlock();


						}
						else
						{
							if(outidx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
							{
								Log.i("decoder", "INFO_OUTPUT_BUFFERS_CHANGED");
							}
							else if(outidx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
							{
								Log.i("decoder", "INFO_OUTPUT_FORMAT_CHANGED");
							}
							else
							{
								break;
							}
						}

					}
				}
				
				catch(Exception e)
				{
					e.printStackTrace();
					continue;
				}
				//				
			}// end of for
		}// end of run
		
		

		public Bitmap Bytes2Bimap(byte[] b) {

			if (b.length != 0) {

				return BitmapFactory.decodeByteArray(b, 0, b.length);

			} else {

				return null;

			}

		}

		public Bitmap decodeToBitMap(byte[] data) {

			Bitmap bmp = null;

			try {

				YuvImage image = new YuvImage(data, ImageFormat.NV21, width,
						width, null);

				if (image != null) {

					Log.d("Fuck", "image != null");

					ByteArrayOutputStream stream = new ByteArrayOutputStream();

					image.compressToJpeg(new Rect(0, 0, width, width), 80,
							stream);

					bmp = BitmapFactory.decodeByteArray(stream.toByteArray(),
							0, stream.size());

					stream.close();

				}

			} catch (Exception ex) {

				Log.e("Fuck", "Error:" + ex.getMessage());

			}

			return bmp;

		}

		public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width,
				int height) {

			final int frameSize = width * height;

			for (int j = 0, yp = 0; j < height; j++) {

				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;

				for (int i = 0; i < width; i++, yp++) {

					int y = (0xff & ((int) yuv420sp[yp])) - 16;

					if (y < 0)

						y = 0;

					if ((i & 1) == 0) {

						v = (0xff & yuv420sp[uvp++]) - 128;

						u = (0xff & yuv420sp[uvp++]) - 128;

					}

					int y1192 = 1192 * y;

					int r = (y1192 + 1634 * v);

					int g = (y1192 - 833 * v - 400 * u);

					int b = (y1192 + 2066 * u);

					if (r < 0)

						r = 0;

					else if (r > 262143)

						r = 262143;

					if (g < 0)

						g = 0;

					else if (g > 262143)

						g = 262143;

					if (b < 0)

						b = 0;

					else if (b > 262143)

						b = 262143;

					rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)

							| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

				}

			}

		}

		public int bytesToInt(byte[] src, int offset) {

			int value;

			value = (int) ((src[offset] & 0xFF)

					| ((src[offset + 1] & 0xFF) << 8)

					| ((src[offset + 2] & 0xFF) << 16)

					| ((src[offset + 3] & 0xFF) << 24));

			return value;

		}

	}// end of class

	private class CustomView extends View {

		private Paint paint = null;

		public CustomView(Context context) {

			super(context);

			paint = new Paint();

			paint.setColor(Color.YELLOW);

			paint.setStrokeJoin(Paint.Join.ROUND);

			paint.setStrokeCap(Paint.Cap.ROUND);

			paint.setStrokeWidth(3);

		}

		@Override
		protected void onDraw(Canvas canvas) {

			canvas.drawCircle(100, 100, 90, paint);

		}

	}
	public static byte[] intToBytes( int value )   

	{   

		byte[] src = new byte[4];  

		src[3] =  (byte) ((value>>24) & 0xFF);  

		src[2] =  (byte) ((value>>16) & 0xFF);  

		src[1] =  (byte) ((value>>8) & 0xFF);    

		src[0] =  (byte) (value & 0xFF);                  

		return src;   

	}
	public int byteToInt(byte src) {

		int value;

		value = (int) (src & 0xFF);

		return value;

	}

	class RDUDPServer
	{
		DatagramSocket mServer;
		DatagramPacket packet;

		public void InitSocket()
		{
			

			try
			{

				mServer = new DatagramSocket(REDIRECTED_SERVERPORT);
				mServer.setReuseAddress(true);

				Log.e("Fuck", "init packet :");

			}
			catch(Exception e){
				Log.e("Fuck", "creat server error: "+e.toString());

			}
			
		}

	}
}