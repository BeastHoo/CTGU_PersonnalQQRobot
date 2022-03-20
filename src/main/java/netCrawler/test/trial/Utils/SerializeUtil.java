package netCrawler.test.trial.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeUtil.class);


    public static byte[] serialize(Object o){
        byte[] byteArray = null ;

        try(ByteArrayOutputStream bty = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bty);){
            oos.writeObject(o);
            byteArray = bty.toByteArray();
        } catch (Exception e) {
            LOGGER.error("序列化失败！",e);
        }

        return byteArray ;
    }

    public static Object deserialize(byte[] bytes){

        Object o = null ;

        try(ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bai);){

            o = ois.readObject();

        } catch (Exception e) {
            LOGGER.error("反序列化失败！",e);
        }

        return o;
    }

}
