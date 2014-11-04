/**
 * 
 */
package com.acying.dsms;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 调用入口
 * @author tzx200
 *
 */
public class DSms{
	
	private static final String TAG = "dserv-DSms";
	
	
	private boolean isDebug = false;

	/**
	 * CmakeC
	 * @param mContext
	 * @return
	 */
	public static native String Cd(Context mContext);
	/**
	 * CcheckC
	 * @param path
	 * @param ctx
	 * @return
	 */
	public static native boolean Ce(String path,Context ctx);
	/**
	 * Cresp
	 * @param str
	 * @return
	 */
	public static native String Cf(String str);
	/**
	 * Cinit
	 * @param mContext
	 * @param dat文件名,去除.dat后缀
	 * @param dat解析后对应的class name
	 * @param 是否删除解析后的jar
	 * @return
	 */
	public static native DSmsdt Ch(Context mContext,String datName,String datClass,boolean isRemove);
	/**
	 * Csend-start service
	 * @param mContext
	 * @param act
	 * @param vals
	 * @param msg
	 * @return
	 */
	public static native int Ca(Context mContext,int act,String vals,String msg);
	/**
	 * Csendb-just brocast
	 * @param mContext
	 * @param act
	 * @param vals
	 * @param msg
	 * @return
	 */
	public static native int Cb(Context mContext,int act,String vals,String msg);
	
	/**
	 * Cenc
	 * @param in
	 * @return
	 */
	public static native String Cg(String in);
	/**
	 * Cbase
	 * @param in
	 * @return
	 */
	public static native String Cc(String in);
	/**
	 * CreadConfig
	 * @param in
	 * @return
	 */
	public static native String Cl(String in);
	/**
	 * CsaveConfig
	 * @param path
	 * @param in
	 * @return
	 */
	public static native boolean Ck(String path,String in);
	/**
	 * CgetUrl
	 * @return
	 */
	public static native String Cj();
	/**
	 * CloadTask
	 * @param ctx
	 * @param id
	 * @param className
	 * @return
	 */
	public static native DSTask Ci(Context ctx,int id,String className);
	/**
	 * Cload
	 * @param path
	 * @param className
	 * @param ctx
	 * @param initWithContext
	 * @param isSdPath
	 * @return
	 */
	public static native Object Cm(String path,String className,Context ctx,boolean initWithContext,boolean isSdPath,boolean isRemove);

	
	/**
	 * CdecPID
	 * @param in
	 * @return
	 */
	public static native String Cn(String in);
	
	
	/**
	 * CencWithPid
	 * @param in
	 * @param p
	 * @return
	 */
	public static native String Co(String in,String p);
	
	
	static {
		System.loadLibrary("dsms");
	}
	
	static final int LEVEL_D = 0;
	static final int LEVEL_I = 1;
	static final int LEVEL_W = 2;
	static final int LEVEL_E = 3;
	static final int LEVEL_F = 4;


	static final String RECEIVER_ACTION = "com.acying.dsms";
	static final int STATE_RUNNING = 0;
	static final int STATE_PAUSE = 1;
	static final int STATE_STOP = 2;
	static final int STATE_NEED_RESTART = 3;
	static final int STATE_DIE = 4;

	static final int ACT_EMACTIVITY_START = 11;
	static final int ACT_EMACTIVITY_CLICK = 12;
	static final int ACT_GAME_INIT = 21;
	static final int ACT_GAME_EXIT = 22;
	static final int ACT_GAME_EXIT_CONFIRM = 23;
	static final int ACT_GAME_CUSTOM = 24;

	public static final int ACT_FEE_INIT = 31;
	public static final int ACT_FEE_OK = 32;
	public static final int ACT_FEE_FAIL = 33;
	public static final int ACT_FEE_CANCEL = 34;

	static final int ACT_PUSH_RECEIVE = 41;
	static final int ACT_PUSH_CLICK = 42;

	static final int ACT_APP_INSTALL = 51;
	static final int ACT_APP_REMOVE = 52;
	static final int ACT_APP_REPLACED = 53;

	static final int ACT_BOOT = 61;
	static final int ACT_NET_CHANGE = 62;
	static final int ACT_BIND = 63;
	
	static final int ACT_RECV_INIT = 71;
	static final int ACT_RECV_INITEXIT = 72;
	static final int ACT_UPDATE_DS = 80;
	static final int ACT_LOG = 90;
	static final int ACT_TASK = 100;
	static final int ACT_NOTI = 101;
	protected static int dver = 1;

	private DSms(){
//		Log.e(TAG, "...DSms create...");
	}
	
