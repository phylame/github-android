package pw.phylame.github.fragment

import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_repo.view.*
import kotlinx.android.synthetic.main.repo_list_item.view.*
import org.json.JSONArray
import pw.phylame.github.*
import pw.phylame.github.databinding.RepoListItemBinding
import pw.phylame.support.dip
import pw.phylame.support.getStyledColor
import pw.phylame.support.iterator
import pw.phylame.support.tintDrawables
import rx.android.schedulers.AndroidSchedulers

class RepositoryFragment : Fragment() {
    companion object {
        const val PAGE_SIZE = 16
        const val TAG = "RepositoryFragment"
    }

    private var isLoaded = false

    private val params = RepoParams()

    private val username: String by lazy { arguments.getString("username", "") }

    private val adapter: RepositoryAdapter by lazy {
        RepositoryAdapter(context.getStyledColor(R.attr.colorAccent), username)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.setTitle(R.string.repositories)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_repo, menu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_repo, container, false)
        if (root != null) {
            root.swipe.setOnRefreshListener {
                refreshRepositories()
            }
            initRecycler(root.recycler)
        }
        return root
    }

    fun initRecycler(recycler: RecyclerView) {
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(context)
        val space = context.resources.getDimensionPixelSize(R.dimen.repo_item_space)
        recycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
                super.getItemOffsets(outRect, view, parent, state)
                val position = parent.findContainingViewHolder(view)!!.adapterPosition
                if (position == 0) {
                    outRect.top += space
                }
                outRect.bottom += space
            }
        })
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recycler: RecyclerView, newState: Int) {

            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isLoaded) {
            view.swipe.isRefreshing = true
            params.limit = PAGE_SIZE
            params.offset = 1
            loadRepositories()
            isLoaded = true
        }
    }

    fun loadRepositories() {
        val root = view!!
        Log.d(TAG, "${params.offset}, ${params.limit}")
        GitHub.getRepository(username, params)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ repos ->
                    adapter.extend(repos)
                    root.swipe.isRefreshing = false
                    root.placeholder.visibility = if (adapter.size == 0) View.VISIBLE else View.GONE
                }, { error ->
                    root.swipe.isRefreshing = false
                    Log.e(TAG, "load repositories error", error)
                    Toast.makeText(context, R.string.error_network_connection, Toast.LENGTH_SHORT).show()
                })
    }

    fun refreshRepositories() {
        val root = view!!
        params.offset = 1
        GitHub.fetchRepository(username, params)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate {
                    root.swipe.isRefreshing = false
                }
                .subscribe({ repos ->
                    adapter.reset(repos)
                }, { error ->
                    Log.e(TAG, "refresh repositories error", error)
                    Toast.makeText(context, R.string.error_network_connection, Toast.LENGTH_SHORT).show()
                })
    }
}

private typealias ViewHolder = RecyclerView.ViewHolder

private class FooterHolder(view: View) : ViewHolder(view) {
    val text = view.findViewById(R.id.text) as TextView
}

private class RepositoryHolder(val binding: RepoListItemBinding) : ViewHolder(binding.root)

private class LanguageStyle(val alias: String?, val color: Int?)

private class RepositoryAdapter(val accentColor: Int, val username: String) : RecyclerView.Adapter<ViewHolder>() {
    private val styles by lazy {
        val map = HashMap<String, LanguageStyle>()
        app.assets.open("languages.json")
                .reader(Charsets.US_ASCII)
                .use {
                    for (obj in JSONArray(it.readText())) {
                        val spec = obj.optString("color", null)
                        val color = if (spec.isNullOrEmpty()) null else Color.parseColor(spec)
                        map[obj.getString("name")] = LanguageStyle(obj.optString("short_name", null), color)
                    }
                }
        map
    }

    private val repos = ArrayList<Repository>()

    var isRefreshing = false

    val size get() = repos.size

    fun reset(repos: Collection<Repository>) {
        this.repos.clear()
        this.repos += repos
        notifyDataSetChanged()
    }

    fun extend(repos: Collection<Repository>) {
        val begin = size
        this.repos += repos
        notifyItemRangeInserted(begin, repos.size)
    }

    override fun getItemCount(): Int = repos.size

    override fun getItemViewType(position: Int): Int = if (position == size) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = if (viewType == 0) {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_list_item, parent, false)
        RepositoryHolder(DataBindingUtil.bind(view))
    } else {
        FooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.refresh_footer, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is RepositoryHolder) {
            val repo = repos[position]
            holder.binding.repo = repo
            val view = holder.binding.root

            view.name.text = if (repo.owner?.username != username) repo.fullName else repo.name
            view.star_count.tintDrawables()
            view.forked_count.tintDrawables()

            val size = view.context.dip(24F).toInt()
            val avatar = repo.owner?.avatar
            if (!avatar.isNullOrEmpty()) {
                Glide.with(view.context)
                        .load(avatar)
                        .crossFade()
                        .override(size, size)
                        .into(view.author_avatar)
            } else {

            }

            val style = styles[repo.language]
            view.label.setBackgroundColor(style?.color ?: accentColor)
            view.label.text = style?.alias ?: repo.language ?: "N/A"
        } else {
            println(isRefreshing)
            holder.itemView.visibility = if (isRefreshing) View.VISIBLE else View.GONE
        }
    }
}