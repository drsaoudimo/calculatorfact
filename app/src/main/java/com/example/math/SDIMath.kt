package com.example.math

import java.math.BigInteger
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

object SDIMath {

    private val random = SecureRandom()
    val TWO = BigInteger.valueOf(2)
    val THREE = BigInteger.valueOf(3)
    val FOUR = BigInteger.valueOf(4)
    val TWENTY_SEVEN = BigInteger.valueOf(27)

    // SCNT Matrix A1 (19 rows by 6 columns)
    val A1 = arrayOf(
        longArrayOf(7, 286, 200, 176, 120, 165),
        longArrayOf(206, 75, 129, 109, 123, 111),
        longArrayOf(43, 52, 99, 128, 111, 110),
        longArrayOf(98, 135, 112, 78, 118, 64),
        longArrayOf(77, 227, 93, 88, 69, 60),
        longArrayOf(34, 30, 73, 54, 45, 83),
        longArrayOf(182, 88, 75, 85, 54, 53),
        longArrayOf(89, 59, 37, 35, 38, 29),
        longArrayOf(18, 45, 60, 49, 62, 55),
        longArrayOf(78, 96, 29, 22, 24, 13),
        longArrayOf(14, 11, 11, 18, 12, 12),
        longArrayOf(30, 52, 52, 44, 28, 28),
        longArrayOf(20, 56, 40, 31, 50, 40),
        longArrayOf(46, 42, 29, 19, 36, 25),
        longArrayOf(22, 17, 19, 26, 30, 20),
        longArrayOf(15, 21, 11, 8, 8, 19),
        longArrayOf(5, 8, 8, 11, 11, 8),
        longArrayOf(3, 9, 5, 4, 7, 3),
        longArrayOf(6, 3, 5, 4, 5, 6)
    )

    // Gram matrix G = A^T * A of dimension (6 x 6)
    val G: Array<DoubleArray> = Array(6) { DoubleArray(6) }

    init {
        for (i in 0 until 6) {
            for (j in 0 until 6) {
                var sum = 0.0
                for (k in 0 until 19) {
                    sum += A1[k][i] * A1[k][j]
                }
                G[i][j] = sum
            }
        }
    }

    /**
     * QR algorithm for eigenvalues of symmetric 6x6 matrix G
     */
    fun computeEigenvalues(matrix: Array<DoubleArray>): DoubleArray {
        val a = Array(6) { matrix[it].clone() }
        val n = 6
        for (iter in 0 until 60) {
            val q = Array(n) { DoubleArray(n) }
            val r = Array(n) { DoubleArray(n) }
            // Classical Gram-Schmidt orthodecomp
            for (j in 0 until n) {
                val v = DoubleArray(n) { a[it][j] }
                for (i in 0 until j) {
                    var dot = 0.0
                    for (k in 0 until n) {
                        dot += q[k][i] * a[k][j]
                    }
                    r[i][j] = dot
                    for (k in 0 until n) {
                        v[k] -= dot * q[k][i]
                    }
                }
                var norm = 0.0
                for (k in 0 until n) {
                    norm += v[k] * v[k]
                }
                norm = sqrt(norm)
                r[j][j] = norm
                if (norm > 1e-9) {
                    for (k in 0 until n) {
                        q[k][j] = v[k] / norm
                    }
                }
            }
            // a_new = R * Q
            for (i in 0 until n) {
                for (j in 0 until n) {
                    var sum = 0.0
                    for (k in 0 until n) {
                        sum += r[i][k] * q[k][j]
                    }
                    a[i][j] = sum
                }
            }
        }
        return DoubleArray(n) { a[it][it] }
    }

    val eigenvalues: DoubleArray by lazy {
        computeEigenvalues(G)
    }

    /**
     * Φ_A(N) = \sum_i \alpha_i(N) R_i
     * High-dimensional projection operator to a 6D space
     */
    fun phiA(n: BigInteger): DoubleArray {
        val result = DoubleArray(6) { 0.0 }
        for (i in 0 until 19) {
            val row = A1[i]
            // \alpha_i coefficient as remainder of N mod (i + 2)
            val modVal = n.remainder(BigInteger.valueOf((i + 2).toLong())).toDouble()
            for (j in 0 until 6) {
                result[j] += modVal * row[j]
            }
        }
        return result
    }

