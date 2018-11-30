package android_serialport_api;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import android_serialport_api.port.OnUpGradeListener;
import android_serialport_api.port.SendSerial;

public class PortBackDataHandler {
    private static final String TAG = "PortBackDataHandler";
    private int boot_line[][] = new int[1024][512];
    private byte[] lastAddrBytes = new byte[4];
    private int size;
    private OnUpGradeListener listener;
    private byte crcBytes[] = null;

    private PortBackDataHandler() {
    }

    public static PortBackDataHandler getInstance() {
        return SingleTon.INSTANCE.getInstance();
    }

    enum SingleTon {
        INSTANCE;
        private PortBackDataHandler info;

        SingleTon() {
            info = new PortBackDataHandler();
        }

        public PortBackDataHandler getInstance() {
            return info;
        }
    }


    public void setOnUpGradeListener(OnUpGradeListener listener) {

        this.listener = listener;
    }

    /**
     * 处理和校验数据
     */
    public void handler(byte[] buffer) {

        //做crc验证
        if (!checkCrc(buffer)) {
            Log.d(TAG, "checkCrc faile");
            return;
        }
        if (!checkLen(buffer)) {
            Log.d(TAG, "checkLen faile");
            return;
        }
        //cmd位 如果为0x95表示是应答包
        if (buffer[4] == Constant.ANSWER) {
            byte subCmd = buffer[5];
            byte data[] = new byte[buffer.length - 9];
            System.arraycopy(buffer, 6, data, 0, data.length);
            switchSubCmd(subCmd, data);
        } else {
            String msg = "buffer is not answer package command";
            Log.d(TAG, msg);
            listener.onFaile(msg);
        }
    }

