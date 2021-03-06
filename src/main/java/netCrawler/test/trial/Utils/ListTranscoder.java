package netCrawler.test.trial.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;




/**
 * Case 1.
 * Jedis does not support cache the Object directly, the Objects needed to be
 * serialized and de-serialized
 *
 * Case 2.
 * coming very soon...
 *
 * @author Josh Wang(Sheng)
 *
 * @email  josh_wang23@hotmail.com
 */
public class ListTranscoder<M extends Serializable> extends SerializeTranscoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListTranscoder.class);

    @SuppressWarnings("unchecked")
    public List<M> deserialize(byte[] in) {
        List<M> list = new ArrayList<>();
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ByteArrayInputStream(in);
                is = new ObjectInputStream(bis);
                while (true) {
                    M m = (M)is.readObject();
                    if (m == null) {
                        break;
                    }

                    list.add(m);

                }
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            LOGGER.error( String.format("Caught IOException decoding %d bytes of data",
                    in == null ? 0 : in.length) );
            if (!(e instanceof EOFException)){
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error( String.format("Caught CNFE decoding %d bytes of data",
                    in == null ? 0 : in.length) + e);
        }  finally {
            close(is);
            close(bis);
        }

        return  list;
    }


    @SuppressWarnings("unchecked")
    @Override
    public byte[] serialize(Object value) {
        if (value == null)
            throw new NullPointerException("Can't serialize null");

        List<M> values = (List<M>) value;

        byte[] results = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;

        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            for (M m : values) {
                os.writeObject(m);
            }

            // os.writeObject(null);
            os.close();
            bos.close();
            results = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            close(os);
            close(bos);
        }

        return results;
    }


}
