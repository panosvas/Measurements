package tsl.measurements.magnetic;

/**
 * Created by panagiotisvasileiou on 30/01/15.
 */
public class MagneticCalibrated {

    private String area;
    private String xValue;
    private String yValue;
    private String zValue;
    private String time;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getxValue() {
        return xValue;
    }

    public void setxValue(String xValue) {
        this.xValue = xValue;
    }

    public String getyValue() {
        return yValue;
    }

    public void setyValue(String yValue) {
        this.yValue = yValue;
    }

    public String getzValue() {
        return zValue;
    }

    public void setzValue(String zValue) {
        this.zValue = zValue;
    }
}
