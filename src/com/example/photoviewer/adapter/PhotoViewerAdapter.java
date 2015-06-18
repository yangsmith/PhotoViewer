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
	 * ��һ�ſɼ�ͼƬ���±�
	 */
	private int mFirstVisibleItem;

	/**
	 * һ���ж�����ͼƬ�ɼ�
	 */
	private int mVisibleItemCount;

	/**
	 * ��¼�Ƿ�մ򿪳������ڽ��������򲻹�����Ļ����������ͼƬ�����⡣
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
	 * ��LruCache�л�ȡһ��ͼƬ����������ھͷ���null��
	 * 
	 * @param key
	 *            LruCache�ļ������ﴫ��ͼƬ��URL��ַ��
	 * @return ��Ӧ�������Bitmap���󣬻���null��
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
	 * ��һ��ͼƬ�洢��LruCache�С�
	 * 
	 * @param key
	 *            LruCache�ļ������ﴫ��ͼƬ��URL��ַ��
	 * @param bitmap
	 *            LruCache�ļ������ﴫ������������ص�Bitmap����
	 */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemoryCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		// ����GridView��ֹʱ��ȥ����ͼƬ��GridView����ʱȡ�������������ص�����
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
	 * ����Bitmap���󡣴˷�������LruCache�м��������Ļ�пɼ���ImageView��Bitmap����
	 * ��������κ�һ��ImageView��Bitmap�����ڻ����У��ͻῪ���첽�߳�ȥ����ͼƬ��
	 * 
	 * @param firstVisibleItem
	 *            ��һ���ɼ���ImageView���±�
	 * @param visibleItemCount
	 *            ��Ļ���ܹ��ɼ���Ԫ����
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
	 * �첽����ͼƬ����
	 * 
	 * @ author ʷ����
	 */
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

		private String imgUrl = null;

		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			imgUrl = params[0];

			// ��̨����ͼƬ
			Bitmap bitmap = downloadBitmap(imgUrl);
			if (bitmap != null) {
				// ͼƬ������ɼ��뻺��Lrucache
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
		 * ����Http ��������ͼƬ
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
				// �����ʵ�ʿ�ߺ�Ŀ���ߵı���
				final int heightRatio = Math.round((float) imageSrcHeight
						/ (float) imageDestHeight);
				final int widthRatio = Math.round((float) imageSrcWidth
						/ (float) imageSrcWidth);
				// ѡ���͸�����С�ı�����ΪinSampleSize��ֵ���������Ա�֤����ͼƬ�Ŀ�͸�
				// һ��������ڵ���Ŀ��Ŀ�͸ߡ�
				inSampleSize = heightRatio < widthRatio ? heightRatio
						: widthRatio;
			}
            options.inSampleSize = inSampleSize;
			options.inJustDecodeBounds = false;
			
			return BitmapFactory.decodeStream(isInputStream, null, options);
		}
	}
}
