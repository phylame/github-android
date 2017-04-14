package pw.phylame.support

import org.json.JSONArray
import org.json.JSONObject

operator fun JSONArray.iterator(): Iterator<JSONObject> = JSONArrayIterator(this)

private class JSONArrayIterator(val array: JSONArray) : Iterator<JSONObject> {
    var index = 0

    override fun hasNext(): Boolean = index != array.length()

    override fun next(): JSONObject = if (hasNext()) {
        array.getJSONObject(index++)
    } else {
        throw NoSuchElementException()
    }
}