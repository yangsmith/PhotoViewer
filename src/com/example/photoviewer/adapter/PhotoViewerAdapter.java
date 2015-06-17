package com.example.photoviewer.adapter;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.HttpClient;

import com.example.photoviewer.R;

import android.R.integer;
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
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoViewerAdapter extends ArrayAdapter<String> implements OnScrollListener{

	private Set<BitmapWorkerTask> taskCollection;
	private LruCache<String, Bitmap> mMemoryCache;
	
	private GridView mPhotowall = null;
	
	public PhotoViewerAdapter(Context context,int textViewResourceId, String[] objects, GridView gridView){
		super(context, textViewResourceId,objects);
		mPhotowall = gridView;
		taskCollection = new HashSet<BitmapWorkerTask>();
		
		int maxMemory = (int)Runtime.getRuntime().maxMemory();
		int cachesize = maxMemory/8;
		mMemoryCache = new LruCache<String, Bitmap>(cachesize){
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
		if(convertView == null){
			view = LayoutInflater.from(getContext()).inflate(R.layout.photo_layout, null);
		}else {
			view = convertView;
		}
		
		final ImageView photo = (ImageView)view.findViewById(R.id.photo);
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
    
    private void setImageView(String imageurl, ImageView imageView){
    	Bitmap bitmap = getBitmapFromMemoryCache(imageurl);
    	if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		}else {
			
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
    
    
	
	
	
	/* �첽����ͼƬ����
	 *@ author ʷ����
	 **/
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{
		
		private String imgUrl = null;
		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			imgUrl = params[0];
			
			//��̨����ͼƬ
			Bitmap bitmap = downloadBitmap(imgUrl);
			if (bitmap != null) {
				//ͼƬ������ɼ��뻺��Lrucache
				 addBitmapToMemoryCache(params[0], bitmap);
			}
			
			return bitmap;
		}
		
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// TODO Auto-generated method stub
			super.onPostExecute(bitmap);
			
			ImageView imageView = (ImageView)mPhotowall.findViewWithTag(imgUrl);
			if (bitmap != null && imageView != null) {
				imageView.setImageBitmap(bitmap);
			}
			taskCollection.remove(this);
		}
		
		/* ����Http ��������ͼƬ
		 * 
		 */
		private Bitmap downloadBitmap(String imageUrl) {  
			Bitmap bitmap = null;
			HttpURLConnection con = null;
			try {
				URL url = new URL(imageUrl);
				con = (HttpURLConnection)url.openConnection();
				con.setConnectTimeout(5*1000);
				con.setReadTimeout(10*1000);
				bitmap = BitmapFactory.decodeStream(con.getInputStream());
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}finally{
				if (con != null) {
					con.disconnect();
				}
			
			}
			
			return bitmap;
		  }
	}
}
