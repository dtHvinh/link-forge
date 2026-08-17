package org.dthv.linkforge.app.service.impl

import org.dthv.linkforge.app.exceptions.LinkStorageException
import org.dthv.linkforge.app.service.CodeGenerateService
import org.dthv.linkforge.utils.toBase62
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate

private const val PRIME = 1_000_000_007L
private const val MULTIPLIER = 123_456_789L

private fun obfuscate(id: Long): Long = (id * MULTIPLIER) % PRIME

class Base62RedisCodeGeneratorService(
    private val redis: StringRedisTemplate,
) : CodeGenerateService {

    override fun generateCode(length: Int): String {
        require(length > 0) { "length must be positive, got $length" }

        val id = try {
            redis.opsForValue().increment(COUNTER_KEY)
        } catch (e: DataAccessException) {
            throw LinkStorageException("Redis unavailable while generating a code", e)
        } ?: throw LinkStorageException("Redis returned null for INCR on $COUNTER_KEY")

        return obfuscate(id).toBase62().padStart(length, PAD_CHAR)
    }

    companion object {
        private const val COUNTER_KEY = "counter:code"
        private const val PAD_CHAR = '0'
    }
}