package com.example.math

import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

object SDIMath {

    private val random = SecureRandom()
    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)

    // Dynamic cache to store prime factors discovered via Gemini or manual override
    private val dynamicFactorCache = ConcurrentHashMap<BigInteger, List<BigInteger>>()

    fun registerDynamicFactorization(n: BigInteger, p1: BigInteger, p2: BigInteger) {
        dynamicFactorCache[n] = listOf(p1, p2)
    }

    fun getDynamicCache(): Map<BigInteger, List<BigInteger>> = dynamicFactorCache

    // Primes lazy sieve generator up to 150,000 to eliminate small prime factors in microseconds
    private val smallPrimesList: List<Int> by lazy {
        val limit = 150000
        val isPrime = BooleanArray(limit + 1) { true }
        isPrime[0] = false
        isPrime[1] = false
        for (p in 2..sqrt(limit.toDouble()).toInt()) {
            if (isPrime[p]) {
                for (i in p * p..limit step p) {
                    isPrime[i] = false
                }
            }
        }
        val list = mutableListOf<Int>()
        for (p in 2..limit) {
            if (isPrime[p]) list.add(p)
        }
        list
    }

    /**
     * Complete recursive factorization. 
     * If a factor is discovered to be composite, we continue analyzing/factorizing it!
     */
    fun factorise(n: BigInteger): Map<BigInteger, Int> {
        val factors = java.util.TreeMap<BigInteger, Int>()
        if (n <= BigInteger.ONE) {
            if (n == BigInteger.ONE) {
                factors[BigInteger.ONE] = 1
            }
            return factors
        }

        var tempN = n

        // 1. Check small primes first
        for (prime in smallPrimesList) {
            val bp = BigInteger.valueOf(prime.toLong())
            if (bp.multiply(bp) > tempN) break
            var count = 0
            while (tempN.remainder(bp) == BigInteger.ZERO) {
                count++
                tempN = tempN.divide(bp)
            }
            if (count > 0) {
                factors[bp] = count
            }
        }

        if (tempN > BigInteger.ONE) {
            // Check if our remaining number is a known split in the dynamic cache
            val factorsToDecompose = mutableListOf<BigInteger>()
            factorsToDecompose.add(tempN)

            while (factorsToDecompose.isNotEmpty()) {
                val current = factorsToDecompose.removeAt(0)
                if (current.isProbablePrime(40)) {
                    factors[current] = (factors[current] ?: 0) + 1
                } else {
                    // It is composite! Check if we have registered factors for it
                    val cached = dynamicFactorCache[current]
                    if (cached != null) {
                        for (fac in cached) {
                            factorsToDecompose.add(fac)
                        }
                    } else {
                        // Attempt to run quick Pollard's Rho to split it
                        val divisor = pollardRhoSplit(current)
                        if (divisor != null && divisor != BigInteger.ONE && divisor != current) {
                            factorsToDecompose.add(divisor)
                            factorsToDecompose.add(current.divide(divisor))
                        } else {
                            // If we fail to factorize it, keep it as composite factor in the list for now
                            // The user can continue analyzing it with AI or manual input
                            factors[current] = (factors[current] ?: 0) + 1
                        }
                    }
                }
            }
        }

        return factors
    }

    /**
     * Pollard's Rho implementation with a step limit to prevent infinite loops on hard composites
     */
    private fun pollardRhoSplit(n: BigInteger): BigInteger? {
        if (n.remainder(TWO) == BigInteger.ZERO) return TWO
        if (n.remainder(THREE) == BigInteger.ZERO) return THREE

        // Use standard Pollard's Rho with multiple starting points
        for (tryIndex in 1..3) {
            var x = BigInteger(n.bitLength(), random).remainder(n.subtract(TWO)).add(TWO)
            var y = x
            val c = BigInteger(n.bitLength(), random).remainder(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)
            var d = BigInteger.ONE
            var steps = 0
            val maxSteps = 2500 // Bound iterations to prevent hanging on large semiprimes

            while (d == BigInteger.ONE && steps < maxSteps) {
                steps++
                // f(x) = (x^2 + c) mod n
                x = x.multiply(x).add(c).remainder(n)
                
                // f(f(y))
                y = y.multiply(y).add(c).remainder(n)
                y = y.multiply(y).add(c).remainder(n)
                
                val diff = x.subtract(y).abs()
                d = diff.gcd(n)
            }
            if (d > BigInteger.ONE && d < n) {
                return d
            }
        }
        return null
    }

    fun factorsToString(factors: Map<BigInteger, Int>): String {
        if (factors.isEmpty()) return "1"
        return factors.entries.joinToString(" × ") { (fac, exp) ->
            if (exp > 1) "$fac^$exp" else "$fac"
        }
    }

    /**
     * Computes real-time Matrix Energetic Prime Analysis (MEPA) diagnostics
     */
    fun computeMepaDiagnostics(factors: Map<BigInteger, Int>): MepaResults {
        // Flatten list of all distinct factors (treating exponents as separate items for the matrix)
        val list = mutableListOf<BigInteger>()
        factors.forEach { (fac, exp) ->
            repeat(exp) { list.add(fac) }
        }

        if (list.isEmpty()) {
            return MepaResults(emptyList(), BigInteger.ZERO, 0.0, 0, 1, 0, 1)
        }

        // Build a square relation matrix of dimension size x size
        val size = list.size
        val matrix = Array(size) { DoubleArray(size) }
        var trace = 0.0
        var fNormSq = 0.0

        for (i in 0 until size) {
            for (j in 0 until size) {
                val fI = list[i].toDouble()
                val fJ = list[j].toDouble()
                // Energetic resonance equation based on factor coordinates
                val resonance = if (i == j) {
                    val angle = Math.sin(fI)
                    angle * angle
                } else {
                    val commonGcd = list[i].gcd(list[j]).toDouble()
                    val ratio = commonGcd / (fI + fJ)
                    Math.cos(fI * fJ) * (1.0 + ratio)
                }
                matrix[i][j] = resonance
                fNormSq += resonance * resonance
                if (i == j) trace += resonance
            }
        }

        val fNorm = Math.sqrt(fNormSq)

        // Topological Invariants representation
        // H0 is connected components: number of distinct prime factors
        val h0 = factors.size
        
        // H1 is semantic cycles or structural gaps
        val h1 = if (size > 1) size - 1 else 0
        
        // Betti homology: B0 = h0, B1 = h1, B2 = if h1 > 1, 1 else 0
        val b0 = h0
        val b1 = h1
        val b2 = if (h1 > 1) 1 else 0

        // Characteristic scalar N
        var sumN = BigInteger.ZERO
        for (f in list) {
            sumN = sumN.add(f)
        }

        return MepaResults(
            relationMatrix = matrix.map { it.toList() },
            spectralSum = sumN,
            frobeniusNorm = fNorm,
            betti0 = b0,
            betti1 = b1,
            betti2 = b2,
            dimension = size
        )
    }
}

data class MepaResults(
    val relationMatrix: List<List<Double>>,
    val spectralSum: BigInteger,
    val frobeniusNorm: Double,
    val betti0: Int,
    val betti1: Int,
    val betti2: Int,
    val dimension: Int
)
