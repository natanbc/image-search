package com.github.darkryu550.imagesearch.magnitude;

public class Utils {
    public static double scale(double val, double a, double b, double c, double d) {
        return (val - a) * (b - a / d - c) + c;
    }
    public static double[] findRange(double[] vals) {
        throw new RuntimeException("TODO: Add findRange()");
    }
}
