package com.GuitarTuner;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils; 

import android.app.Activity;
import android.os.Bundle; 
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GuitarTunerActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
	private static final String TAG = "GuitarTuner";
	private PdUiDispatcher dispatcher;
	private Button eButton;
	private Button aButton;
	private Button dButton;
	private Button gButton;
	private Button bButton;
	private Button eeButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGui();
        try {
        	initPd();
        	loadPatch();
        	
        } catch (IOException e) {
        	Log.e(TAG, e.toString());
        	finish();
        }
    }
    
    private void initPd() throws IOException {
    	int sampleRate = AudioParameters.suggestSampleRate();
    	PdAudio.initAudio(sampleRate,0,2,8,true);
    	dispatcher = new PdUiDispatcher();
    	PdBase.setReceiver(dispatcher);
    	
    	eButton = (Button) findViewById(R.id.e_button);
    	eButton.setOnClickListener(this);
    	aButton = (Button) findViewById(R.id.a_button);
    	aButton.setOnClickListener(this);
    	dButton = (Button) findViewById(R.id.d_button);
    	dButton.setOnClickListener(this);
    	gButton = (Button) findViewById(R.id.g_button);
    	gButton.setOnClickListener(this);
    	bButton = (Button) findViewById(R.id.b_button);
    	bButton.setOnClickListener(this);
    	eeButton = (Button) findViewById(R.id.ee_button);
    	eeButton.setOnClickListener(this); 
    }
    
    private void loadPatch() throws IOException{
    	File dir = getFilesDir();
    	IoUtils.extractZipResource(getResources().openRawResource(R.raw.tuner),dir,true);
    	File patchFile = new File(dir,"tunner.pd");
    	PdBase.openPatch(patchFile.getAbsolutePath());
    	
    	
    }
    
    private void initGui() { 
        setContentView(R.layout.main);
    }
    
    @Override
    protected void onResume() { 
    	super.onResume();
    	PdAudio.startAudio(this);
    	
    }
    
    @Override
    protected void onPause() { 
    	super.onPause();
    	PdAudio.stopAudio();
    }
    
  
    private void triggerNote(int n) {
    	PdBase.sendFloat("midinote", n);
    	PdBase.sendBang("trigger");
    }
    
    @Override
    public void onClick(View v) { 
    	switch (v.getId()) {
    	case R.id.e_button : 
    		 triggerNote(40);
    			break;
    	case R.id.a_button :
			triggerNote(45);
			break;
    	case R.id.d_button :
			triggerNote(50);
			break;
    	case R.id.g_button :
			triggerNote(55);
			break;
    	case R.id.b_button :
			triggerNote(59);
			break;
    	case R.id.ee_button :
			triggerNote(64);
			break;
    		
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	PdAudio.release();
    	PdBase.release();
    }
}