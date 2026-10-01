package id.bca.bcamobile.data.content

import id.bca.bcamobile.core.network.ApiCaller
import id.bca.bcamobile.data.content.remote.ContentApi
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.domain.common.map
import id.bca.bcamobile.domain.content.ContentRepository
import id.bca.bcamobile.domain.content.model.ContactCs
import id.bca.bcamobile.domain.content.model.HelpCategory
import id.bca.bcamobile.domain.content.model.HelpItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val api: ContentApi,
    private val caller: ApiCaller,
) : ContentRepository {

    override suspend fun helpCenter(): DataResult<List<HelpCategory>> =
        caller.call { api.helpCenter() }.map { dto ->
            dto.categories.map { category ->
                HelpCategory(
                    key = category.key,
                    title = category.title,
                    items = category.items.map { HelpItem(it.question, it.answer) },
                )
            }
        }

    override suspend fun contactCs(): DataResult<ContactCs> =
        caller.call { api.contactCs() }.map {
            ContactCs(
                phone = it.phone,
                phoneFree = it.phoneFree,
                whatsapp = it.whatsapp,
                email = it.email,
                chatUrl = it.chatUrl,
                hours = it.hours,
            )
        }
}
