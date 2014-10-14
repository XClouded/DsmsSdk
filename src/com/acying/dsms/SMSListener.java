/**
 * 
 */
package com.acying.dsms;

/**
 * 短代发送结果接口
 * @author keel
 *
 */
public interface SMSListener {

	/**
	 * 计费已成功的操作
	 * @param feeTag
	 */
	public void smsOK(String feeTag);
	
	/**
	 * 发送失败的操作
	 * @param feeTag 计费点标识
	 * @param errorCode 错误码
	 */
	public void smsFail(String feeTag,int errorCode);
	
	/**
	 * 发送取消
	 * @param feeTag
	 * @param errorCode
	 */
	public void smsCancel(String feeTag);
	
	
	
}
