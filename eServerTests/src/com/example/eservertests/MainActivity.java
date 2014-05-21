package com.example.eservertests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.eservertests.NanoHTTPD.Response.Status;

public class MainActivity extends Activity {

	EditText txtLog;
	
	public byte[] imgNow;
	ByteArrayOutputStream outStream;
	
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;
	private Camera camera = null;
	private boolean inPreview = false;
	private boolean cameraConfigured = false;

	private Handler handler = new Handler();
	public final int PORT = 7070;
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";
	public static final String CMDSTOP = "stop";
	public static final String CMDPLAY = "play";
	public static final String SERVICECMD = "com.android.music.musicservicecommand";
	public static final String CMDNAME = "command";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		txtLog = (EditText) findViewById(R.id.txtLog);

		preview = (SurfaceView) findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		preview.setKeepScreenOn(true);
	}

	@Override
	  public void onResume() {
	    super.onResume();
	    
	    camera=Camera.open();
//	    Camera.Parameters parameters = camera.getParameters();
//        parameters.setPictureFormat(ImageFormat.NV21);
//        camera.setParameters(parameters);
	    
	    startPreview();
	    camera.setPreviewCallback(new PreviewCallback() {
			
			public void onPreviewFrame(byte[] data, Camera camera) {
			
				imgNow = data;
				//Log.d("Camera Data", "Camera " + data.length);
				
//				Parameters parameters = camera.getParameters();
//			    int imageFormat = parameters.getPreviewFormat();
//			    if (imageFormat == ImageFormat.NV21)
//			    {
//			        
//			    }
				
			}
		});
	    
	  }
	    
	  @Override
	  public void onPause() {
	    if (inPreview) {
	      camera.stopPreview();
	    }
	    
	    camera.release();
	    camera=null;
	    inPreview=false;
	          
	    super.onPause();
	  }
	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	private void initPreview(int width, int height) {
		if (camera != null && previewHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback",
						"Exception in setPreviewDisplay()", t);
				Toast.makeText(MainActivity.this, t.getMessage(),
						Toast.LENGTH_LONG).show();
			}

			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				Camera.Size size = getBestPreviewSize(width, height, parameters);

				if (size != null) {
					parameters.setPreviewSize(size.width, size.height);
					camera.setParameters(parameters);
					cameraConfigured = true;
				}
			}
		}
	}

	private void startPreview() {
		if (cameraConfigured && camera != null) {
			camera.startPreview();
			inPreview = true;
		}
	}

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			// no-op -- wait until surfaceChanged()
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			initPreview(width, height);
			startPreview();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// no-op
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public String GetClipboard() {
		String clipText = "";
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipText = (String) clipboard.getText();
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard.hasPrimaryClip())
				clipText = (String) clipboard.getPrimaryClip().getItemAt(0)
						.getText();

		}
		return clipText;
	}

	public void SetClipboard(String text) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(text);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData
					.newPlainText("simple text", text);
			clipboard.setPrimaryClip(clip);
		}
	}

	public void MusicControl(String cmd) {

		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		if (mAudioManager.isMusicActive()) {
			Intent i = new Intent(SERVICECMD);
			i.putExtra(CMDNAME, cmd);
			sendBroadcast(i);
		} else {
			Intent i = new Intent(SERVICECMD);
			i.putExtra(CMDNAME, CMDTOGGLEPAUSE);
			sendBroadcast(i);
		}

	}

	public void VolUp() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
				AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
	}

	public void VolDown() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
				AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
	}

	public void btnStartServer_Click(View v) {

		try {

			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
			final String formatedIpAddress = String.format("%d.%d.%d.%d",
					(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

//			Toast.makeText(this,
//					"Please access! http://" + formatedIpAddress + ":" + PORT,
//					1).show();
			txtLog.append("\nPlease access this URL from your web browser:\n http://" + formatedIpAddress + ":" + PORT+"\n");
			new eServer(PORT).start();

			Toast.makeText(this, "Server Started !", 1).show();

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private class eServer extends NanoHTTPD {

		final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html",
				MIME_JS = "application/javascript", MIME_CSS = "text/css",
				MIME_PNG = "image/png",
				MIME_DEFAULT_BINARY = "application/octet-stream",
				MIME_XML = "text/xml", MIME_JSON = "application/json";

		public eServer(int port) {
			super(null, port);

			// TODO Auto-generated constructor stub
		}

		public Response serveFile(String uri, Method method,
				Map<String, String> headers, Map<String, String> parms,
				Map<String, String> files) {

			final String PATH = "web";
			String filename = "/";

			filename = uri;
			if (filename.equals("/"))
				filename = "/index.html";

			InputStream in = null;

			try {
				/*
				 * String[] fs =getApplicationContext().getAssets().list("web");
				 * boolean found=false; for (String f : fs) {
				 * if(f.equals(uri.substring(1))) found=true; } if(found==false)
				 * return new Response(Status.NOT_FOUND, MIME_HTML,
				 * "Not Found !");
				 */

				in = getApplicationContext().getAssets().open(PATH + filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String mime = MIME_HTML;

			if (uri.endsWith(".html"))
				mime = MIME_HTML;
			else if (uri.endsWith(".css"))
				mime = MIME_CSS;
			else if (uri.endsWith(".js"))
				mime = MIME_JS;
			else if (uri.endsWith(".png"))
				mime = MIME_PNG;
			else if (uri.endsWith(".xml"))
				mime = MIME_XML;
			else if (uri.endsWith(".json"))
				mime = MIME_JSON;
			// else
			// mime = MIME_DEFAULT_BINARY;

			return new Response(Status.OK, mime, in);

		}

		@Override
		public Response serve(final String uri, Method method,
				Map<String, String> headers, Map<String, String> parms,
				Map<String, String> files) {

			final JSONObject cmd;
			String clipText = "";

			if (uri.startsWith("/api")) {

				if(uri.endsWith("preview")){
//					BitmapFactory.Options options = new BitmapFactory.Options();
//				    options.inMutable = true;
//					Bitmap bitmap = BitmapFactory.decodeByteArray(imgNow, 0, imgNow.length,options);
//					ByteArrayOutputStream stream = new ByteArrayOutputStream();
//					bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
					
			        YuvImage imgC = new YuvImage(imgNow,ImageFormat.NV21,camera.getParameters().getPreviewSize().width,camera.getParameters().getPreviewSize().height,null);
					outStream = new ByteArrayOutputStream();
					imgC.compressToJpeg(new Rect(0,0,camera.getParameters().getPreviewSize().width,camera.getParameters().getPreviewSize().height), 80, outStream);
			        try {
						outStream.flush();
						outStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					byte[] image = outStream.toByteArray();
					String img_str = Base64.encodeToString(image, 2);
					
					String img = "data:image/jpeg;base64,"+img_str;
					String html = "<img src=\""+img+"\" />";
					return new Response(Status.OK,MIME_JSON, "\""+img+"\"");
				}
				
				
				try {
					cmd = new JSONObject(files.get("postData"));

					handler.post(new Runnable() {

						@Override
						public void run() {
							txtLog.append(cmd.toString());

							if (uri.endsWith("musicplayer")) {

								try {

									if (cmd.getJSONObject("command")
											.getString("text").equals("play"))
										MusicControl(CMDPLAY);
									else if (cmd.getJSONObject("command")
											.getString("text").equals("pause"))
										MusicControl(CMDPAUSE);
									else if (cmd.getJSONObject("command")
											.getString("text").equals("next"))
										MusicControl(CMDNEXT);
									else if (cmd.getJSONObject("command")
											.getString("text")
											.equals("previous"))
										MusicControl(CMDPREVIOUS);
									else if (cmd.getJSONObject("command")
											.getString("text").equals("stop"))
										MusicControl(CMDSTOP);
									else if (cmd.getJSONObject("command")
											.getString("text").equals("volup"))
										VolUp();
									else if (cmd.getJSONObject("command")
											.getString("text")
											.equals("voldown"))
										VolDown();

								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}

							if (uri.endsWith("clipboard")) {

								try {
									if (cmd.getJSONObject("command")
											.getString("type")
											.equals("System.SetClipboard")) {
										String clip = cmd.getJSONObject(
												"command").getString("text");
										SetClipboard(clip);
									}

								} catch (JSONException e) {

									e.printStackTrace();
								}

							}

						}
					});

					if (cmd.getJSONObject("command").getString("type")
							.equals("System.GetClipboard")) {
						clipText = GetClipboard();
						return new Response(Status.OK, MIME_JSON, "\""
								+ clipText + "\"");

					}

				} catch (JSONException e) {

					e.printStackTrace();
				}

				return new Response(Status.OK, MIME_JSON, "\"Success!\"");

			}

			return serveFile(uri, method, headers, parms, files);
		}
	}

}
