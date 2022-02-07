package io.smallrye.stork.loadbalancer.leastresponsetime.impl.util;

public class FastPower {
    final double base;
    final double[] baseToPowersOfTwo;

    public FastPower(double base) {
        this.base = base;

        baseToPowersOfTwo = new double[17];
        // e^2^0, e^2^1, e^2^2, e^2^3, e^2^4,
        baseToPowersOfTwo[0] = base;
        // this gives us the ability to quickly calculate powers up to 2^17 - 1
        for (int i = 1; i <= 16; i++) {
            baseToPowersOfTwo[i] = baseToPowersOfTwo[i - 1] * baseToPowersOfTwo[i - 1];
        }
    }

    public double toPower(long exponent) {
        double result = 1;
        for (int i = 0; i <= 16; i++) {
            int powerOf2 = 1 << i;
            if ((exponent & powerOf2) != 0) {
                result *= baseToPowersOfTwo[i];
            }
        }
        return result;
    }
}
