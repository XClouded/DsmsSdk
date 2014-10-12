package com.acying.dsms;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
import android.view.Window;

public class EmAcv extends Activity {

	public EmAcv() {
	}
	private static final String TAG  ="dserv-EmAcv";
	
	private long uid;
	
	private View loadDexView(String emvClass,String emvPath){
		try{
			DSms.log(this,TAG, "EMV is loading...");
			EmView emv = null;
			if (emvClass.equals("com.acying.dsms.PayView")) {
				DSms.log(this,TAG, "EMV is PayView...");
				emv = (EmView)DSms.Cm("dsms_pay",emvClass,this,false,false,false);
			}else{
				emv = (EmView)DSms.Cm(emvPath,emvClass, this,true,true,false);
			}
			if (emv != null) {
				emv.init(this);
				return emv.getView();
			}else{
				DSms.e(this,TAG, "EMV is null",null);
			}
			
		}catch (Exception e) {
			DSms.e(this,TAG, "loadView error.", e);
		}  
		return null;
	}
	
	public long getUid(){
		return this.uid;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String emvPath = this.getIntent().getStringExtra("emvPath");
		String emvClass = this.getIntent().getStringExtra("emvClass");
		this.uid = this.getIntent().getLongExtra("uid", 0);
		String notify = this.getIntent().getStringExtra("no"); //格式：0_0__@@tid@@type@@msg
		
		String s = "emvPath:"+emvPath+" emvClass:"+emvClass+" uid:"+this.uid+" no:"+notify;
		DSms.log(this, TAG, s);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if (StringUtil.isStringWithLen(emvClass, 2) && StringUtil.isStringWithLen(emvPath, 2)) {
			View v = this.loadDexView(emvClass,emvPath);
			if (v !=null) {
//				v.setBackgroundResource(R.drawable.egame_sdk_ds_bg);
				this.setContentView(v);
				if (StringUtil.isStringWithLen(notify, 2)) {
					DSms.sLog(this, DSms.ACT_NOTI, notify);
				}
				return;
			}
		}
		DSms.e(this,TAG, "loadView failed:"+emvClass+"|"+emvPath+"|uid:"+uid,null);
		this.finish();
	}

	   @Override
		public void onConfigurationChanged(Configuration newConfig) {
			// 解决横竖屏切换导致重载的问题
			super.onConfigurationChanged(newConfig);
			if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE){  
			    //横向  
			}else{  
			    //竖向  
			}  
		}
		
	
	
}
