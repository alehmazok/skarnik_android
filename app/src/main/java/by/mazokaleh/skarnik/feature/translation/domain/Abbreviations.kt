package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/** An abbreviation found in a text: its [range] there, the [abbreviation] as written and its [expansion]. */
data class AbbreviationMatch(val range: IntRange, val abbreviation: String, val expansion: String)

/** What a run of translation text is, which decides the abbreviations looked for in it. */
enum class TextPlace {
    /** Grammar labels («жен.», «закончанае трыванне», «//»). */
    Label,

    /** Uncoloured text: definitions and word origins («(у 1 знач.)», «[Ад грэч. ...]»). */
    Plain,

    /** Usage examples and citations. */
    Quote,

    /** The source of a citation («Колас.»): always one whole span. */
    Source
}

/** The abbreviations one dictionary's translations use, and where. */
fun interface AbbreviationScheme {
    fun findIn(text: String, place: TextPlace): List<AbbreviationMatch>
}

object Abbreviations {
    /**
     * Grammar and subject labels of the Рус-Бел and Бел-Рус dictionaries («разг.», «в разн. знач.») with their
     * full text in Russian (list from Aleh, 2026-11-15, plus the forms Бел-Рус actually uses: «жен.», «муж.»,
     * «совер.», «несовер.»). In the data only Бел-Рус abbreviates them, inside its green label spans; Рус-Бел
     * spells them out.
     */
    val russian = AbbreviationList(RUSSIAN)

    /** Рус-Бел and Бел-Рус: [russian] labels, only in label spans. */
    val russianScheme = AbbreviationScheme { text, place ->
        if (place == TextPlace.Label) russian.findIn(text) else emptyList()
    }

    fun of(dictionary: Dictionary): AbbreviationScheme = when (dictionary) {
        Dictionary.BelRus, Dictionary.RusBel -> russianScheme
        Dictionary.Tsbm -> TsbmAbbreviations
    }
}

/**
 * The Тлумачальны слоўнік (list from Aleh, 2026-11-15). Its grammar labels are mostly spelled out; it abbreviates
 * the sources of citations («Колас.», one per span), the languages words come from («[Ад грэч. ...]»), references
 * such as «(у 1 знач.)», and marks its layout with signs («//», «▪», «..» for a cut in a citation).
 */
object TsbmAbbreviations : AbbreviationScheme {
    private val sources by lazy { AbbreviationList(TSBM_SOURCES_1 + TSBM_SOURCES_2) }
    private val grammar by lazy { AbbreviationList(TSBM_GRAMMAR) }
    private val label by lazy { AbbreviationList(TSBM_GRAMMAR + TSBM_CASES + TSBM_SIGNS) }
    private val plain by lazy { AbbreviationList(TSBM_GRAMMAR + TSBM_SIGNS, without = LABEL_SIGNS + CUT) }
    private val quote by lazy { AbbreviationList(TSBM_SIGNS, without = LABEL_SIGNS) }

    override fun findIn(text: String, place: TextPlace): List<AbbreviationMatch> = when (place) {
        TextPlace.Label -> label.findIn(text)
        TextPlace.Plain -> plain.findIn(text)
        TextPlace.Quote -> quote.findIn(text)
        TextPlace.Source -> listOfNotNull(sourceIn(text))
    }

    /**
     * The whole source span: «Колас.» without its closing dot, a source spelled with the dot («Ядвігін Ш.»), or a
     * grammar abbreviation written with a capital («З нар.»).
     */
    private fun sourceIn(text: String): AbbreviationMatch? {
        val start = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return null
        val name = text.substring(start).trimEnd()
        val withoutDot = name.removeSuffix(".")
        val lowercase = name.replaceFirstChar { it.lowercase() }
        return sequenceOf(name to sources, withoutDot to sources, lowercase to grammar)
            .firstNotNullOfOrNull { (written, list) ->
                list.expansionOf(written)?.let {
                    AbbreviationMatch(start until start + written.length, name.take(written.length), it)
                }
            }
    }

    /** Only between label colours: «/» is also a plain slash. */
    private val LABEL_SIGNS = setOf("//", "/")

    /** Only in citations: elsewhere «..» is punctuation. */
    private val CUT = setOf("..")
}

