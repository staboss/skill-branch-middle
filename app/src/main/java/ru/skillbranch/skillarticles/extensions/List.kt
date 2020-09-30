package ru.skillbranch.skillarticles.extensions

fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
    val result = mutableListOf<List<Pair<Int, Int>>>()
    bounds.forEach { (lBound, rBound) ->
        run {
            val insideBounds = filter { (_lBound: Int, _rBound: Int) ->
                _lBound >= lBound && _rBound <= rBound
            }
            result.add(insideBounds)
        }
    }
    return result
}