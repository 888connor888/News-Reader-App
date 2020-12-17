package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity {
    int newsNumber;
    SQLiteDatabase articleDB;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> urlList = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    public class DownloadTask extends AsyncTask<String,Void,String>{
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try{
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data!=-1){
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                JSONArray jsonArray = new JSONArray(result);
                if(jsonArray.length()<newsNumber){
                    newsNumber = jsonArray.length();
                }
                articleDB.execSQL("DELETE FROM articles");
//                Log.i(MainActivity.TAG,"Database cleared!");
                for(int i=0;i<newsNumber;i++){
                    String newsId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+newsId+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleInfo="";
                    while(data!=-1){
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo);
                    Log.i(MainActivity.TAG,"Data Insertion init");
                    String articleTitle = jsonObject.getString("title");
                    String articleUrl = jsonObject.getString("url");
//                    articleDB.execSQL("INSERT INTO articles(articleId,title,url) VALUES ("+newsId+","+articleTitle+","+articleUrl+")");
                    String sql = "INSERT INTO articles(articleId,title) VALUES (?,?,?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1,newsId);
                    statement.bindString(2,articleTitle);
                    statement.bindString(3,articleUrl);
                    statement.executeInsert();
                    Log.i(MainActivity.TAG,"Data Insertion Completed!");
                    }
                return result;
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
//            Log.i(MainActivity.TAG,"Leaving on post execution");
        }
    }

    public void updateListView(){
        Log.i(MainActivity.TAG,"Inside updateListView");
        Cursor c = articleDB.rawQuery("SELECT title,url FROM articles",null);
        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("url");
        boolean val = c.moveToFirst();
        Log.i(MainActivity.TAG,Boolean.toString(val));
        if(c.moveToFirst()) {
            titles.clear();
            urlList.clear();


            while (!c.isAfterLast()) {
              //  Log.i(MainActivity.TAG, c.getString(titleIndex) + " lund bc " + c.getString(urlIndex));
                titles.add(c.getString(titleIndex));
                urlList.add(c.getString(urlIndex));
                c.moveToNext();
            }
            arrayAdapter.notifyDataSetChanged();
        }
//            do{
//                Log.i(MainActivity.TAG,"Looping...");
//
//            }while(c.moveToNext());
//            ;

        Log.i(MainActivity.TAG,"Leaving updateListView");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        Intent intent = getIntent();
        newsNumber = intent.getIntExtra(MainActivity.EXTRA,5);
        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId VARCHAR,title VARCHAR,url VARCHAR)");
        DownloadTask task = new DownloadTask();
        task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,R.layout.mytextview,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),WebActivity.class);
                intent.putExtra("url",urlList.get(position));
                startActivity(intent);
            }
        });

        updateListView();
    }
}