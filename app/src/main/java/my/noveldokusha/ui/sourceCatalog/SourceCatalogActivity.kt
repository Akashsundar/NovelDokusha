package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivitySourceCatalogBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.sourceCatalog.SourceCatalogModel.CatalogItem
import my.noveldokusha.uiAdapters.ProgressBarAdapter
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.addBottomMargin
import my.noveldokusha.uiUtils.inflater
import my.noveldokusha.uiUtils.switchLiveData
import java.util.*
import kotlin.properties.Delegates

class SourceCatalogActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var sourceBaseUrl by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, sourceBaseUrl: String) : super(ctx, SourceCatalogActivity::class.java)
		{
			this.sourceBaseUrl = sourceBaseUrl
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<SourceCatalogModel>()
	private val viewHolder by lazy { ActivitySourceCatalogBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { BooksItemAdapter(viewModel.list) }
		val progressBar by lazy { ProgressBarAdapter() }
	}
	
	private val viewLayoutManager = object
	{
		val recyclerView by lazy { LinearLayoutManager(this@SourceCatalogActivity) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(scrubber.getCompatibleSourceCatalog(extras.sourceBaseUrl)!!)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.booksFetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				pos >= viewModel.list.size - 3
			}
		}
		
		viewModel.booksFetchIterator.onSuccess.observe(this) {
			viewAdapter.recyclerView.addAll(it.data.map(::CatalogItem))
		}
		viewModel.booksFetchIterator.onError.observe(this) {
			viewHolder.errorMessage.visibility = View.VISIBLE
			viewHolder.errorMessage.text = it.message
		}
		viewModel.booksFetchIterator.onCompletedEmpty.observe(this) {
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.booksFetchIterator.onFetching.observe(this) {
			viewAdapter.progressBar.visible = it
		}
		viewModel.booksFetchIterator.onReset.observe(this) {
			viewHolder.errorMessage.visibility = View.GONE
			viewHolder.noResultsMessage.visibility = View.GONE
			viewAdapter.recyclerView.notifyDataSetChanged()
		}
		
		supportActionBar!!.let {
			it.title = "Source"
			it.subtitle = viewModel.source.name.capitalize(Locale.ROOT)
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.source_catalog_menu__appbar, menu)
		
		val searchViewItem = menu!!.findItem(R.id.action_search)
		val searchView = searchViewItem.actionView as SearchView
		
		searchViewItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener
		{
			override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true
			
			override fun onMenuItemActionCollapse(item: MenuItem?): Boolean
			{
				viewModel.startCatalogListMode()
				return true
			}
		})
		
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				query?.let { viewModel.startCatalogSearchMode(it) }
				return true
			}
			
			override fun onQueryTextChange(newText: String?): Boolean = true
		})
		
		return true
	}
	
	inner class BooksItemAdapter(private val list: MutableList<CatalogItem>) : RecyclerView.Adapter<BooksItemAdapter.ViewBinder>()
	{
		fun addAll(newItems: List<CatalogItem>)
		{
			val size = list.size
			list.addAll(newItems)
			notifyItemRangeInserted(size, newItems.size)
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
			ViewBinder(BookListItemBinding.inflate(parent.inflater, parent, false))
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			binder.itemData = itemData
			
			itemView.title.text = itemData.bookMetadata.title
			itemView.title.setOnClickListener {
				ChaptersActivity.IntentData(
					this@SourceCatalogActivity,
					bookMetadata = itemData.bookMetadata
				).let(this@SourceCatalogActivity::startActivity)
			}
			itemView.title.setOnLongClickListener {
				lifecycleScope.launch(Dispatchers.IO) { bookstore.bookLibrary.toggleBookmark(itemData.bookMetadata) }
				true
			}
			
			binder.addBottomMargin { position == list.lastIndex }
		}
		
		inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			var itemData: CatalogItem? by Delegates.observable(null) { _, oldValue, newValue ->
				isInLibraryObserver.switchLiveData(oldValue, newValue, this@SourceCatalogActivity) { isInLibraryLiveData }
			}
			
			private val unselectedTextColor = viewHolder.title.currentTextColor
			private val selectedTextColor by lazy { ContextCompat.getColor(this@SourceCatalogActivity, R.color.dark_green) }
			private val isInLibraryObserver = Observer<Boolean> { isInLibrary ->
				viewHolder.title.setTextColor(if (isInLibrary) selectedTextColor else unselectedTextColor)
			}
		}
	}
}