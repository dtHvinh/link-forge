package org.dthv.linkforge.app.service

interface LinkService {
    fun resolve(code: String): String
    fun generateLink(link: String): String
}