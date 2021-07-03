package my.noveldokusha

import my.noveldokusha.scraper.scrubber
import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

private val NodeList.elements get() = (0..length).asSequence().mapNotNull { item(it) as? Element }
private val Node.childElements get() = childNodes.elements
private fun Document.selectFirstTag(tag: String) = getElementsByTagName(tag).item(0)
private fun Node.selectFirstChildTag(tag: String) = childElements.find { it.tagName == tag }
private fun Node.selectChildTag(tag: String) = childElements.filter { it.tagName == tag }
private fun parseXMLFile(inputSteam: InputStream) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSteam)
private fun parseXMLFile(byteArray: ByteArray) = parseXMLFile(byteArray.inputStream())
private data class EpubManifestItem(val id: String, val href: String, val mediaType: String)

data class EpubChapter(val url: String, val title: String, val body: String)
data class EpubBook(
	val url: String,
	val title: String,
	val chapters: List<EpubChapter>
)

private fun ZipInputStream.entries() = generateSequence { nextEntry }

fun epubImport(inputSteam: InputStream): EpubBook
{
	val zipFile = ZipInputStream(inputSteam).use { zipInputStream ->
		zipInputStream
			.entries()
			.filterNot { it.isDirectory }
			.associate { it.name to (it to zipInputStream.readBytes()) }
	}
	
	val opfEntry = zipFile.keys.find { it.endsWith(".opf") } ?: throw Exception(".opf file missing")
	val opfFile = zipFile[opfEntry]!!
	
	val docuemnt = parseXMLFile(opfFile.second)
	val metadata = docuemnt.selectFirstTag("metadata") ?: throw Exception(".opf file metadata section missing")
	val manifest = docuemnt.selectFirstTag("manifest") ?: throw Exception(".opf file manifest section missing")
	val spine = docuemnt.selectFirstTag("spine") ?: throw Exception(".opf file spine section missing")
	
	val title = metadata.selectFirstChildTag("dc:title")?.textContent ?: throw Exception(".opf metadata title tag missing")
	//	val language = metadata.selectFirstChildTag("dc:language")?.textContent ?: throw Exception(".opf metadata language tag missing")
	//	val identifier = metadata.selectFirstChildTag("dc:identifier")?.textContent ?: throw Exception(".opf metadata identifier tag missing")
	val bookUrl = "local://$title"
	
	val items = manifest.selectChildTag("item").map {
		EpubManifestItem(
			id = it.getAttribute("id"),
			href = it.getAttribute("href"),
			mediaType = it.getAttribute("media-type")
		)
	}.associateBy { it.id }
	
	val idRef = spine.selectChildTag("itemref").map { it.getAttribute("idref") }
	
	data class TempEpubChapter(val url: String, val title: String?, val body: String, val chapterIndex: Int)
	
	var chapterIndex = 0
	val chapters = idRef
		.mapNotNull { items.get(it) }
		.filter { it.href.endsWith(".xhtml") }
		.mapNotNull { zipFile["OEBPS/${it.href}"] }
		.mapIndexedNotNull { index, (entry, byteArray) ->
			val doc = Jsoup.parse(byteArray.inputStream(), "UTF-8", "")
			val body = doc.body()
			// A full chapter usually is split in multiple sequential entries,
			// try to merge them and extract the main title of each one.
			// Is is not perfect but better than dealing with a table of contents
			val chapterTitle = body.selectFirst("h1, h2, h3, h4, h5, h6")?.text() ?: if (index == 0) title else null
			body.selectFirst("h1, h2, h3, h4, h5, h6")?.remove()
			val text = scrubber.getNodeStructuredText(body)
			if (chapterTitle != null)
				chapterIndex += 1
			
			TempEpubChapter(
				url = "$bookUrl/${entry.name}",
				title = chapterTitle,
				body = text,
				chapterIndex = chapterIndex,
			)
		}.groupBy {
			it.chapterIndex
		}.map { (_, list) ->
			EpubChapter(
				url = list.first().url,
				title = list.first().title!!,
				body = list.joinToString("\n\n") { it.body }
			)
		}.filter {
			it.body.isNotBlank()
		}
	
	return EpubBook(url = bookUrl, title = title, chapters = chapters.toList())
}