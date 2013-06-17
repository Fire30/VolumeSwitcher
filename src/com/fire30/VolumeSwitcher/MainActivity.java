//TJ Corley
//https://github.com/Fire30/
//App licensed under MIT

package com.fire30.VolumeSwitcher;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class MainActivity extends Activity{


	//would make as Array, but I need to add 'select school'.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        
        CheckBox doubletap = (CheckBox)findViewById(R.id.doubletap);
        SharedPreferences settings = MainActivity.this.getSharedPreferences("com.fire30.VolumeSwitcher", 
				Context.MODE_WORLD_READABLE);
        if(settings.getBoolean("doubletap",false))
        	doubletap.setChecked(true);
        else
        	doubletap.setChecked(false);
        	
        doubletap.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				CheckBox doubletap = (CheckBox)findViewById(R.id.doubletap);
				SharedPreferences settings = MainActivity.this.getSharedPreferences("com.fire30.VolumeSwitcher", 
																					Context.MODE_WORLD_READABLE);
				SharedPreferences.Editor editore = settings.edit();
				if(doubletap.isChecked())
					editore.putBoolean("doubletap", true);
	        	else
	        		editore.putBoolean("doubletap", false);
				editore.commit();
				editore.apply();
				
			}
        
        });
        
    }
   
}
