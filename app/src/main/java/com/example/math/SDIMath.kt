package com.example.math

import kotlin.math.abs
import kotlin.math.sqrt

enum class RowRepresentativeMode(val displayNameAr: String, val displayNameEn: String) {
    ROW_GCD("القاسم المشترك الأصغر للصف", "Row GCD"),
    FIRST_ELEMENT("العنصر الأول للصف", "First Row Element"),
    ROW_SUM("مجموع عناصر الصف", "Row Sum"),
    ROW_MAX("القيمة القصوى للصف", "Row Max")
}

object SDIMath {

    // Reference matrix A1 (19 x 6)
    val A1 = arrayOf(
        doubleArrayOf(7.0, 286.0, 200.0, 176.0, 120.0, 165.0),
        doubleArrayOf(206.0, 75.0, 129.0, 109.0, 123.0, 111.0),
        doubleArrayOf(43.0, 52.0, 99.0, 128.0, 111.0, 110.0),
        doubleArrayOf(98.0, 135.0, 112.0, 78.0, 118.0, 64.0),
        doubleArrayOf(77.0, 227.0, 93.0, 88.0, 69.0, 60.0),
        doubleArrayOf(34.0, 30.0, 73.0, 54.0, 45.0, 83.0),
        doubleArrayOf(182.0, 88.0, 75.0, 85.0, 54.0, 53.0),
        doubleArrayOf(89.0, 59.0, 37.0, 35.0, 38.0, 29.0),
        doubleArrayOf(18.0, 45.0, 60.0, 49.0, 62.0, 55.0),
        doubleArrayOf(78.0, 96.0, 29.0, 22.0, 24.0, 13.0),
        doubleArrayOf(14.0, 11.0, 11.0, 18.0, 12.0, 12.0),
        doubleArrayOf(30.0, 52.0, 52.0, 44.0, 28.0, 28.0),
        doubleArrayOf(20.0, 56.0, 40.0, 31.0, 50.0, 40.0),
        doubleArrayOf(46.0, 42.0, 29.0, 19.0, 36.0, 25.0),
        doubleArrayOf(22.0, 17.0, 19.0, 26.0, 30.0, 20.0),
        doubleArrayOf(15.0, 21.0, 11.0, 8.0, 8.0, 19.0),
        doubleArrayOf(5.0, 8.0, 8.0, 11.0, 11.0, 8.0),
        doubleArrayOf(3.0, 9.0, 5.0, 4.0, 7.0, 3.0),
        doubleArrayOf(6.0, 3.0, 5.0, 4.0, 5.0, 6.0)
    )

    // Calculate long GCD
    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    // Helper to calculate GCD of list of doubles
    fun gcdOfList(list: List<Double>): Long {
        if (list.isEmpty()) return 1L
        var result = list[0].toLong()
        for (i in 1 until list.size) {
            result = gcd(result, list[i].toLong())
        }
        return result
    }

    // Safe trial division factorization of an arbitrarily large N supporting BigInteger
    fun factorise(n: java.math.BigInteger): Map<java.math.BigInteger, Int> {
        val factors = sortedMapOf<java.math.BigInteger, Int>()
        if (n <= java.math.BigInteger.ONE) return factors
        var temp = n
        
        // Check for 2
        var count2 = 0
        val TWO = java.math.BigInteger.valueOf(2)
        while (temp.mod(TWO) == java.math.BigInteger.ZERO) {
            count2++
            temp = temp.divide(TWO)
        }
        if (count2 > 0) {
            factors[TWO] = count2
        }

        // Check odd numbers
        var d = java.math.BigInteger.valueOf(3)
        // Set a max search limit of 100,000 for trial division to keep calculations instantaneous
        val trialLimit = java.math.BigInteger.valueOf(100000L)
        while (d.multiply(d) <= temp && d <= trialLimit) {
            var count = 0
            while (temp.mod(d) == java.math.BigInteger.ZERO) {
                count++
                temp = temp.divide(d)
            }
            if (count > 0) {
                factors[d] = count
            }
            d = d.add(TWO)
        }

        // If after limit, temp is still composite / prime
        if (temp > java.math.BigInteger.ONE) {
            if (temp.isProbablePrime(25)) {
                factors[temp] = 1
            } else {
                // Try up to 1,000,000 to catch intermediate primes if achievable
                val secondLimit = java.math.BigInteger.valueOf(1000000L)
                while (d.multiply(d) <= temp && d <= secondLimit) {
                    var count = 0
                    while (temp.mod(d) == java.math.BigInteger.ZERO) {
                        count++
                        temp = temp.divide(d)
                    }
                    if (count > 0) {
                        factors[d] = count
                    }
                    d = d.add(TWO)
                }

                if (temp > java.math.BigInteger.ONE) {
                    factors[temp] = 1
                }
            }
        }
        return factors
    }

