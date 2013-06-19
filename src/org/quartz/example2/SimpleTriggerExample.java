/* 
 * Copyright 2005 - 2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.example2;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.impl.calendar.AnnualCalendar;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.quartz.CalendarIntervalTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Example will demonstrate all of the basics of scheduling capabilities
 * of Quartz using Simple Triggers.
 * 
 * @author Bill Kratzer
 */
public class SimpleTriggerExample {

    
    public void run() throws Exception {
        Logger log = LoggerFactory.getLogger(SimpleTriggerExample.class);

        log.info("------- Initializing -------------------");

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory("com/zhaoxi/alarm/config.properties");
        Scheduler sched = sf.getScheduler();
        sched.start();

        log.info("------- Initialization Complete --------");

        log.info("------- Scheduling Jobs ----------------");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
       // Date startTime = DateBuilder.nextGivenSecondDate(null, 5);
        Date startTime = sdf.parse("2013-3-16 09:00:00");
        System.out.print("startTime: ");
        System.out.println(startTime);
        Date firstTrigTime = new Date(startTime.getTime() - 10l * 60l * 60l * 1000l);
        System.out.print("firstTrigTime: ");
        System.out.println(firstTrigTime);
        
        Date previousTime = new Date(firstTrigTime.getTime());
        Date nextFireTime = new Date(firstTrigTime.getTime());
        
        CalendarIntervalTriggerImpl calendar = new CalendarIntervalTriggerImpl();
        calendar.setStartTime(startTime);
        calendar.setRepeatInterval(2);
        calendar.setRepeatIntervalUnit(DateBuilder.IntervalUnit.MONTH);
        for (int i=1; i<=10;i++)
        {
        	previousTime = nextFireTime;
        	nextFireTime = calendar.getFireTimeAfter(nextFireTime);
        	//System.out.print("nextTrigTime: ");
            //System.out.println(nextFireTime);
        }

        // job1 will only fire once at date/time "ts"
        JobDetail job = sched.getJobDetail(new JobKey("job4","group1"));
        System.out.println(job);
        if (job == null)
        {
        	System.out.println("fdafasf");
        }
        
        startTime = DateBuilder.nextGivenSecondDate(null, 10);
        System.out.print("startTime: ");
        System.out.println(startTime);
        firstTrigTime = new Date(startTime.getTime() - 5000l);
        Date endTime = new Date(firstTrigTime.getTime() + 100000l);
        System.out.print("firstTrigTime: ");
        System.out.println(firstTrigTime);
        System.out.print("endTime: ");
        System.out.println(endTime);
        
     // Add the holiday calendar to the schedule
        //AnnualCalendar b = new AnnualCalendar();
        AnnualCalendar b = (AnnualCalendar) sched.getCalendar("b");
        if (b!=null)
        	System.out.println(b.getDaysExcluded().size());
        // fourth of July (July 4)
        //sched.deleteCalendar("bbb");
        Date excludeDay = sdf1.parse("2013-3-15");
        Calendar ex = Calendar.getInstance();
        ex.setTime(excludeDay);
        //b.setDayExcluded(ex, true);
        b.removeExcludedDay(ex);
        // tell the schedule about our holiday calendar
        sched.addCalendar("b", b, true, true);
        
        
        JobDetail job1 = newJob(SimpleJob.class)
                .withIdentity("job4", "group1")
                .build();
       CalendarIntervalTrigger trigger = (CalendarIntervalTrigger) newTrigger() 
       //SimpleTrigger trigger = (SimpleTrigger) newTrigger()
            .withIdentity("trigger6", "group2")
            .startAt(startTime)
            .setMisfireInstruction(0)
            //.setMisfireInstruction(2)  //
            .withSchedule(calendarIntervalSchedule()
                    .withInterval(5, DateBuilder.IntervalUnit.SECOND))
            //.withSchedule(simpleSchedule().withIntervalInSeconds(5).repeatForever())
           // .endAt(DateBuilder.nextGivenSecondDate(null, 25))
            //.forJob(job)
            // .modifiedByCalendar("aaa")
            .build();
     // schedule it to run!
       
       trigger = (CalendarIntervalTrigger) sched.getTrigger(new TriggerKey("trigger6", "group2"));
       System.out.println(trigger.getNextFireTime());
       System.out.println(trigger.getNextFireTime().getTime());
       String calName = null;
       calName = trigger.getCalendarName();
       if(calName!=null)
    	   System.out.println(calName);
     trigger = trigger.getTriggerBuilder().modifiedByCalendar("b").build();
        int flag = 1;
        if (flag ==1){
        	//JobDetail job1 = sched.getJobDetail(new JobKey("job4","group1"));
        //sched.scheduleJob(job1,trigger);
        	//sched.rescheduleJob(new TriggerKey("trigger6", "group2"), trigger);
        	}
        else {
       // TriggerKey triggerKey = new TriggerKey("trigger4","group2");
        //sched.pauseTrigger(triggerKey);
        //sched.unscheduleJob(triggerKey);
       JobKey jobKey = new JobKey("job4","group1");
       sched.deleteJob(jobKey);
        }
        

    }

    public static void main(String[] args) throws Exception {

        SimpleTriggerExample example = new SimpleTriggerExample();
        example.run();

    }

}
