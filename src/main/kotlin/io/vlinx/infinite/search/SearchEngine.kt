package io.vlinx.infinite.search


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/27
 */

interface SearchEngine {
    // 输入关键字，返回搜索结果的摘要
    fun search(keyword: String): List<SearchResult>
}