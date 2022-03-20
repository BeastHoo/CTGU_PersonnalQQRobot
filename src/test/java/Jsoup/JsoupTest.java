package Jsoup;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.net.URL;

public class JsoupTest {

    @Test
    public void urlAnalyse() throws Exception{
        Document doc = Jsoup.parse(new URL("http://www.baidu.com"),1000);
        String content = doc.getElementsByTag("title").first().text();
        System.out.println(content);
    }

    @Test
    public void htmlAnalyse() throws Exception{
        String content = FileUtils.readFileToString(new File("C:\\Users\\86133\\OneDrive\\桌面\\selfie.html"),"utf-8");
        Document doc = Jsoup.parse(content);
        String get = doc.getElementsByTag("title").first().text();
        System.out.println(get);
    }

    @Test
    public void domRead() throws Exception{
        Document doc=Jsoup.parse(new File("C:\\Users\\86133\\OneDrive\\桌面\\selfie.html"),"utf-8");
        //Element element = doc.getElementById("contact");
        //Element element = doc.getElementsByClass("class_a").first();
        Element element = doc.getElementsByAttributeValue("href","未命名五子棋.exe").first();

        System.out.println("数据: "+element.text());
    }

    @Test
    public void selectorTest() throws Exception{
        // 1.创建Jedis连接
        String host = "127.0.0.1";
        int port = 6379;
        Jedis jedis = new Jedis(host, port);
        jedis.auth("123@456");
        // 2.检测连通性
        String result = jedis.ping();
        System.out.println("result=" + result);
        // 3.关闭Jedis连接
        jedis.close();
    }

}
