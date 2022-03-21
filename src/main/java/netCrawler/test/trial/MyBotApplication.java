package netCrawler.test.trial;

import love.forte.simbot.Identifies;
import love.forte.simbot.component.mirai.MiraiBot;
import love.forte.simbot.component.mirai.MiraiBotManager;
import love.forte.simbot.component.mirai.MiraiFriend;
import love.forte.simbot.core.event.CoreListenerManager;
import netCrawler.test.trial.Utils.SerializeUtil;
import netCrawler.test.trial.Listener.MyFriendListenerConfig;
import netCrawler.test.trial.Listener.MyGroupListenerConfig;
import netCrawler.test.trial.crawler.ClassScheduleCrawler;
import netCrawler.test.trial.crawler.ctguYiQingCrawler;
import netCrawler.test.trial.factory.MyBotManagerFactory;
import netCrawler.test.trial.factory.MyListenerManagerFactory;
import netCrawler.test.trial.monitor.HttpClientConnectionMonitor;
import netCrawler.test.trial.scheduledTask.ScheduledTask;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.*;


public class MyBotApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyBotApplication.class);
    private static HttpClientConnectionMonitor httpClientConnectionMonitor;
    private static final Jedis jedis = new Jedis("127.0.0.1",6379);

    static {
        jedis.auth("123@456");
    }

    // 你bot的账号
    public static final long BOT_CODE = 2879304663L;

    // 你bot的密码
    // 3.x中，也支持使用密码的MD5字节数组作为密码，而不是明文
    public static final String BOT_PASS = "1206847819";

    // public static final byte[] PASS_MD5 = new byte[]{-24, 7, -15, -4, -8, 45, 19, 47, -101, -80, 24, -54, 103, 56, -95, -97};

    /**
     * Main
     * @param args args
     */
    public static void main(String[] args) throws InterruptedException {
        // 构建最主要的监听管理器，也就是事件处理器。
        final CoreListenerManager listenerManager = MyListenerManagerFactory.newManager();

        // 之后的步骤没有特别严格的前后顺序
        // 此处我们再构建一个mirai组件中的bot管理器
        final MiraiBotManager miraiBotManager = MyBotManagerFactory.newManager(listenerManager);

        //创建连接池
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(100);
        manager.setDefaultMaxPerRoute(10);
        httpClientConnectionMonitor = new HttpClientConnectionMonitor(manager);
        final ctguYiQingCrawler crawler = new ctguYiQingCrawler(manager);
        //课程爬虫
        final ClassScheduleCrawler classScheduleCrawler = new ClassScheduleCrawler(manager);

        LOGGER.debug("爬虫创建成功...");
        Map<Integer, Queue<String>> map = null;
        try {
            map = classScheduleCrawler.action();
            byte[] serialize = SerializeUtil.serialize(map);
//            System.out.println(map.get(2).toString());
//            将json序列化转换为Byte[]后，刷入redis
            jedis.set("classes".getBytes(StandardCharsets.UTF_8),serialize);

        } catch (Exception e) {
            e.printStackTrace();
        }


        // 接下来，我们可以开始注册一些监听函数，用于处理各种事件。
        // 通过事件管理器进行监听函数注册。

        //// 好友消息
        MyFriendListenerConfig.config(listenerManager);
        MyFriendListenerConfig.setCrawler(crawler,classScheduleCrawler);
        MyFriendListenerConfig.setJedis(jedis);
        //// 群聊消息
        MyGroupListenerConfig.config(listenerManager);

        // 这里改成你自己bot的账号密码
        final MiraiBot bot = miraiBotManager.register(BOT_CODE, BOT_PASS);

        // 注册后的bot不会立刻启动，你可以继续注册，或尝试启动它。
        bot.startBlocking();

        // 在启动之后，你可以进行一些操作。
        //每日8.00pm自动报平安
        //每周一0.05自动查询本周课表
        ScheduledTask.executeEightAtNightPerDay(crawler,bot);
        ScheduledTask.executeAtMidNightPerMonday(jedis,classScheduleCrawler,bot);
        ScheduledTask.executeSevenInMoriningPerDay(jedis,bot);
        String dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1+"";
        if ("0".equals(dayOfWeek))
        {
            dayOfWeek = "7";
        }
        int dayOfWeekAfter = Integer.parseInt(dayOfWeek);
        ScheduledTask.execute40MinutesBeforeClass(map.get(dayOfWeekAfter),bot);

        // 此处给一个账号发送一句话
        final MiraiFriend friend = bot.getFriend(Identifies.ID(1369843192));
        assert friend != null;
        friend.sendBlocking("谦桑の小助手准备就绪就绪~\n\n" +
                "发送“今日情况”，查看上报情况\n\n"+
                "发送“报平安”，进行自动报平安\n\n"+
                "发送“今日课表”，查看课程");
        miraiBotManager.waiting();
    }



}

