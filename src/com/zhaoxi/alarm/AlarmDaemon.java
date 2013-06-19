package com.zhaoxi.alarm;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

import javax.jms.*;

import net.sf.json.*;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.AnnualCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmDaemon {
	/**
	 * @param args
	 */
	static String QUEUE_USER = "zhaoxi";
	static String QUEUE_PASSWORD = "zhaoxi2013";
	static String QUEUE_URL = "tcp://42.121.2.100:61616";
	
	public static void main(String[] args) {
		// ConnectionFactory ：连接工厂，JMS 用它创建连接
		ConnectionFactory connectionFactory;
		// Connection ：JMS 客户端到JMS Provider 的连接
		Connection connection = null;
		// Session： 一个发送或接收消息的线程
		Session session;
		// Destination ：消息的目的地;消息发送给谁.
		Destination destination;
		// 消费者，消息接收者
		MessageConsumer consumer;
		connectionFactory = new ActiveMQConnectionFactory(
				QUEUE_USER,
				QUEUE_PASSWORD,
				QUEUE_URL);
		
		Alarm alarmDaemon = new Alarm();
		
		while(true){
		try {
			// 构造从工厂得到连接对象
			connection = connectionFactory.createConnection();
			// 启动
			connection.start();
			// 获取操作连接
			session = connection.createSession(Boolean.FALSE,
					Session.AUTO_ACKNOWLEDGE);
			// 获取session注意参数值xingbo.xu-queue是一个服务器的queue，须在在ActiveMq的console配置
			destination = session.createQueue("alarmQueue");
			consumer = session.createConsumer(destination);
			while (true) {
				// 设置接收者接收消息的时间，为了便于测试，这里谁定为100s
				TextMessage message = (TextMessage) consumer.receive();
				JSONObject json = JSONObject.fromObject(message.getText());
				
				if (null != message) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
					Date now = new Date();
					
					String url = json.getString("calendar_url");
					String aid = json.getString("calendar_aid");
					String type = json.getString("alarm_type");
					String method = json.getString("alarm_method");
					
					String jobName = aid + "/" + type;
					String jobGroup = url;
					String triggerName = aid + "/" + type;
					String triggerGroup = url + aid;
					
					
					if (method.equals("ADD"))
					{
						String isAllDay = json.getString("alarm_isAllDay");
						Date startTime = null;
						if (isAllDay.equals("true"))
						{
							startTime = sdf2.parse(json.getString("event_start"));
							startTime.setHours(9);//if isAllDay ,set startTime as 9:00 of that day
						}
						else
						{
							startTime = sdf.parse(json.getString("event_start"));
						}
						
						int alarm_value = Integer.parseInt(json.getString("alarm_value"));
						String alarm_unit = json.getString("alarm_unit");
						long advance = alarmDaemon.computeAdvance(alarm_value,alarm_unit);//get the advance
						
						String isRepeat = "false";
						if(json.containsKey("isRepeat")){
							isRepeat = json.getString("isRepeat");
						}
						String recurrence_id = null;
						if(json.containsKey("recurrence_id"))
							recurrence_id = json.getString("recurrence_id");
						if (!recurrence_id.equals("null")){
							triggerName = triggerName + "/" + sdf.parse(recurrence_id).toString();
							isRepeat = "false";   //as this event is an ex-event, it should be stored as a simple one
						}
						
						if (!isRepeat.equals("true"))//if isRepeat is false OR it's an ex-event, add a simpleTrigger
						{
							System.out.println("[" + now + "]" + "Get a simple alarm:" + url +"|"+triggerName +"|"+isAllDay +"|"+ startTime+"|"+ advance+"|"+ recurrence_id);
							alarmDaemon.addAlarmJob(jobName, jobGroup, triggerName, triggerGroup, advance, startTime);
						}
						else
						{
							JSONObject repeat_rule = json.getJSONObject("repeat_rule");
							String FREQ = repeat_rule.getString("FREQ");
							int interval = Integer.parseInt(repeat_rule.getString("INTERVAL"));
							
							if (FREQ.equals("WEEKLY"))
							{
								JSONArray BYDAY = repeat_rule.getJSONArray("BYDAY");
								int repeatMount = BYDAY.size();
								int k =0;
								for(k=0;k<repeatMount;k++) //find the position the day of the startTime
								{
									JSONObject jsonday = BYDAY.getJSONObject(k);
									String strday = jsonday.getString("DAY");
									int intday = alarmDaemon.dayStrTransform(strday);
									if (intday >= startTime.getDay())  //the startTime may not included in the repeat days!!
										break;
								}
								int [] startDay = new int [repeatMount]; //reorder the days in a week, the startDay in the first
								int j=0;
								for (j=0;j<repeatMount-k;j++)
								{
									startDay[j] = alarmDaemon.dayStrTransform(BYDAY.getJSONObject(j+k).getString("DAY"));
								}
								while (j<repeatMount){
									startDay[j] = alarmDaemon.dayStrTransform(BYDAY.getJSONObject(j-repeatMount+k).getString("DAY"));
									j++;
								}
								for (j=0;j<repeatMount;j++)
									System.out.println(startDay[j]);
								for(int i = 0;i<repeatMount;i++) //for each day of the week, add a independent trigger with the same job 
								{
									Date startTimebyday =  alarmDaemon.computeStartTimebyday(startTime, startTime.getDay(),startDay[i],interval);//compute the startTime of each day
									Date endTime = null;
									boolean flag = true;
									if(repeat_rule.containsKey("UNTIL"))
									{
										endTime = sdf2.parse(repeat_rule.getString("UNTIL"));
										endTime = new Date(endTime.getTime() + alarmDaemon.DAY);  //as the event should include the 'UNTIL_TIME',the endTime should be one day later
									}
									else if(repeat_rule.containsKey("COUNT")){
										int count = Integer.parseInt(repeat_rule.getString("COUNT"));  //get the trigger times of each day, in a clever way!
										int countbyday = count/repeatMount; //the average trigger times of each day
										if (i < count%repeatMount) //if the day is in the front position, its trigger times should add 1
										{
											countbyday ++;
										}
										endTime = alarmDaemon.computeEndTime(startTimebyday,FREQ,interval,countbyday);
										if (countbyday==0)  //if countbyday == 0 ,don't create the trigger
										{
											flag = false;
										}
									}
									if(flag == true)
									{
										String triggerName1 = triggerName + startDay[i];
										System.out.println("[" + now + "]" + "Get a weekly repeat alarm:" + url +"|"+triggerName1 +"|"+isAllDay +"|"+FREQ +"|"+ startTimebyday +"|"+ endTime);					
										alarmDaemon.addAlarmJob(jobName, jobGroup,triggerName1,triggerGroup,advance,startTimebyday,endTime,interval,FREQ);
									}
								}
							}
							else   //repeat daily , monthly or yearly
							{
								Date endTime = null;
								if(repeat_rule.containsKey("UNTIL"))
								{
									endTime = sdf2.parse(repeat_rule.getString("UNTIL"));
									endTime = new Date(endTime.getTime() + alarmDaemon.DAY);  //as the event should include the 'UNTIL_TIME',the endTime should be one day later
								}
								else if(repeat_rule.containsKey("COUNT")){
									int count = Integer.parseInt(repeat_rule.getString("COUNT"));
									endTime = alarmDaemon.computeEndTime(startTime,FREQ,interval,count);
								}
								System.out.println("[" + now + "]" +"Get a repeat alarm:" + url +"|"+triggerName +"|"+isAllDay +"|"+FREQ +"|"+ startTime +"|"+ endTime+"|"+ advance);
								
								alarmDaemon.addAlarmJob(jobName, jobGroup,triggerName,triggerGroup,advance,startTime,endTime,interval,FREQ);
							}
						}
					}
					else if(method.equals("UNTRIGGER")){
						String recurrence_id = null;
						if(json.containsKey("recurrence_id"))
						{
							recurrence_id = json.getString("recurrence_id");
						}
						else{
							System.out.println("recurrence_id does not exit, UNTRIGGER can't excute.");
						}
						if(!recurrence_id.equals("null")){   //这种方式可以检测出null和“null”两种情况
						Date untriggerTime = sdf.parse(recurrence_id);
						JSONObject repeat_rule = json.getJSONObject("repeat_rule");
						if (repeat_rule.containsKey("FREQ"))
						{
						String FREQ = repeat_rule.getString("FREQ");
						if(FREQ.equals("WEEKLY"))
						{
							triggerName = triggerName + untriggerTime.getDay();
							System.out.println(triggerName);
						}
						}
						System.out.println("[" + now + "]" + "Get one to UNTRIGGER:" + url +"|"+triggerName+"|"+recurrence_id);
						alarmDaemon.untriggerAlarmJob(triggerName, triggerGroup, untriggerTime);
						}
					}
					else if(method.equals("DELETE")){
						System.out.println("[" + now + "]" +"Get one to DELETE:" + url +"|"+aid +"|"+type);
						String recurrence_id = null;
						if(json.containsKey("recurrence_id"))
							recurrence_id = json.getString("recurrence_id");
						if (!recurrence_id.equals("null")){
							triggerName = triggerName + "/" + sdf.parse(recurrence_id).toString();  //as this event is an ex-event, only its trigger should be deleted, not the job 
							alarmDaemon.deleteAlarmTrigger(triggerName, triggerGroup);
						}
						else{
							alarmDaemon.deleteAlarmJob(jobName, jobGroup);
						}
					}
					else if(method.equals("CHANGE_ENDTIME")){
						JSONObject repeat_rule = json.getJSONObject("repeat_rule");
						Date endTime = sdf2.parse(repeat_rule.getString("UNTIL"));
						endTime = new Date(endTime.getTime() + alarmDaemon.DAY);  //as the event should include the 'UNTIL_TIME',the endTime should be one day later
						System.out.println("[" + now + "]" + "Get one to CHANGE_ENDTIME:" + url +"|"+aid +"|"+type+"|"+endTime);
						alarmDaemon.changeEndTime(jobName, jobGroup, endTime);
					}
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
			try {
				if (null != connection)
					connection.close();
			} 
			catch (Throwable ignore) {
			}
		}	
		}

	}


}
