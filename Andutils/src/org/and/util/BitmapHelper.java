package org.and.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class BitmapHelper {
	
	private final static String TAG = "BitmapHelper";
	
	// -------------------------------------------------------------------
	public static interface OnFetchCompleteListener1 {
		public void onFetchComplete(int viewId);
	}

	public static void fetchImage1(final Context context, final int viewId,
			String url[], final OnFetchCompleteListener1 callback,
			final MessageDigest mDigest) {

		new AsyncTask<String, Void, Integer>() {

			@Override
			protected Integer doInBackground(String... params) {
				for (String url : params) {
					if (url == null || url.length() == 0) {
						return null;
					}
					try {
						mDigest.update(url.getBytes());
						final String cacheKey = StringHelper
								.bytesToHexString(mDigest.digest());
						File cacheFile = new File(
								context.getCacheDir(), cacheKey);
						if (!cacheFile.exists()) {
							final HttpClient httpClient = HttpHelper
									.getHttpClient(context
											.getApplicationContext());
							final HttpResponse resp = httpClient
									.execute(new HttpGet(url));
							final HttpEntity entity = resp.getEntity();

							final int statusCode = resp.getStatusLine()
									.getStatusCode();
							if (statusCode == HttpStatus.SC_OK
									|| entity != null) {
								final byte[] respBytes = EntityUtils
										.toByteArray(entity);
								if (cacheFile != null) {
									try {
										cacheFile.getParentFile().mkdirs();
										cacheFile.createNewFile();
										FileOutputStream fos = new FileOutputStream(
												cacheFile);
										fos.write(respBytes);
										fos.close();
									} catch (FileNotFoundException e) {
										Log.w(TAG,
												"Error writing to bitmap cache: "
														+ cacheFile.toString(),
												e);
									} catch (IOException e) {
										Log.w(TAG,
												"Error writing to bitmap cache: "
														+ cacheFile.toString(),
												e);
									}
								}
							}
						}
					} catch (Exception ex) {

					}
				}
				return viewId;
			}

			@Override
			protected void onPostExecute(Integer result) {
				callback.onFetchComplete(result);
			}
		}.execute(url);
	}
	
	//-----------------------------------------------------------------------------------------------
	 public static interface OnFetchCompleteListener2 {
	        public void onFetchComplete(Object cookie, Bitmap result);
	    }

	    /**
	     * Only call this method from the main (UI) thread. The {@link OnFetchCompleteListener} callback
	     * be invoked on the UI thread, but image fetching will be done in an {@link AsyncTask}.
	     */
	    public static void fetchImage2(final Context context, final String url,
	            final OnFetchCompleteListener2 callback) {
	        fetchImage2(context, url, null, null, callback);
	    }

	    /**
	     * Only call this method from the main (UI) thread. The {@link OnFetchCompleteListener} callback
	     * be invoked on the UI thread, but image fetching will be done in an {@link AsyncTask}.
	     *
	     * @param cookie An arbitrary object that will be passed to the callback.
	     */
	    public static void fetchImage2(final Context context, final String url,
	            final BitmapFactory.Options decodeOptions,
	            final Object cookie, final OnFetchCompleteListener2 callback) {
	        new AsyncTask<String, Void, Bitmap>() {
	            @Override
	            protected Bitmap doInBackground(String... params) {
	                final String url = params[0];
	                if (TextUtils.isEmpty(url)) {
	                    return null;
	                }

	                // First compute the cache key and cache file path for this URL
	                File cacheFile = null;
	                try {
	                    MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
	                    mDigest.update(url.getBytes());
	                    final String cacheKey = StringHelper.bytesToHexString(mDigest.digest());
	                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	                        cacheFile = new File(
	                                Environment.getExternalStorageDirectory()
	                                        + File.separator + "Android"
	                                        + File.separator + "data"
	                                        + File.separator + context.getPackageName()
	                                        + File.separator + "cache"
	                                        + File.separator + "bitmap_" + cacheKey + ".tmp");
	                    }
	                } catch (NoSuchAlgorithmException e) {
	                    // Oh well, SHA-1 not available (weird), don't cache bitmaps.
	                }

	                if (cacheFile != null && cacheFile.exists()) {
	                    Bitmap cachedBitmap = BitmapFactory.decodeFile(
	                            cacheFile.toString(), decodeOptions);
	                    if (cachedBitmap != null) {
	                        return cachedBitmap;
	                    }
	                }

	                try {
	                    // TODO: check for HTTP caching headers
	                    final HttpClient httpClient = HttpHelper.getHttpClient(
	                            context.getApplicationContext());
	                    final HttpResponse resp = httpClient.execute(new HttpGet(url));
	                    final HttpEntity entity = resp.getEntity();

	                    final int statusCode = resp.getStatusLine().getStatusCode();
	                    if (statusCode != HttpStatus.SC_OK || entity == null) {
	                        return null;
	                    }

	                    final byte[] respBytes = EntityUtils.toByteArray(entity);

	                    // Write response bytes to cache.
	                    if (cacheFile != null) {
	                        try {
	                            cacheFile.getParentFile().mkdirs();
	                            cacheFile.createNewFile();
	                            FileOutputStream fos = new FileOutputStream(cacheFile);
	                            fos.write(respBytes);
	                            fos.close();
	                        } catch (FileNotFoundException e) {
	                            Log.w(TAG, "Error writing to bitmap cache: " + cacheFile.toString(), e);
	                        } catch (IOException e) {
	                            Log.w(TAG, "Error writing to bitmap cache: " + cacheFile.toString(), e);
	                        }
	                    }

	                    // Decode the bytes and return the bitmap.
	                    return BitmapFactory.decodeByteArray(respBytes, 0, respBytes.length,
	                            decodeOptions);
	                } catch (Exception e) {
	                    Log.w(TAG, "Problem while loading image: " + e.toString(), e);
	                }
	                return null;
	            }

	            @Override
	            protected void onPostExecute(Bitmap result) {
	                callback.onFetchComplete(cookie, result);
	            }
	        }.execute(url);
	    }
	
	//------------------------------------------------------------------------------------------------
	
	/**
	 * 该方法用于将一个矩形图片的边角钝化
	 * 
	 * @param bitmap
	 *            待修改的图片
	 * @param roundPx
	 *            边角的弧度
	 * @return 返回修改过边角的新图片
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	/**
	 * 该方法用于任意缩放指定大小的图片
	 * 
	 * @param bitmap
	 *            待修改的图片
	 * @param newWidth
	 *            新图片的宽度
	 * @param newHeight
	 *            新图片的高度
	 * @return 缩放后的新图片
	 */
	public static Bitmap zoomBitmap(Bitmap bitmap, int newWidth, int newHeight) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Matrix matrix = new Matrix();
		float scaleWidht = ((float) newWidth / width);
		float scaleHeight = ((float) newHeight / height);
		matrix.postScale(scaleWidht, scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
		return newbmp;
	}

	/**
	 * 该方法用于生成图片的下方倒影效果
	 * 
	 * @param bitmap
	 *            代修改的图片
	 * @return 有倒影效果的新图片
	 */
	public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap) {
		final int reflectionGap = 4;
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);
		Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 2,
				width, height / 2, matrix, false);
		Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
				(height + height / 2), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapWithReflection);
		canvas.drawBitmap(bitmap, 0, 0, null);
		Paint deafalutPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, deafalutPaint);
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,
				bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,
				0x00ffffff, TileMode.CLAMP);
		paint.setShader(shader);
		//  Set the Transfer mode to be porter duff and destination in  
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		//  Draw a rectangle using the paint with our linear gradient  
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);
		return bitmapWithReflection;
	}

	/**
	 * 该方法用于将bitmap转换为字节数组，png格式质量为100的。
	 * 
	 * @param bm
	 *            待转化的bitmap
	 * @return 返回btmap的字节数组
	 */
	public static byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	/**
	 * 该方法用于将字节数组转化为bitmap图
	 * 
	 * @param b
	 *            字节数组
	 * @return bitmap位图
	 */
	public static Bitmap Bytes2Bitmap(byte[] b) {
		if (b.length != 0) {
			return BitmapFactory.decodeByteArray(b, 0, b.length);
		} else {
			return null;
		}
	}

	/**
	 * Drawable转换成Bitmap
	 * @param drawable
	 * @return bitmap 位图
	 * */
	public static Bitmap drawableToBitmap(Drawable drawable) {

		Bitmap bitmap = Bitmap
				.createBitmap(
						drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight(),
						drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
								: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		// canvas.setBitmap(bitmap);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
		drawable.draw(canvas);
		return bitmap;
	}
}
