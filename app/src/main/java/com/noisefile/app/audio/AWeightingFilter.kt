package com.noisefile.app.audio

class AWeightingFilter {
    // Stage 1 Coefficients for 48kHz
    private val b0_1 = 0.96525096525
    private val b1_1 = -1.34730163086
    private val b2_1 = 0.38205066561
    private val a1_1 = -1.34730722798
    private val a2_1 = 0.34905752979

    // Stage 1 State
    private var x1_1 = 0.0
    private var x2_1 = 0.0
    private var y1_1 = 0.0
    private var y2_1 = 0.0

    // Stage 2 Coefficients for 48kHz
    private val b0_2 = 0.94696969696
    private val b1_2 = -1.89393939393
    private val b2_2 = 0.94696969696
    private val a1_2 = -1.89387049481
    private val a2_2 = 0.89515976917

    // Stage 2 State
    private var x1_2 = 0.0
    private var x2_2 = 0.0
    private var y1_2 = 0.0
    private var y2_2 = 0.0

    fun process(samples: ShortArray, count: Int): DoubleArray {
        val output = DoubleArray(count)
        for (i in 0 until count) {
            val x = samples[i].toDouble()

            // Stage 1 Biquad
            val y_1 = b0_1 * x + b1_1 * x1_1 + b2_1 * x2_1 - a1_1 * y1_1 - a2_1 * y2_1
            x2_1 = x1_1
            x1_1 = x
            y2_1 = y1_1
            y1_1 = y_1

            // Stage 2 Biquad
            val y_2 = b0_2 * y_1 + b1_2 * x1_2 + b2_2 * x2_2 - a1_2 * y1_2 - a2_2 * y2_2
            x2_2 = x1_2
            x1_2 = y_1
            y2_2 = y1_2
            y1_2 = y_2

            output[i] = y_2
        }
        return output
    }
}
