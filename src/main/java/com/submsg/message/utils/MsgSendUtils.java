package com.submsg.message.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aspire.cmppsgw.GlobalEnv;
import com.aspire.cmppsgw.LongSMSUtil;
import com.aspire.cmppsgw.util.MySMProxy20;
import com.huawei.insa2.comm.cmpp.message.CMPPMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPSubmitMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPSubmitRepMessage;
import com.huawei.insa2.util.TypeConvert;
import com.sr178.game.framework.log.LogSystem;

public class MsgSendUtils {
	public static int tp_Pid =  GlobalEnv.getInstance().getIntValue("tp_Pid");
	public static int fee_UserType =  GlobalEnv.getInstance().getIntValue("fee_UserType");
	public static String msgSrc =  GlobalEnv.getInstance().getValue("send.sms.spuser");
	
	public static boolean isOpen =  GlobalEnv.getInstance().getBoolValue("is_open",false);
	
	public static final String GBK = "GBK";
	
  	
	private static final int Re_Send_Times = 3;
	
	
	private final static String YZM_MESSAGE = "验证码";
	private final static String YZM_SIGN_NUM = "08";
	
	/**
	 * 短信发送
	 * @param merchantID  商户id
	 * @param projectId   项目id
	 * @param modeId      模板id
	 * @param targetPhone  目标手机
	 * @return String[]  第一个元素为返回消息在网关那里生成的序列号  第二个元素为返回的code
	 * @throws IOException 
	 */
	public static String[] sendMessage(String targetPhone,String content,String signNum) {
		LogSystem.info("send mobile= [" +targetPhone + "] send content= ["+ content+"]");
		
		if(!isOpen){
			LogSystem.info("不是真的发送了消息,但返回一个随机的结果，");
			return  new String[]{System.currentTimeMillis()+"","0"};
		}
		//验证码短信直接走验证码通道
		if(content.indexOf(YZM_MESSAGE)!=-1){
			signNum = YZM_SIGN_NUM;
		}
		try{
			int contentLength = content.getBytes(LongSMSUtil.CHARSET_UCS2).length;
			LogSystem.info("content length="+contentLength+"");
			if(contentLength<140){//短短信
				String[] result=sendShortMsg(targetPhone,content,signNum);
				int count=0;
				while(result==null){//
					count++;
					LogSystem.info("short retry send，times="+count);
					result=sendShortMsg(targetPhone,content,signNum);
					if(count>=(Re_Send_Times-1)){//如果再次连接次数超过两次则终止连接
						break;
					}
				}
				return result;
			}else{//长短信
				String[] result=sendLongMsg(targetPhone,content,signNum);
				int count=0;
				while(result==null){
					count++;
					LogSystem.info("long retry send，times="+count);
					result=sendLongMsg(targetPhone,content,signNum);
					if(count>=(Re_Send_Times-1)){//如果再次连接次数超过两次则终止连接
						break;
					}
				}
				return result;
			}
		}catch(Exception e){
			 LogSystem.error(e, "发送短信失败");
		}
		return null;
	}
	
	private static String[] sendShortMsg(String targetMobile,String msgContent,String signNum){
		String[] result = null;	
		int fmt=8;
		int tp_Udhi=0;
		LogSystem.info("tp_Pid:"+tp_Pid+",tp_Udhi:"+tp_Udhi+",fmt:"+fmt+",msgSrc:"+msgSrc);
		try {
				String[] ms = {targetMobile};
				LogSystem.info("short send mobile= [" +targetMobile + "] send content= ["+ msgContent+"]");
				MySMProxy20 mySMProxy = MySMProxy20.getInstance();
				if(mySMProxy == null){
					LogSystem.info("mySMProxy is null--->direct return null");
					return null;
				}
				CMPPSubmitMessage csm = new CMPPSubmitMessage(1, 1, 1, 5, GlobalEnv.getInstance().getValue("send.sms.serviceid"), fee_UserType,
						targetMobile, tp_Pid, tp_Udhi, fmt, GlobalEnv.getInstance().getValue("send.sms.spuser"), "01", "0", null, null,
						GlobalEnv.getInstance().getValue("send.sms.srcid")+signNum,ms,msgContent.getBytes(LongSMSUtil.CHARSET_UCS2), "");
				
				CMPPMessage submitRepMsg = mySMProxy.send(csm);
				CMPPSubmitRepMessage crm = (CMPPSubmitRepMessage) submitRepMsg;
				long msgId = TypeConvert.byte2long(crm.getMsgId());
				result = new String[]{msgId+"",crm.getResult()+""};
				LogSystem.info(targetMobile+"," + msgId + ",result:" + crm.getResult());	
		} catch (Exception e) {
			LogSystem.error(e,"发送短短信发生错误");
		}
		return result;
	}
	
	private static String[] sendLongMsg(String targetMobile,String msgContent,String signNum){
		String[] result = null;	
		LogSystem.info("开始发送长短信");
		int fmt=8;
		int tp_Udhi=1;
		LogSystem.info("tp_Pid:"+tp_Pid+",tp_Udhi:"+tp_Udhi+",fmt:"+fmt+",msgSrc:"+msgSrc);
		try {
				String[] ms = {targetMobile};
				LogSystem.info("long send mobile= [" +targetMobile + "] send content= ["+ msgContent+"]");
				MySMProxy20 mySMProxy = MySMProxy20.getInstance();
				if(mySMProxy == null){
					LogSystem.info("mySMProxy is null--->direct return null");
					return null;
				}
				
				byte[][] smschars = LongSMSUtil.enCodeBytes(msgContent,true);
				List<CMPPMessage> list = new ArrayList<CMPPMessage>();
				for (int i = 0; i < smschars.length; i++) {
					CMPPSubmitMessage csm = new CMPPSubmitMessage(smschars.length, i+1, 1, 5, GlobalEnv.getInstance().getValue("send.sms.serviceid"), fee_UserType,
							targetMobile, tp_Pid, tp_Udhi, fmt, msgSrc, "01", "0", null, null,
							GlobalEnv.getInstance().getValue("send.sms.srcid")+signNum,ms,smschars[i], "");
					list.add(csm);
				}
				LogSystem.info("拆分出来的短信条数"+list.size());
				for(CMPPMessage csm:list){
					CMPPMessage submitRepMsg = mySMProxy.send(csm);
					CMPPSubmitRepMessage crm = (CMPPSubmitRepMessage) submitRepMsg;
					long msgId = TypeConvert.byte2long(crm.getMsgId());
					result = new String[]{msgId+"",crm.getResult()+""};
					LogSystem.info(targetMobile+"," + msgId + ",result:" + crm.getResult());					}
				
				
		} catch (Exception e) {
			LogSystem.error(e,"发送长短信发生错误");
		}
		return result;
	}
	
	public static void main(String[] args) {
		String mobile = GlobalEnv.getInstance().getValue("send.sms.mobile");
		String content = GlobalEnv.getInstance().getValue("send.sms.content");
		String area = GlobalEnv.getInstance().getValue("send.sms.area");
		LogSystem.info("mobile=["+mobile+"],content:[" +content+"],area=["+area+"]");
		sendMessage(mobile,content,area);
	}
}
