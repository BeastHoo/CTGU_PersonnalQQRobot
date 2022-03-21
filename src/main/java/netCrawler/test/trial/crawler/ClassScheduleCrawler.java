package netCrawler.test.trial.crawler;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import netCrawler.test.trial.Utils.WeekInTernTransUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * JSON代码意义：
 * KSJC:课程上课节次（1，3，5，7，9）
 * JSJC:下课节次（2，4，6，8，10）
 * SKXQ:上课日期（星期几）1-7
 * JASMC:教室名称
 * JXLDM_DISPLAY:教学楼名称
 * SKJS:上课教师
 * KCM:课程名
 *
 * author:beasthoo
 * 2022/3/16
 */
//课程爬虫
public class ClassScheduleCrawler {
    private static final SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
    private PoolingHttpClientConnectionManager cm ;
    private CloseableHttpClient httpclient = null;
    final Logger logger = LoggerFactory.getLogger("课程爬虫事件");

    private static RequestConfig config;

    public ClassScheduleCrawler(PoolingHttpClientConnectionManager manager){
        cm = manager;

        httpclient= HttpClients.custom().setConnectionManager(cm).build();
        config = RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000)
                .setSocketTimeout(10*1000).build();

    }


    /**
     * 教务处登录
     * @return
     * @throws Exception
     */
    private boolean LoginJWC() throws Exception{
        URIBuilder uriBuilder = new URIBuilder("http://jwxt.ctgu.edu.cn/jwapp/sys/emapfunauth/loginValidate.do");
        //学号
        uriBuilder.addParameter("userName","aaaaaaaaaaaaaaa");
        //加密过的登录密码，由于不知道加密方法，所可以需要自家抓包
        //推荐使用fiddler，也可以打开浏览器的调试器，点击网络(network) 然后在所有链接里找到/jwapp/sys/emapfunauth/loginValidate.do
        //就可以在payload里面找到
        //不过因为教务处网页登录后是在当前网页跳转，所以通过调试器拿到加密后的密码有难度
        uriBuilder.addParameter("password","aaaaaaaaaaaaaaaaaa");
        uriBuilder.addParameter("isWeekLogin","false");

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setConfig(config);

        CloseableHttpResponse response = httpclient.execute(httpPost);
        if(response.getStatusLine().getStatusCode()==200)
        {
            HttpEntity entity=response.getEntity();
            String str= EntityUtils.toString(entity,"utf-8");
//            System.out.println(str.length());
            httpPost.abort();
            //登录成功后的处理
            JSONObject jsonObject = JSONObject.fromObject(str);
            str = jsonObject.getString("validate");

//            System.out.println("登录信息:"+str);

            if (str.equals("success")){
                //获取jsessionID

                logger.debug("登录成功，「{}」","2019112128");
//                return true;
                return getSessionId();
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
     * 获得sessionId
     * @return
     * @throws Exception
     */
    private boolean getSessionId() throws Exception{
        URIBuilder uriBuilder = new URIBuilder("http://jwxt.ctgu.edu.cn/jwapp/sys/emaphome/portal/index.do");

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setConfig(config);

        CloseableHttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 200){
            httpGet.abort();
            return true;
        }
        return false;
    }

    /**
     * 入口
     * @throws Exception
     * @return
     */
    public Map<Integer, Queue<String>> action() throws Exception{
        boolean isLogged = LoginJWC();
        if (isLogged){
            return onLoginSuccess();
        }else
        {
            logger.error("登录状态出错，请尝试重新启动程序");
            return null;
        }
    }

    /**
     * 登录成功后的操作
     * @throws Exception
     * @return
     */
    private Map<Integer, Queue<String>> onLoginSuccess() throws Exception{
        //跳转到首页，获取appid
        String url = redirectToIndexPage();
        url = "http://jwxt.ctgu.edu.cn"+url;
//        System.out.println("url:::"+url);
        //访问appshow.do
        URIBuilder uriBuilder = new URIBuilder(url);

        HttpPost httpPost = new HttpPost(uriBuilder.build());
//        由服务器的响应设置cookie
//        httpPost.setHeader("Cookie","_ht=person");
        httpPost.setConfig(config);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        String str = EntityUtils.toString(entity, "utf-8");
        httpPost.abort();
        System.out.println(str);


        return onAskForKCB();
    }


    /**
     * 获取课程表
     * 周次需要自己计算
     * @return
     * @throws Exception
     */
    private Map<Integer, Queue<String>> onAskForKCB() throws Exception {
        URIBuilder uriBuilder = new URIBuilder("http://jwxt.ctgu.edu.cn/jwapp/sys/wdkb/modules/xskcb/cxxszhxqkb.do");
        uriBuilder.addParameter("SKZC", Long.toString(WeekInTernTransUtil.transfer()));
        uriBuilder.addParameter("XNXQDM","2021-2022-2");

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setConfig(config);

        CloseableHttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        String str = EntityUtils.toString(entity,"utf-8");
//        System.out.println(str);
        JSONObject object = (JSONObject) JSONObject.fromObject(str).get("datas");
//        System.out.println(object.toString());
        JSONObject cxxszhxqkb = (JSONObject) object.get("cxxszhxqkb");
        JSONArray rows = cxxszhxqkb.getJSONArray("rows");
        httpPost.abort();

        return transferJsonArrayToSortedMap(rows);
    }

    /**
     * 登录后重定向至首页以获取相关的凭证等(主要是通过JSOUP解析HTML获得查看课程表的带有appWID的连接)
     * @return
     * @throws Exception
     */
    private String redirectToIndexPage() throws  Exception{
        URIBuilder uriBuilder = new URIBuilder("http://jwxt.ctgu.edu.cn/jwapp/sys/emaphome/portal/index.do");

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setConfig(config);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
//        System.out.println(EntityUtils.toString(entity));
        Document html = Jsoup.parse(EntityUtils.toString(entity));
        Elements value = html.getElementsByAttributeValue("title", "我的课表");
        httpGet.abort();
        return value.attr("data-url");
    }


    /**
     * 转换为map
     * @param kcbArray
     * @return
     */
    private Map<Integer,Queue<String>> transferJsonArrayToSortedMap(JSONArray kcbArray){
        Map<Integer,Queue<String>> integerListMap = new HashMap<>();
        Queue<String> mondayClasses = new LinkedList<>();
        Queue<String> tuesdayClasses = new LinkedList<>();
        Queue<String> wednesdayClasses = new LinkedList<>();
        Queue<String> thursdayClasses = new LinkedList<>();
        Queue<String> fridayClasses = new LinkedList<>();
        Queue<String> saturdayClasses = new LinkedList<>();
        Queue<String> sundayClasses = new LinkedList<>();

        //根据星期数整理数据
        for(int i = 0; i < kcbArray.size(); i++){
            JSONObject json = kcbArray.getJSONObject(i);
            int skxq = json.getInt("SKXQ");

            switch (skxq)
            {
                case 1:
                    mondayClasses.add(json.toString());
                    break;
                case 2:
                    tuesdayClasses.add(json.toString());
                    break;
                case 3:
                    wednesdayClasses.add(json.toString());
                    break;
                case 4:
                    thursdayClasses.add(json.toString());
                    break;
                case 5:
                    fridayClasses.add(json.toString());
                    break;
                case 6:
                    saturdayClasses.add(json.toString());
                    break;
                case 7:
                    sundayClasses.add(json.toString());
                    break;
                default:
                    break;
            }
        }

        //排序
        Queue<String> mque  = sortListBySKJC(mondayClasses);
        Queue<String> tueque  = sortListBySKJC(tuesdayClasses);
        Queue<String> wque  = sortListBySKJC(wednesdayClasses);
        Queue<String> thursque  = sortListBySKJC(thursdayClasses);
        Queue<String> frique  = sortListBySKJC(fridayClasses);
        Queue<String> satque  =sortListBySKJC(saturdayClasses);
        Queue<String> sunque  =sortListBySKJC(sundayClasses);


        //填入map
        integerListMap.put(1,mque);
        integerListMap.put(2,tueque);
        integerListMap.put(3,wque);
        integerListMap.put(4,thursque);
        integerListMap.put(5,frique);
        integerListMap.put(6,satque);
        integerListMap.put(7,sunque);

        return integerListMap;
    }

    private Queue<String> sortListBySKJC(Queue<String> classes){

        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                JSONObject obj1 = JSONObject.fromObject(o1);
                JSONObject obj2 = JSONObject.fromObject(o2);
                Integer jc1 = obj1.getInt("KSJC");
                Integer jc2 = obj2.getInt("KSJC");
                return jc2.compareTo(jc1);
            }
        };
        return classes.stream().sorted(cmp.reversed()).collect(Collectors.toCollection(LinkedList::new));
//        classes.sort(new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                JSONObject obj1 = JSONObject.fromObject(o1);
//                JSONObject obj2 = JSONObject.fromObject(o2);
//                Integer jc1 = obj1.getInt("KSJC");
//                Integer jc2 = obj2.getInt("KSJC");
//                return jc1.compareTo(jc2);
//            }
//        });
    }
}
