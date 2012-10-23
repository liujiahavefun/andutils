

package org.and.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;

/**
 * TODO: log日志统计保存、上传-工具类
 *
 * @author hljdrl@gmail.com
 * @date 2012-8-27 上午11:43:52
 */
public class LogcatHelper {
	
    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT ;
    private LogDumper mLogDumper = null;
    private Context mContext;
    private int mPId;
    /**
     * 初始化目录
     * */
    public static void init(Context context)
    {
    	StringBuffer LogPath = new StringBuffer();
    	LogPath.append(Environment.getExternalStorageDirectory());
    	LogPath.append("/Android/data/");
    	LogPath.append(context.getPackageName()).append("/");
    	LogPath.append("logs").append("/");
    	PATH_LOGCAT = LogPath.toString();
    	//
		File file =new  File(PATH_LOGCAT);
		if(!file.exists()){
			file.mkdirs();
		}
    }
    public static LogcatHelper getInstance(Context context)
    {
    	if(INSTANCE == null){
    		INSTANCE = new LogcatHelper(context);
    	}
    	return INSTANCE;
    }
    private LogcatHelper(Context context) {
    	mContext = context;
    	 mPId = android.os.Process.myPid();
	}
	public void start() {
		if(mLogDumper==null){
			mLogDumper = new LogDumper(mPId,PATH_LOGCAT);
			mLogDumper.start();
		}
	}
	
	public void stop()
	{
		if(mLogDumper!=null){
			mLogDumper.stopLogs();
			mLogDumper = null;
		}
	}
	
	public  void sendLogMessage(Context context,String user)
	{
		if(mLogDumper!=null){
			mLogDumper.setLogFileLock(true);
			
			String file  = mLogDumper.getLogFileName();
			File sendFile = new File(file);
			if(sendFile.exists() && sendFile.length()>2000){
				try{
				EmailHelper.sendMail(context, user, file);
				
				}catch(Exception ex){
					ex.printStackTrace();
				}
				File newFile = new File(file);
				try {
					newFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mLogDumper.setLogFileLock(false);
		}
		
	}
	
	private class LogDumper extends Thread{
		String fileName;
		private Process logcatProc;
	    private BufferedReader mReader = null;
		private boolean mRunning = false;
		String cmds=null;
		private final String mPID;
		private FileOutputStream out = null;
		private List<String> logsMessage = new ArrayList<String>();
		private boolean mLogFileLock = false;
		private String logFileName;
		public void setLogFileLock(boolean lock){
			mLogFileLock = lock;
		}
		public boolean isLogFileLock()
		{
			return mLogFileLock;
		}
		
		public LogDumper(int pid,String file) {
			mPID = paresPID(pid);
			fileName = file;
			File mFile = new File(fileName,"error.txt");
			if(!mFile.exists()){
				try {
					mFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				logFileName = mFile.toString();
				out = new FileOutputStream(mFile,true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			/**
			 * 日志等级：*:v  , *:d  , *:w , *:e , *:f  , *:s
			 * 显示当前mPID程序的 E和W等级的日志.
			 * */
			cmds ="logcat *:e *:w | grep \"("+mPID+")\"";
		}
		public String getLogFileName()
		{
			return logFileName;
		}
		
		public void stopLogs() {
			mRunning = false;
		}
		
		private boolean checkFileMaxSize(String file){
			File sizefile = new File(file);
			if(sizefile.exists()){
				//1.5MB
				if(sizefile.length()>1572864){
					return true;
				}
				else {
					return false;
				}
			}else {
				return false;
			}
		}

		@Override
		public void run() {
			System.out.println("LogCatHelper'");
			mRunning = true;
			try {
				logcatProc = Runtime.getRuntime()
						.exec(cmds);

				mReader = new BufferedReader(new InputStreamReader(
						logcatProc.getInputStream()), 1024);
				
				String line = null;
				while (mRunning && (line = mReader.readLine()) != null) {
					if (!mRunning) {
						break;
					}
					if (line.length() == 0) {
						continue;
					}
					
					synchronized (out) {
						if (out != null) {
							
							boolean maxSize = checkFileMaxSize(getLogFileName());
							if(maxSize){
								//文件大小超过1.5mb
								sendLogMessage(mContext, DeviceHelper.getInstance(mContext).getImei());
							}
							
							if (isLogFileLock()) {
								if(line.contains(mPID)){
								logsMessage.add(line.getBytes() + "\n");
								}
							} else {
								if(logsMessage.size()>0){
									for(String _log:logsMessage){
										out.write(_log.getBytes());
									}
									logsMessage.clear();
								}
								/**
								 * 再次过滤日志，筛选当前日志中有 mPID 则是当前程序的日志.
								 * */
								if(line.contains(mPID)){
								out.write(line.getBytes());
								out.write("\n".getBytes());
								}
							}
						}
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				if (logcatProc != null) {
					logcatProc.destroy();
					logcatProc = null;
				}
				if (mReader != null) {
					try {
						mReader.close();
						mReader = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(out!=null){
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					out = null;
				}
			}
			
		}
	}
	public static final String paresPID(int pid) {
		StringBuffer tmp = new StringBuffer();
		tmp.append(pid);
		while (tmp.length() < 5) {
			tmp.insert(0, " ");
		}

		return tmp.toString();
	}
}
