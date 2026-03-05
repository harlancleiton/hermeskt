package br.com.olympus.hermes.shared.infrastructure.readmodel

import arrow.core.Either
import br.com.olympus.hermes.shared.config.TemplateMongo
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.util.Optional

@ApplicationScoped
class CachingTemplateRepository(
    @TemplateMongo private val delegate: TemplateRepository,
    @ConfigProperty(name = "hermes.template.cache-ttl-seconds") private val ttlSeconds: Long,
) : TemplateRepository {
    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
            .maximumSize(10_000)
            .build<String, Optional<NotificationTemplate>>()

    override fun findByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, NotificationTemplate?> {
        val key = cacheKey(name, channel)
        val cached = cache.getIfPresent(key)
        if (cached != null) {
            return Either.Right(cached.orElse(null))
        }

        return delegate.findByNameAndChannel(name, channel).onRight { template ->
            cache.put(key, Optional.ofNullable(template))
        }
    }

    override fun findAllByChannel(
        channel: NotificationType?,
        page: Int,
        size: Int,
    ): Either<BaseError, List<NotificationTemplate>> = delegate.findAllByChannel(channel, page, size)

    override fun save(template: NotificationTemplate): Either<BaseError, NotificationTemplate> =
        delegate.save(template).onRight { saved ->
            cache.invalidate(cacheKey(saved.name, saved.channel))
        }

    override fun update(template: NotificationTemplate): Either<BaseError, NotificationTemplate> =
        delegate.update(template).onRight { updated ->
            cache.invalidate(cacheKey(updated.name, updated.channel))
        }

    override fun deleteByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean> =
        delegate.deleteByNameAndChannel(name, channel).onRight { deleted ->
            if (deleted) {
                cache.invalidate(cacheKey(name, channel))
            }
        }

    override fun existsByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean> = delegate.existsByNameAndChannel(name, channel)

    private fun cacheKey(
        name: TemplateName,
        channel: NotificationType,
    ): String = "${name.value}:${channel.name}"
}