    /**
     * Stage 0: QMFG Direct Arithmetic spectral factorization.
     * Incorporates Direct Resonances: G_ij(N) = gcd(N, A_ij), row sum, and column sum alignments.
     */
    fun qmfgSpectralSplit(n: BigInteger): BigInteger? {
        // Checking elements gcd(N, a_ij) directly
        for (i in 0 until 19) {
            for (j in 0 until 6) {
                val elemVal = BigInteger.valueOf(A1[i][j])
                val g = n.gcd(elemVal)
                if (g > BigInteger.ONE && g < n) {
                    return g
                }
            }
        }
        // Checking row sum gcd alignments
        for (i in 0 until 19) {
            var rowSum = 0L
            for (j in 0 until 6) {
                rowSum += A1[i][j]
            }
            val g = n.gcd(BigInteger.valueOf(rowSum))
            if (g > BigInteger.ONE && g < n) {
                return g
            }
        }
        // Checking column sum gcd alignments
        for (j in 0 until 6) {
            var colSum = 0L
            for (i in 0 until 19) {
                colSum += A1[i][j]
            }
            val g = n.gcd(BigInteger.valueOf(colSum))
            if (g > BigInteger.ONE && g < n) {
                return g
            }
        }
        // Matrix trace/overall sum alignment
        var overallSum = 0L
        for (i in 0 until 19) {
            for (j in 0 until 6) {
                overallSum += A1[i][j]
            }
        }
        val gOverall = n.gcd(BigInteger.valueOf(overallSum))
        if (gOverall > BigInteger.ONE && gOverall < n) {
            return gOverall
        }
        return null
    }

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
     * Complete recursive factorization with state-of-the-art continuous extraction:
     * - Stage 0: QMFG Direct Arithmetic Matrix Resonances
     * - Stage 1: Trial division using small primes
     * - Stage 2: Fermat factorization (for close factors)
     * - Stage 3: Pollard's Rho Brent cycle-detection (infused with matrix dimensions)
     * - Stage 4: Lenstra Elliptic Curve Method (ECM)
     * - Stage 5: Traditional Pollard's Rho fallback
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
            val factorsToDecompose = mutableListOf<BigInteger>()
            factorsToDecompose.add(tempN)

