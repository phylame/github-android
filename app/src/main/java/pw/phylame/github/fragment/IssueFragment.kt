package pw.phylame.github.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import pw.phylame.github.R

class IssueFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity.setTitle(R.string.issues)
    }
}