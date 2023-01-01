package com.qqbot;

import net.mamoe.mirai.message.data.MessageChain;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Thousand Dust
 */
public class Utils {

    /**
     * 文件操作
     */

    /**
     * 读取文件内容
     *
     * @param path 文件路径
     * @return 成功返回文件内容，失败返回null
     */
    public static byte[] readFile(String path) {
        InputStream input = null;
        byte[] result = null;

        try {
            input = new FileInputStream(path);

            byte[] buf = new byte[1024];
            int len = input.read(buf);

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            while (len != -1) {
                bytesOut.write(buf, 0, len);
                len = input.read(buf);
            }
            result = bytesOut.toByteArray();
        } catch (IOException e) {
        } finally {
            close(input);
        }

        return result;
    }

    /**
     * 向文件写入内容
     *
     * @param path    文件路径
     * @param content 内容
     * @param append  是否追加
     */
    public static void writeFile(String path, byte[] content, boolean append) {
        OutputStream output = null;

        try {
            output = new FileOutputStream(path, append);
            output.write(content);
            output.flush();
        } catch (IOException e) {
        } finally {
            close(output);
        }
    }

    /**
     * 删除文件夹
     * @param path
     */
    public static void deleteDir(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            //删除文件夹里面的文件
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDir(f.getAbsolutePath());
                    }
                    f.delete();
                }
            }
            file.delete();
        }
    }

    /**
     * 关闭I/O流
     * @param closeable
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对比两个MessageChain的消息内容是否相同
     * @return 相同返回true，不同返回false
     */
    public static boolean messageChainEqual(MessageChain messageChain1, MessageChain messageChain2) {
        if (messageChain1.size() != messageChain2.size()) {
            return false;
        }
        for (int i = 1; i < messageChain1.size(); i++) {
            if (!messageChain1.get(i).toString().equals(messageChain2.get(i).toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 时间戳保留到天
     */
    public static long getDayTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

}
