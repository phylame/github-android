package pw.phylame.github

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmObject
import io.realm.Sort
import io.realm.annotations.PrimaryKey
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

open class User : RealmObject() {
    @PrimaryKey
    var id: Int = 0

    var type: String? = null

    @SerializedName("avatar_url")
    var avatar: String? = null

    @SerializedName("login")
    var username: String? = null

    var bio: String? = null

    var name: String? = null

    var blog: String? = null

    var email: String? = null

    var company: String? = null

    var location: String? = null

    @SerializedName("created_at")
    var createTime: Date? = null

    @SerializedName("updated_at")
    var updateTime: Date? = null

    var stars: Int = 0

    var following: Int = 0

    var followers: Int = 0

    @SerializedName("disk_usage")
    var diskUsage: Int = 0

    @SerializedName("public_repos")
    var publicRepos: Int = 0

    @SerializedName("total_private_repos")
    var totalPrivateRepos: Int = 0

    @SerializedName("owned_private_repos")
    var ownedPrivateRepos: Int = 0

    @SerializedName("public_gists")
    var publicGists: Int = 0

    @SerializedName("private_gists")
    var privateGists: Int = 0
}

open class Repository : RealmObject() {
    @PrimaryKey
    var id: Int = 0

    var name: String? = null

    var language: String? = null

    var homepage: String? = null

    var description: String? = null

    @SerializedName("owner")
    var owner: User? = null

    @SerializedName("full_name")
    var fullName: String? = null

    @SerializedName("fork")
    var isFork: Boolean = false

    @SerializedName("private")
    var isPrivate: Boolean = false

    @SerializedName("pushed_at")
    var pushTime: Date? = null

    @SerializedName("created_at")
    var createTime: Date? = null

    @SerializedName("updated_at")
    var updateTime: Date? = null

    @SerializedName("forks_count")
    var forks: Int = 0

    @SerializedName("stargazers_count")
    var stargazers: Int = 0

    @SerializedName("subscribers_count")
    var subscribers: Int = 0
}

open class Issue : RealmObject() {
    @PrimaryKey
    var id: Int = 0

    var number: Int = 0

    var title: String? = null

    var state: String? = null

    @SerializedName("locked")
    var isLocked: Boolean = false

    var body: String? = null

    var comments: Int = 0

    @SerializedName("created_at")
    var createTime: Date? = null

    @SerializedName("updated_at")
    var updateTime: Date? = null

    @SerializedName("closed_at")
    var closeTime: Date? = null
}

open class PageParams(var offset: Int = 1, var limit: Int = 16) {
    open fun toMap(): MutableMap<String, String> = mutableMapOf(
            "page" to offset.toString(),
            "per_page" to limit.toString()
    )
}

class RepoParams(var visibility: String = "",
                 var affiliation: String = "",
                 var type: String = "",
                 var sort: String = "",
                 var direction: String = "") : PageParams(0, 0) {
    override fun toMap(): MutableMap<String, String> {
        val map = super.toMap()
        if (visibility.isNotEmpty()) {
            map["visibility"] = visibility
        }
        if (affiliation.isNotEmpty()) {
            map["affiliation"] = affiliation
        }
        if (type.isNotEmpty()) {
            map["type"] = type
        }
        if (sort.isNotEmpty()) {
            map["sort"] = sort
        }
        if (direction.isNotEmpty()) {
            map["direction"] = direction
        }
        return map
    }
}

private interface GitHubService {
    @GET("/user")
    fun login(): Observable<User>

    @GET("/users/{username}")
    fun getUser(@Path("username") username: String): Observable<User>

    @GET("/users/{username}/repos")
    fun getRepository(@Path("username") username: String, @QueryMap params: Map<String, String>?): Observable<List<Repository>>
}

object GitHub {
    private const val BASE_URL = "https://api.github.com/"

    private val userConfig by lazy {
        RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .directory(app.cacheDir)
                .name("users.realm")
                .build()
    }

    private val repoConfig by lazy {
        RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .directory(app.cacheDir)
                .name("repos.realm")
                .build()
    }

    private val realmGson: Gson by lazy {
        GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

            override fun shouldSkipField(f: FieldAttributes): Boolean = f.declaringClass === RealmObject::class.java
        }).create()
    }

    private val userCache: Realm get() = Realm.getInstance(userConfig)

    private val repoCache: Realm get() = Realm.getInstance(repoConfig)

    private var authToken: String? = app.githubPreferences.getString("authToken", null)

    private lateinit var apiService: GitHubService

    val isSignedIn get() = !app.githubPreferences.getString("authToken", null).isNullOrEmpty()

    init {
        if (!authToken.isNullOrEmpty()) { // already signed in
            apiService = createService(authToken!!)
        }
    }

    private fun cacheUser(user: User) {
        val realm = userCache
        realm.beginTransaction()
        realm.insertOrUpdate(user)
        realm.commitTransaction()
    }

    private fun cacheRepository(repo: Repository) {
        val realm = repoCache
        realm.beginTransaction()
        realm.insertOrUpdate(repo)
        realm.commitTransaction()
    }

    private fun createService(token: String): GitHubService = Retrofit.Builder()
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create(realmGson))
            .client(OkHttpClient.Builder().addInterceptor {
                it.proceed(it.request().newBuilder().header("Authorization", token).build())
            }.build())
            .baseUrl(BASE_URL)
            .build()
            .create(GitHubService::class.java)

    fun login(username: String, password: String): Observable<User> {
        if (!authToken.isNullOrEmpty()) {
            throw IllegalStateException("Already signed in")
        }
        return Observable.create<String> {
            it.onNext(Credentials.basic(username, password))
            it.onCompleted()
        }.subscribeOn(Schedulers.computation())
                .flatMap { token ->
                    val service = createService(token)
                    service.login()
                            .doOnNext { user ->
                                if (user != null) {
                                    cacheUser(user)
                                    apiService = service
                                    app.githubPreferences
                                            .edit()
                                            .putString("authToken", token)
                                            .putString("username", user.username)
                                            .commit()
                                }
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun fetchUser(username: String): Observable<User> = apiService.getUser(username)
            .doOnNext(this::cacheUser)

    fun getUser(username: String): Observable<User> {
        val user = userCache.where(User::class.java)
                .equalTo("username", username)
                .findFirst()
        return if (user != null) {
            Observable.just(user)
        } else {
            fetchUser(username)
        }
    }

    fun fetchRepository(username: String, params: RepoParams?): Observable<List<Repository>> {
        return apiService.getRepository(username, params?.toMap())
                .doOnNext { repos ->
                    for (repo in repos) {
                        cacheRepository(repo)
                    }
                }
    }

    fun getRepository(username: String, params: RepoParams?): Observable<out List<Repository>> {
        val sort = when (params?.sort) {
            "created" -> "createTime"
            "updated" -> "updateTime"
            "pushed" -> "pushTime"
            "full_name" -> "fullName"
            else -> null
        }
        val order = when (params?.direction) {
            "", null -> null
            "asc" -> Sort.ASCENDING
            "desc" -> Sort.DESCENDING
            else -> throw IllegalArgumentException("direction must be 'asc' or 'desc'")
        }
        val query = repoCache.where(Repository::class.java).equalTo("owner.username", username)
        val repos = if (sort != null) {
            if (order != null) {
                query.findAllSorted(sort, order)
            } else {
                query.findAllSorted(sort)
            }
        } else {
            query.findAll()
        }
        return if (repos.isNotEmpty()) {
            Observable.just(repos)
        } else {
            fetchRepository(username, params)
        }
    }
}