/**
 * Abbreviations from a `abbreviation[, other form] | expansion` list. A space after a dot is optional when matching
 * («и т.п.» = «и т. п.»), and an abbreviation never starts or ends inside a word («ж.» is not found in «муж.»).
 * Longer abbreviations win («в знач. прил.» over «прил.»). Signs («//», «..») are not found inside a longer run of
 * the same sign or right after `?`, `!` or `…` («?..» is punctuation).
 */
class AbbreviationList(list: String, without: Set<String> = emptySet()) {
    private val byKey: Map<String, String> = list.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .flatMap { line ->
            val (keys, expansion) = line.split(" | ", limit = 2)
            keys.split(", ").map { normalize(it) to expansion.trim() }
        }
        .filterNot { (key, _) -> key in without }
        .toMap()

    private val pattern: Regex by lazy {
        Regex(byKey.keys.sortedByDescending { it.length }.joinToString("|", transform = ::patternOf))
    }

    fun expansionOf(abbreviation: String): String? = byKey[normalize(abbreviation)]

    fun findIn(text: String): List<AbbreviationMatch> = pattern.findAll(text).mapNotNull { match ->
        byKey[normalize(match.value)]?.let { AbbreviationMatch(match.range, match.value, it) }
    }.toList()
}

private fun patternOf(key: String): String = if (key.none(Char::isLetter)) {
    // A backslash before a sign that is not a letter always means the sign itself.
    "(?:(?<![\\${key.first()}?!…])${key.map { "\\$it" }.joinToString("")}(?!\\${key.last()}))"
} else {
    buildString {
        append("(?:")
        if (key.first().isLetter()) append("(?<![\\p{L}-])")
        key.forEachIndexed { index, char ->
            when (char) {
                ' ' -> append(SPACE).append('+')
                '.' -> {
                    append("\\.")
                    if (index < key.lastIndex) append(SPACE).append('*')
                }
                '-' -> append("\\-")
                else -> append(char)
            }
        }
        append("(?!\\p{L}))")
    }
}

/** No space after a dot, other spaces (also `&nbsp;`) as one space. */
private fun normalize(text: String): String = text.trim().replace(SPACE_AFTER_DOT, ".").replace(SPACES, " ")

private const val SPACE = "[ \\u00a0]"
private val SPACE_AFTER_DOT = Regex("\\.[ \\u00a0]+")
private val SPACES = Regex("[ \\u00a0]+")

