package org.dthv.linkforge.app.service

interface CodeGenerateService {
    fun generateCode(length: Int = 6): String
}