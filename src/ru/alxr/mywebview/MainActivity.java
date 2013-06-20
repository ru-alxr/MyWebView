package ru.alxr.mywebview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnLongClickListener {
	private static final String TAG = "MainActivity";
	CountDownTimer timer;
	MyWebView mv;
	String httpPageString;
	ArrayList <String> words, wordsSortedByLenth;
	char [] en, ru;
	ArrayList <Replacement> rep;
	TextView tv;
	int spanId = 0;
	
	public String getHttpPageString() {
		return httpPageString;
	}
	
	class Replacement{
	int index;
	String word;
		public Replacement (int startIndex, String wordToReplace){
			this.index = startIndex;
			this.word = wordToReplace;
		}
	}


	public void setHttpPageString(String httpPageString) {
		this.httpPageString = httpPageString;// получил html от asyncTask
		rep = new ArrayList <Replacement> ();
		generateListOfWords(httpPageString); // ПАРСЕР!
	}
	




	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.tv);
		mv = new MyWebView(getApplicationContext());
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
		mv.getSettings().setJavaScriptEnabled(true);
		Button callBtn = new Button(this);
		callBtn.setText("callBtn");
		callBtn.setOnClickListener(new OnClickListener() {
		    @SuppressWarnings("unchecked")
			@Override
		     public void onClick(View v) {
		    	GetHttpUrlAsString action = new GetHttpUrlAsString("http://alexchemist.narod.ru/task2.html", MainActivity.this);
		    	action .execute();
		    }
		});
		ll.addView(callBtn);
		ll.addView(mv);

		
	}
	
	 public class JavaScriptInterface {
	     Context mContext;
	     JavaScriptInterface(Context c) {
	         mContext = c;
	     }
	     
	     public void receiveValueFromJs(String str) {
	    	  Toast.makeText(mContext, "Received Value from JS: " + str, Toast.LENGTH_SHORT).show();
	    	}
	}
	 
	 public void generateListOfWords (String string){
		 final char START = '>';
		 
		 String buff = "";
		 StringBuffer builder = new StringBuffer (); 
		 boolean startParse = false, br = false;
		 for (int i = 0; i < string.length(); i++){
			 if (!startParse) builder.append(string.charAt(i));
			 if (string.charAt(i) == START){
				 startParse = true;
				 buff = "";
				 continue;
			 }
			 if (startParse && string.charAt(i) == '<' && string.charAt(i+1) != '/'){
				 // начили парсить и встретили символ < без закрывающего слеша
				 // значит надо обнулять буфер парсера если это не <br />
				 String checkForBrTag = "";
				 checkForBrTag = "" + string.charAt(i) + string.charAt(i+1) + string.charAt(i+2) + string.charAt(i + 3)+ string.charAt(i + 4);
				 if (checkForBrTag.equals("<br /") || checkForBrTag.equals("<span")){
					 startParse = false;
					 br = !br;
					 builder.append(insertJsCall(buff));
					 builder.append(string.charAt(i));
					 Log.d(TAG, "br " + buff);
					 continue;					 
				 }
				 else{
					 buff = buff + string.charAt(i);
					 builder.append(buff);
					 startParse = false;
					 continue;
				 }
			 }
			 if (startParse && string.charAt(i) != '<') {
				 // начили парсить и встретили обычный символ
				 buff = buff + string.charAt(i);
			 }
			 if (string.charAt(i) == '<' && string.charAt(i+1) == '/') {
				// начили парсить и встретили символ < с закрывающим слешем
				 startParse = false; 
				 builder.append(insertJsCall(buff));
				 builder.append(string.charAt(i));
			 }
			 

		 }
		 String javaScript = "<script type='text/JavaScript'> function getValue(id){var val = document.getElementById(id).innerText;  MyAndroid.receiveValueFromJs(val);}</script>";
		 builder.append(javaScript);
		mv.loadData(builder.toString(),  "text/html", "utf-8"); 
		mv.addJavascriptInterface(new JavaScriptInterface(this), "MyAndroid");
	 }

	private String insertJsCall(String b){
		String [] a = b.split(" ");
		StringBuilder builder = new StringBuilder();
		for (String s : a){
			builder.append("<span element id='");
			builder.append(Integer.toString(spanId++));
			builder.append("'");
			builder.append(" onclick='getValue(id)'>");
			builder.append(s);
			builder.append(" </span>");
		}
		return builder.toString();
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		Log.d("onLongClick", "onLongClick");
		mv.loadUrl("javascript:getValue()");
		Toast.makeText(getApplicationContext(), "onLongClick", Toast.LENGTH_SHORT).show();
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	private class GetHttpUrlAsString extends AsyncTask{
		private static final String TAG = "GetHttpUrlAsString";
		private HttpGet httpget;
		private HttpClient httpclient;
		HttpResponse response;
		private StatusLine statusLine;
		private String responseString = "no resp.";
		String url;
		Activity activity;
		
		public GetHttpUrlAsString(String _url, Activity _activity){
			Log.d(TAG, "GetHttpUrlAsString constructor");
			this.url = _url;
			this.activity = _activity;
		}
		
		@Override
		protected Object doInBackground(Object... params) {
			httpclient = new DefaultHttpClient();
			response = null;
			httpget = new HttpGet(url);
			responseString = null;
			try {
				Log.d(TAG, "doInBackground");
				response = httpclient.execute(httpget);
			} catch (ClientProtocolException e1) {
				Log.d(TAG, "ClientProtocolException " + e1.toString());
				e1.printStackTrace();
			} catch (IOException e1) {
				Log.d(TAG, "IOException " + e1.toString());
				e1.printStackTrace();
			}
			statusLine = response.getStatusLine();
			int a = statusLine.getStatusCode();
			Log.d(TAG, "StatusCode = " + a);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				response.getEntity().writeTo(out);
				out.close();
				responseString =  out.toString();
				response.getEntity().consumeContent();
			} catch (IOException e) {
				Log.d(TAG, "IOException " + e.toString());
				return null;
			}
			Log.d(TAG, "responseString.length() = " + responseString.length());
			
			if (responseString != null) {
				activity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						setHttpPageString(responseString);
					}
				});
			}
			return null;
		}
		
	}
	
 
  	// может пригодится в будущем
  	@SuppressWarnings("unused")
	private String onlyAcceptableChars(String source){
  		en = new char [52]; 
  		for (int i = 0; i < 26; i++){
  			en[i] = (char) ('a' + i);
  			en[i+26] = (char) ('A' + i);
  		}
  		ru = new char [64]; 
  		for (int i = 0; i < 32; i++){
  			ru[i] = (char) ('а' + i);
  			ru[i+32] = (char) ('А' + i);
  		}
  		StringBuilder stringBuilder = new StringBuilder(source);
  		for (int j = 0; j < source.length(); j++){
  			char current = source.charAt(j);
  			if (!hasAcceptableChar(source, current)) {
  				stringBuilder.setCharAt(j, ' ');
  			}
  		}
  		source = stringBuilder.toString();
  		return source;
  	}
  	
  	private boolean hasAcceptableChar(String source, char current){
  		for (int k = 0; k < en.length; k++) if (current == en[k]) {
  			return true;
  			}
		for (int k = 0; k < ru.length; k++) if (current == ru[k]) {
			return true;}
  		return false;
  	}


}
