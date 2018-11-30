package android_serialport_api;

public class Test {
    public static void main(String[] args) {
        /*byte[] bytes = intToByteArray(35*512);
        for (int i = 0; i < bytes.length; i++) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            System.out.println("s = [" + s + "]");
        }*/

//        test();
      /*  byte[] bytes = intToByteArray(0xFF10);
        String s = ByteUtil.byteToHexStr(bytes, " ");
        System.out.println("s = [" + s + "]");*/
//        sssss();



        test();

    }

    private static void kkk() {
        byte bytes[]={0x00, (byte) 0xFF,0x10,0x00};
        int i = ByteUtil.byteToInt(bytes);
        System.out.println("HEX="+Integer.toHexString(i));
        System.out.println("DEC="+i);

    }

    public static void sssss(){
        byte bytes[]={5,98,9,33,0,-1,98,43};
        String s = ByteUtil.byteToHexStr(bytes, " ");
        System.out.println("s="+s);
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    private static void test3() {
        byte bytes[]={0x00, (byte) 0xFF,0x10,0x00};


    }


    static int ChartoByte(char c) {
        if (c - 'a' >= 0) return (c - 'a' + 10);
        else if (c - 'A' >= 0) return (c - 'A' + 10);
        else return (c - '0');
    }


    private static void test2() {
        int data = 0x4000;
        int data1 = data >> 24;
        int data2 = data >> 16 & 0x00FF;
        int data3 = data >> 8 & 0x0000FF;
        int data4 = data & 0x000000FF;
        System.out.println("data1=" + data1);
        System.out.println("data2=" + data2);
        System.out.println("data3=" + data3);
        System.out.println("data4=" + data4);
    }

    private static void test() {
//        byte bytes[] = {0x68, 0x00, 0x0C, 0x68, (byte) 0x95, (byte) 0xFD, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x46, 0x00, 0x05, (byte) 0xB1};
//        byte bytes[] = {0x68, 0x00, 0x0C, 0x68, (byte) 0x95, (byte) 0xFD, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x46, 0x00, 0x35, (byte) 0x84};
//        byte bytes[] = {0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x46, 0x00, (byte) 0xEB, (byte) 0x84};
//        byte bytes[] = {0x00, 0x00, 0x40, 0x00};
//        byte bytes[] = {0x68,0x00 ,0x0C ,0x68 , (byte) 0x95, (byte) 0xFD,0x00 ,0x00 ,0x40 ,0x00 ,0x00 ,0x00 ,0x46 ,0x00 ,0x35 , (byte) 0x84};
        byte bytes[] = {0x68,0x00 ,0x0C ,0x68 , (byte) 0x95, (byte) 0xFD,0x00 ,0x00 ,0x40 ,0x00 ,0x00 ,0x00 ,0x46 ,0x00};
        int crc = CrcUtils.getCrc(bytes);
        byte crcHight = (byte) (crc >> 8);
        byte crcLow = (byte) (crc & 0xFF);
        System.out.println(Integer.toHexString(crcHight&0xFF));
        System.out.println(Integer.toHexString(crcLow&0xFF));
    }
}
