package org.dthv.linkforge.app.service.impl

import org.dthv.linkforge.app.service.CodeGenerateService
import org.dthv.linkforge.domain.constants.Alphabet
import java.security.SecureRandom

class RandomCodeGeneratorService : CodeGenerateService {
    private val random = SecureRandom()

    override fun generateCode(length: Int): String =
        (1..length)
            .map { Alphabet.VALUE[random.nextInt(Alphabet.VALUE.length)] }
            .joinToString("")
}
