package com.zhaoxi.alarm;

import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.AnnualCalendar;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Alarm {
	static Logger logger = LoggerFactory.getLogger(AlarmDaemon.class);
	static long SECOND = 1000L;
	static long MINUTE = 60L * 1000L;
	static long HOUR = 60L * 60L * 1000L;
	static long DAY = 24L * 60L * 60L * 1000L;
	static long WEEK = 7L * 24L * 60L * 60L * 1000L;
	private Scheduler scheduler = null;

	public Alarm() {
		try {
			// Create a default instance of the Scheduler
			SchedulerFactory sf = new StdSchedulerFactory("com/zhaoxi/alarm/config.properties");
			scheduler = sf.getScheduler();
			scheduler.start();
		} catch (SchedulerException ex) {
			ex.printStackTrace();
			logger.error(ex.toString());
		}
	}
	
	//the function to add a job with simpleTrigger, only trigger once
	public void addAlarmJob(String jobName, String jobGroup,String triggerName,String triggerGroup,
			long advance,Date startTime){
		Date trigTime = new Date(startTime.getTime() - advance);
		JobDetail jobDetail = null;
		SimpleTrigger trigger = null;
		
		try {
			trigger = (SimpleTrigger) scheduler.getTrigger(new TriggerKey(triggerName,triggerGroup));
		} catch (SchedulerException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(trigger !=null)   //if this trigger exist already ,return
			return;
		
		//add a trigger
		try {
			jobDetail = scheduler.getJobDetail(new JobKey(jobName, jobGroup));
		} catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (jobDetail != null) //if jobDetail exit, point this trigger to it, otherwise create a new jobDetail
		{
			trigger = (SimpleTrigger) newTrigger()
			        .withIdentity(triggerName, triggerGroup)
			        .startAt(trigTime)
			        .inAdvance(advance)
			        .setMisfireInstruction(4)
			        .forJob(jobDetail)
			        .build();
			try {
				scheduler.scheduleJob(trigger);
				System.out.println("trigger build successfully, the next fire time is:" + trigger.getNextFireTime());
				} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				}		
		}
		else
		{
			jobDetail = newJob(AlarmJob.class).withIdentity(jobName, jobGroup)
					.build();
			trigger = (SimpleTrigger) newTrigger()
	        .withIdentity(triggerName, triggerGroup)
	        .startAt(trigTime)
	        .inAdvance(advance)
	        .setMisfireInstruction(4)
	        .build();
			
			try {
				scheduler.scheduleJob(jobDetail, trigger);
				System.out.println("trigger build successfully, the next fire time is:" + trigger.getNextFireTime());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	//the function to add a job with CalendarIntervalTrigger,trigger with an interval in an intervalUnit
	public void addAlarmJob(String jobName, String jobGroup,String triggerName,String triggerGroup,
			long advance,Date startTime,Date endTime,int interval,String intervalUnit){
		
		IntervalUnit unit = DateBuilder.IntervalUnit.DAY;
		if (intervalUnit.equals("DAILY")){
			unit = DateBuilder.IntervalUnit.DAY;
		}else if(intervalUnit.equals("WEEKLY")){
			unit = DateBuilder.IntervalUnit.WEEK;
		}
		else if(intervalUnit.equals("MONTHLY")){
			unit = DateBuilder.IntervalUnit.MONTH;
		}
		else if(intervalUnit.equals("YEARLY")){
			unit = DateBuilder.IntervalUnit.YEAR;
		}
		
		JobDetail jobDetail = null;
		CalendarIntervalTrigger trigger = null;
		
		try {
			trigger = (CalendarIntervalTrigger) scheduler.getTrigger(new TriggerKey(triggerName,triggerGroup));
		} catch (SchedulerException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(trigger !=null)   //if this trigger exist already ,return
			return;
		
		try {
			jobDetail = scheduler.getJobDetail(new JobKey(jobName, jobGroup));
		} catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (jobDetail != null) //if jobDetail exit, point this trigger to it, otherwise create a new jobDetail
		{
			trigger = newTrigger()
				     .withIdentity(triggerName, triggerGroup)
				     .inAdvance(advance)
				     .startAt(startTime)
				     .withSchedule(calendarIntervalSchedule().withInterval(interval,unit))
				     .endAt(endTime)
				     .forJob(jobDetail)
				     .build();
			try {
				scheduler.scheduleJob(trigger);
				System.out.println("trigger build successfully, the next fire time is:" + trigger.getNextFireTime());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			jobDetail = newJob(AlarmJob.class).withIdentity(jobName, jobGroup)
					.build();
			trigger = newTrigger()
					.withIdentity(triggerName, triggerGroup)
					.inAdvance(advance)
					.startAt(startTime)
					.withSchedule(calendarIntervalSchedule().withInterval(interval,unit))
					.endAt(endTime)
					.build();
			try {
				scheduler.scheduleJob(jobDetail, trigger);
				System.out.println("trigger build successfully, the next fire time is:" + trigger.getNextFireTime());
				} catch (Exception e) {
				// TODO Auto-generated catch block
					e.printStackTrace();
				}		
		}
	}
	
	//untrigger a event in a serial repeat-events at a certain time
	public void untriggerAlarmJob(String triggerName, String triggerGroup, Date untriggerTime) throws SchedulerException{
		TriggerKey triggerKey = new TriggerKey(triggerName,triggerGroup);
		OperableTrigger trigger = null;
		trigger = (OperableTrigger) scheduler.getTrigger(triggerKey);
		
		if(trigger != null){
			if(untriggerTime.before(trigger.getStartTime()))
			{
				System.out.println("untrigger time is before start time, need not to do any thing");
				return;
			}
			
			untriggerTime = new Date(untriggerTime.getTime() - trigger.getAdvance());  //this untriggerTime should minus the advance
			AnnualCalendar calendar = null;
			String calName = null;
			Calendar day = Calendar.getInstance();
			day.setTime(untriggerTime);
			
			calName = trigger.getCalendarName();
			if (calName!=null)   //if the trigger has a calendar, find it
			{
				calendar = (AnnualCalendar) scheduler.getCalendar(calName);
			}
			
			if (calendar!=null)   //if the calendar does exist, add the new excluded day
			{
				calendar.setDayExcluded(day, true);
				scheduler.addCalendar(calName, calendar, true, true);
			}
			else{ //if the trigger does not has a calendar, or can't find the calendar, then create one with its triggername
				calName = triggerName;
				//calendar = (AnnualCalendar) scheduler.getCalendar(calName);
				calendar = new AnnualCalendar();
				calendar.setDayExcluded(day, true);
				scheduler.addCalendar(calName, calendar, true, true);
			}

			trigger = (OperableTrigger) trigger.getTriggerBuilder().modifiedByCalendar(calName).build();
			if(trigger.computeFirstFireTime(calendar)==null)  //if this trigger will never fire with this calendar,delete it
			{
				System.out.println("this trigger will never fire with this calendar, delete this trigger.");
				scheduler.pauseTrigger(triggerKey);
				scheduler.unscheduleJob(triggerKey);
			}
			else{
				System.out.println("the first fire time after untriggered:"+trigger.computeFirstFireTime(calendar));
				scheduler.rescheduleJob(triggerKey, trigger);
			}
		}
		else{
			System.out.println("this trigger does not exit!");
		}
	}
	
	public void changeEndTime(String jobName, String jobGroup, Date endTime) throws SchedulerException{
		JobKey jobKey = new JobKey(jobName,jobGroup);
		JobDetail job = null;
		job = scheduler.getJobDetail(jobKey);
		Trigger trigger = null;
		int num = 0;
		List<? extends Trigger> trigs = scheduler.getTriggersOfJob(jobKey);
		num = trigs.size();
		if(num>0)
		{
			for(int i = 0;i<num;i++)
			{
				trigger = trigs.get(i);
				Date triggerEndTime = new Date(endTime.getTime() - trigger.getAdvance());
				if(triggerEndTime.before(trigger.getNextFireTime()) || triggerEndTime.before(trigger.getStartTime()))
				{
					System.out.println("the end time is before the next fire time, delete this trigger.");
					scheduler.pauseTrigger(trigger.getKey());
					scheduler.unscheduleJob(trigger.getKey());
				}
				else{
					trigger = trigger.getTriggerBuilder().endAt(triggerEndTime).build();
					System.out.println("the first fire time after changeEndTime:"+trigger.getNextFireTime());
					scheduler.rescheduleJob(trigger.getKey(), trigger);
				}
			}
		}
	}
	
	//delete a job
	public void deleteAlarmJob(String jobName, String jobGroup){
		JobKey jobkey = new JobKey(jobName,jobGroup);
		try {
			scheduler.deleteJob(jobkey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//delete a trigger
		public void deleteAlarmTrigger(String triggerName, String triggerGroup){
			TriggerKey triggerKey = new TriggerKey(triggerName,triggerGroup);
			try {
				scheduler.pauseTrigger(triggerKey);
				scheduler.unscheduleJob(triggerKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	//the function transform a day to int
	public int dayStrTransform(String day){
		int result = 1;
		if(day.equals("MO")){
			result = 1;
		}
		else if(day.equals("TU")){
			result = 2;
		}
		else if(day.equals("WE")){
			result = 3;
		}
		else if(day.equals("TH")){
			result = 4;
		}
		else if(day.equals("FR")){
			result = 5;
		}
		else if(day.equals("SA")){
			result = 6;
		}
		else if(day.equals("SU")){
			result = 0;
		}
		return result;
	}
	
	//the function to computer advance with the value and unit
	public long computeAdvance(int value,String unit){
		long advance = 0;
		long multiplier=1;
		if(unit.equals("MINUTE"))
		{
			multiplier = 60l * 1000l;
		}
		else if(unit.equals("HOUR"))
		{
			multiplier = 60l * 60l * 1000l;
		}
		else if(unit.equals("DAY"))
		{
			multiplier = 24l * 60l * 60l * 1000l;
		}
		else if(unit.equals("WEEK"))
		{
			multiplier = 7l * 24l * 60l * 60l * 1000l;
		}
		advance = value * multiplier;
		return advance;
	}
	
	//the function to compute endTime with startTime, repeatType,interval and count
	public Date computeEndTime(Date startTime,String FREQ,int interval,int count){
		Date endTime = null;
		int adder = count * interval;
		if(FREQ.equals("DAILY"))
		{
			endTime = new Date(startTime.getTime() + adder * 24l* 60l * 60l * 1000l);
		}
		else if(FREQ.equals("WEEKLY"))
		{
			endTime = new Date(startTime.getTime() + adder  * 7l * 24l* 60l * 60l * 1000l);
		}
		else if(FREQ.equals("MONTHLY"))
		{
			Calendar eTime = Calendar.getInstance();
			eTime.setTime(startTime);
			eTime.add(java.util.Calendar.MONTH, adder);
			endTime = eTime.getTime();
		}
		else if(FREQ.equals("YEARLY"))
		{
			endTime = new Date(startTime.getTime());
			int year = endTime.getYear();
			int endyear = year + adder;
			endTime.setYear(endyear);
		}
		return endTime;
	}
	
	//when the job trigger on several days in a week with one startTime, it has to compute the startTime of each day
	public Date computeStartTimebyday(Date startTime,int startDay,int day,int interval){
		long adder = (day - startDay) * 24l * 60l * 60l * 1000l;
		if(adder < 0) //if day < starDay, it means the first trigTime of this day is in the next week
		{
			adder = adder + interval * 7l * 24l * 60l * 60l * 1000l;
		}
		Date startTimebyday = new Date(startTime.getTime() + adder);
		return startTimebyday;
	}
}
