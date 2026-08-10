package org.dthv.linkforge.app.repository.impl

import org.dthv.linkforge.app.exceptions.LinkStorageException
import org.dthv.linkforge.app.repository.LinkMappingRepository
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate

class RedisMappingRepository(
    private val redis: StringRedisTemplate,
) : LinkMappingRepository {

    override fun map(original: String, code: String) {
        try {
            redis.opsForValue().set(codeKey(code), original)
            redis.opsForSet().add(originalKey(original), code)
        } catch (e: DataAccessException) {
            throw LinkStorageException("Redis unavailable while storing code $code", e)
        }
    }

    override fun getOriginal(code: String): String? =
        try {
            redis.opsForValue().get(codeKey(code))
        } catch (e: DataAccessException) {
            throw LinkStorageException("Redis unavailable while resolving code $code", e)
        }

    override fun getCodes(original: String): Set<String> =
        try {
            redis.opsForSet().members(originalKey(original)) ?: emptySet()
        } catch (e: DataAccessException) {
            throw LinkStorageException("Redis unavailable while listing codes for $original", e)
        }

    companion object {
        private fun codeKey(code: String) = "code:$code"
        private fun originalKey(original: String) = "original:$original"
    }
}