    // Convert map of BigInteger factors to custom superscripts string
    fun factorsToString(factors: Map<java.math.BigInteger, Int>): String {
        if (factors.isEmpty()) return "1"
        val superscriptMap = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
        )
        return factors.entries.joinToString(" × ") { (prime, power) ->
            val powerStr = power.toString().map { superscriptMap[it] ?: it }.joinToString("")
            "$prime$powerStr"
        }
    }

    // Get the representative value of row i
    fun getRowRepresentative(rowIndex: Int, mode: RowRepresentativeMode): Long {
        val row = A1[rowIndex]
        return when (mode) {
            RowRepresentativeMode.ROW_GCD -> gcdOfList(row.toList())
            RowRepresentativeMode.FIRST_ELEMENT -> row[0].toLong()
            RowRepresentativeMode.ROW_SUM -> row.sum().toLong()
            RowRepresentativeMode.ROW_MAX -> row.maxOrNull()?.toLong() ?: 1L
        }
    }

    // Check if the BigInteger N is completely coprime with ALL elements in the reference matrix A1
    fun isSpectralOpacityTriggered(n: java.math.BigInteger): Boolean {
        for (row in A1) {
            for (cell in row) {
                if (n.gcd(java.math.BigInteger.valueOf(cell.toLong())) > java.math.BigInteger.ONE) {
                    return false
                }
            }
        }
        return true
    }

    // Compute Diagonal Matrix DN values d_ii = gcd(N, R_i) for BigInteger N
    fun computeDN(n: java.math.BigInteger, mode: RowRepresentativeMode): DoubleArray {
        val dn = DoubleArray(19)
        for (i in 0 until 19) {
            val ri = getRowRepresentative(i, mode)
            dn[i] = n.gcd(java.math.BigInteger.valueOf(ri)).toDouble()
        }
        return dn
    }

    // Compute H_N = A1^T * DN * A1
    fun computeHN(dn: DoubleArray): Array<DoubleArray> {
        val hn = Array(6) { DoubleArray(6) }
        // H_N[j][k] = sum_{i=0..18} A1[i][j] * d_{ii} * A1[i][k]
        for (j in 0 until 6) {
            for (k in 0 until 6) {
                var sum = 0.0
                for (i in 0 until 19) {
                    sum += A1[i][j] * dn[i] * A1[i][k]
                }
                hn[j][k] = sum
            }
        }
        return hn
    }

    // Symmetric Jacobi Eigenvalue algorithm
    fun solveEigenvalues(matrix: Array<DoubleArray>, maxIterations: Int = 100): DoubleArray {
        val n = matrix.size
        val a = Array(n) { matrix[it].clone() }
        val d = DoubleArray(n)

        for (iter in 0 until maxIterations) {
            var p = 0
            var q = 1
            var maxVal = abs(a[p][q])
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val absVal = abs(a[i][j])
                    if (absVal > maxVal) {
                        maxVal = absVal
                        p = i
                        q = j
                    }
                }
            }

            if (maxVal < 1e-9) {
                break
            }

            val app = a[p][p]
            val aqq = a[q][q]
            val apq = a[p][q]

            val phi = (aqq - app) / (2.0 * apq)
            val t = if (phi >= 0) {
                1.0 / (phi + sqrt(1.0 + phi * phi))
            } else {
                -1.0 / (-phi + sqrt(1.0 + phi * phi))
            }

            val c = 1.0 / sqrt(1.0 + t * t)
            val s = t * c
            val tau = s / (1.0 + c)

            a[p][p] = app - t * apq
            a[q][q] = aqq + t * apq
            a[p][q] = 0.0
            a[q][p] = 0.0

            for (i in 0 until n) {
                if (i != p && i != q) {
                    val aip = a[i][p]
                    val aiq = a[i][q]
                    a[i][p] = aip - s * (aiq + aip * tau)
                    a[p][i] = a[i][p]
                    a[i][q] = aiq + s * (aip - aiq * tau)
                    a[q][i] = a[i][q]
                }
            }
        }

        for (i in 0 until n) {
            d[i] = a[i][i]
        }
        d.sortDescending()
        return d
    }

    // Calculate spectral similarity or divergence metric from opacity baseline
    // Baseline Opacity is when DN = Identity (each d_ii = 1.0)
    fun computeOpacitySpectrum(mode: RowRepresentativeMode): DoubleArray {
        val dnOpacity = DoubleArray(19) { 1.0 }
        val hnOpacity = computeHN(dnOpacity)
        return solveEigenvalues(hnOpacity)
    }

    // Visibility Distance (Euclidean divergence between current spectrum and opacity baseline spectrum)
    fun calculateVisibilityDistance(currentSpectrum: DoubleArray, opacitySpectrum: DoubleArray): Double {
        var sumSquares = 0.0
        for (i in 0 until currentSpectrum.size) {
            val diff = currentSpectrum[i] - opacitySpectrum[i]
            sumSquares += diff * diff
        }
        return sqrt(sumSquares)
    }
}
