package android_serialport_api.sample;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

/**
 * 描述：
 * 作者：dc on 2016/8/31 11:05
 * 邮箱：597210600@qq.com
 */
public class SerialPortTestActivity extends SerialPortActivity {
    private static final String TAG = SerialPortTestActivity.class.getSimpleName();

    private EditText out = null;
    private EditText input = null;
    private Button send = null;
    private Button clean = null;
    private String result = "";
    private CheckBox cb_send;
    private CheckBox cb_rec;
    private static final String REGEX = "^[A-Fa-f0-9]+$";
    private static final String FULL_REGEX = "^[A-Za-z0-9]+$";
    private boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serialport_test);
        out = (EditText) findViewById(R.id.serial_port_test_get_content_et);
        input = (EditText) findViewById(R.id.serial_port_test_input_content_et);
        send = (Button) findViewById(R.id.serial_port_test_input_content_send_btn);
        clean = (Button) findViewById(R.id.serial_port_test_input_content_clean_btn);
        cb_send = (CheckBox) findViewById(R.id.cb_send);
        cb_rec = (CheckBox) findViewById(R.id.cb_rec);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                System.out.println("s = [" + s + "], start = [" + start + "], count = [" + count + "], after = [" + after + "]");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    String str = s.toString();
                    String newStr = str.substring(start, s.length());
                    if (cb_send.isChecked()) {
                        newStr = newStr.trim();
                        if (!newStr.equals("")) {
                            if (!newStr.matches(REGEX)) {
                                Toast.makeText(SerialPortTestActivity.this, "不是有效的HEX字符组合", Toast.LENGTH_SHORT).show();
                                str = str.substring(0, str.length() - count);
                                input.setText(str);
                                return;
                            }
                        }
                    }

                }
            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

        clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                out.setText("");
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputContentStr = input.getText().toString().trim();
                out.append(inputContentStr);
                out.append("\n");
               inputContentStr=inputContentStr.replace(" ","");
                if (inputContentStr.equals("")) {
                    Toast.makeText(SerialPortTestActivity.this, "发送内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    if (cb_send.isChecked()) {
                        byte[] bytes = hexToBytes(inputContentStr);
                        if(bytes!=null&&bytes.length>0){
                            mOutputStream.write(bytes);
                        }

                    }else{
                        char[] chars = new char[inputContentStr.length()];
                        for (int i = 0; i < inputContentStr.length(); i++) {
                            chars[i]=inputContentStr.charAt(i);
                        }
                        mOutputStream.write(new String(chars).getBytes());

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
//                SendSerial.getInstance().sendResetCmd();
            }
        });
    }

    @Override
    protected void onDataReceived(final byte[] buffer, final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (out != null) {
                    if (cb_rec.isChecked()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < size; i++) {
                            String hex = Integer.toHexString(0xFF & buffer[i]);
                            if (hex.length() == 1) {
                                sb.append("0");
                            }
                            if (i != size - 1) {
                                sb.append(hex.toUpperCase() + " ");
                            } else {
                                sb.append(hex.toUpperCase());

                            }
                        }
                        out.append(sb);
                        out.append("\n");
                    } else {
                        result = new String(buffer, 0, size);
                        out.append(result);
                        out.append("\n");
                    }
                }
            }
        });
    }


    public byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }

        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789ABCDEF";
        for (int i = 0; i < length; i++) {
            int pos = i * 2; // 两个字符对应一个byte
            int h = hexDigits.indexOf(hexChars[pos]) << 4; // 注1
            int l = hexDigits.indexOf(hexChars[pos + 1]); // 注2
            if (h == -1 || l == -1) { // 非16进制字符
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }
}
