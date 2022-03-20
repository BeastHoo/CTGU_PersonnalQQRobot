package netCrawler.test.trial.scheduledTask;

import love.forte.simbot.Identifies;
import love.forte.simbot.component.mirai.MiraiBot;
import love.forte.simbot.component.mirai.MiraiFriend;
import net.sf.json.JSONObject;
import netCrawler.test.trial.Utils.SerializeUtil;
import netCrawler.test.trial.Utils.WeekInTernTransUtil;
import netCrawler.test.trial.crawler.ClassScheduleCrawler;
import netCrawler.test.trial.crawler.ctguYiQingCrawler;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTask {
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);

    private static final SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Map<Integer,String> map = new HashMap<>();
    static {
        map.put(1,"08:00:00");
        map.put(2,"09:40:00");
        map.put(3,"10:00:00");
        map.put(4,"11:40:00");
        map.put(5,"14:00:00");
        map.put(6,"15:40:00");
        map.put(7,"16:00:00");
        map.put(8,"17:40:00");
        map.put(9,"19:00:00");
        map.put(10,"20:40:00");
        map.put(11,"21:35:00");
    }

    /**
     * ScheduledExecutorService定时任务
     * 每天晚上8:00定时查找未上报平安的学生
     */
    public static void executeEightAtNightPerDay(ctguYiQingCrawler crawler, MiraiBot bot) {
        long oneDay = 24 * 60 * 60 * 1000;

        String day = format.format(new Date());
        String time = "20:00:00";
        try {
            long timeInMillis;
            timeInMillis = simpleDateFormat.parse(day + " " + time).getTime();
            long initDelay  = timeInMillis - System.currentTimeMillis();
            initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;

            executor.scheduleAtFixedRate(
                    () -> {
                        try {
                            final MiraiFriend friend = bot.getFriend(Identifies.ID(1369843192));
                            assert friend != null;

                            String msg = crawler.recordMyInfo();
                            friend.sendBlocking("定时安全上报结果:\n"+msg);

                            List<String> action = crawler.action();
                            friend.sendBlocking(action.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    initDelay,
                    oneDay,
                    TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }


    /**
     * 每周一自动查询当周课表
     * @param jedis
     * @param classScheduleCrawler
     * @param bot
     */
    public static void executeAtMidNightPerMonday(Jedis jedis, ClassScheduleCrawler classScheduleCrawler, MiraiBot bot){
        //当前是星期几
        String dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1+"";
        if ("0".equals(dayOfWeek))
        {
            dayOfWeek = "7";
        }

        int dayOfWeekAfter = Integer.parseInt(dayOfWeek);
        System.out.println("dayOfWeek:"+dayOfWeekAfter);
        int nextMondayInterval = 7 - dayOfWeekAfter + 1; //距离下次周一的天数

        long oneWeek = 24 * 60 * 60 * 1000 * 7;//一周

        String day = format.format(new Date())+" ";
        String curDay = day.substring(day.lastIndexOf("-")+1,day.length()-1);
        System.out.println("curDay:"+curDay);

        int nextMonday = Integer.parseInt(curDay)+nextMondayInterval;
        System.out.println("nextModay:"+nextMonday);

        day = day.replaceAll("-"+curDay+" ","-"+nextMonday);
        System.out.println("day:"+day);
//        采用距离下个周一的时间来计算

        String time = "00:05:00";
        try {
            long timeInMillis;
            timeInMillis = simpleDateFormat.parse(day + " " + time).getTime();
            long initDelay  = timeInMillis - System.currentTimeMillis();  //>0
            System.out.println("initDelay:"+ (initDelay/60/1000));
//            initDelay = initDelay > 0 ? initDelay : oneWeek + initDelay;

            executor.scheduleAtFixedRate(
                    () -> {
                        try {
                            final MiraiFriend friend = bot.getFriend(Identifies.ID(1369843192));
                            assert friend != null;
                            Map<Integer, Queue<String>> map = classScheduleCrawler.action();
                            byte[] serialize = SerializeUtil.serialize(map);
                            //            System.out.println(map.get(2).toString());
                            //            将json序列化转换为Byte[]后，刷入redis
                            String set = jedis.set("classes".getBytes(StandardCharsets.UTF_8), serialize);

                            friend.sendBlocking("查询第 "+ WeekInTernTransUtil.transfer() + " 周课程成功!");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    initDelay,
                    oneWeek,
                    TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public static void executeSevenInMoriningPerDay(Jedis jedis,MiraiBot bot) {
        long oneDay = 24 * 60 * 60 * 1000;

        String day = format.format(new Date());
        String time = "7:00:00";
        try {
            long timeInMillis;
            timeInMillis = simpleDateFormat.parse(day + " " + time).getTime();
            long initDelay  = timeInMillis - System.currentTimeMillis();
            initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
            executor.scheduleAtFixedRate(
                    () -> {
                        try {
                            String dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1+"";
                            if ("0".equals(dayOfWeek))
                            {
                                dayOfWeek = "7";
                            }
                            int dayOfWeekAfter = Integer.parseInt(dayOfWeek);
                            byte[] bytes = jedis.get("classes".getBytes(StandardCharsets.UTF_8));
                            Map<Integer, Queue<String>> deserialize = (Map<Integer, Queue<String>>) SerializeUtil.deserialize(bytes);
                            execute40MinutesBeforeClass(deserialize.get(dayOfWeekAfter),bot);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    initDelay,
                    oneDay,
                    TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void execute40MinutesBeforeClass(Queue<String> queue, MiraiBot bot){
        System.out.println("执行课前提醒");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this)
                {
                    try {
                        JSONObject object = JSONObject.fromObject(queue.poll());
                        int ksjc = object.getInt("KSJC");
                        int jsjc = object.getInt("JSJC");

                        String kssj = map.get(ksjc);
                        String jssj = map.get(jsjc);
                        String day = format.format(new Date());
                        long timeInterval = simpleDateFormat.parse(day + " " + kssj).getTime()-System.currentTimeMillis();
                        this.wait(timeInterval);

                        MiraiFriend friend = bot.getFriend(Identifies.ID("1369843192"));
                        assert friend != null;

                        String info = "谦桑，您有新的课程安排：\n\n"+
                                " 课程名：" + object.getString("KCM") + "\n"+
                                "   教师：" + object.getString("SKJS") + "\n"+
                                "   节次：" + object.getString("KSJC") + " - " +object.getString("JSJC") + "节\n"+
                                "上课时间：" + kssj + " - " + jssj + "\n" +
                                " 教学楼：" + object.getString("JXLDM_DISPLAY") + "\n" +
                                "上课教室：" + object.getString("JASMC") + "\n\n"+
                                "播报完毕，请记得及时去上课哟~";

                        friend.sendBlocking(info);
                        if (!queue.isEmpty())
                        {
                            execute40MinutesBeforeClass_2(queue, bot);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    private static void execute40MinutesBeforeClass_2(Queue<String> queue, MiraiBot bot){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this)
                {
                    try {
                        JSONObject object = JSONObject.fromObject(queue.poll());
                        int ksjc = object.getInt("KSJC");
                        int jsjc = object.getInt("JSJC");

                        String kssj = map.get(ksjc);
                        String jssj = map.get(jsjc);
                        String day = format.format(new Date());
                        long timeInterval = simpleDateFormat.parse(day + " " + kssj).getTime()-System.currentTimeMillis();
                        this.wait(timeInterval);


                        MiraiFriend friend = bot.getFriend(Identifies.ID("1369843192"));
                        assert friend != null;

                        String info = "谦桑，您有新的课程安排：\n\n"+
                                " 课程名：" + object.getString("KCM") + "\n"+
                                "   教师：" + object.getString("SKJS") + "\n"+
                                "   节次：" + object.getString("KSJC") + " - " +object.getString("JSJC") + "节\n"+
                                "上课时间：" + kssj + " - " + jssj + "\n" +
                                " 教学楼：" + object.getString("JXLDM_DISPLAY") + "\n" +
                                "上课教室：" + object.getString("JASMC") + "\n\n"+
                                "播报完毕，请记得及时去上课哟~";

                        friend.sendBlocking(info);

                        if (!queue.isEmpty()){
                            execute40MinutesBeforeClass(queue, bot);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }
}
