package netCrawler.test.trial.crawler;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import netCrawler.test.trial.monitor.HttpClientConnectionMonitor;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ctguYiQingCrawler {
    private static final SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
    private  PoolingHttpClientConnectionManager cm ;
    private CloseableHttpClient httpclient = null;
    final Logger logger = LoggerFactory.getLogger("疫情上报爬虫事件");
    private static RequestConfig config;

    public  ctguYiQingCrawler(PoolingHttpClientConnectionManager manager){
        //设置连接池
        cm = manager;

//        try {
//            Login();
//            System.out.println(httpclient);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        httpclient= HttpClients.custom().setConnectionManager(cm).build();
        config = RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000)
                .setSocketTimeout(10*1000).build();
    }

    private boolean Login()throws Exception{
        //通过httpclients登录三峡大学报平安平台
        URIBuilder uriBuilder= new URIBuilder("http://yiqing.ctgu.edu.cn/wx/index/loginSubmit.do");  //设置uri
        //设置账号及密码
        uriBuilder.setParameter("username","2019112128");
        uriBuilder.setParameter("password","11111111");
        HttpGet httpGet= new HttpGet(uriBuilder.build());
        httpGet.setConfig(config);
        //执行访问
        CloseableHttpResponse response=httpclient.execute(httpGet);

        //连接成功
        if(response.getStatusLine().getStatusCode()==200)
        {
            HttpEntity entity=response.getEntity();
            String str= EntityUtils.toString(entity,"utf-8");
//            System.out.println(str.length());
            httpGet.abort();
            //登录成功后的处理
            //角色切换为班级助理
            if (str.equals("success")){
                logger.debug("登录成功，「{}」","2019112128");
                return true;
            }else {
                logger.error("登录失败，请检查账号和密码是否正确！");
                return false;
            }
        }else {
            logger.error("连接出错，请重试！！！");
            return false;
        }
    }

    /**
     * 执行登录
     * @throws Exception
     */
    public  List<String> action() throws Exception{
        boolean isLogged = Login();
        if (isLogged) {
            return onLoginSuccess();
        } else
        {
            logger.error("登录状态出错，请尝试重新启动程序");
            return null;
        }
    }

    /**
     * 登录成功后的处理
     * 切换角色
     *
     * @throws Exception
     */
    private  List<String> onLoginSuccess() throws Exception{
        URIBuilder builder = new URIBuilder("http://yiqing.ctgu.edu.cn/home/changeRole.do?current_roleid=bjzl");
        HttpGet httpGet= new HttpGet(builder.build());
        CloseableHttpResponse response=httpclient.execute(httpGet);
        EntityUtils.consumeQuietly(response.getEntity());
        //获取未上报名单
        httpGet.abort();
        List<String> infos = getUnrecorded();
        return infos;
    }


    /**
     * 这个功能是班长才有权限使用的
     * 获取未上报名单
     *
     */
    private  List<String> getUnrecorded() throws Exception{
        URIBuilder builder = new URIBuilder("http://yiqing.ctgu.edu.cn/wx/health/listStd.do?" +
                "sbrq=" + format.format(new Date()) +
                "&ifsb=2&tjlx=zrs&province=&city=&xy=&nj=&bjmc=&pycclb=&status=&sfqz=&sfys=&sfzy=&mqzz=&sfgl=&sffr=&sffy=&sfjc=&sfgr=&searchcontext=&page=0");
        HttpGet httpGet= new HttpGet(builder.build());
        CloseableHttpResponse response=httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();

        //数据解析
        String json = EntityUtils.toString(entity, "utf-8");
        JSONObject jsonObject = JSONObject.fromObject(json);
        JSONArray list = jsonObject.getJSONArray("list");
        Iterator iterator = list.iterator();
        System.out.println();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        List<String> infoList = new ArrayList<>();
        infoList.add(format.format(new Date())+" 截止 "+simpleDateFormat.format(new Date())+" 共有 "+list.size()+"人 未报平安,分别是：");
        System.out.println(format.format(new Date())+" 截止 "+simpleDateFormat.format(new Date())+" 共有 "+list.size()+"人 未上报,分别是：");

        while(iterator.hasNext())
        {
            JSONObject next = (JSONObject) iterator.next();
            String xm = next.getString("xm");
            String xh = next.getString("xh");
            System.out.println("姓名:"+xm+" "+"学号:"+xh);
            infoList.add("\n姓名:"+xm+" "+"学号:"+xh);
        }
        infoList.add("\n\n\n请以上同学尽快完成微信报平安!");
        httpGet.abort();
        return infoList;
    }


    public String recordMyInfo() throws Exception{
        String msg = "错误";
        boolean isLogged = Login();
        if (isLogged) {
            URIBuilder uriBuilder = new URIBuilder("http://yiqing.ctgu.edu.cn/wx/health/toApply.do");
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            CloseableHttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String str= EntityUtils.toString(entity,"utf-8");

            //通过JSoup进行html解析获取ttoken
            Document document = Jsoup.parse(str);

            Elements value = document.getElementsByAttributeValue("name", "ttoken");
//            System.out.println();
//            System.out.println(value.val());
//            System.out.println(value);
//            System.out.println();
            logger.debug("ttoken,{}",value.val());

            if (value.val().equals(""))
            {
                msg = "今日已提交!";
            }
            else
            {
                msg = uploadRecord(value.val());
            }
//            System.out.println(str);
            httpGet.abort();
        } else {
            logger.error("登录状态出错，请尝试重新启动程序");
            msg = "登录状态出错，请尝试重新启动程序";
        }

        return msg;
    }

    /**
     * 这个里面的uri也需要自己抓包哈
     * @param val
     * @return
     * @throws Exception
     */
    private String uploadRecord(String val) throws Exception{
        URIBuilder uriBuilder = new URIBuilder("http://yiqing.ctgu.edu.cn/wx/health/saveApply.do" +
                "?ttoken=" + val +
                "&city=%E6%88%90%E9%83%BD%E5%B8%82&district=%E9%83%BD%E6%B1%9F%E5%A0%B0%E5%B8%82&adcode=510181&longitude=104&" +
                "latitude=31&sfqz=%E5%90%A6&sfys=%E5%90%A6&sfzy=%E5%90%A6&sfgl=%E5%90%A6&status=1&" +
                "szdz=%E5%9B%9B%E5%B7%9D%E7%9C%81+%E6%88%90%E9%83%BD%E5%B8%82+%E9%83%BD%E6%B1%9F%E5%A0%B0%E5%B8%82&" +
                "sjh=18771808603&lxrxm=%E4%BD%95%E5%BB%BA&lxrsjh=17713805235&sffr=%E5%90%A6&sffrAm=%E5%90%A6&" +
                "sffrNoon=%E5%90%A6&sffrPm=%E5%90%A6&sffy=%E5%90%A6&sfgr=%E5%90%A6&qzglsj=&qzgldd=&glyy=&mqzz=&sffx=%E5%90%A6&qt=");
        HttpGet httpGet= new HttpGet(uriBuilder.build());
        CloseableHttpResponse response=httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String str= EntityUtils.toString(entity,"utf-8");
        JSONObject jsonObject = JSONObject.fromObject(str);
        String msg = jsonObject.getString("msgStatus")+", "+jsonObject.getString("msgText");
        System.out.println(msg);
        httpGet.abort();
        return msg;
    }

}
