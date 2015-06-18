package com.example.photoviewer.adapter;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.HttpClient;

import com.example.photoviewer.ImageSet;
import com.example.photoviewer.R;

import android.R.integer;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoViewerAdapter extends ArrayAdapter<String> implements
		OnScrollListener {

	private Set<BitmapWorkerTask> taskCollection;
	private LruCache<String, Bitmap> mMemoryCache;

	private GridView mPhotowall = null;

	/**
	 * 第一张可见图片的下标
	 */
	private int mFirstVisibleItem;

	/**
	 * 一屏有多少张图片可见
	 */
	private int mVisibleItemCount;

	/**
	 * 记录是否刚打开程序，用于解决进入程序不滚动屏幕，不会下载图片的问题。
	 */
	private boolean isFirstEnter = true;

	public PhotoViewerAdapter(Context context, int textViewResourceId,
			String[] objects, GridView gridView) {
		super(context, textViewResourceId, objects);
		mPhotowall = gridView;
		taskCollection = new HashSet<BitmapWorkerTask>();

		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cachesize = maxMemory / 8;
		mMemoryCache = new LruCache<String, Bitmap>(cachesize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};

		mPhotowall.setOnScrollListener(this);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final String url = getItem(position);

		View view = null;
		if (convertView == null) {
			view = LayoutInflater.from(getContext()).inflate(
					R.layout.photo_layout, null);
		} else {
			view = convertView;
		}

		final ImageView photo = (ImageView) view.findViewById(R.id.photo);
		photo.setTag(url);
		setImageView(url, photo);
		return view;
	};

	/**
	 * 从LruCache中获取一张图片，如果不存在就返回null。
	 * 
	 * @param key
	 *            LruCache的键，这里传入图片的URL地址。
	 * @return 对应传入键的Bitmap对象，或者null。
	 */
	public Bitmap getBitmapFromMemoryCache(String key) {
		return mMemoryCache.get(key);
	}

	private void setImageView(String imageurl, ImageView imageView) {
		Bitmap bitmap = getBitmapFromMemoryCache(imageurl);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {

			imageView.setImageResource(R.drawable.empty_photo);
		}
	}

	/**
	 * 将一张图片存储到LruCache中。
	 * 
	 * @param key
	 *            LruCache的键，这里传入图片的URL地址。
	 * @param bitmap
	 *            LruCache的键，这里传入从网络上下载的Bitmap对象。
	 */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemoryCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		// 仅当GridView静止时才去下载图片，GridView滑动时取消所有正在下载的任务
		if (scrollState == SCROLL_STATE_IDLE) {
			loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
		} else {
			cancelAllTasks();
		}
	}

	public void cancelAllTasks() {
		if (taskCollection != null) {
			for (BitmapWorkerTask bitmapWorkerTask : taskCollection) {
				bitmapWorkerTask.cancel(false);
			}
		}
	}

	@Override
	public void onScroll(android.widget.AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mFirstVisibleItem = firstVisibleItem;
		mVisibleItemCount = visibleItemCount;
		if (isFirstEnter && visibleItemCount > 0) {
			loadBitmaps(firstVisibleItem, visibleItemCount);
			isFirstEnter = false;
		}
	};

	/**
	 * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
	 * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
	 * 
	 * @param firstVisibleItem
	 *            第一个可见的ImageView的下标
	 * @param visibleItemCount
	 *            屏幕中总共可见的元素数
	 */
	private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {

		try {
			for (int i = firstVisibleItem; i < firstVisibleItem
					+ visibleItemCount; i++) {
				String url = ImageSet.imageThumUrls[i];
				Bitmap bitmap = getBitmapFromMemoryCache(url);

				if (bitmap == null) {
					BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask();
					taskCollection.add(bitmapWorkerTask);
					bitmapWorkerTask.execute(url);

				} else {
					ImageView imageView = (ImageView) mPhotowall
							.findViewWithTag(url);
					if (imageView != null) {
						imageView.setImageBitmap(bitmap);
					}
				}

			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	/*
	 * 异步下载图片任务
	 * 
	 * @ author 史杨杨
	 */
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

		private String imgUrl = null;

		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			imgUrl = params[0];

			// 后台下载图片
			Bitmap bitmap = downloadBitmap(imgUrl);
			if (bitmap != null) {
				// 图片下载完成加入缓存Lrucache
				addBitmapToMemoryCache(params[0], bitmap);
			}

			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// TODO Auto-generated method stub
			super.onPostExecute(bitmap);

			ImageView imageView = (ImageView) mPhotowall
					.findViewWithTag(imgUrl);
			if (bitmap != null && imageView != null) {
				imageView.setImageBitmap(bitmap);
			}
			taskCollection.remove(this);
		}

		/*
		 * 建立Http 请求下载图片
		 */
		private Bitmap downloadBitmap(String imageUrl) {
			Bitmap bitmap = null;
			ImageView imageView = (ImageView) mPhotowall
					.findViewWithTag(imageUrl);
			HttpURLConnection con = null;
			try {
				URL url = new URL(imageUrl);
				con = (HttpURLConnection) url.openConnection();
				con.setConnectTimeout(5 * 1000);
				con.setReadTimeout(10 * 1000);

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(con.getInputStream(), null, options);
                bitmap = ScaleBitmap(con.getInputStream(), imageView.getWidth(), imageView.getHeight(), options);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			} finally {
				if (con != null) {
					con.disconnect();
				}

			}

			return bitmap;
		}

		private Bitmap ScaleBitmap(InputStream isInputStream,
				int imageDestWidth, int imageDestHeight,
				BitmapFactory.Options options) {

			int imageSrcWidth = options.outWidth;
			int imageSrcHeight = options.outHeight;
			int inSampleSize = 1;
			if (imageSrcHeight > imageDestHeight
					|| imageSrcWidth > imageSrcWidth) {
				// 计算出实际宽高和目标宽高的比率
				final int heightRatio = Math.round((float) imageSrcHeight
						/ (float) imageDestHeight);
				final int widthRatio = Math.round((float) imageSrcWidth
						/ (float) imageSrcWidth);
				// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
				// 一定都会大于等于目标的宽和高。
				inSampleSize = heightRatio < widthRatio ? heightRatio
						: widthRatio;
			}
            options.inSampleSize = inSampleSize;
			options.inJustDecodeBounds = false;
			
			return BitmapFactory.decodeStream(isInputStream, null, options);
		}
	}
}
