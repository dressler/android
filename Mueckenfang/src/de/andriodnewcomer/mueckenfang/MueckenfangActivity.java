package de.andriodnewcomer.mueckenfang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class MueckenfangActivity extends Activity implements OnClickListener, Html.ImageGetter{
	
	private Animation animationEinblenden, animationWackeln;
	private Button startButton;
	private Handler handler = new Handler();
	private Runnable wackelnRunnable = new WackleButton();
	private static final int WACKELN_NACH_MS = 10000;
	private static final String HIGHSCORE_SERVER_BASE_URL = "http://hirschbusch.appspot.com/myhighscoreserver";
	private static final String HIGHSCORESERVER_GAME_ID = "mueckenfang";
	private LinearLayout namenseingabe;
	private Button speichern;
	private TextView tv;
	private String highscoresHtml = "";
	private TextView highscores;
	private Spinner schwierigkeitsgrad;
	private ArrayAdapter<String> schwierigkeitsgradAdapter;
	private ListView list;
	private ToplistAdapter adapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		startButton = (Button) findViewById(R.id.gameoverbtn);
		startButton.setOnClickListener(this);
		animationEinblenden = AnimationUtils.loadAnimation(this, R.anim.einblenden);
		animationWackeln = AnimationUtils.loadAnimation(this, R.anim.wackeln);
		speichern = (Button) findViewById(R.id.speichern);
		speichern.setOnClickListener(this);
		namenseingabe = (LinearLayout) findViewById(R.id.namenseingabe);
		namenseingabe.setVisibility(View.GONE);
		tv = (TextView) findViewById(R.id.highscore);
		highscores = (TextView) findViewById(R.id.highscores);
		highscores.setOnClickListener(this);
		
		schwierigkeitsgrad = (Spinner) findViewById(R.id.schwierigkeitsgrad);
		schwierigkeitsgradAdapter = new ArrayAdapter<String>(this, 
			android.R.layout.simple_spinner_item, new String[]{"leicht", "mittel", "schwer"});
		schwierigkeitsgradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		schwierigkeitsgrad.setAdapter(schwierigkeitsgradAdapter);
	}
	
	@Override
	protected void onResume() {
		View v = findViewById(R.id.wurzel);
		v.startAnimation(animationEinblenden);
		super.onResume();
		handler.postDelayed(wackelnRunnable, WACKELN_NACH_MS);
		//TextView tv = (TextView) findViewById(R.id.highscore);
		//tv.setText(Integer.toString(leseHighscore()));		
		highscoreAnzeigen();
		TextView tv = (TextView) findViewById(R.id.highscores);
		tv.setText("Highscores laden...");
		Thread t = new Thread(new HoleHighscores(100));
		t.start();
	}
	
	private int leseHighscore() {
		SharedPreferences pref = getSharedPreferences("GAME", 0);
		return pref.getInt("HIGHSCORE", 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode >= leseHighscore()) {
				schreibeHighscore(resultCode);
				namenseingabe.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void schreibeHighscore(int highscore) {
		SharedPreferences pref = getSharedPreferences("GAME", 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt("HIGHSCORE", highscore);
		editor.commit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(wackelnRunnable);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.gameoverbtn) {
			//startActivity(new Intent(this, GameActivity.class));
			int s = schwierigkeitsgrad.getSelectedItemPosition();
			Intent intent = new Intent(this, GameActivity.class);
			intent.putExtra("schwierigkeitsgrad", s);
			startActivityForResult(intent, 1);
		} else if (v.getId() == R.id.speichern) {
			schreibeHighscoreName();
			highscoreAnzeigen();
			namenseingabe.setVisibility(View.GONE);
			Thread t = new Thread(new SendeHighscore());
			t.start();
		} else if (v.getId() == R.id.highscores) {
			setContentView(R.layout.toplist);
			Thread t = new Thread(new HoleHighscores(100));
			t.start();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if(findViewById(R.id.list)!=null){
				startActivity(new Intent(this, MueckenfangActivity.class));
				finish();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void schreibeHighscoreName() {
		EditText et = (EditText) findViewById(R.id.spielername);
		String name = et.getText().toString().trim();
		SharedPreferences pref = getSharedPreferences("GAME", 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("HIGHSCORE_NAME", name);
		editor.commit();
	}

	private String liesHighscoreName() {
		SharedPreferences pref = getSharedPreferences("GAME", 0);
		return pref.getString("HIGHSCORE_NAME", "");
	}
	
	private void highscoreAnzeigen() {
		int highscore = leseHighscore();
		if (highscore > 0) {
			tv.setText(Integer.toString(highscore) + " " + 
				getResources().getString(R.string.von) + " " + liesHighscoreName());
		} else {
			tv.setText(R.string.noLocalHighscore);
		}
	}
	
	private class WackleButton implements Runnable {

		@Override
		public void run() {
			startButton.startAnimation(animationWackeln);
			handler.postDelayed(wackelnRunnable, WACKELN_NACH_MS);
		}
	}
	
	private void internetHighscores(String nickname, int score, int max) {
		String highscores = "";
		try{
			AndroidHttpClient client = AndroidHttpClient.newInstance("Mueckenfang");
			String url = HIGHSCORE_SERVER_BASE_URL 
					+ "?game=" + HIGHSCORESERVER_GAME_ID
					+ "&name=" + URLEncoder.encode(nickname)
					+ "&score=" + Integer.toString(score)
					+ "&max=" + max;
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			InputStreamReader reader = new InputStreamReader(entity.getContent(), "utf-8");
			int c = reader.read();
			highscores = "";
			while (c != -1) {
				highscores += (char) c;
				c = reader.read();
			}
			//highscores = highscores.replace(",", ": ");
		} catch (UnknownHostException e) {
			highscores = "UnknownHostException";
		} catch(IOException e) {
			highscores = "IOException";
		}
		
		//highscores = "Franz Beckenbauer, 2000\nUdo Jürgens, 1000\nPatrick Owomojela, 200";
		
		//highscoresHtml = "";
		List<String> highscoreList = new ArrayList<String>();
		SimpleStringSplitter sss = new SimpleStringSplitter('\n');
		sss.setString(highscores);
		while(sss.hasNext()){
			highscoreList.add(sss.next());
		}
		/*for(String s : highscoreList) {
			highscoresHtml += "<b>" + s.replace(",", "</b><font color='red'>") 
				+ "</font><img src='muecke_'><br>";
		}
		runOnUiThread(new ZeigeHighscores());*/
		
		// standard layout
		if (findViewById(R.id.highscores) != null) {
			highscoresHtml = "";
			for(String s : highscoreList) {
				highscoresHtml += "<b>" + s.replace(",", "</b><font color='red'>") 
					+ "</font><img src='muecke_'><br>";
			}
			runOnUiThread(new ZeigeHighscores());
		}
		
		// toplist layout
		if (findViewById(R.id.list) != null) {
			list = (ListView) findViewById(R.id.list);
			adapter = new ToplistAdapter(this, 0, highscoreList);
			runOnUiThread(new ZeigeTopliste());
		}
	}
	
	private class ZeigeTopliste implements Runnable {
		@Override
		public void run() {
			list.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
	}
	
	private class HoleHighscores implements Runnable {
		private int max;
		public HoleHighscores(int i) {
			max = i;
		}

		@Override
		public void run() {
			internetHighscores("", 0, max);			
		}		
	}
	
	private class ZeigeHighscores implements Runnable {
		@Override
		public void run() {
			TextView tv = (TextView) findViewById(R.id.highscores);
//			if (highscores.equalsIgnoreCase("IOException")) {
//				tv.setVisibility(View.INVISIBLE);
//				TextView tvLabel = (TextView) findViewById(R.id.highscoreLabel);
//				tvLabel.setVisibility(View.INVISIBLE);
//			} else {
				tv.setText(Html.fromHtml(highscoresHtml, MueckenfangActivity.this, null));
//			}
		}
	}
	
	private class SendeHighscore implements Runnable {
		@Override
		public void run() {
			internetHighscores(liesHighscoreName(), leseHighscore(), 100);
		}
	}

	@Override
	public Drawable getDrawable(String name) {
		int id = getResources().getIdentifier(name, "drawable", this.getPackageName());
		Drawable d = getResources().getDrawable(id);
		d.setBounds(0, 0, 20, 20);//d.getIntrinsicWidth(), d.getIntrinsicHeight());
		return d;
	}
}
