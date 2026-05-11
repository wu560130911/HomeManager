package com.wms.homemanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    private static final String CERTS_DIR_NAME = "certs";

    /**
     * 复制文件到内部存储
     * @param context 上下文
     * @param uri 文件 URI
     * @return 内部存储中的文件路径，如果失败则返回 null
     */
    public static String copyFileToInternalStorage(Context context, Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            // 创建内部存储目录
            File certDir = new File(context.getFilesDir(), CERTS_DIR_NAME);
            if (!certDir.exists()) {
                certDir.mkdirs();
            }

            // 创建临时文件
            File tempFile = File.createTempFile("cert", ".pem", certDir);

            // 从 URI 读取内容并写入临时文件
            ContentResolver contentResolver = context.getContentResolver();
             inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
                return tempFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
