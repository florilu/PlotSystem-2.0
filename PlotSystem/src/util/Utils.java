package util;

import com.mysql.jdbc.Util;

import java.io.*;

/**
 * Created by Florian on 12.06.14.
 */
public class Utils {
    public Utils(){

    }

    public void copy(InputStream in, File file){
        try{
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) > 0){
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
