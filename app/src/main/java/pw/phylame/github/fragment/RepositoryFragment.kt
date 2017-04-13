package pw.phylame.github.fragment

import android.annotation.SuppressLint
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
import org.json.JSONObject
import pw.phylame.github.App
import pw.phylame.github.GitHub
import pw.phylame.github.R
import pw.phylame.github.Repository
import pw.phylame.github.databinding.RepoListItemBinding
import pw.phylame.support.getStyledColor
import pw.phylame.support.tintDrawables
import java.text.SimpleDateFormat
import java.util.*

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
                loadRepositories()
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
        GitHub.getRepository("JakeWharton")
                .subscribe({ repos ->
                    adapter.reset(repos)
                    content.swipe.isRefreshing = false
                    content.placeholder.visibility = if (repos.isEmpty()) View.VISIBLE else View.GONE
                }, { error ->
                    error.printStackTrace()
                    Toast.makeText(context, "Connection Error!", Toast.LENGTH_SHORT).show()
                })
    }
}

private class LanguageItem(val alias: String?, val color: Int?)

private class RepositoryHolder(val binding: RepoListItemBinding) : RecyclerView.ViewHolder(binding.root)

private class RepositoryAdapter(val accentColor: Int, val username: String) : RecyclerView.Adapter<RepositoryHolder>() {
    private val styles by lazy {
        val m = HashMap<String, LanguageItem>()
        App.openAssets("languages.json")
                .reader(Charsets.US_ASCII)
                .use {
                    val jsonArray = JSONArray(it.readText())
                    for (i in 0..jsonArray.length() - 1) {
                        val obj = jsonArray.get(i) as JSONObject
                        val spec = obj.optString("color", null)
                        m[obj.getString("name")] =
                                LanguageItem(obj.optString("short_name", null), if (spec.isNullOrEmpty()) null else Color.parseColor(spec))
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

        if (repo.pushTime != null) {
            view.update_info.visibility = View.VISIBLE
            view.update_info.text = dateDiff(repo.pushTime!!)
        } else {
            view.update_info.visibility = View.GONE
        }

        val style = styles[repo.language]
        view.label.setBackgroundColor(style?.color ?: accentColor)
        view.label.text = style?.alias ?: repo.language ?: "N/A"
    }

    fun dateDiff(date: Date): String {
        val now = Calendar.getInstance()
        val spec = Calendar.getInstance()
        spec.time = date

        val v = now.timeInMillis - date.time
        println("$v, ${v / 1000.0/60/24}")

        var diff = now[Calendar.YEAR] - spec[Calendar.YEAR]
        if (diff > 0) {
            return App.getString(R.string.update_info_on, formatDate(date, "yyyy-M-d"))
        }
        diff = now[Calendar.MONTH] - spec[Calendar.MONTH]
        if (diff > 0) {
            return App.getString(R.string.update_info_on, formatDate(date, "M-d"))
        }
        diff = now[Calendar.DAY_OF_MONTH] - spec[Calendar.DAY_OF_MONTH]
        if (diff > 0) {
            return App.getString(R.string.update_info_day_ago, diff)
        }
        diff = now[Calendar.HOUR_OF_DAY] - spec[Calendar.HOUR_OF_DAY]
        if (diff > 0) {
            return App.getString(R.string.update_info_hour_ago, diff)
        }
        diff = now[Calendar.MINUTE] - spec[Calendar.MINUTE]
        if (diff > 0) {
            return App.getString(R.string.update_info_minute_ago, diff)
        }
        diff = now[Calendar.SECOND] - spec[Calendar.SECOND]
        return App.getString(R.string.update_info_second_ago, diff)
    }

    @SuppressLint("SimpleDateFormat")
    fun formatDate(date: Date, pattern: String): String = SimpleDateFormat(pattern).format(date)
}