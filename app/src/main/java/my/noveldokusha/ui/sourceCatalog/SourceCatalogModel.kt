package my.noveldokusha.ui.sourceCatalog

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.scraper.source_interface
import my.noveldokusha.ui.BaseViewModel

class SourceCatalogModel(val source: source_interface.catalog) : BaseViewModel()
{
	val fetchIterator = FetchIterator(viewModelScope) { source.getCatalogList(it).transform() }
	
	init
	{
		startCatalogListMode()
	}
	
	private fun Response<List<BookMetadata>>.transform() = when (val res = this)
	{
		is Response.Error -> Response.Error(res.message)
		is Response.Success -> Response.Success(res.data.map(::CatalogItem))
	}
	
	data class CatalogItem(val bookMetadata: BookMetadata)
	{
		val isInLibraryLiveData = bookstore.bookLibrary.existInLibraryFlow(bookMetadata.url).asLiveData()
	}
	
	fun startCatalogListMode()
	{
		fetchIterator.setFunction { source.getCatalogList(it).transform() }
		fetchIterator.reset()
		fetchIterator.fetchNext()
	}
	
	fun startCatalogSearchMode(input: String)
	{
		fetchIterator.setFunction { source.getCatalogSearch(it, input).transform() }
		fetchIterator.reset()
		fetchIterator.fetchNext()
	}
}