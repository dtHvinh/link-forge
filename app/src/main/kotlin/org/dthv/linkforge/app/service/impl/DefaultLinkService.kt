package org.dthv.linkforge.app.service.impl

import org.dthv.linkforge.app.config.AppConfig
import org.dthv.linkforge.app.exceptions.LinkStorageException
import org.dthv.linkforge.app.repository.LinkMappingRepository
import org.dthv.linkforge.app.service.CodeGenerateService
import org.dthv.linkforge.app.service.LinkService
import org.springframework.stereotype.Service

@Service
class DefaultLinkService(
    val codeGenerateService: CodeGenerateService,
    val mappingRepository: LinkMappingRepository,
    val appConfig: AppConfig
) : LinkService {
    override fun generateLink(link: String): String {
        val code = codeGenerateService.generateCode()
        val resultUrl = "https://${appConfig.domain}/${code}"
        mappingRepository.map(link, code);
        return resultUrl;
    }

    override fun resolve(code: String): String {
        return mappingRepository.getOriginal(code)
            ?: throw LinkStorageException("Invalid code: $code")
    }
}