package de.andriodnewcomer.mueckenfang;

import java.util.Date;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class GameActivity extends Activity implements 
	OnClickListener, Runnable, PreviewCallback{
	
	private static final int INTERVALL = 100;
	private static final int ZEITSCHEIBEN = 1;
	private Random zufallsgenerator = new Random();
	private Handler handler = new Handler();
	private FrameLayout spielbereich;
	private int runde;
	private int punkte;
	private int muecken;
	private int gefangeneMuecken;
	private int zeit;
	private float massstab;
	private static final long HOECHSTALTER_MS = 2000;
	private static final String ELEFANT = "ELEFANT";
	private MediaPlayer mp;
	private String[][] HIMMELSRICHTUNGEN = {
		{"nw", "n", "no"},
		{"w", "", "o"},
		{"sw", "s", "so"},
	};
	private int schwierigkeitsgrad;
	private CameraView cameraView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        
        cameraView = (CameraView) findViewById(R.id.camera);
		
        schwierigkeitsgrad = getIntent().getIntExtra("schwierigkeitsgrad", 0);
        spielbereich = (FrameLayout) findViewById(R.id.spielbereich);
        massstab = getResources().getDisplayMetrics().density;
        mp = MediaPlayer.create(this, R.raw.summen);
        spielStarten();
	}
	
	@Override
	protected void onDestroy() {
		mp.release();
		handler.removeCallbacks(this);
		super.onDestroy();
	}
	
	private void spielStarten() {
		runde = 0;
		punkte = 0;
		cameraView.setOneShotPreviewCallback(this);
		starteRunde();
	}
	
	private void starteRunde() {

		runde = runde + 1;
		muecken = runde * (20 + schwierigkeitsgrad*10);
		gefangeneMuecken = 0;
		zeit = ZEITSCHEIBEN;
		
		/*int id = getResources().getIdentifier("hintergrund"+Integer.toString(runde), "drawable", this.getPackageName());
		if(id>0) {
			LinearLayout l = (LinearLayout) findViewById(R.id.vordergrund);
			l.setBackgroundResource(id);
		}*/
		
		bildschirmAktualisieren();
        handler.postDelayed(this,INTERVALL);
	}
	
	@Override
    protected void onResume() {
    	super.onResume();
    	cameraView = (CameraView) findViewById(R.id.camera);
    }
	
	private boolean pruefeRundenende() {
		if (zeit == 0 || gefangeneMuecken == muecken) {
			starteRunde();
			return true;
		}
		return false;
	}
	
	private boolean pruefeSpielende() {
		if (zeit == 0 && gefangeneMuecken < muecken) {
			gameOver();
			return true;
		}
		return false;
	}
	
	private void gameOver() {
		setResult(punkte);
		Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
		dialog.setContentView(R.layout.gameover);
		
		/*Button btn = (Button) dialog.findViewById(R.id.gameoverbtn);
		btn.setOnClickListener(this);/*new View.OnClickListener(){
			public void onClick(View v) {
				Intent intent = new Intent(this, MueckenfangActivity.class);
				startActivityForResult(intent, 1);
			}}
		);*/
		dialog.show();
	}
	
	private void zeitHerunterzaehlen() {

		zeit = zeit - 1;
		
		float zufallszahl = zufallsgenerator.nextFloat();
		double wahrscheinlichkeit = muecken * 1.5 / ZEITSCHEIBEN;
		if (wahrscheinlichkeit >1 ) {
			eineMueckeAnzeigen();
			if (zufallszahl < wahrscheinlichkeit - 1) {
				eineMueckeAnzeigen();
			}
		} else {
			if (zufallszahl < wahrscheinlichkeit) {
				eineMueckeAnzeigen();
			}
		}
		mueckenVerschwinden();
		mueckenBewegen();
		bildschirmAktualisieren();
		if(!pruefeSpielende()) {
      		if(!pruefeRundenende()) {
      			handler.postDelayed(this, INTERVALL);
      			cameraView.setOneShotPreviewCallback(this);
      		}
   		}
	}
	
	private void mueckenBewegen() {
		int nummer = 0;
		while( nummer < spielbereich.getChildCount() ){
			ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
			int vx = (Integer) muecke.getTag(R.id.vx);
			int vy = (Integer) muecke.getTag(R.id.vy);
			
			FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) 
					muecke.getLayoutParams();
			params.leftMargin += vx * runde;
			params.topMargin += vy * runde;
			muecke.setLayoutParams(params);
			
			nummer++;
		}
	}

	private void bildschirmAktualisieren() {		
		TextView tvPunkte = (TextView) findViewById(R.id.points);
		tvPunkte.setText(Integer.toString(punkte));
		TextView tvRunde = (TextView) findViewById(R.id.round);
		tvRunde.setText(Integer.toString(runde));
		TextView tvTreffer= (TextView) findViewById(R.id.hits);
		tvTreffer.setText(Integer.toString(gefangeneMuecken));
		TextView tvRestzeit = (TextView) findViewById(R.id.time);
		tvRestzeit.setText(Integer.toString(zeit));
		FrameLayout flTreffer = (FrameLayout)findViewById(R.id.bar_hits);
		LayoutParams lpTreffer = flTreffer.getLayoutParams();
		lpTreffer.width = Math.round( massstab * 300 * Math.min( gefangeneMuecken,muecken) / muecken );
		FrameLayout flZeit = (FrameLayout)findViewById(R.id.bar_time);
		LayoutParams lpZeit = flZeit.getLayoutParams();
		lpZeit.width = Math.round( massstab * zeit * 300 / ZEITSCHEIBEN );
	}
	
	private void setzeBild(ImageView muecke, int vx, int vy) {
		muecke.setImageResource(
			getResources().getIdentifier(
					"muecke_" + HIMMELSRICHTUNGEN[vy+1][vx+1], 
					"drawable", 
					this.getPackageName()));
	}
	
	private void eineMueckeAnzeigen() {
		int breite = spielbereich.getWidth();
		int hoehe = spielbereich.getHeight();
		int muecke_breite = (int) Math.round(massstab * 50);
		int muecke_hoehe = (int) Math.round(massstab * 42);
		int links = zufallsgenerator.nextInt(breite - muecke_breite);
		int oben = zufallsgenerator.nextInt(hoehe - muecke_hoehe);
		
		int vx;
		int vy;
		
		ImageView muecke = new ImageView(this);
		
		do {
			
			vx = zufallsgenerator.nextInt(3) - 1;
			vy = zufallsgenerator.nextInt(3) - 1;
			
		} while (vx == 0 && vy == 0);
		
		
		double faktor = 1.0;
		if (vx != 0 && vy != 0) {
			faktor = 0.70710678;
		}
		
		if (zufallsgenerator.nextFloat() < 0.05) {
			muecke.setImageResource(R.drawable.elefant);
			muecke.setTag(R.id.tier, ELEFANT);
		} else {
			//muecke.setImageResource(R.drawable.muecke);
			setzeBild(muecke, vx, vy);
		}
		
		vx = (int) Math.round(massstab * vx * faktor);
		vy = (int) Math.round(massstab * vy * faktor);	
		
		muecke.setTag(R.id.vx, vx);
		muecke.setTag(R.id.vy, vy);
		
		muecke.setOnClickListener(this);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
		params.leftMargin = links;
		params.topMargin = oben;
		params.gravity = Gravity.TOP + Gravity.LEFT;
		muecke.setTag(R.id.geburtsdatum, new Date());
		spielbereich.addView(muecke, params);
		
		if (muecke.getTag(R.id.tier) != ELEFANT){
			mp.seekTo(0);
			mp.start();
		}
	}
	
	private void mueckenVerschwinden() {
		int nummer = 0;
		while(nummer < spielbereich.getChildCount()) {
			ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
			Date geburstdatum = (Date) muecke.getTag(R.id.geburtsdatum);
			long alter = (new Date()).getTime() - geburstdatum.getTime();
			if (alter > HOECHSTALTER_MS) {
				spielbereich.removeView(muecke);
				mp.pause();
			} else {
				nummer++;
			}
		}		
	}

	@Override
	public void onClick(View v) {
		if (v.getAnimation() == null){
			mp.pause();
		
			if (v.getTag(R.id.tier) == ELEFANT) {
				punkte -= 1000;
			} else {
				gefangeneMuecken++;
				punkte += 100 + schwierigkeitsgrad*100;
			}
			bildschirmAktualisieren();
			//spielbereich.removeView(v);
			Animation animationTreffer = AnimationUtils.loadAnimation(this, R.anim.treffer);
			animationTreffer.setAnimationListener(new MueckeAnimationListner(v));
			v.startAnimation(animationTreffer);
		}	
	}

	@Override
	public void run() {
		zeitHerunterzaehlen();
	}
	
	private class MueckeAnimationListner implements AnimationListener {

		private View muecke;
		
		public MueckeAnimationListner(View m) {
			muecke = m;
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					spielbereich.removeView(muecke);
				}
			});
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
	}

	@Override
	public void onPreviewFrame(byte[] bild, Camera camera) {
		
		int breite = camera.getParameters().getPreviewSize().width;
		int hoehe = camera.getParameters().getPreviewSize().height;
		
		
		if (camera.getParameters().getPreviewFormat() == ImageFormat.NV21 && spielbereich.getChildCount()>0) {
			NV21Image nv21 = new NV21Image(bild, breite, hoehe);
			mueckenAufTomatenPruefen(nv21);
		}
		
		
	}

	private void mueckenAufTomatenPruefen(NV21Image nv21) {
		int nummer = 0;
		while(nummer < spielbereich.getChildCount()){
			ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
			if (mueckeBeruehrtTomate(muecke, nv21)) {
				mp.pause();
				gefangeneMuecken++;
				punkte += 100 + schwierigkeitsgrad*100;
				bildschirmAktualisieren();
				spielbereich.removeView(muecke);
			} else {
				nummer++;
			}
		}
	}

	private boolean mueckeBeruehrtTomate(ImageView muecke, NV21Image nv21) {
		/*float faktorHorizontal = nv21.getHoehe() * 1.0f / getResources().getDisplayMetrics().widthPixels;
		float faktorVertical = nv21.getBreite() * 1.0f / getResources().getDisplayMetrics().heightPixels;
		Rect ausschnitt = new Rect();
		ausschnitt.bottom = Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getLeft());
		ausschnitt.top = Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getRight());
		ausschnitt.right = Math.round(faktorVertical * muecke.getBottom());
		ausschnitt.left = Math.round(faktorVertical * muecke.getTop());
		int rotePixel = nv21.zaehleRotePixel(ausschnitt);
		if (rotePixel > 10) {
			return true;
		}
		return false;
		*/
		float faktorHorizontal = nv21.getHoehe()*1.0f / getResources().getDisplayMetrics().widthPixels;
		float faktorVertikal = nv21.getBreite()*1.0f / getResources().getDisplayMetrics().heightPixels;
		Rect ausschnitt = new Rect();
		ausschnitt.bottom= Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getLeft());
		ausschnitt.top   = Math.round(nv21.getHoehe() - faktorHorizontal * muecke.getRight());
		ausschnitt.right = Math.round(faktorVertikal * muecke.getBottom());
		ausschnitt.left  = Math.round(faktorVertikal * muecke.getTop());
		int rotePixel = nv21.zaehleRotePixel(ausschnitt);
		if(rotePixel > 10) {
			return true;
		}
		return false;
	}
}
