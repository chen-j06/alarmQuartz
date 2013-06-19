package com.zhaoxi.alarm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Trigger;

public class AlarmJob implements Job {

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		// TODO Auto-generated method stub
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Trigger trigger = context.getTrigger();
		Date eventStartTime = trigger.getStartTime();
		Date trigTime = trigger.getPreviousFireTime();
		Date startTime = new Date(trigTime.getTime() + trigger.getAdvance());//当前触发时间加上提前量，即为本次事件时间
		String startTimeStr = sdf.format(startTime);
		JobDetail jobDetail = context.getJobDetail();
		JobKey jobKey = jobDetail.getKey();
		String url = jobKey.getGroup();
		String aid = jobKey.getName();
		Date now = new Date();
		System.out.println("[" + now + "]" + "Info:" + url + "|" +  aid + "|" + trigTime + "|" + startTimeStr + "|" + trigger.getAdvance()+ "|" + eventStartTime);
		triggerByHttp(url, aid, startTimeStr);
	}
	
	public void triggerByHttp(String calendarUrl, String calendarAid, String startTime){
		HttpClient httpClient = new DefaultHttpClient();
		String url = "http://www.ezhaoxi.com/calendar/triggerAlarm";
		HttpPost httpPost = new HttpPost(url);
		List<NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("calendar_url", calendarUrl));
		nvps.add(new BasicNameValuePair("calendar_aid", calendarAid));
		nvps.add(new BasicNameValuePair("start_time", startTime));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse res = httpClient.execute(httpPost);
			System.out.println("res:" + res.toString());
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
