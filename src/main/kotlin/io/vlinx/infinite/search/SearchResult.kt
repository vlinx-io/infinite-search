package io.vlinx.infinite.search


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/27
 */

class SearchResult(val title: String, val link: String, val snippet: String) {



    override fun toString(): String {
        return "SearchResult(title='$title', link='$link', snippet='$snippet')"
    }

}