package com.example.deocder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.FileInputStream;

import java.io.FileNotFoundException;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.http.util.EncodingUtils;

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

import android.hardware.Camera.Size;

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

	private final int width = 176;

	private final int height = 144;

	// private static String fileString = Envqironment

	// .getExternalStorageDirectory() + "/h264/test.h264";

	// private static String fileString = "/sdcard/test.h264";

	// private static String fileString =

	// "/sdcard/yangjin_and_xieaini_h264.mp4";

//	 private FileOutputStream file = null;
//	private static String filename = "/sdcard/recive.h264";


	private PlayerThread mPlayer = null;

	private SurfaceHolder holder = null;

	private ImageView imageView = null;
	

	// socket
	 private RDUDPServer server=new RDUDPServer();;

	 private byte[] h264=new byte[1000*1024];
	 private ReceiveThread mReceive=null;
	 DatagramPacket packet = null;

	private static final int REDIRECTED_SERVERPORT = 8999;
	

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
		
		packet = new DatagramPacket(h264, h264.length);

		
		// setContentView(new CustomView(this));
	}

	protected void onDestroy() {

		super.onDestroy();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {


		if (mPlayer == null) {

			mPlayer = new PlayerThread(imageView, holder.getSurface(), holder);

			mPlayer.start();

		}
		if (mReceive == null) {

			mReceive = new ReceiveThread(server,packet);

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
		try {

			server.file.flush();

			server.file.close();
			

		} catch (IOException e) {

			Log.d("Fuck", "File close error");

			e.printStackTrace();

		}

	}
	private class ReceiveThread extends Thread{
		private RDUDPServer server=null;
		private DatagramPacket packet=null;

		
		public ReceiveThread(RDUDPServer server,DatagramPacket packet){
			
			this.server=server;
			this.packet=packet;

			
		}
		public void run() {
			FileOutputStream file = null;

			 
			
			while(true){
			try {

				server.mServer.receive(packet);
				boolean ret=h264==packet.getData();
				//server.file.write(h264);
				Log.e("Fuck", "接收的是否一致 "+ret);
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("Fuck", "receive error: "+e.toString());
				e.printStackTrace();
			}
			//Log.e("Fuck", "receive end: ");
		}
		}
	}
	private class PlayerThread extends Thread {

		private MediaCodec decoder = null;

		private ImageView imageView = null;

		private Surface surface = null;

		private SurfaceHolder surfaceHolder = null;

		public PlayerThread(ImageView imageView2, Surface surface,
				SurfaceHolder surfaceHolder) {

			this.imageView = imageView2;

			this.surface = surface;

			this.surfaceHolder = surfaceHolder;

		}

		@SuppressLint("NewApi")
		@Override
		public void run() {
			Log.e("Fuck", "start run");

			MediaFormat mediaFormat = MediaFormat.createVideoFormat(
					"video/avc", width, width);

			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);

			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);

			decoder = MediaCodec.createDecoderByType("video/avc");

			decoder.configure(mediaFormat, surface, null, 0);

			// decoder.configure(mediaFormat, null, null, 0);

			decoder.start();
			Log.e("Fuck", "decoder start: ");

			// new BufferInfo();

			ByteBuffer[] inputBuffers = decoder.getInputBuffers();

			ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

			if (null == inputBuffers) {

				Log.d("Fuck", "null == inputBuffers");

			}

			if (null == outputBuffers) {

				Log.d("Fuck", "null == outbputBuffers 111");

			}

			//FileInputStream file = null;

			// try {
			//
			// file = new FileInputStream(fileString);
			//
			// } catch (FileNotFoundException e) {
			//
			// Log.d("Fuck", "open file error: " + e.toString());
			//
			// return;
			//
			// }

			int read_size = -1;

			int mCount = 0;
	///////////////////////////////////////////////////		
			 
//			try
//			{
//				server = new DatagramSocket(REDIRECTED_SERVERPORT);
//				server.setReuseAddress(true);
//				
//				Log.e("Fuck", "creat server!!!!!!!!!!!: ");
//			}
//			catch(Exception e){
//				Log.e("Fuck", "creat server erro: "+e.toString());
//			}

			for (;;) {
				//byte[] h264 = null;
				//Log.e("Fuck", "socket start: ");

				//					h264 = new byte[1000*1024];
				//					DatagramPacket packet = new DatagramPacket(h264, h264.length);
				//					Log.e("Fuck", "receive start: ");
				//					server.receive(packet);
				//					Log.e("Fuck", "receive end: ");
	
									//Log.e("Fuck", "Time Past: " + diff);
								
									
//									try {
//										rcvfile.write( diffbyte);
//									} catch (IOException e) {
//										// TODO Auto-generated catch block
//										Log.e("Fuck", "write file  error!!!!!!!!!!!");
//										e.printStackTrace();
//									}


				
				int inputBufferIndex = decoder.dequeueInputBuffer(-1);
				// Log.e("Fuck", "inputBufferIndex : " + inputBufferIndex );
				if (inputBufferIndex >= 0) {
					ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];

					inputBuffer.clear();

					inputBuffer.put(h264);
					//Log.e("Fuck", "start time:"+System.currentTimeMillis());	
					// long sample_time = ;
//					int newTime = (int) System.currentTimeMillis();
//
//					int diff = newTime - declastTime;
//
//					declastTime = newTime;
//					Log.e("Fuck", "Time Past: " + diff);
//					byte[] diffbyte=intToBytes(diff);
//					
//					try {
//						decfile.write( diffbyte);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						Log.e("Fuck", "write file  error!!!!!!!!!!!");
//						e.printStackTrace();
//					}

					decoder.queueInputBuffer(inputBufferIndex, 0, h264.length,
							mCount * 1000000 / 20, 0);

					++mCount;
					

				} else {

					Log.e("Fuck", "dequeueInputBuffer error");

				}

				ByteBuffer outputBuffer = null;

				MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

				int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo,0);
				Log.e("Fuck", "outputBufferIndex:"+outputBufferIndex);
				while (outputBufferIndex >= 0) {
					//Log.e("Fuck", "end time:"+System.currentTimeMillis());

					outputBuffer = outputBuffers[outputBufferIndex];

					decoder.releaseOutputBuffer(outputBufferIndex, true);//释放缓冲区解码的数据到surfaceview，一般到了这一步，surfaceview上就有画面了
					Log.e("Fuck", "渲染结束:"+outputBufferIndex);
					outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo,
							0);

				}

				//
				//
				// Canvas canvas = null;
				//
				// synchronized (surfaceHolder) {
				//
				// try {
				//
				// // Log.d("Fuck", "Drawing-------------");
				//
				// canvas = surfaceHolder.lockCanvas();
				//
				// //canvas.drawColor(Color.WHITE);
				//
				// //Paint p = new Paint();
				//
				// //p.setColor(Color.BLACK);
				//
				//
				//
				//
				//
				// //canvas.drawRect(r, p);
				//
				// byte[] outData = new byte[width*width*3/2];
				//
				// if(null != outputBuffer){
				//
				// outputBuffer.get(outData, 0, outData.length);
				//
				// outputBuffer.clear();
				//
				//
				//
				// //Bitmap bitmap = Bytes2Bimap(outData);
				//
				// //if(null == bitmap) Log.d("Fuck", "null == bitmap");
				//
				// //canvas.drawBitmap(bitmap, null, r, null);
				//
				// //canvas.drawText("���ǵ�" + (mCount++) + "��", 100, 310, p);
				//
				//
				//
				// //
				//
				// //Bitmap bmp = decodeToBitMap(outData);
				//
				// if(null == imageView) Log.d("Fuck", "null == imageView");
				//
				// //if(null == bmp) Log.d("Fuck", "null == bmp");
				//
				// //Log.d("Fuck", "count=" + mCount + ", bmp.width=" +
				// bmp.getWidth() + ", bmp.height="+bmp.getHeight());
				//
				// //imageView.setImageBitmap(bmp);
				//
				// Rect r = new Rect(0, 0, width, width);
				//
				// //canvas.drawBitmap(bmp, 0, 0, null);
				//
				// int[] rgb = new int[width * width];
				//
				// decodeYUV420SP(rgb, outData, width, width);
				//
				// canvas.drawBitmap(rgb, 0, width, 0, 0, width, width, false,
				// null);
				//
				//
				//
				// Thread.sleep(1000/20);//
				//
				// }
				//
				// } catch (Exception e) {
				//
				// Log.d("Fuck", "throw Exception in run");
				//
				// e.printStackTrace();
				//
				// break;
				//
				// } finally {
				//
				// if (null != canvas) {
				//
				// surfaceHolder.unlockCanvasAndPost(canvas);
				//
				// }
				//
				// }
				//
				//
				//
				// }// end of synchronized

				if (outputBufferIndex >= 0) {

					decoder.releaseOutputBuffer(outputBufferIndex, false);

				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

					outputBuffers = decoder.getOutputBuffers();

					Log.d("Fuck",
							"outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");

				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

					// Subsequent data will conform to new format.

					Log.d("Fuck",
							"outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");

				}
////////////////////////////////////////////////
//				try {
//
//					Thread.sleep(1000 / 20);
//
//				} catch (InterruptedException e) {
//
//					// TODO Auto-generated catch block
//
//					e.printStackTrace();
//
//				}

			}// end of for

//			if (null != file) {
//
//				try {
//
//					file.close();
//
//				} catch (IOException e) {
//
//					// TODO Auto-generated catch block
//
//					e.printStackTrace();
//
//				}
//
//			}

			

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
	
	
	class RDUDPServer
	{
		DatagramSocket mServer;
		DatagramPacket packet;
		 public FileOutputStream file;
		private String filename ;

		public void InitSocket()
		{
			filename = "/sdcard/recive.h264";
			
			try
			{
				
				mServer = new DatagramSocket(REDIRECTED_SERVERPORT);
				mServer.setReuseAddress(true);
				
				Log.e("Fuck", "init packet :");
				
			}
			catch(Exception e){
				Log.e("Fuck", "creat server error: "+e.toString());
				
			}
			try {

				file = new FileOutputStream(filename);
				Log.e("Fuck", "File open ");
			} catch (FileNotFoundException e) {

				Log.e("Fuck", "File open error"+ e.toString());

				e.printStackTrace();

			}
		}

	}
}