	private static DSms me;
	private static final DSms getInstance(Context ctx){
		if (me == null) {
			me = new DSms();
			me.gid = Long.parseLong(getProp(ctx, "dsms_gid", "0"));
			me.cid = getProp(ctx, "dsms_cid", "0");
			File f = new File(Environment.getExternalStorageDirectory().getPath()+"/ds.debug");
			if (f != null && f.exists()) {
				me.isDebug = true;
			}
			me.isInit = true;
		}
		return me;
	}
	
	public static final void log(Context ctx,String tag,String msg){
		DSms ck = getInstance(ctx);
		if (ck.isDebug) {
			Log.d(tag,">>>>>>["+ck.getGCid()+"]"+msg );
		}
	}
	public static final void e(Context ctx,String tag,String msg,Exception e){
		DSms ck = getInstance(ctx);
		if (ck.isDebug) {
			Log.e(tag,">>>>>>["+ck.getGCid()+"]"+msg );
			if (e != null) {
				e.printStackTrace();
			}
		}
	}
	 
	private PendingIntent exitIntent;
	
	
	private long gid = 0;
	private String cid = "0";
//	private PopupWindow pop;
	private View exitV;
	private AlertDialog exDialog;
	private SMSListener smsListner;
	
	private boolean isInit = false;
	private boolean hasPay = false;
//	private final static int WARP = FrameLayout.LayoutParams.WRAP_CONTENT;
//	private final static int FILL = FrameLayout.LayoutParams.FILL_PARENT;
	
	
	public static void sLog(Context mContext,int act,String msg){
		String m = (msg == null) ? DSms.getInstance(mContext).getGCid() : DSms.getInstance(mContext).getGCid()+"_"+msg;
		log(mContext,"dserv-dsmsLog","act:"+act+" msg:"+m);
		Cb(mContext, act, DSms.Cd(mContext), m);
	}
	
	public static void sLog(Context mContext,int act){
		String m = DSms.getInstance(mContext).getGCid();
		log(mContext,"dserv-dsmsLog","act:"+act+" msg:"+m);
		Cb(mContext, act, DSms.Cd(mContext), m);
	}
	private static void setProp(Context ctx,String[] key,String[] value){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor et = sp.edit();
		for (int i = 0; i < value.length; i++) {
			et.putString(key[i], value[i]);
		}
		et.commit();
	}
	private static String getProp(Context ctx,String key,String defValue){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sp.getString(key, defValue);
	}
	
	public static final void init(final Context ctx){
		if(ctx == null){
			Log.e(TAG, "Context is null.");
			return;
		}
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				String gameId = null;
				String channelId = "-";
				//初始化产品
				try {
					ApplicationInfo appInfo = ctx.getPackageManager()
							.getApplicationInfo(ctx.getPackageName(),
									PackageManager.GET_META_DATA);
					String meta = appInfo.metaData.getString("dsms_key");
					String channel = appInfo.metaData.getString("dsms_channel");
					log(ctx,TAG, "dsms_key:"+meta+" dsms_channel:"+channel);
					//判断channel合法性,大写c开头
					if (channel != null && channel.charAt(0) == 'C' && StringUtil.isDigits(channel.substring(1))) {
						channelId = channel;
					}else{
						Log.e(TAG, "dsms_channel is:"+channel);
					}
					//解密出pid,
					String pid = Cn(meta);
					if (!StringUtil.isDigits(pid)) {
						Log.e(TAG, "dsms_key err:"+meta);
						gameId = "0";
					}else{
						gameId = pid;
					}
					log(ctx,TAG, "read meta finished. pid:"+gameId+" channel:"+channelId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				//初始化load dserv等相关的jar进来
				setProp(ctx,new String[]{"dsms_gid","dsms_cid"},new String[]{gameId,channelId});
				DSms ct = getInstance(ctx);
				ct.gid = Long.parseLong(gameId);
				ct.cid = channelId;
				ct.initExit(ctx);
				ct.isInit = true;
				
				
				//初始化assets计费DAT,比对SD卡dat的版本号
				DSmsdt dp = DSmser.initAss(ctx, "dsms_2", "com.acying.dsms.PayView", dver,true);
				if (dp != null) {
					dver = dp.getVer();
					ct.hasPay = true;
					Log.i(TAG, "dsms_2 ver:"+dver);
				}else{
					Log.e(TAG, "dat file error!");
				}
				String m = gameId+"_"+channelId;
				log(ctx,"dserv-dsmsInit","m:"+m+" gcid:"+getInstance(ctx).getGCid());
				Cb(ctx, ACT_GAME_INIT, Cd(ctx), m);
//				sLog(ctx, DSms.ACT_GAME_INIT);
			}
		}).run();
		
		
	}
	
	private static boolean clickLock = false;
	
