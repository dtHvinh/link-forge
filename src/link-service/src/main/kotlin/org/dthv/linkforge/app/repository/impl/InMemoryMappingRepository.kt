package org.dthv.linkforge.app.repository.impl

import org.dthv.linkforge.app.repository.LinkMappingRepository
import java.util.concurrent.ConcurrentHashMap

private typealias OriginalLink = String
private typealias Code = String

class InMemoryMappingRepository : LinkMappingRepository {

    private val codeToOriginal: MutableMap<Code, OriginalLink> = ConcurrentHashMap()
    private val originalToCodes: MutableMap<OriginalLink, MutableSet<Code>> = ConcurrentHashMap()

    override fun map(original: String, code: String) {
        codeToOriginal[code] = original
        originalToCodes
            .computeIfAbsent(original) { ConcurrentHashMap.newKeySet() }
            .add(code)
    }

    override fun getOriginal(code: Code): OriginalLink? =
        codeToOriginal[code]

    override fun getCodes(original: OriginalLink): Set<Code> =
        originalToCodes[original] ?: emptySet()
}