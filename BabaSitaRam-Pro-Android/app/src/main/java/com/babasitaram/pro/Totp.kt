package com.babasitaram.pro

import java.net.URLDecoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238 TOTP (Google Authenticator jaisa). Secret base32 ya otpauth:// link dono chalte hain. */
object Totp {

    class Params(val secret: ByteArray, val digits: Int, val period: Int, val algo: String)

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    private fun base32(input: String): ByteArray? {
        val clean = input.uppercase().replace(" ", "").replace("-", "").trimEnd('=')
        if (clean.isEmpty()) return null
        var buffer = 0
        var bits = 0
        val out = java.io.ByteArrayOutputStream()
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xFF)
                bits -= 8
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return out.toByteArray()
    }

    fun parse(input: String): Params? {
        val t = input.trim()
        if (t.isEmpty()) return null
        var secretStr = t
        var digits = 6
        var period = 30
        var algo = "HmacSHA1"
        if (t.startsWith("otpauth://", ignoreCase = true)) {
            val q = t.substringAfter('?', "")
            for (pair in q.split('&')) {
                val k = pair.substringBefore('=').lowercase()
                val v = try {
                    URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
                } catch (e: Exception) { "" }
                when (k) {
                    "secret" -> secretStr = v
                    "digits" -> digits = v.toIntOrNull() ?: 6
                    "period" -> period = v.toIntOrNull() ?: 30
                    "algorithm" -> {
                        algo = when (v.uppercase()) {
                            "SHA256" -> "HmacSHA256"
                            "SHA512" -> "HmacSHA512"
                            else -> "HmacSHA1"
                        }
                    }
                }
            }
        }
        val key = base32(secretStr) ?: return null
        if (key.isEmpty()) return null
        if (digits < 6 || digits > 8) digits = 6
        if (period <= 0) period = 30
        return Params(key, digits, period, algo)
    }

    fun code(input: String, nowMs: Long = System.currentTimeMillis()): String? {
        val p = parse(input) ?: return null
        return try {
            var counter = nowMs / 1000L / p.period
            val msg = ByteArray(8)
            for (i in 7 downTo 0) {
                msg[i] = (counter and 0xFFL).toByte()
                counter = counter shr 8
            }
            val mac = Mac.getInstance(p.algo)
            mac.init(SecretKeySpec(p.secret, p.algo))
            val h = mac.doFinal(msg)
            val off = h[h.size - 1].toInt() and 0x0F
            val bin = ((h[off].toInt() and 0x7F) shl 24) or
                ((h[off + 1].toInt() and 0xFF) shl 16) or
                ((h[off + 2].toInt() and 0xFF) shl 8) or
                (h[off + 3].toInt() and 0xFF)
            var mod = 1
            repeat(p.digits) { mod *= 10 }
            (bin % mod).toString().padStart(p.digits, '0')
        } catch (e: Exception) { null }
    }

    fun secondsLeft(input: String, nowMs: Long = System.currentTimeMillis()): Int {
        val period = parse(input)?.period ?: 30
        return period - ((nowMs / 1000L) % period).toInt()
    }
}