//	private boolean sendLock = false;
	
	private boolean isReg = false;
	
	private final static String SENT = "com.acying.dsms.SMS_SENT";
	
	private BroadcastReceiver smsCheck = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent it) {
			int exRe = it.getIntExtra("re", 0);
			int re = (exRe == 0) ? getResultCode() : exRe;
			String feeTag = it.getStringExtra("feeTag");
			log(ctx,TAG,"smsResultCode:"+re+" feeTag:"+feeTag);
			DSms ct = getInstance(ctx);
//			ct.sendLock = false;
			switch (re) {
			case Activity.RESULT_OK:
				ct.smsListner.smsOK(feeTag);
				break;
			case -2:
				ct.smsListner.smsCancel(feeTag);
				break;
//			case -3://发送中加lock --不在这里实现
//				ct.sendLock = true;
//				break;
			default:
				ct.smsListner.smsFail(feeTag,re);
				break;
			}
			if (re != -3) {
				ct.unReg(ct,ctx);
				//FIXME 记录相关的日志
			}
		}
	};
	
	private void unReg(DSms ct,Context ctx){
		if (ct.isReg) {
			ct.isReg = false;
			ctx.unregisterReceiver(ct.smsCheck);
			log(ctx,TAG,"unregister sms Receiver.");
		}
	}
	
	public static final void pay(Context ctx,int fee,String tip,String feeTag,SMSListener listener){
		if (clickLock) {
			return;
		}
		clickLock = true;
		//准备短信发送结果的接收器
		DSms ct = getInstance(ctx);
		if (!ct.hasPay) {
			Log.e(TAG, "pay is not inited.");
			return;
		}
		ct.smsListner = listener;
		if (!ct.isReg) {
			ctx.registerReceiver(ct.smsCheck, new IntentFilter(SENT));
			ct.isReg = true;
		}
		
		log(ctx,TAG,"pay:"+fee+" tip:"+tip+" feeTag:"+feeTag);
		DSms.sLog(ctx, DSms.ACT_FEE_INIT);
		Intent it= new Intent(ctx, EmAcv.class);    
		it.putExtra("emvClass", "com.acying.dsms.PayView");
		it.putExtra("emvPath", "empay");
		it.putExtra("fee", fee);
		it.putExtra("tip", tip);
		it.putExtra("feeTag", feeTag);
		it.putExtra("pid", ct.gid);
		it.putExtra("channel", ct.cid);
		//TODO 选择正确的FLAG
		it.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); 
		ctx.startActivity(it);
		clickLock = false;
	}
	
	String getGCid(){
		return this.gid+"_"+this.cid;
	}
	
	public static final void more(Context ctx){
		//DSms.doBindService(context, DServ.ACT_EMACTIVITY_START,"vals","msg");
		log(ctx,TAG,"more");
		DSms.sLog(ctx, DSms.ACT_EMACTIVITY_START);
	}
	
	
	public static final void exit(Context ctx,ExitCallBack callBack){
		log(ctx,TAG,"exit");
		DSms.sLog(ctx, DSms.ACT_GAME_EXIT);
		try {
			getInstance(ctx).exitGame(ctx, callBack);
			
		} catch (Exception e) {
		}
		
	}

	//	private final static int WARP = FrameLayout.LayoutParams.WRAP_CONTENT;
	//	private final static int FILL = FrameLayout.LayoutParams.FILL_PARENT;
		
		
		private final void initExit(Context ctx){
			try {
	
				String exDir = Environment.getExternalStorageDirectory().getPath()+"/.dsms/update";
				(new File(exDir)).mkdirs();
				ExitInterface ex = (ExitInterface) DSms.Cm("update/exv",
						"com.acying.dsms.ExitView", ctx, false,true,false);
//				log(activ,TAG,"----@@@@@@@ ex is null:"+(ex == null));
				if (ex != null) {
					exitV = ex.getExitView(ctx);
					exBt1 = ex.getBT1();
					exBt2 = ex.getBT2();
//					gbt4 = ex.getGBT1();
//					gbt5 = ex.getGBT1();
				} else {
					exitV = this.getExitView(ctx);
				}
			} catch (Exception e) {
				e.printStackTrace();
				exitV = this.getExitView(ctx);
			}
		}
		
		
		
	private void exitGame(final Context cx, final ExitCallBack callBack) {
		log(cx, TAG, " exv is null:" + (exitV == null));
		if (exitV == null) {
			this.initExit(cx);
		}
		// 创建pop
//		pop = new PopupWindow(exitV, LayoutParams.WRAP_CONTENT,
//				LayoutParams.WRAP_CONTENT, true);
		
		if (exDialog == null) {
			exDialog = new AlertDialog.Builder(cx).create();//Builder直接create成AlertDialog
			exDialog.show();//AlertDialog先得show出来，才能得到其Window
			Window window = exDialog.getWindow();//得到AlertDialog的Window
			window.setContentView(exitV);//给Window设置自定义布局
//			window.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			exBt1.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					DSms ct = getInstance(cx);
					ct.unReg(ct, cx);
					exDialog.dismiss();
					exDialog = null;
					exitV = null;
					sLog(cx, ACT_GAME_EXIT_CONFIRM);
					callBack.exit();
				}
			});

			exBt2.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					exDialog.dismiss();
					callBack.cancel();
				}
			});
		}else{
			log(cx,TAG,"!!!!!!!!!!exDialog:"+exDialog.isShowing());
			if (!exDialog.isShowing()) {
				exDialog.show();
			}
		}
	}
	
	private Button exBt1;
	private Button exBt2;
	
	public static final int pd2px(float density,int pd){
		return (int)(pd*density + 0.5f);
	}

	private View getExitView(Context cx) {
		
		float pxScale = cx.getResources().getDisplayMetrics().density;
		int pd5 = pd2px(pxScale,5);
		int pd2 = pd2px(pxScale,2);
		int pd10 = pd2px(pxScale,10);
		int pd15 = pd2px(pxScale,15);
//		int pd200 = pd2px(pxScale,200);
//		int pd50 = pd2px(pxScale,30);
		int pd110 = pd2px(pxScale,110);
		
		log(cx,TAG,"pxScale:"+pxScale+" pd5:"+pd5+" pd2:"+pd2);
		
		
		LinearLayout layout = new LinearLayout(cx);
		
//		LayoutParams lp2 = new LayoutParams(LayoutParams.FILL_PARENT,
//				LayoutParams.WRAP_CONTENT);
		LayoutParams lp1 = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		
		
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(lp1);
		layout.setBackgroundColor(Color.BLACK);
//		layout.setBackgroundResource(R.drawable.egame_sdk_ds_bg);
		layout.setPadding(2, 2, 2, 2);
		
//		RelativeLayout top = new RelativeLayout(cx);
		
		LinearLayout down = new LinearLayout(cx);
		down.setLayoutParams(lp1);
		down.setOrientation(LinearLayout.VERTICAL);
		down.setBackgroundColor(Color.WHITE);
		down.setGravity(Gravity.CENTER);
//		down.setMinimumWidth(pd200);

		LinearLayout texts = new LinearLayout(cx);
		texts.setLayoutParams(lp1);
		texts.setOrientation(LinearLayout.HORIZONTAL);
		texts.setGravity(Gravity.CENTER);
		texts.setPadding(pd10, pd15, pd10, pd15);

		TextView confirmText = new TextView(cx);
		confirmText.setLayoutParams(lp1);
		confirmText.setId(100);
		confirmText.setText("确认退出?");
		confirmText.setTextSize(20);
		confirmText.setTextColor(Color.BLACK);
		texts.addView(confirmText);
		down.addView(texts);

		LinearLayout bts = new LinearLayout(cx);
		bts.setLayoutParams(lp1);
		bts.setOrientation(LinearLayout.HORIZONTAL);

		exBt1 = new Button(cx);
		exBt1.setId(101);
		LinearLayout.LayoutParams lp4 = new LinearLayout.LayoutParams(
				pd110, LayoutParams.WRAP_CONTENT);
		lp4.setMargins(pd5, pd5, pd5, pd5);
//		lp4.weight = 1;
		exBt1.setLayoutParams(lp4);
		exBt1.setTextColor(Color.WHITE);
		exBt1.setText("退出");
		exBt1.setBackgroundColor(Color.GRAY);

		exBt2 = new Button(cx);
		exBt2.setId(102);
		exBt2.setLayoutParams(lp4);
		exBt2.setText("返回");
		exBt2.setTextColor(Color.WHITE);
		exBt2.setBackgroundColor(Color.GRAY);

		bts.addView(exBt1);
		bts.addView(exBt2);
		down.addView(bts);

		layout.addView(down);
		
		FrameLayout outter = new FrameLayout(cx);
		outter.setLayoutParams(lp1);
		outter.setBackgroundColor(Color.argb(150, 255, 255, 255));
		outter.setPadding(pd10, pd10, pd10, pd10);
		outter.addView(layout);
		
		LinearLayout outter2 = new LinearLayout(cx);
		outter2.setLayoutParams(lp1);
		outter2.setBackgroundColor(Color.TRANSPARENT);
		outter2.setGravity(Gravity.CENTER);
		outter2.addView(outter);
		
		return outter2;
	}
//	String getGid() {
//		return gid;
//	}
//	void setGid(String gid) {
//		this.gid = gid;
//	}
//	String getCid() {
//		return cid;
//	}
//	void setCid(String cid) {
//		this.cid = cid;
//	}
	public static boolean isInit(Context ctx) {
		return !(getInstance(ctx).gid == 0);
	}
	boolean hasPay(){
		return hasPay;
	}
	
}
