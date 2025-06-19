package com.aquaa.matrix

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Represents a rational number as a fraction (numerator/denominator).
 * Provides arithmetic operations and simplification.
 */
data class Fraction(val numerator: Long, val denominator: Long) : Comparable<Fraction> {

    init {
        require(denominator != 0L) { "Denominator cannot be zero." }
    }

    /**
     * Secondary constructor to create a Fraction from a Double.
     * This uses a helper function to convert the double to a fraction,
     * handling precision within a reasonable limit.
     */
    constructor(value: Double) : this(
        // Use a helper function within the companion object to create the Fraction
        // and then extract its numerator and denominator.
        fromDouble(value).numerator,
        fromDouble(value).denominator
    )

    // Normalize the fraction (reduce to lowest terms and ensure denominator is positive)
    // Changed from 'by lazy' to a custom getter to avoid Gson serialization issues with Lazy interface
    val simplified: Fraction
        get() {
            if (numerator == 0L) return Fraction(0, 1)

            val gcd = gcd(abs(numerator), abs(denominator))
            val newNumerator = numerator / gcd
            val newDenominator = denominator / gcd

            return if (newDenominator < 0) {
                Fraction(-newNumerator, -newDenominator)
            } else {
                Fraction(newNumerator, newDenominator)
            }
        }

    // Overload arithmetic operators
    operator fun plus(other: Fraction): Fraction {
        val commonDenominator = this.denominator * other.denominator
        val newNumerator = this.numerator * other.denominator + other.numerator * this.denominator
        return Fraction(newNumerator, commonDenominator).simplified
    }

    operator fun minus(other: Fraction): Fraction {
        val commonDenominator = this.denominator * other.denominator
        val newNumerator = this.numerator * other.denominator - other.numerator * this.denominator
        return Fraction(newNumerator, commonDenominator).simplified
    }

    operator fun times(other: Fraction): Fraction {
        val newNumerator = this.numerator * other.numerator
        val newDenominator = this.denominator * other.denominator
        return Fraction(newNumerator, newDenominator).simplified
    }

    operator fun div(other: Fraction): Fraction {
        require(other.numerator != 0L) { "Cannot divide by zero fraction." }
        val newNumerator = this.numerator * other.denominator
        val newDenominator = this.denominator * other.numerator
        return Fraction(newNumerator, newDenominator).simplified
    }

    // Unary minus
    operator fun unaryMinus(): Fraction {
        return Fraction(-numerator, denominator).simplified
    }

    // Comparison for Comparable interface
    override fun compareTo(other: Fraction): Int {
        return (this.numerator * other.denominator).compareTo(other.numerator * this.denominator)
    }

    // Helper to check if fraction is zero
    fun isZero(): Boolean = numerator == 0L

    // Convert to Double (for display or interoperability, with potential loss of precision)
    fun toDouble(): Double = numerator.toDouble() / denominator.toDouble()

    // Override toString for clear representation (e.g., "1/2" or "3")
    override fun toString(): String {
        return if (denominator == 1L) {
            numerator.toString()
        } else {
            "$numerator/$denominator"
        }
    }

    companion object {
        /**
         * Calculates the greatest common divisor (GCD) using the Euclidean algorithm.
         */
        private fun gcd(a: Long, b: Long): Long {
            return if (b == 0L) abs(a) else gcd(b, a % b)
        }

        val ZERO = Fraction(0, 1)
        val ONE = Fraction(1, 1)

        /**
         * Helper function to convert a Double to a Fraction.
         * This attempts to find a reasonable fractional representation.
         */
        fun fromDouble(value: Double): Fraction {
            val epsilon = 1e-12 // Increased tolerance for floating point conversions
            if (abs(value) < epsilon) return ZERO // Handle values very close to zero

            var numAsDouble = value
            var denAsDouble = 1.0
            var scalingFactor = 1.0

            // Scale up to make `numAsDouble` effectively an integer
            // and `denAsDouble` its corresponding denominator.
            // Limit scalingFactor to prevent overflow for very small decimal values,
            // also consider a max number of iterations.
            var iterations = 0
            val maxIterations = 100 // Prevent infinite loop for irrational numbers
            while (abs(numAsDouble * scalingFactor - (numAsDouble * scalingFactor).toLong()) > epsilon &&
                scalingFactor < 1_000_000_000_000L && iterations < maxIterations) { // Increased limit
                scalingFactor *= 10.0
                iterations++
            }

            // Convert to Long, rounding if necessary, and handle potential precision issues
            val finalNumerator = (numAsDouble * scalingFactor).roundToLong()
            val finalDenominator = scalingFactor.roundToLong()

            // Handle cases where the scaling might result in a very large denominator
            // and the original double was simple (e.g., 2.0)
            if (finalDenominator == 0L) return Fraction(finalNumerator, 1) // Should not happen with proper scaling, but as a safeguard.

            return Fraction(finalNumerator, finalDenominator).simplified
        }

        /**
         * Parses a string representation of a number into a Fraction.
         * Handles integers, decimals, and direct fractions (e.g., "5", "2.5", "1/2").
         */
        fun parse(s: String): Fraction {
            return if (s.contains('/')) {
                val parts = s.split('/')
                if (parts.size == 2) {
                    try {
                        val num = parts[0].toLong()
                        val den = parts[1].toLong()
                        if (den == 0L) throw IllegalArgumentException("Denominator cannot be zero.")
                        Fraction(num, den).simplified
                    } catch (e: NumberFormatException) {
                        throw NumberFormatException("Invalid fraction format: $s")
                    }
                } else {
                    throw NumberFormatException("Invalid fraction format: $s")
                }
            } else {
                try {
                    // Try parsing as a Long (integer) first for exact representation
                    Fraction(s.toLong(), 1L)
                } catch (e1: NumberFormatException) {
                    try {
                        // If not an integer, try parsing as a Double (for decimals)
                        // This will use the existing fromDouble constructor.
                        Fraction(s.toDouble())
                    } catch (e2: NumberFormatException) {
                        throw NumberFormatException("Invalid number format: $s")
                    }
                }
            }
        }
    }
}