            while (factorsToDecompose.isNotEmpty()) {
                val current = factorsToDecompose.removeAt(0)
                if (current.isProbablePrime(40)) {
                    factors[current] = (factors[current] ?: 0) + 1
                } else {
                    // It is composite! Check if we have registered factors for it in our cache/overrides
                    val cached = dynamicFactorCache[current]
                    if (cached != null) {
                        for (fac in cached) {
                            factorsToDecompose.add(fac)
                        }
                    } else {
                        // Continuous mathematical factorization hierarchy with our hybrid QMFG split
                        var divisor: BigInteger? = null
                        
                        // Stage 0: Trial split based on SCNT/QMFG resonances
                        divisor = qmfgSpectralSplit(current)

                        // Stage 1: Try Fermat's (highly efficient for close factors)
                        if (divisor == null) {
                            divisor = fermatSplit(current)
                        }
                        
                        // Stage 2: Try Pollard's Rho Brent variant (optimal random search with power of 2 steps)
                        if (divisor == null) {
                            divisor = pollardRhoBrentSplit(current)
                        }
                        
                        // Stage 3: Try Lenstra Elliptic Curve Method (ECM)
                        if (divisor == null) {
                            divisor = ecmSplit(current)
                        }
                        
                        // Stage 4: Try traditional Pollard's Rho fallback
                        if (divisor == null) {
                            divisor = pollardRhoSplit(current)
                        }
                        
                        if (divisor != null && divisor != BigInteger.ONE && divisor != current) {
                            factorsToDecompose.add(divisor)
                            factorsToDecompose.add(current.divide(divisor))
                        } else {
                            // If everything fails, report as composite for now
                            factors[current] = (factors[current] ?: 0) + 1
                        }
                    }
                }
            }
        }

        return factors
    }

    /**
     * Pollard's Rho with Brent's cycle detection (uses powers of 2 for step jumps with multiplied diff block GCD).
     */
    fun pollardRhoBrentSplit(n: BigInteger): BigInteger? {
        if (n.remainder(TWO) == BigInteger.ZERO) return TWO
        if (n.remainder(THREE) == BigInteger.ZERO) return THREE
        
        // Calculate the positive spectral resonance constant from Gram matrix eigenvalues
        val spectralNudge = BigInteger.valueOf((eigenvalues.sum() * 10.0).toLong().coerceAtLeast(1L))

        for (tryIndex in 1..4) {
            var x = BigInteger(n.bitLength(), random).remainder(n.subtract(TWO)).add(TWO)
            var y = x
            val c = BigInteger(n.bitLength(), random).remainder(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)
            var q = BigInteger.ONE
            var g = BigInteger.ONE
            
            var r = 1L
            val m = 100L
            
            var ys = y
            var steps = 0
            val maxSteps = 5000
            
            while (g == BigInteger.ONE && steps < maxSteps) {
                x = y
                for (i in 0 until r) {
                    val matrixOffset = BigInteger.valueOf(A1[((steps + i) % 19).toInt()][((steps + i) % 6).toInt()])
                    y = y.multiply(y).add(c).add(spectralNudge).add(matrixOffset).remainder(n)
                }
                
                var k = 0L
                while (k < r && g == BigInteger.ONE && steps < maxSteps) {
                    ys = y
                    val limit = minOf(m, r - k)
                    for (i in 0 until limit) {
                        val matrixOffset = BigInteger.valueOf(A1[(steps % 19).toInt()][(steps % 6).toInt()])
                        y = y.multiply(y).add(c).add(spectralNudge).add(matrixOffset).remainder(n)
                        val diff = x.subtract(y).abs()
                        q = q.multiply(diff).remainder(n)
                        steps++
                    }
                    g = q.gcd(n)
                    k += m
                }
                r *= 2
            }
            
            if (g == n) {
                // backtrack and obtain real factor
                g = BigInteger.ONE
                y = ys
                var backSteps = 0
                while (g == BigInteger.ONE && backSteps < 500) {
                    val matrixOffset = BigInteger.valueOf(A1[(backSteps % 19).toInt()][(backSteps % 6).toInt()])
                    y = y.multiply(y).add(c).add(spectralNudge).add(matrixOffset).remainder(n)
                    val diff = x.subtract(y).abs()
                    g = diff.gcd(n)
                    backSteps++
                }
            }
            
            if (g > BigInteger.ONE && g < n) {
                return g
            }
        }
        return null
    }

    /**
     * Lenstra Elliptic Curve Method (ECM)
     * Fits a randomized elliptic Weierstrass curve y^2 = x^3 + ax + b mod n with point adding.
     */
    data class EcPoint(val x: BigInteger, val y: BigInteger, val isZero: Boolean = false)

    fun ecmSplit(n: BigInteger): BigInteger? {
        if (n.remainder(TWO) == BigInteger.ZERO) return TWO
        if (n.remainder(THREE) == BigInteger.ZERO) return THREE

        val ecmPrimes = smallPrimesList.filter { it <= 500 }
        val ecmB1 = 500

        // Attempt on 12 distinct randomized elliptic curves
        for (curveIndex in 1..12) {
            val x0 = BigInteger(n.bitLength(), random).remainder(n)
            val y0 = BigInteger(n.bitLength(), random).remainder(n)
            val a = BigInteger(n.bitLength(), random).remainder(n)
            
            val ySq = y0.multiply(y0).remainder(n)
            val xCubePlusAx = x0.multiply(x0).multiply(x0).add(a.multiply(x0)).remainder(n)
            val b = ySq.subtract(xCubePlusAx).remainder(n).add(n).remainder(n)
            
            // Check determinant discriminant 4a^3 + 27b^2 mod n
            val disc = FOUR.multiply(a).multiply(a).multiply(a).add(TWENTY_SEVEN.multiply(b).multiply(b)).remainder(n)
            val discGcd = disc.gcd(n)
            if (discGcd > BigInteger.ONE && discGcd < n) {
                return discGcd
            }
            if (discGcd == n) continue // Singular curve, change config

            var currentPt: Any = EcPoint(x0, y0)
            for (prime in ecmPrimes) {
                val pVal = BigInteger.valueOf(prime.toLong())
                var limit = 1
                var temp = prime
                while (temp * prime <= ecmB1) {
                    temp *= prime
                    limit++
                }
                
                for (step in 1..limit) {
                    if (currentPt is BigInteger) {
                        return currentPt
                    }
                    val pt = currentPt as EcPoint
                    if (pt.isZero) break
                    currentPt = ecmMultiply(pt, pVal, a, n)
                }
            }
            if (currentPt is BigInteger) {
                return currentPt
            }
        }
        return null
    }

    private fun ecmAdd(p1: EcPoint, p2: EcPoint, a: BigInteger, n: BigInteger): Any {
        if (p1.isZero) return p2
        if (p2.isZero) return p1

        if (p1.x == p2.x) {
            if (p1.y.add(p2.y).remainder(n) == BigInteger.ZERO) {
                return EcPoint(BigInteger.ZERO, BigInteger.ZERO, isZero = true)
            }
            // Point doubling: slope = (3*x1^2 + a) * (2*y1)^(-1) mod n
            val num = THREE.multiply(p1.x).multiply(p1.x).add(a).remainder(n)
            val den = TWO.multiply(p1.y).remainder(n)
            val gcd = den.gcd(n)
            if (gcd > BigInteger.ONE && gcd < n) {
                return gcd
            }
            if (gcd == n) {
                return EcPoint(BigInteger.ZERO, BigInteger.ZERO, isZero = true)
            }
            val inv = den.modInverse(n)
            val lambda = num.multiply(inv).remainder(n)
            
            val x3 = lambda.multiply(lambda).subtract(TWO.multiply(p1.x)).remainder(n).add(n).remainder(n)
            val y3 = lambda.multiply(p1.x.subtract(x3)).subtract(p1.y).remainder(n).add(n).remainder(n)
            return EcPoint(x3, y3)
        } else {
            // Point addition: slope = (y2 - y1) * (x2 - x1)^(-1) mod n
            val num = p2.y.subtract(p1.y).remainder(n).add(n).remainder(n)
            val den = p2.x.subtract(p1.x).remainder(n).add(n).remainder(n)
            val gcd = den.gcd(n)
            if (gcd > BigInteger.ONE && gcd < n) {
                return gcd
            }
            if (gcd == n) {
                return EcPoint(BigInteger.ZERO, BigInteger.ZERO, isZero = true)
            }
            val inv = den.modInverse(n)
            val lambda = num.multiply(inv).remainder(n)
            
            val x3 = lambda.multiply(lambda).subtract(p1.x).subtract(p2.x).remainder(n).add(n).remainder(n)
            val y3 = lambda.multiply(p1.x.subtract(x3)).subtract(p1.y).remainder(n).add(n).remainder(n)
            return EcPoint(x3, y3)
        }
    }

    private fun ecmMultiply(p: EcPoint, d: BigInteger, a: BigInteger, n: BigInteger): Any {
        var result: Any = EcPoint(BigInteger.ZERO, BigInteger.ZERO, isZero = true)
        var temp: Any = p
        var bits = d
        while (bits > BigInteger.ZERO) {
            if (bits.testBit(0)) {
                if (result is BigInteger) return result
                if (temp is BigInteger) return temp
                result = ecmAdd(result as EcPoint, temp as EcPoint, a, n)
            }
            if (result is BigInteger) return result
            if (temp is BigInteger) return temp
            temp = ecmAdd(temp as EcPoint, temp, a, n)
            bits = bits.shiftRight(1)
        }
        return result
    }

    /**
     * Fermat's factorization method: optimal when the factors are geographically close.
     */
    fun fermatSplit(n: BigInteger): BigInteger? {
        if (n.remainder(TWO) == BigInteger.ZERO) return TWO
        val sqrtN = bigIntSqrt(n)
        var x = sqrtN
        if (x.multiply(x) < n) {
            x = x.add(BigInteger.ONE)
        }
        
        var ySq = x.multiply(x).subtract(n)
        var steps = 0
        val maxSteps = 5000 
        
        while (steps < maxSteps) {
            val y = bigIntSqrt(ySq)
            if (y.multiply(y) == ySq) {
                val p = x.subtract(y)
                val q = x.add(y)
                if (p > BigInteger.ONE && p < n) {
                    return p
                }
            }
            x = x.add(BigInteger.ONE)
            ySq = x.multiply(x).subtract(n)
            steps++
        }
        return null
    }

    private fun bigIntSqrt(n: BigInteger): BigInteger {
        if (n == BigInteger.ZERO) return BigInteger.ZERO
        var g = n.shiftRight(n.bitLength() / 2)
        if (g == BigInteger.ZERO) g = BigInteger.ONE
        while (true) {
            val nextG = g.add(n.divide(g)).shiftRight(1)
            if (nextG >= g || nextG.subtract(g).abs() <= BigInteger.ONE) {
                val candidate = if (nextG < g) nextG else g
                if (candidate.multiply(candidate) <= n) {
                    return candidate
                }
                return candidate.subtract(BigInteger.ONE)
            }
            g = nextG
        }
    }

    /**
     * Traditional Pollard's Rho solver.
     */
    fun pollardRhoSplit(n: BigInteger): BigInteger? {
        if (n.remainder(TWO) == BigInteger.ZERO) return TWO
        if (n.remainder(THREE) == BigInteger.ZERO) return THREE

        for (tryIndex in 1..3) {
            var x = BigInteger(n.bitLength(), random).remainder(n.subtract(TWO)).add(TWO)
            var y = x
            val c = BigInteger(n.bitLength(), random).remainder(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)
            var d = BigInteger.ONE
            var steps = 0
            val maxSteps = 2500

            while (d == BigInteger.ONE && steps < maxSteps) {
                steps++
                x = x.multiply(x).add(c).remainder(n)
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