    /**
     * 仿照给的c程序，计算扇区，并给bootline二维数组赋值
     */
    private long calculateSpace() {
        for (int i = 0; i < boot_line.length; i++) {
            Arrays.fill(boot_line[i], 0xFF);
        }
        int offsetPage = 0;
        int length;
        long offsetAddr;
        int type;
        long boot_length = 0;
        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        try {
            File file = new File(Constant.PATH);
            if (!file.exists() || (!file.isFile())) {
                return 0;
            }
            System.out.println("file=" + file.length());
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals(":00000001FF")) break;
                char[] buff = line.toCharArray();
                if (buff[0] != ':') continue;
                length = ByteUtil.Char2toByte(buff[1], buff[2]);
                offsetAddr = ByteUtil.Char2toByte(buff[3], buff[4]) * 256 + ByteUtil.Char2toByte(buff[5], buff[6]);
                type = ByteUtil.Char2toByte(buff[7], buff[8]);
                if (type == 0) {
                    long offset = (offsetPage << 16) + offsetAddr;
                    if (offset < Constant.ADDRESS_OFFSET)//偏移0x00004000
                    {
                        String msg = ".hex 非升级文件:offset<0x00004000";
                        Log.d(TAG, msg);
                        listener.onFaile(msg);
                        return 0;
                    }
                    if (offset >= 0x00080000)//最大512K
                    {
                        String msg = ".hex 非升级文件:offset>=0x00080000";
                        Log.d(TAG, msg);
                        listener.onFaile(msg);
                        return 0;
                    }
                    for (int i = 0; i < length; i++) {
                        int chr_tmp = ByteUtil.Char2toByte(buff[9 + 2 * i], buff[10 + 2 * i]);
                        long boot_temp_length = offset - Constant.ADDRESS_OFFSET + i;
                        boot_line[(int) (boot_temp_length / Constant.DATANUM)][(int) (boot_temp_length % Constant.DATANUM)] = chr_tmp;
                        if (boot_length < boot_temp_length)
                            boot_length = boot_temp_length;
                    }
                }
                if (type == 4) {
                    offsetPage = ByteUtil.Char2toByte(buff[9], buff[10]) * 256 + ByteUtil.Char2toByte(buff[11], buff[12]);
                }
            }
            bufferedReader.close();
            fileReader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return boot_length++;
    }

    /**
     * 根据不同的应答包解析数据
     *
     * @param subCmd
     * @param data
     */
    private void switchSubCmd(byte subCmd, byte[] data) {
        printData(subCmd, data);
        switch (subCmd) {
            //进入bootload模式
            case Constant.BOOT_LOAD:
                Constant.IS_BOOT_LOAD = false;
                for (int i = 0; i < Constant.FOR_TIMES; i++) {
                    if (!Constant.IS_CLEAR_ALL) break;
                    clearAll();
                    SystemClock.sleep(Constant.SPACE_TEME);
                }
                break;
            //清楚所有扇区
            case Constant.CLEAR_ALL:
                Constant.IS_CLEAR_ALL = false;
                long bootlength = calculateSpace();
                if (bootlength == 0) {
                    String msg = "switchSubCmd: bootlength is 0";
                    Log.d(TAG, msg);
                    return;
                }
                size = (int) ((bootlength + Constant.DATANUM - 1) / Constant.DATANUM);

                writeAPart(size);
                break;
            case Constant.CLEAR_ONE:
                break;
            case Constant.READ_ONE:
                break;
            //写一个扇区
            case Constant.WRITE_ONE:
                for (int i = 0; i < Constant.FOR_TIMES; i++) {
                    if (!Constant.IS_CHECK_DATA) break;
                    sendCheckData(data);
                    SystemClock.sleep(Constant.SPACE_TEME);
                }
                break;
            //校验
            case Constant.CHECK:
                Constant.IS_CHECK_DATA = false;
                handleAllCheck(data);

                break;
            //跳转
            case Constant.SKIP:
                Constant.IS_SKIP = false;
                Constant.IS_BOOT_LOAD = true;
                Constant.IS_CLEAR_ALL = true;
                Constant.IS_CHECK_DATA = true;
                String msg = "upgrade success";
                listener.onSuccess(msg);
                break;


        }
    }

    /**
     * 处理总数据的校验，判断从起始地址到最后地址的crc校验值，和数据包中返回的是否一致
     *
     * @param data
     */
    private void handleAllCheck(byte[] data) {
        byte[] startAndEndPackBytes = getStartAndEndPackBytes();
        if (startAndEndPackBytes == null || startAndEndPackBytes.length == 0) return;
        if (data.length == startAndEndPackBytes.length + 2) {
            //因为返回的数据带crc数据2位，所以+2
            byte[] tempBytes = new byte[startAndEndPackBytes.length];
            System.arraycopy(data, 0, tempBytes, 0, tempBytes.length);
            if(Arrays.equals(tempBytes,startAndEndPackBytes)){
                byte[] bytes = new byte[4];
                int crc = CrcUtils.getCrc(crcBytes);
                int hight = crc >> 8;
                int low = crc & 0xFF;
                Log.e("SendSerial", "hight=" + Integer.toHexString(hight));
                Log.e("SendSerial", "low=" + Integer.toHexString(low));
                //如果所写数据的crc校验和mcu返回的数据的crc校验一致就跳转到用户区
                if((data[8]&0xFF)==hight&&(data[9]&0xFF)==low){
                    for (int i = 0; i < 10; i++) {
                        if (!Constant.IS_SKIP) break;
                        SendSerial.getInstance().sendData(Constant.SKIP, bytes);
                        SystemClock.sleep(Constant.SPACE_TEME);
                    }
                }else{
                    Log.d(TAG, "handleAllCheck: 校验的crc不一致");
                }

            }else{
                Log.d(TAG, "handleAllCheck: 起始地址和结束地址不匹配");
            }
        }else{
            Log.d(TAG, "handleAllCheck: 不是对所有数据的校验，不处理");
        }






    }

    private void printData(byte subCmd, byte[] data) {
        String str = Integer.toHexString(subCmd & 0xFF);
        str = str.toUpperCase();
        if (str.length() == 1) {
            str += "0x0" + str;
        } else {
            str = "0x" + str;
        }
        Log.d(TAG, "switchSubCmd: subCmd:" + str + "  data:" + ByteUtil.byteToHexStr(data, "  "));
    }

    /**
     * 单个扇区写入完成后，根据起始地址和最后一个扇区的地址，发送校验的数据(对数据的全部校验)
     *
     * @param data
     */
    private void sendCheckData(byte[] data) {
        //如果最后一个地址相同就说明写入成功了，就做校验操作
        if (Arrays.equals(lastAddrBytes, data)) {
            Log.d(TAG, "is  last disk data");
            //原始地址
            byte[] tempBytes = getStartAndEndPackBytes();
            SendSerial.getInstance().sendData(Constant.CHECK, tempBytes);
        } else {
//            Log.d(TAG, "is not last disk data,not check");
        }
    }

    /**
     * 获取开始和结束地址的bytes数组
     *
     * @return
     */
    private byte[] getStartAndEndPackBytes() {
        byte[] addrBytes = ByteUtil.longToByteArray(Constant.ADDRESS_OFFSET);
        //扇区总大小
        byte[] totalSize = ByteUtil.intToByteArray(size * Constant.DATANUM);
        byte[] tempBytes = new byte[addrBytes.length + totalSize.length];
        System.arraycopy(addrBytes, 0, tempBytes, 0, addrBytes.length);
        System.arraycopy(totalSize, 0, tempBytes, addrBytes.length, totalSize.length);
        return tempBytes;
    }

    /**
     * 删除所有扇区
     */
    private void clearAll() {
        byte[] bytes = ByteUtil.longToByteArray(Constant.ADDRESS_OFFSET);
        SendSerial.getInstance().sendData(Constant.CLEAR_ALL, bytes);
    }

    /**
     * 判断数据长度
     *
     * @param buffer
     */
    private boolean checkLen(byte[] buffer) {
        int len = buffer.length - 3 - 4;
        int hight = len >> 8;
        int low = len & 0xFF;
        if (buffer[1] == hight && low == buffer[2]) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 校验crc
     *
     * @param buffer
     * @return
     */
    private boolean checkCrc(byte[] buffer) {
        int crcLen = buffer.length - 3;
        byte[] crcDatas = new byte[crcLen];
        System.arraycopy(buffer, 0, crcDatas, 0, crcLen);
        int crc = CrcUtils.getCrc(crcDatas);
        byte hight = (byte) (crc >> 8);
        byte low = (byte) (crc & 0xFF);
        if (hight == buffer[buffer.length - 3] && low == buffer[buffer.length - 2]) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 循环写数据
     *
     * @param size
     */
    private void writeAPart(int size) {
        crcBytes = new byte[size * Constant.DATANUM];
        for (int i = 0; i < size; i++) {
            String str = "第" + (i + 1) + "数据段";
            byte[] tempData = getBytesByTwoDArray(boot_line, i);
            if (tempData == null) {
                continue;
            }
            if (!is0xFF(tempData)) {
                continue;
            }
            long sector_addr = Constant.ADDRESS_OFFSET + i * Constant.DATANUM;
            byte[] addrData = ByteUtil.longToByteArray(sector_addr);
            byte[] sendData = new byte[tempData.length + 4];
            System.arraycopy(addrData, 0, sendData, 0, addrData.length);
            System.arraycopy(tempData, 0, sendData, 4, tempData.length);
            Log.e(TAG, str + " " + ByteUtil.byteToHexStr(sendData, " "));
            if (i == size - 1) {
                System.arraycopy(addrData, 0, lastAddrBytes, 0, addrData.length);
            }
            SendSerial.getInstance().sendData(Constant.WRITE_ONE, sendData);
            SystemClock.sleep(100);
            if (crcBytes != null) {
                System.arraycopy(tempData, 0, crcBytes, i * Constant.DATANUM, tempData.length);
            }

        }

    }

    /**
     * 如果是全0xFF 就不写入
     *
     * @param tempData
     * @return
     */
    private boolean is0xFF(byte[] tempData) {
        for (int i = 0; i < tempData.length; i++) {
            if (tempData[i] != (byte) 0xFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从bootline二维数组里，取出每个扇区所写入的对应数据，并返回
     */

    private byte[] getBytesByTwoDArray(int[][] boot_line, int i) {
        if (boot_line != null && boot_line.length > 0) {
            //TODO 缺少对是否有第i行的判断
            int rowLength = boot_line[i].length;
            byte[] retByte = new byte[rowLength];
            for (int j = 0; j < rowLength; j++) {
                retByte[j] = (byte) boot_line[i][j];
            }
            return retByte;
        } else {
            return null;
        }
    }


}
