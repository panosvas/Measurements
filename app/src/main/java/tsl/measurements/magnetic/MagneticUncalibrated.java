package tsl.measurements.magnetic;

/**
 * Created by panagiotisvasileiou on 30/01/15.
 */
public class MagneticUncalibrated {

    private String area;
    private String xValueUncalib;
    private String yValueUncalib;
    private String zValueUncalib;
    private String xBias;
    private String yBias;
    private String zBias;
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

    public String getxValueUncalib() {
        return xValueUncalib;
    }

    public void setxValueUncalib(String xValueUncalib) {
        this.xValueUncalib = xValueUncalib;
    }

    public String getyValueUncalib() {
        return yValueUncalib;
    }

    public void setyValueUncalib(String yValueUncalib) {
        this.yValueUncalib = yValueUncalib;
    }

    public String getzValueUncalib() {
        return zValueUncalib;
    }

    public void setzValueUncalib(String zValueUncalib) {
        this.zValueUncalib = zValueUncalib;
    }

    public String getxBias() {
        return xBias;
    }

    public void setxBias(String xBias) {
        this.xBias = xBias;
    }

    public String getyBias() {
        return yBias;
    }

    public void setyBias(String yBias) {
        this.yBias = yBias;
    }

    public String getzBias() {
        return zBias;
    }

    public void setzBias(String zBias) {
        this.zBias = zBias;
    }
}
