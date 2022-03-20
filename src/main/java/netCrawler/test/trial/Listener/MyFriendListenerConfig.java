package netCrawler.test.trial.Listener;

import love.forte.simbot.action.ReplySupport;
import love.forte.simbot.core.event.CoreListenerUtil;
import love.forte.simbot.definition.Friend;
import love.forte.simbot.event.EventListenerRegistrar;
import love.forte.simbot.event.FriendMessageEvent;
import net.sf.json.JSONObject;
import netCrawler.test.trial.Utils.SerializeUtil;
import netCrawler.test.trial.crawler.ClassScheduleCrawler;
import netCrawler.test.trial.crawler.ctguYiQingCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.Queue;

/**
 * @author ForteScarlet
 */
public class MyFriendListenerConfig {
    private static ctguYiQingCrawler crawler;
    private static ClassScheduleCrawler classScheduleCrawler;
    private static Jedis jedisConn;

    public static void setCrawler(ctguYiQingCrawler crawler,ClassScheduleCrawler classScheduleCrawler) {
        MyFriendListenerConfig.crawler = crawler;
        MyFriendListenerConfig.classScheduleCrawler = classScheduleCrawler;
    }

    public static void setJedis(Jedis jedis){
        jedisConn = jedis;
    }


    /**
     * 提供一个事件监听注册器，向其中注册各种事件。
     * <p>
     * {@link EventListenerRegistrar} 由 {@link love.forte.simbot.event.EventListenerManager} 实现，
     * 因此可以直接使用事件管理器。
     *
     * @param registrar 注册器。
     */
    public static void config(EventListenerRegistrar registrar) {
        // 此实例假设注册一个监听函数，
        // 它监听 好友消息事件，并在可以的情况下回复发消息的好友一句"是的"。

        // 好友消息事件属于一个simbot-api提供的标准通用事件类型，
        // 它可能被很多事件类型所实现, 引用范围大，同时api受限也比较大。

        final Logger logger = LoggerFactory.getLogger("好友消息事件");

        // 注册一个事件
        registrar.register(CoreListenerUtil.newCoreListener(
                FriendMessageEvent.Key, // 事件类型
                (context, event) -> {
                    logger.debug("收到消息消息, 其纯文本内容：「{}」", event.getMessageContent().getPlainText());
                    if (event instanceof ReplySupport) {
                        if(event.getMessageContent().getPlainText().trim().equals("今日情况"))
                        {
                            try {
                                ((ReplySupport) event).replyBlocking(crawler.action().toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if(event.getMessageContent().getPlainText().trim().equals("报平安")){
                            try {
                                ((ReplySupport) event).replyBlocking(crawler.recordMyInfo());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if (event.getMessageContent().getPlainText().trim().equals("今日课表")){
                            byte[] bytes = jedisConn.get("classes".getBytes(StandardCharsets.UTF_8));
                            Map<Integer, Queue<String>> deserialize = (Map<Integer, Queue<String>>) SerializeUtil.deserialize(bytes);

                            String dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1+"";
                            if ("0".equals(dayOfWeek))
                            {
                                dayOfWeek = "7";
                            }
                            int dayOfWeekAfter = Integer.parseInt(dayOfWeek);

                            Queue<String> queue = deserialize.get(dayOfWeekAfter);
                            int i = 0;
                            for (String s : queue) {
                                JSONObject object = JSONObject.fromObject(s);
                                i++;
                                String info = "今日第 " + i + " 节" + "\n\n"+
                                        " 课程名：" + object.getString("KCM") + "\n"+
                                        "   教师：" + object.getString("SKJS") + "\n"+
                                        "   节次：" + object.getString("KSJC") + " - " +object.getString("JSJC") + "节\n"+
                                        " 教学楼：" + object.getString("JXLDM_DISPLAY") + "\n" +
                                        "上课教室：" + object.getString("JASMC") + "\n\n"+
                                        "播报完毕，请记得及时去上课哟~";

                                ((ReplySupport) event).replyBlocking(info);
                            }
                            if (i == 0)
                            {
                                ((ReplySupport) event).replyBlocking("谦桑今天没课了，努力考研吧");
                            }
                        }
                        else{
                            ((ReplySupport) event).replyBlocking("你好呀~");
                        }
                    } else {
                        logger.error("事件「{}」不支持直接回复消息。", event);
                        // 事件本体无法进行回复的时候，尝试获取好友对象然后发送消息。
                        // 当然，你也可以选择一上来就这么做。
                        final Friend friend = event.getFriend();
                        friend.sendBlocking("请检查您的指令是否正确...");
                    }
                }
        ));
    }

}
