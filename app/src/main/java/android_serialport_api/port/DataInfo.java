package android_serialport_api.port;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataInfo {
    private long boot_length;
    private List<HashMap> list;

    public DataInfo(long boot_length, List<HashMap> list) {
        this.boot_length = boot_length;
        this.list = list;
    }

    public long getBoot_length() {
        return boot_length;
    }

    public void setBoot_length(long boot_length) {
        this.boot_length = boot_length;
    }

    public List<HashMap> getList() {
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }

    public void setList(List<HashMap> list) {
        this.list = list;
    }
}
