import netCrawler.test.trial.Utils.WeekInTernTransUtil;
import org.junit.Test;

public class TransferTool {

    @Test
    public void test(){
        System.out.println("当前周数："+Long.toString(WeekInTernTransUtil.transfer()));
    }
}
