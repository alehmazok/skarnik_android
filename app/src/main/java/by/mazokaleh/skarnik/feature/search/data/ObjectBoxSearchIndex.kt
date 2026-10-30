package by.mazokaleh.skarnik.feature.search.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.search.domain.SearchIndex
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.Property
import io.objectbox.query.QueryBuilder.StringOrder
import io.objectbox.query.QueryCondition
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ObjectBoxSearchIndex @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SearchIndex {
    override suspend fun findByWord(prefixes: Set<String>, limit: Int): List<SearchWord> =
        find(startsWithAny(ObjectboxSearchWord_.lword, prefixes), ObjectboxSearchWord_.lword, limit)

    override suspend fun findByWordMask(prefixes: Set<String>, excludedIds: Set<Long>, limit: Int): List<SearchWord> {
        var condition = startsWithAny(ObjectboxSearchWord_.lwordMask, prefixes)
        if (excludedIds.isNotEmpty()) {
            condition = condition.and(ObjectboxSearchWord_.id.notOneOf(excludedIds.toLongArray()))
        }
        return find(condition, ObjectboxSearchWord_.lwordMask, limit)
    }

    override suspend fun findByLetter(letter: String, limit: Int): List<SearchWord> =
        find(ObjectboxSearchWord_.letter.equal(letter, StringOrder.CASE_SENSITIVE), order = null, limit = limit)

    private fun startsWithAny(
        property: Property<ObjectboxSearchWord>,
        prefixes: Set<String>
    ): QueryCondition<ObjectboxSearchWord> = prefixes
        .map<String, QueryCondition<ObjectboxSearchWord>> { property.startsWith(it, StringOrder.CASE_SENSITIVE) }
        .reduce { acc, next -> acc.or(next) }

    private suspend fun find(
        condition: QueryCondition<ObjectboxSearchWord>,
        order: Property<ObjectboxSearchWord>?,
        limit: Int
    ): List<SearchWord> = withContext(ioDispatcher) {
        val builder = database.stores().searchBox.query(condition)
        if (order != null) builder.order(order)
        builder.build().use { query -> query.find(0, limit.toLong()) }.mapNotNull { it.toSearchWord() }
    }

    private fun ObjectboxSearchWord.toSearchWord(): SearchWord? {
        val dictionary = Dictionary.byLangId(langId) ?: return null
        return SearchWord(
            id = id,
            dictionary = dictionary,
            wordId = wordId,
            word = word,
            letter = letter,
            lword = lword
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface SearchModule {
    @Binds
    fun bindSearchIndex(impl: ObjectBoxSearchIndex): SearchIndex
}
