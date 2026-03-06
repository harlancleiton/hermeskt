package br.com.olympus.hermes.template.infrastructure.config

import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.services.TemplateEngine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import jakarta.ws.rs.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class TemplateConfig {
    @ConfigProperty(name = "hermes.template.placeholder-regex")
    private lateinit var placeholderRegex: String

    @Produces
    @Singleton
    fun templateEngine(templateRepository: TemplateRepository): TemplateEngine =
        TemplateEngine(templateRepository, placeholderRegex.toRegex())
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
)
annotation class TemplateMongo
