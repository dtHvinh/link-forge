package org.dthv.linkforge.app.repository

interface LinkMappingRepository {
    fun map(original: String, code: String)
    fun getOriginal(code: String) : String?
    fun getCodes(original: String): Set<String>
}