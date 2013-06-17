/*
 *Fire30
 *MIT License
 *Note this only works when screen is off. 
 *if someone want to help make it work at all times, it would be great.
 */
package com.fire30.VolumeSwitcher;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.saurik.substrate.MS;

public class Main {
	public static boolean stillPressing;
	public static int volumePresesSinceFirst;
	public static boolean isPaused = true;//Device starts off as paused...eg not playing music
	static void initialize() {
		MS.hookClassLoad("com.android.internal.policy.impl.PhoneWindowManager", new MS.ClassLoadHook() {
			@Override
			public void classLoaded(Class<?> resources) {
				Method interceptKeyBeforeQueueing;
				try {
					interceptKeyBeforeQueueing = resources.getMethod("interceptKeyBeforeQueueing"
							,KeyEvent.class,Integer.TYPE,Boolean.TYPE);
				} catch (Exception e) {
					interceptKeyBeforeQueueing = null;
				}
				if (interceptKeyBeforeQueueing != null) {
					MS.hookMethod(resources, interceptKeyBeforeQueueing, alteredMethod());
				}
			};
		});
	}
	static MS.MethodAlteration<Object, Integer> alteredMethod()
	{
		return (new MS.MethodAlteration<Object, Integer>() {
			@Override
			public Integer invoked(final Object resources, Object... args)
					throws Throwable
					{
				KeyEvent event = (KeyEvent)args[0];
				//Don't use any arguments but the first
				int keyCode = event.getKeyCode();
				boolean isMusicActive = (Boolean) resources.getClass().getDeclaredMethod("isMusicActive")
						.invoke(resources,(Object[])null);
				boolean down = (event.getAction() == KeyEvent.ACTION_DOWN);
				Object telephonyService = resources.getClass().getDeclaredMethod("getTelephonyService")
						.invoke(resources,(Object[])null);
				final Method isRinging = telephonyService.getClass().getDeclaredMethod("isRinging");
				final Method isOffhook = telephonyService.getClass().getDeclaredMethod("isOffhook");
				Handler mHandler = (Handler)resources.getClass().getDeclaredField("mHandler").get(resources);
				final Method handleVolumeKey = resources.getClass()
						.getDeclaredMethod("handleVolumeKey",Integer.TYPE,Integer.TYPE);
				final int ACTION_PASS_TO_USER = resources.getClass().getInterfaces()[0]
						.getDeclaredField("ACTION_PASS_TO_USER").getInt(null);
				final Context mContext = (Context)resources.getClass().getDeclaredField("mContext").get(resources);
				final Context settingsContext = mContext.createPackageContext("com.fire30.VolumeSwitcher", 
																			  Context.MODE_WORLD_WRITEABLE);
				SharedPreferences settings = settingsContext.getSharedPreferences("com.fire30.VolumeSwitcher", 
																				  Context.MODE_WORLD_READABLE);
				System.out.println(settings.getAll().toString());
				final boolean doubletap = settings.getBoolean("doubletap", false);
				//Get all variables and methods that we are using via reflection, pretty ugly
				//Since most are from the android internals we can't ever cast them.
				Runnable mVolumeUpLongPress = new Runnable() {
					@Override
					public void run() {
						try 
						{
							Integer theKeyCode = null;
							boolean shouldDoAction = true;
							if(volumePresesSinceFirst > 1)
							{
								if(doubletap)
								{
									theKeyCode = isPaused ? KeyEvent.KEYCODE_MEDIA_PLAY :
															KeyEvent.KEYCODE_MEDIA_PAUSE;
								
									isPaused = !isPaused;
								}
								else
								{
									shouldDoAction = false;
								}
							}
							else if(stillPressing && volumePresesSinceFirst <= 1)
							{
								theKeyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
							}
							if(!(theKeyCode == null) && shouldDoAction)
							{
								Object[] volumeArgs = {AudioManager.STREAM_MUSIC,KeyEvent.KEYCODE_VOLUME_DOWN};
								handleVolumeKey.invoke(resources,volumeArgs);
								long eventtime = SystemClock.uptimeMillis();
								Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
								KeyEvent keyEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, theKeyCode, 0);
								keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
								mContext.sendOrderedBroadcast(keyIntent, null);
								keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
								keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
								mContext.sendOrderedBroadcast(keyIntent, null);
							}
							volumePresesSinceFirst = 0;
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					};
				};
				Runnable mVolumeDownLongPress = new Runnable() {
					@Override
					public void run() {
						try 
						{
							Integer theKeyCode = null;
							boolean shouldDoAction = true;
							if(volumePresesSinceFirst > 1)
							{
								if(doubletap)
								{
									theKeyCode = isPaused ? KeyEvent.KEYCODE_MEDIA_PLAY :
															KeyEvent.KEYCODE_MEDIA_PAUSE;
								
									isPaused = !isPaused;
								}
								else
								{
									shouldDoAction = false;
								}
							}
							else if(stillPressing && volumePresesSinceFirst <= 1)
							{
								theKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
							}
							if(!(theKeyCode == null) && shouldDoAction)
							{
								Object[] volumeArgs = {AudioManager.STREAM_MUSIC,KeyEvent.KEYCODE_VOLUME_UP};
								handleVolumeKey.invoke(resources,volumeArgs);
								long eventtime = SystemClock.uptimeMillis();
								Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
								KeyEvent keyEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, theKeyCode, 0);
								keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
								mContext.sendOrderedBroadcast(keyIntent, null);
								keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
								keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
								mContext.sendOrderedBroadcast(keyIntent, null);
							}
							volumePresesSinceFirst = 0;
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					};
				};
				if(isMusicActive)
					isPaused = false;
				if((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || 
						keyCode == KeyEvent.KEYCODE_VOLUME_UP) && 
						/*isMusicActive &&*/
						((invoke(resources,args)) & ACTION_PASS_TO_USER) == 0)
					//If Music is on, Volume Key is pressed
					//and the original action does not pass to user
					//then we can move on.
					//this is only true on lockscreen
				{	
					if(!down)
					{
						stillPressing = false;
						mHandler.removeCallbacks(mVolumeUpLongPress);
						mHandler.removeCallbacks(mVolumeDownLongPress);
						//removing callbacks does not seem to work for me.
						//Even though the original class is littered with them.
						//It's why I am using the static variables
						if (isMusicActive &&
								!resources.getClass().getDeclaredField("mIsLongPress").getBoolean(resources))
						{
							//If no long press, than just do original(change volume).
							return invoke(resources,args);
						}
					}
					if(down)
					{
						if(telephonyService != null && 
								((Boolean)isRinging.invoke(telephonyService,(Object[])null) || 
										(Boolean)isOffhook.invoke(telephonyService,(Object[])null)))
						{
							//If something with the telephone is happening just do original function
							return invoke(resources,args);
						}
						else
						{
							resources.getClass().getDeclaredField("mIsLongPress").setBoolean(resources, false);
							Runnable btnHandler;
							if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
								btnHandler = mVolumeUpLongPress;
							else
								btnHandler = mVolumeDownLongPress;
							//Sets what button is pressed.
							stillPressing = true;
							volumePresesSinceFirst++;
							mHandler.postDelayed(btnHandler, ViewConfiguration.getLongPressTimeout());
							//set it to go off after button press.
						}
					}
					return 0;
				}
				else
				{
					return invoke(resources,args);
				}
			}
		});
	}
}