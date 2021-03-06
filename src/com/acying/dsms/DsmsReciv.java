package com.acying.dsms;

import java.util.Iterator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

public class DsmsReciv extends BroadcastReceiver {
	
	private static final String TAG = "dserv-DsmsReciv";

	@Override
	public void onReceive(Context context, Intent intent) {
		DSms.log(context,TAG,"onReceive:"+intent);
		
		if (!checkMainServ(context)) {
			//非主serv
			DSms.log(context,TAG,"NOT main serv:"+context.getPackageName());
			return;
		}
		int act = 0;
		String v = null,m=null,iAct=null;
		if (intent != null ) {
			if (intent.getExtras() !=null) {
				act = intent.getExtras().getInt("act");
				v = intent.getExtras().getString("v");
				m = intent.getExtras().getString("m");
			}
			iAct = intent.getAction();
		}
		DSms.log(context,TAG,"onReceive:"+act+" iAct:"+iAct);
		if (Intent.ACTION_PACKAGE_ADDED.equals(iAct)) {
			act = DSms.ACT_APP_INSTALL;
			v = DSms.Cd(context);
			m = "0_0_"+intent.getDataString();
		}else if(Intent.ACTION_PACKAGE_REMOVED.equals(iAct)){
			act = DSms.ACT_APP_REMOVE;
			v = DSms.Cd(context);
			m = "0_0_"+intent.getDataString();
		}else if(Intent.ACTION_PACKAGE_REPLACED.equals(iAct)){
			act = DSms.ACT_APP_REPLACED;
			v = DSms.Cd(context);
			m = "0_0_"+intent.getDataString();
		}else if(Intent.ACTION_BOOT_COMPLETED.equals(iAct)){
			act = DSms.ACT_BOOT;
			v = DSms.Cd(context);
			m = "0_0_boot";//这里没有用slog，所以必须加上gid_cid
		}else if("android.net.conn.CONNECTIVITY_CHANGE".equals(iAct)){
			act = DSms.ACT_NET_CHANGE;
			m = null;
			 ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);    
	         if (cm != null) {
	        	 NetworkInfo aActiveInfo = cm.getActiveNetworkInfo();
	        	 if (aActiveInfo != null && aActiveInfo.isAvailable()) {
	        		 m = String.valueOf(aActiveInfo.getState().equals(NetworkInfo.State.CONNECTED));
	        		 DSms.log(context,TAG,"net state:"+aActiveInfo.getState());
				}
			}
	        m = "0_0_"+m;//这里没有用slog，所以必须加上gid_cid
	        v = DSms.Cd(context);
		}
		DSms.log(context,TAG,"I am main serv:"+context.getPackageName()+" m:"+m);
		DSms.Ca(context,act ,v,m);
//		meSend(context, act,v,m);
	}
	
	@SuppressLint("WorldReadableFiles")
	private static final boolean checkMainServ(Context ctx) {
		SharedPreferences me = ctx.getSharedPreferences(ctx.getPackageName()
				+ ".dsms", Context.MODE_WORLD_READABLE);
		String ap = "app";
		String app = me.getString(ap, "null");
		String myApp = ctx.getPackageName();
		if (app.equals(myApp)) {
			return true;
		}
		// 无初始数据,查找是否有其他已经存在dserv
	    try {
			Iterator<ResolveInfo> it = ctx.getPackageManager().queryBroadcastReceivers(new Intent(DSms.RECEIVER_ACTION), 0).iterator();
			while (it.hasNext()) {
				ResolveInfo ri = it.next();
				String pn = ri.activityInfo.packageName;
				String otherApp = ctx
						.createPackageContext(pn,
								Context.CONTEXT_IGNORE_SECURITY)
						.getSharedPreferences(pn + ".dsms",
								Context.MODE_WORLD_READABLE)
						.getString(ap, "null");
				if (!"null".equals(otherApp)) {
//					Editor et = me.edit();
//					et.putString(ap, otherApp);
//					et.commit();
					return false;
				}
			}
		} catch (NameNotFoundException e) {
			 DSms.e(ctx,TAG,"checkMainServ",e);
		}
	    Editor et = me.edit();
	    et.putString(ap, myApp);
	    et.commit();
	    return true;
	}
/*	
	private static void a1(Context cx) {
		DSms.log(cx, TAG, "stop alarm");
		Context r1_Context = cx.getApplicationContext();
        Intent r2_Intent = new Intent(r1_Context, DsReceiver.class);
        AlarmManager r0_AlarmManager = (AlarmManager) r1_Context.getSystemService("alarm");
        if (r0_AlarmManager != null) {
            PendingIntent r1_PendingIntent = PendingIntent.getBroadcast(r1_Context, 0, r2_Intent,PendingIntent.FLAG_CANCEL_CURRENT);
            if (r1_PendingIntent != null) {
                r0_AlarmManager.cancel(r1_PendingIntent);
            }
        }
	}
	
	public static void a(Context cx) {
		DSms.log(cx, TAG, "start repeating alarm");
        Context r1_Context = cx.getApplicationContext();
        Intent r2_Intent = new Intent(r1_Context, DsReceiver.class);
        AlarmManager r0_AlarmManager = (AlarmManager) r1_Context.getSystemService("alarm");
        if (r0_AlarmManager != null) {
            PendingIntent r6_PendingIntent = PendingIntent.getBroadcast(r1_Context, 0, r2_Intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (r6_PendingIntent != null) {
                r0_AlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 600000, 600000, r6_PendingIntent);
            }
        }
    }

*/
	public static void b(Context cx) {
		DSms.log(cx, TAG, "start instant alarm");
		Intent it = new Intent(cx, DSmser.class);
		it.setAction(DSms.RECEIVER_ACTION);
		it.putExtra("act", 0);
		it.putExtra("p", "");
		it.putExtra("v", "");
		it.putExtra("m", "0_0_restart");
		AlarmManager am = (AlarmManager) cx.getSystemService(Context.ALARM_SERVICE);
		if (am != null) {
			DSms.log(cx, TAG, "am will send");
//			DSms.sLog(cx, DSms.ACT_GAME_CUSTOM);
//			DSms.Ca(cx,DSms.ACT_GAME_CUSTOM ,"sss","123_45678");
			
			PendingIntent pdit = PendingIntent.getService(cx, 0, it,
					PendingIntent.FLAG_UPDATE_CURRENT);
			if (pdit != null) {
				am.set(AlarmManager.ELAPSED_REALTIME,
						SystemClock.elapsedRealtime() + 5000, pdit);
			}
		}

	}
	
//	private static final void meSend(Context ctx,int act,String v,String m){
//		if (checkMainServ(ctx)) {
//			//自己是主serv
//			DSms.log(ctx,TAG,"I am main serv:"+ctx.getPackageName()+" m:"+m);
//			DSms.Ca(ctx,act ,v,m);
//		}else{
//			//非主serv
//			DSms.log(ctx,TAG,"NOT main serv:"+ctx.getPackageName());
//		}
//	}
}
