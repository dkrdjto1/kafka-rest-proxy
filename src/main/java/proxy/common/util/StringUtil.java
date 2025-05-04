package proxy.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * String utility 클래스
 */
public class StringUtil {
    
    public static final String EMPTY = "";

    /**
     * inputstream -> string 반환
     * default utf-8 charset, default auto close input-stream
     * @param is
     * @return
     * @throws IOException
     */
    public static String from(InputStream is) throws IOException {
        return from(is, StandardCharsets.UTF_8.name(), true);
    }
    
    /**
     * inputstream -> string 반환
     * @param is
     * @param charset
     * @param autoCloseInputStream
     * @return
     * @throws IOException
     */
    public static String from(InputStream is, String charset, boolean autoCloseInputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] byteArray = buffer.toByteArray();

        if (autoCloseInputStream) {
            is.close();
        }

        return new String(byteArray, charset);
    }
}
