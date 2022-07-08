package org.zywx.wbpalmstar.plugin.uexcamera.utils;

/**
 * File Description:
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/29.
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtils {

    final static int BUFFER_SIZE = 4096;

    /**
     * 将InputStream转换成byte数组
     * @param in InputStream
     * @return byte[]
     * @throws java.io.IOException
     */
    public static byte[] InputStreamTOByte(InputStream in) throws IOException{
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while((count = in.read(data,0,BUFFER_SIZE)) != -1) {
            outStream.write(data, 0, count);
        }
        data = null;
        byte[] result = outStream.toByteArray();
        outStream.close();
        return result;
    }

}
