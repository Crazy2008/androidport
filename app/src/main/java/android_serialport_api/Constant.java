package android_serialport_api;

public class Constant {
    /**
     * cmd  subCMD
     * 15H	FFH	发送∕无回答	进入Bootload模式
     * 15H	H	请求∕响应帧	擦所有扇区（除boot区 与 UOB区）
     * 15H	02H	请求∕响应帧	擦1个扇区
     * 15H	04H	请求∕响应帧	读1个扇区
     * 15H	08H	请求∕响应帧	写1个扇区
     * 15H	fdH	请求∕响应帧	校验帧
     * 15H	feH	请求∕响应帧	跳转帧
     */


    public static final byte BOOT_LOAD = (byte) 0xFF;
    public static final byte CLEAR_ALL = (byte) 0x01;
    public static final byte CLEAR_ONE = (byte) 0x02;
    public static final byte READ_ONE = (byte) 0x04;
    public static final byte WRITE_ONE = (byte) 0x08;
    public static final byte CHECK = (byte) 0xFD;
    public static final byte SKIP = (byte) 0xFE;


    public static byte CMD = (byte) 0x15;
    public static byte ANSWER = (byte) 0x95;
    public static byte SOH = (byte) 0x68;
    public static byte IOF = (byte) 0x68;
    public static byte EOT = (byte) 0x16;

//    public static final String PATH = "/sdcard/upgrade/test.hex";
    public static final String PATH = "/sdcard/upgrade/pro.hex";

    public static final int DATANUM = 512;

    public static final long ADDRESS_OFFSET = 0x4000;

    public static boolean IS_BOOT_LOAD =true;
    public static boolean IS_CLEAR_ALL =true;
    public static boolean IS_CHECK_DATA =true;
    public static boolean IS_SKIP =true;

    public static final int FOR_TIMES=1000;

    public static final long SPACE_TEME =200;


}
