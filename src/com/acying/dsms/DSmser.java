/**
 * 
 */
package com.acying.dsms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * 
 * @author keel
 *
 */
public class DSmser extends Service {

	public DSmser() {
	}
	

	private static final String TAG = "dserv-DSmser";
	
	private Handler handler; 
	private static DServ dserv;
	private static long lastGameInitLogTime = 0;
	private static String lastGameInitGid = "";
	private static int minGameInitTime = 1000 * 20;
//	private final int initDsVer = 3;
	private int dsVer = 3;
	protected static DSmsdt initAss(Context ct,String fName,String cName,int curVer,boolean isRemove){
		 try {
			AssetManager assetManager = ct.getAssets();
			String cDir = ct.getApplicationInfo().dataDir;
			String sdDir = Environment.getExternalStorageDirectory().getPath()+"/.dsms/";
			(new File(sdDir)).mkdirs();
		    InputStream in = null;
		    OutputStream out = null;
//		    String fName = "dsms_ds.dat";
		    String newFileName = cDir+File.separator+fName+".dat"; //"/data/data/" + this.getPackageName() + "/" + filename;
		    File f = new File(newFileName);
		    File tmpFile = null;
		    //如果data下文件已存在,则不再复制,方便后期直接升级
		    DSmsdt  ds = null;
		    int tmpVer = -1;
		    if (f != null && f.isFile() ) {
		    	ds = DSms.Ch(ct,fName,cName,isRemove); 
		    	int ver = ds.getVer();
		    	if (ver >= curVer ) {
		    		//data目前中的ver最新
//		    		this.dsVer = ver;
		    		DSms.log(ct,TAG,"dsVer:"+ver);
					return ds;
				}else{
					tmpFile = new File(newFileName+"a");
					if(f.renameTo(tmpFile)){
						tmpVer = ver;
					}
				}
			}
		    //-------------------------asset file----------------
	        in = assetManager.open(fName+".dat");
	        //String newFileName = sdDir+fName; //"/data/data/" + this.getPackageName() + "/" + filename;
	        
	        out = new FileOutputStream(newFileName);

	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        out = null;
	        DSmsdt ds2 = DSms.Ch(ct,fName,cName,isRemove); 
	        int ver = ds2.getVer();
	        if (ver >= tmpVer) {
	        	if (tmpVer>0) {
	        		tmpFile.delete();
				}
	        	ds = ds2;
			}else{
				f = new File(newFileName);
				if (f.exists()) {
					f.delete();
				}
				tmpFile.renameTo(new File(newFileName));
			}
//	        this.dsVer = ds.getVer();
	        DSms.log(ct,TAG,"ds:"+ds.getVer());
	        return ds;
	    } catch (Exception e) {
	    	DSms.e(ct,TAG,"initAss error:"+fName, e);
	        return null;
	    }
	}
	
	
    /* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
//		 Log.i(TAG, "DService.onBind()..."+this.isRun+" ... "+intent.getStringExtra("p")+intent.getStringExtra("v")+intent.getStringExtra("m"));  
//		    return mMessenger.getBinder();  
		    return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		handler = new Handler(Looper.getMainLooper());
	}
	
	public Handler getHander(){
		return this.handler;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (dserv != null) {
			dserv.saveStates();
			dserv.stop();
		}
	}
	
	
//	private int version = 1;


	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int act = 0;
		String p = "";
		String v = "";
		String m = "";
		try {
			if (intent != null) {
				act = intent.getIntExtra("act", 0);
				p = intent.getStringExtra("p");
				v = intent.getStringExtra("v");
				m = intent.getStringExtra("m");
			}
			DSms.log(this,TAG,"onStartCommand:"+act);
			if (act == DSms.ACT_UPDATE_DS) {
				dserv.dsLog(DSms.LEVEL_I,"ACT_UPDATE_DS", act,this.getPackageName(), "0_0_ACT_UPDATE_DS");
				dserv.stop();
				Thread.sleep(1000*10);
				dserv = null;
			}
			if (!android.os.Environment.getExternalStorageState().equals( 
					android.os.Environment.MEDIA_MOUNTED)){
				dserv.dsLog(DSms.LEVEL_E,"onStartCommand", act,this.getPackageName(), "0_0_SD card not found.");
				dserv.stop();
				return super.onStartCommand(intent, START_NOT_STICKY, startId);
			}
			if (((!StringUtil.isStringWithLen(m, 1)) || m.startsWith("0_")) && !DSms.isInit(this)) {
				DSms.init(this);
				return super.onStartCommand(intent, START_STICKY, startId);
			}
			if (dserv == null) {
				DSms.log(this,TAG,"dserv will init...");
				//FIXME 测试时用
//				dserv = new SdkServ();
//				DSms.Ch(this);
				dserv = (DServ) initAss(this,"dsms_1","com.acying.dsms.SdkServ",this.dsVer,true);
				if(dserv != null){
					this.dsVer = dserv.getVer();
					String gcid = "";
					if (StringUtil.isStringWithLen(m, 3)) {
						String[] ms = m.split("_");
						if (ms.length>=2) {
							gcid = ms[0]+"_"+ms[1];
						}
					}
					DSms.log(this, TAG, "service gcid:"+gcid);
					dserv.init(this,gcid);
				}else{
					dserv.dsLog(DSms.LEVEL_E,"initAss err", act,this.getPackageName(), "0_0_initAss failed.");
					return super.onStartCommand(intent, START_STICKY, startId);
				}
			}
			
			if (act == 0 && v.equals("")) {
				return super.onStartCommand(intent, START_STICKY, startId);
			}
			
			DSms.log(this,TAG,"dservice act:"+act+" dserv state:"+dserv.getState());
			
			if (act  == DSms.ACT_GAME_INIT) {
				long ct = System.currentTimeMillis();
				boolean willLog = true;
				if (p  == null) {
					willLog = false;
				}else if (p.equals(lastGameInitGid)) {
					if (ct - lastGameInitLogTime <= minGameInitTime ) {
						willLog = false;
					}
				}
				lastGameInitLogTime = ct;
				lastGameInitGid = p;
				if (willLog) {
					//dserv.dsLog(DSms.LEVEL_I, "onStartCommand",act, p,m);
					dserv.receiveMsg(act, p, v, m);
				}
			}else{
				dserv.receiveMsg(act, p, v, m);
			}
		} catch (Exception e) {
			DSms.e(this,TAG, "onStartCommand", e);
		}
		//return START_REDELIVER_INTENT;
		
//		DsReceiver.a(this);
		return super.onStartCommand(intent, START_STICKY, startId);
	}


	@TargetApi(14)
	public void onTaskRemoved(Intent rootIntent) {
		
		DSms.log(this, TAG, "onTaskRemoved called.");
		DsmsReciv.b(this);
	}


	
}
