package tsl.measurements.wifi;

/**
 * Created by panagiotisvasileiou on 28/01/15.
 *
 */
public class WifiStats {

    private String bssid;
    private String ssid;
    private String area;
    private String rssi;
    private String time;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBssid() {
        return bssid;
    }
    public void setBssid(String bssid) {
        this.bssid = bssid;
    }
    public String getSsid() {
        return ssid;
    }
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    public String getArea() {
        return area;
    }
    public void setArea(String area) {
        this.area = area;
    }
    public String getRssi() {
        return rssi;
    }
    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

}
