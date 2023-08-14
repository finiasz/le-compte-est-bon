package net.finiasz.lecompte

data class GameState(
    val chiffres : MutableList<Int?> = MutableList(11) { null },
    val usedChiffres : MutableList<Boolean> = MutableList(11) { false },
    val selected : MutableList<Int?> = MutableList(10) { null },
    val selectedOps : MutableList<String?> = MutableList(5) { null },
    val solution : MutableList<Step?> =  MutableList(5) { null },
    var solutionBest : Int? = null,
    var target : Int = 0,
    var won : Won = Won.NOT_WON,
    var wonPos : Int? = null,
    val alwaysSolution : Boolean,
) {
    val opEnabled: Boolean
        get() {
            val selPos = with(selected.indexOf(null)) {
                if (this < 0) selected.size else this
            }
            val opPos = with(selectedOps.indexOf(null)) {
                if (this < 0) selectedOps.size else this
            }
            return (selPos != 0) && opPos <= selPos / 2
        }

    val chiffreEnabled: Boolean
        get() {
            val selPos = with(selected.indexOf(null)) {
                if (this < 0) selected.size else this
            }
            val opPos = with(selectedOps.indexOf(null)) {
                if (this < 0) selectedOps.size else this
            }
            return (selPos and 1 == 0) || opPos >= (selPos + 1) / 2
        }

    val undoEnabled: Boolean
        get() = selected[0] != null

    val hasSolution: Boolean
        get() = !solution.isEmpty() && solution[0] != null
}

data class Step(
    val a : Int,
    val b: Int,
    val op: String
)

enum class Won {
    NOT_WON,
    WON_EXACT,
    WON_BEST
}