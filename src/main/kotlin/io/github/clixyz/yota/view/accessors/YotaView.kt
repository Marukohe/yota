package io.github.clixyz.yota.view.accessors

import io.github.clixyz.yota.view.YotaView
import java.util.LinkedList

//
// tree operations
//

typealias YotaViewVisitor = (view: YotaView, meta: YotaViewMeta) -> Unit

enum class YotaViewOrder {
    DFS, BFS
}

data class YotaViewMeta(
    var depth: Int
)

fun YotaView.accept(ord: YotaViewOrder, vis: YotaViewVisitor) {
    return when (ord) {
        YotaViewOrder.DFS -> dfs(vis)
        else -> bfs(vis)
    }
}

private fun YotaView.dfs(vis: YotaViewVisitor) = doDfs(vis, YotaViewMeta(depth = 0))

private fun YotaView.doDfs(vis: YotaViewVisitor, meta: YotaViewMeta) {
    vis(this, meta)
    children.forEach {
        it.doDfs(vis, meta.copy(depth = meta.depth + 1))
    }
}

private fun YotaView.bfs(vis: YotaViewVisitor) {
    val q = LinkedList<Pair<YotaView, YotaViewMeta>>()
    q.offer(Pair(this, YotaViewMeta(depth = 1)))

    while (q.isNotEmpty()) {
        val (v, m) = q.poll()
        children.forEach {
            q.offer(Pair(it, m.copy(depth = m.depth + 1)))
        }
        vis(v, m)
    }
}

//
// Specific visitors
//

// ViewCollector to collect views satisfying some conditions
class YotaViewFilter(val cond: (view: YotaView) -> Boolean)
    : ArrayList<YotaView>(), YotaViewVisitor {

    override fun invoke(view: YotaView, meta: YotaViewMeta) {
        if (cond.invoke(view)) {
            add(view)
        }
    }
}
