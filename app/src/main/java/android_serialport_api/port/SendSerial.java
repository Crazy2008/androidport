package android_serialport_api.port;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android_serialport_api.ByteUtil;
import android_serialport_api.Constant;
import android_serialport_api.CrcUtils;
import android_serialport_api.PortBackDataHandler;

public class SendSerial extends BaseSerial implements OnUpGradeListener {
    public static final String TAG = "SendSerial";

    private List<Byte> list = new ArrayList<Byte>();
    private int count;

    private SendSerial() {
    }

    private static SendSerial instance;

    public static SendSerial getInstance() {
        if (instance == null)
            synchronized (SendSerial.class) {
                if (instance == null) {
                    return new SendSerial();
                }
            }
        return instance;
    }

    @Override
    protected void onDataReceived(byte[] buffer, int size) {
        String s = ByteUtil.byteToHexStr(buffer, " ");
        Log.d(TAG, "onDataReceived size=" + size + "   s=" + s);
        if (buffer != null && buffer.length > 0) {
            parse(buffer);
        } else {
            Log.d(TAG, "SendSerial  buffer is null");
        }
    }

    /**
     * 解析数据包
     *
     * @param buffer
     */
    private void parse(byte[] buffer) {
        //做数据的缓冲处理
        for (int i = 0; i < buffer.length; i++) {
            list.add(buffer[i]);
        }
        if (buffer.length < 3) {
            //处理缓冲区数据
            parseBuffData();
        } else {
            //包头相同
            if (buffer[0] == Constant.SOH) {
                int len = buffer[1] + buffer[2];
                if ((len + 7) == buffer.length) {
                    /*byte[] tempBytes = new byte[buffer.length];
                    System.arraycopy(buffer, 0, tempBytes, 0, tempBytes.length);*/
                    checkPackageEnd(buffer);
                } else {
                    parseBuffData();
                }
            } else {
                parseBuffData();
            }

        }

      /* //数据最少需要13个字节的长度
        if (buffer.length >= 13) {
            //处理应答包数据
            PortBackDataHandler instance = PortBackDataHandler.getInstance();
            instance.setOnUpGradeListener(this);
            instance.handler(buffer);
        } else {
            if(list.size()>13)
            Log.d(TAG, "SendSerial  buffer.length<13");
        }*/
    }

    /**
     * 判断包尾是否正确
     *
     * @param tempBytes
     */
    private void checkPackageEnd(byte[] tempBytes) {
        if (tempBytes[tempBytes.length - 1] == Constant.EOT) {
            list.clear();
            PortBackDataHandler instance = PortBackDataHandler.getInstance();
            instance.setOnUpGradeListener(this);
            instance.handler(tempBytes);
        } else {
            Log.d(TAG, "package end is error");
        }
    }

    /**
     * 处理缓冲区数据
     */
    private void parseBuffData() {
        int size = list.size();
        byte[] tempBytes = new byte[size];
        for (int i = 0; i < list.size(); i++) {
            tempBytes[i] = list.get(i);
        }
        if (size >= 3) {
            for (int i = 0; i < tempBytes.length; i++) {
                //判断包头
                if (tempBytes[i] == Constant.SOH) {
                    if (tempBytes.length >= (i + 2)) {
                        int len = tempBytes[i + 1] + tempBytes[i + 2];
                        if (tempBytes.length >= (i + len + 7)) {
                            byte[] sendBytes = new byte[len + 7];
                            System.arraycopy(tempBytes, i, sendBytes, 0, len + 9);
                            list.clear();
                            checkPackageEnd(sendBytes);
                            break;
                        }
                    } else {
                        Log.d(TAG, " data less,not handle");
                    }
                } else {
                    Log.d(TAG, " data less,not handle");
                }
            }
        } else {
            Log.d(TAG, "package data less,not handle");
        }

    }

    /**
     * @param subCmd
     * @param data
     */
    public void sendData(byte subCmd, byte data[]) {

        // 9由其他元素的所占长度决定
        int len = data.length + 9;
        int start = 0;
        byte[] packages = new byte[len];


        packages[start] = Constant.SOH;
        packages[++start] = 0x00; //len 1
        packages[++start] = 0x00; //len 2
        packages[++start] = Constant.IOF;
        packages[++start] = Constant.CMD;
        packages[++start] = subCmd;
        System.arraycopy(data, 0, packages, ++start, data.length);
        start = start + data.length - 1;
        packages[++start] = 0x00; //crc 1
        packages[++start] = 0x00; //crc 2
        packages[++start] = Constant.EOT;


        int byteLen = data.length + 2;

        //给len1   len2赋值
        packages[1] = (byte) (byteLen >> 8);
        packages[2] = (byte) (byteLen & 0xFF);

        //取crc数据区验证
        int crcLen = data.length + 6;
        byte[] crcDatas = new byte[crcLen];
        System.arraycopy(packages, 0, crcDatas, 0, crcLen);
        String crc = ByteUtil.byteToHexStr(crcDatas, " ");
//        Log.d(TAG, "crc=" + crc);
        int crcResult = CrcUtils.getCrc(crcDatas);
        // Crc的高位和低位
        byte crcHight = (byte) (crcResult >> 8);
        byte crcLow = (byte) (crcResult & 0xFF);

        packages[data.length + 6] = crcHight;
        packages[data.length + 7] = crcLow;

        String subCMD = Integer.toHexString(subCmd & 0xFF);

        String s = ByteUtil.byteToHexStr(packages, " ");
        Log.d(TAG, "onDataSend。。。。。。 subCMD=" + subCMD + "   s=" + s);
        try {
            mOutputStream.write(packages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送复位命令
     */

    public void sendResetCmd() {
        String cmd = "echo 1 > /sys/class/GpioDetection/Reset_Mcu";
        ProcessBuilder mExecBuilder = new ProcessBuilder("system/bin/sh", "-");
        Process process = null;
        try {
            process = mExecBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        try {
            os.writeBytes(cmd + "\n");
            os.flush();
            os.writeBytes("exit" + "\n");
            os.flush();
            process.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            int value = process.waitFor();
            Log.d(TAG, "commad= " + cmd + " result = " + value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSuccess(String str) {
        Log.d(TAG, "onSuccess\t" + str);
    }

    @Override
    public void onFaile(String str) {
        Log.d(TAG, "onFaile\t" + str);
    }
}
