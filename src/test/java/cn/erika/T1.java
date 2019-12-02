package cn.erika;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class T1 {
    public static void main(String[] args) throws Exception {
        String filepathSrc = "/home/erika/tmp/index.html";
        File src = new File(filepathSrc);

        String filepathDst = "/home/erika/tmp/demo1.html";
        File dst = new File(filepathDst);

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            int len = 0;
            byte[] data = new byte[4096];
            while ((len = in.read(data)) > -1) {
                out.write(data, 0, len);
            }
            out.flush();
        }
    }
}