/** `abbreviation[, other form] | expansion`, one per line. */
private const val RUSSIAN = """
        ав. | авиация
        авт. | автомобильное дело
        анат. | анатомия
        антр., антроп. | антропология
        археол. | археология
        архит. | архитектура
        астр. | астрономия
        бакт. | бактериология
        безл. | безличная форма
        биол. | биология
        бот. | ботаника
        бран. | бранное слово, выражение
        бухг. | бухгалтерский термин
        вводн. сл. | вводное слово
        в др. знач. | в других значениях
        вет., ветер. | ветеринария
        в знач. вводн. сл. | в значении вводного слова
        в знач. межд. | в значении междометия
        в знач. нареч. | в значении наречия
        в знач. повелит. накл. | в значении повелительного наклонения
        в знач. прил. | в значении прилагательного
        в знач. сказ. | в значении сказуемого
        в знач. сущ. | в значении существительного
        в знач. част. | в значении частицы
        вин. | винительный падеж
        воен. | военное дело
        возвр. | возвратный залог
        в разн. знач. | в разных значениях
        вульг. | вульгарное слово, выражение
        высок. | слово высокого стиля
        г. | город
        геогр. | география
        геод. | геодезия
        геол. | геология
        гидр. | гидрология
        глаг. | глагол
        горн. | горное дело
        г.п. | городской посёлок
        грам. | грамматика, грамматический термин
        груб. | грубое слово, выражение
        дат. | дательный падеж
        деепр., дееприч. | деепричастие
        действ. | действительный залог
        детск. | детская речь
        дипл. | дипломатия
        доп. | дополнение
        дорев. | дореволюционное слово, выражение
        ед. | единственное число
        ж. | женский род
        ж.-д. | железнодорожный термин
        жив. | живопись
        зоол. | зоология
        и пр. | и прочее
        ирон. | в ироническом смысле
        иск. | искусство
        ист. | исторический термин
        и т. д. | и так далее
        и т. п. | и тому подобное
        канц. | канцелярское слово, выражение
        карт. | карточный термин
        кино | кинематография
        книж., книжн. | книжное слово, выражение
        комп. | компьютерные технологии
        кратк. ф. | краткая форма
        кул. | кулинария
        -л. | либо
        ласк. | ласкательная форма
        лес. | лесоводство
        лингв. | лингвистика
        лит. | литературоведение
        лог. | логика
        м. | мужской род
        мат. | математика
        мед. | медицина
        межд. | междометие
        мест. | местоимение
        мет. | металлургия
        метеор. | метеорология
        мех. | механика
        мин. | минералогия
        миф. | мифология
        мн. | множественное число
        мног. | о многом, о многих (лицах, предметах, явлениях)
        многокр. | многократный вид
        мор. | морской термин
        муз. | музыка
        напр. | например
        нареч. | наречие
        нар.-поэт. | народнопоэтическое слово, выражение
        неизм. | неизменяемое
        неодобр. | неодобрительно
        неоконч. | неоконченное (действие)
        нескл. | несклоняемое
        несов. | несовершенный вид
        о. | остров
        обл. | областное слово, выражение
        о-ва | острова
        однокр. | однократный вид
        оз. | озеро
        оконч. д. | оконченное действие
        опт. | оптика
        офиц. | официальное слово, выражение
        охот., охотн. | охотничий термин
        палеонт. | палеонтология
        п-в | полуостров
        пед. | педагогика
        перен. | переносное значение
        плот., плотн. | плотничье дело
        повелит., повелит. накл. | повелительное наклонение
        погов. | поговорка
        полигр. | полиграфия
        полит. | политический термин
        портн. | портняжное дело
        пос. | посёлок
        посл. | пословица
        поэз. | в поэзии, поэтическое слово, выражение
        поэт. | поэтика
        пр. | прочее
        превосх. ст. | превосходная степень
        пред., предл. | предложный падеж
        презр. | презрительное слово, выражение
        пренебр. | пренебрежительное слово, выражение
        прил. | имя прилагательное
        прич. | причастие
        прост. | просторечное слово, выражение
        прям. | прямое значение
        психол. | психология
        р. | река
        радио | радиотехника
        разг. | разговорное слово, выражение
        рел. | религия
        род. | родительный падеж
        рыб. | рыболовство, рыбоводство
        с. | село
        сад. | садоводство
        сапожн. | сапожное дело
        слес. | слесарное дело
        см. | смотри
        собир. | собирательное
        сов. | совершенный вид
        спец. | специальный термин
        спорт. | спортивный термин
        ср. | средний род
        сравн. ст., сравнит. ст. | сравнительная степень
        средн. | средний залог
        стат. | статистика
        стол. | столярное дело
        стр. | строительное дело
        страд. | страдательный залог
        сущ. | имя существительное
        с.-х. | сельское хозяйство
        твор. | творительный падеж
        театр. | театральный термин
        текст. | текстильный термин
        тел., телегр. | телеграфия
        телеф., телефон. | телефония
        тех., техн. | техника
        тип., типогр. | типографский термин
        торг. | торговый термин
        увелич. | увеличительная форма
        уменьш. | уменьшительная форма
        уменьш.-ласк. | уменьшительно-ласкательная форма
        унич., уничиж. | уничижительное
        уст., устар. | устаревшее слово, выражение
        фам. | фамильярное слово, выражение
        фарм. | фармакология, фармацевтика
        физ. | физика
        физиол. | физиология
        филол. | филология
        филос. | философия
        фин. | финансовый термин
        фольк. | фольклор
        фото | фотография
        хим. | химия
        церк. | церковное слово, выражение
        част. | частица
        числ., числит. | числительное
        шахм. | шахматный термин
        школ., школьн. | школьное слово, выражение
        шутл. | шутливое слово, выражение
        эвф. | эвфемистическое слово, выражение
        эк. | экономика
        эл. | электротехника
        этн., этногр. | этнография
        этнол. | этнология
        юр. | юридический термин
        жен. | женский род
        муж. | мужской род
        совер. | совершенный вид
        несовер. | несовершенный вид
        в знач. | в значении
        в перен. знач. | в переносном значении
        т. е. | то есть
        неопр. | неопределённая форма
        уменьш.-уничиж. | уменьшительно-уничижительная форма
"""
