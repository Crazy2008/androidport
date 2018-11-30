package android_serialport_api.sample;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import android_serialport_api.Constant;
import android_serialport_api.port.SendSerial;


public class TestPortActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "TestPortActivity";
    private int boot_line[][] = new int[1024][512];
    /**
     * 扇区个数
     */
    private int size;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_port);
        findViewById(R.id.test_port).setOnClickListener(this);
        findViewById(R.id.writePart).setOnClickListener(this);
        findViewById(R.id.jump).setOnClickListener(this);
        findViewById(R.id.check).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_port:
                sendBootLoadPackage();
                break;
            case R.id.writePart:
                sendBootLoad();
                break;

            case R.id.jump:
                jump();
                break;
            case R.id.check:
                check();
                break;
        }
    }

    public static void sendBootLoadPackage() {
        SendSerial.getInstance().sendResetCmd();
        Constant.IS_BOOT_LOAD = true;
        Constant.IS_CLEAR_ALL = true;
        Constant.IS_CHECK_DATA = true;
        Constant.IS_SKIP = true;

        for (int i = 0; i < Constant.FOR_TIMES; i++) {
            if (!Constant.IS_BOOT_LOAD) break;
            sendBootLoad();
            SystemClock.sleep(Constant.SPACE_TEME);
        }
        SystemClock.sleep(Constant.SPACE_TEME);
    }

    private void check() {
        byte bytes[] = {0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x46, 0x00};
        SendSerial.getInstance().sendData(Constant.CHECK, bytes);

    }

    private void jump() {
        byte data[] = {0x00, 0x00, 0x00, 0x00};
        SendSerial.getInstance().sendData(Constant.SKIP, data);
    }

    public  static void sendBootLoad() {
        byte data[] = {0x00, 0x00, 0x00, 0x00};
        SendSerial.getInstance().sendData(Constant.BOOT_LOAD, data);
    }
}
