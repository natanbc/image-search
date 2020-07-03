package com.github.darkryu550.imagesearch.frequency;

final class Utils {
    public static double[][] magnitude(double[][] combined) {
        double[][] buffer = new double[combined.length][combined[0].length / 2];
        for(int i = 0; i < combined.length; ++i) {
            for(int j = 0; j < combined[0].length / 2; ++j) {
                double a = combined[i][j];
                double b = combined[i][j + 1];

                buffer[i][j] = Math.sqrt(a * a + b * b);
            }
        }

        return buffer;
    }

    public static double[][] realPlane(double[][] combined) {
        double[][] buffer = new double[combined.length][combined[0].length / 2];
        for(int i = 0; i < combined.length; ++i) {
            System.arraycopy(
                combined[i],
                0,
                buffer[i],
                0,
                combined[0].length / 2);
        }

        return buffer;
    }

    public static double[][] imaginaryPlane(double[][] combined) {
        double[][] buffer = new double[combined.length][combined[0].length / 2];
        for(int i = 0; i < combined.length; ++i) {
            System.arraycopy(
                combined[i],
                1,
                buffer[i],
                0,
                combined[0].length / 2);
        }

        return buffer;
    }
}
