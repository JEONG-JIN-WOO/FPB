package com.kit.fpb.bike;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import android.location.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchActivity extends AppCompatActivity{

    private Location myLocation = null;
    double latPoint = 0;
    double lngPoint = 0;
    public static final String SEARCHTAG = "SearchTag";

    protected JSONObject mResult = null;

    protected ArrayList<SearchInfo> mArray = new ArrayList<SearchInfo>();
    protected ListView mList;
    protected SearchAdapter mAdapter;
    protected RequestQueue mQueue = null;
    protected ImageLoader mImageLoader = null;

    Button btn1;
    EditText search;

   String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAdapter = new SearchAdapter(this, R.layout.activity_search);
        mList = (ListView) findViewById(R.id.listView3);
        mList.setAdapter(mAdapter);
        mList.setFocusable(false);


        btn1 = (Button)findViewById(R.id.search);
        search = (EditText)findViewById(R.id.edit1) ;

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               name = search.getText().toString();

                String url = "http://128.199.238.222/search.php";

                Request jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                mResult = response;
                                drawList();
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(SearchActivity.this, "DB 연동 에러", Toast.LENGTH_LONG).show();
                            }
                        });

                jsObjRequest.setTag(SEARCHTAG);
                mQueue.add(jsObjRequest);

            }
        }   );

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchInfo info = mArray.get(position);
                String url = "http://128.199.238.222/coupang.php";

                final String link = info.getLink();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(SearchActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                            }
                        });

                RequestQueue requestQueue = Volley.newRequestQueue(SearchActivity.this);
                requestQueue.add(stringRequest);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                finish();
                startActivity(intent);
            }
        });

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB
        Network network = new BasicNetwork(new HurlStack());
        mQueue = new RequestQueue(cache, network);
        mQueue.start();

        mImageLoader = new ImageLoader(mQueue,
                new LruBitmapCache(LruBitmapCache.getCacheSize(this)));

       // requestSearch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void drawList(){
        mArray.clear();
        try{
            JSONArray jsonMainNode=mResult.getJSONArray("list");

            for(int i=0;i<jsonMainNode.length();i++){
                JSONObject jsonChildNode=jsonMainNode.getJSONObject(i);

                if(jsonChildNode.getString("title").contains(name)) {
                    String img = jsonChildNode.getString("img");
                    String title = jsonChildNode.getString("title");
                    String price = jsonChildNode.getString("price");
                    String link = jsonChildNode.getString("link");

                    mArray.add(new SearchInfo(img, title, price, link));
                }
            }
        }catch(JSONException |NullPointerException e){
            Toast.makeText(getApplicationContext(),"Error"+e.toString(),Toast.LENGTH_LONG).show();
            mResult=null;
        }
        mAdapter.notifyDataSetChanged();
    }

    public class SearchInfo {
        String img;
        String title;
        String price;
        String link;

        public SearchInfo(String img, String title, String price, String link) {
            this.img = img;
            this.title = title;
            this.price = price;
            this.link = link;

        }
        public String getImg() { return img; }

        public String getTitle() { return title; }

        public String getPrice() { return price; }

        public String getLink() { return link; }
    }

    static class SearchViewHolder {
        NetworkImageView imImage;
        TextView txTitle;
        TextView txPrice;
        TextView txLink;
    }

    public class SearchAdapter extends ArrayAdapter<SearchInfo> {
        private LayoutInflater mInflater = null;
        public SearchAdapter(Context context, int resource) {
            super(context, resource);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mArray.size();
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            SearchViewHolder viewHolder;
            if(v == null) {
                v = mInflater.inflate(R.layout.lodge_item, parent, false);
                viewHolder = new SearchViewHolder();

                viewHolder.imImage = (NetworkImageView) v.findViewById(R.id.img);
                viewHolder.txTitle = (TextView) v.findViewById(R.id.title);
                viewHolder.txPrice = (TextView) v.findViewById(R.id.price);
                //viewHolder.txLink = (TextView) v.findViewById(R.id.link);

                v.setTag(viewHolder);
            }
            else {
                viewHolder = (SearchViewHolder) v.getTag();
            }

            SearchInfo info = mArray.get(position);
            if(info != null) {
                viewHolder.imImage.setImageUrl(info.getImg(), mImageLoader);
                viewHolder.txTitle.setText(info.getTitle());
                viewHolder.txPrice.setText(info.getPrice()+"원");
                //   viewHolder.txLink.setText(info.getLink());
            }
            return  v;
        }
    }
}
