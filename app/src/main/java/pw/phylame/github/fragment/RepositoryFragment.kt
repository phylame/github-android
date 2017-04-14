package pw.phylame.github.fragment

import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_repo.view.*
import kotlinx.android.synthetic.main.repo_list_item.view.*
import org.json.JSONArray
import pw.phylame.github.*
import pw.phylame.github.databinding.RepoListItemBinding
import pw.phylame.support.getStyledColor
import pw.phylame.support.iterator
import pw.phylame.support.tintDrawables
import rx.Observer

class RepositoryFragment : Fragment() {
    private var isLoaded = false

    private val username: String by lazy {
        arguments.getString("username", "")
    }

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
        val view = inflater.inflate(R.layout.fragment_repo, container, false)
        if (view != null) {
            view.swipe.setOnRefreshListener {
                // todo refresh repos
            }
            initRecycler(view.recycler)
        }
        return view
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
                    outRect.top += space / 2
                }
                if (position == adapter.size - 1) {
                    outRect.bottom += space / 2
                } else {
                    outRect.bottom += space
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isLoaded) {
            view.swipe.isRefreshing = true
            loadRepositories()
            isLoaded = true
        }
    }

    fun loadRepositories() {
        val content = view!!
        val params = RepoParams()
        GitHub.getRepository("JakeWharton", params)
                .subscribe(object : Observer<Repository> {
                    override fun onNext(repo: Repository) {
                        adapter.append(repo)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        Toast.makeText(context, R.string.error_network_connection, Toast.LENGTH_SHORT).show()
                    }

                    override fun onCompleted() {
                        content.swipe.isRefreshing = false
                        content.placeholder.visibility = if (adapter.size == 0) View.VISIBLE else View.GONE
                    }
                })
    }
}

private class LanguageItem(val alias: String?, val color: Int?)

private class RepositoryHolder(val binding: RepoListItemBinding) : RecyclerView.ViewHolder(binding.root)

private class RepositoryAdapter(val accentColor: Int, val username: String) : RecyclerView.Adapter<RepositoryHolder>() {
    private val styles by lazy {
        val m = HashMap<String, LanguageItem>()
        app.assets.open("languages.json")
                .reader(Charsets.US_ASCII)
                .use {
                    for (obj in JSONArray(it.readText())) {
                        val color = obj.optString("color", null)
                        m[obj.getString("name")] =
                                LanguageItem(obj.optString("short_name", null), if (color.isNullOrEmpty()) null else Color.parseColor(color))
                    }
                }
        m
    }

    private val repos = ArrayList<Repository>()

    val size get() = repos.size

    fun append(repo: Repository) {
        repos += repo
        notifyItemInserted(repos.size - 1)
    }

    fun extend(repos: Collection<Repository>) {
        val begin = this.repos.size
        this.repos += repos
        notifyItemRangeInserted(begin, repos.size)
    }

    fun reset(repos: Collection<Repository>) {
        this.repos.clear()
        this.repos += repos
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = repos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_list_item, parent, false)
        return RepositoryHolder(DataBindingUtil.bind(view))
    }

    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {
        val repo = repos[position]
        holder.binding.repo = repo
        val view = holder.binding.root

        view.name.text = if (repo.username != username) repo.fullName else repo.name
        view.star_count.tintDrawables()
        view.forked_count.tintDrawables()

        val style = styles[repo.language]
        view.label.setBackgroundColor(style?.color ?: accentColor)
        view.label.text = style?.alias ?: repo.language ?: "N/A"
    }
}