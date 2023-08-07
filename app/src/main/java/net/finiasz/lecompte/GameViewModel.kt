package net.finiasz.lecompte

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())

    val state : StateFlow<GameState> = _state.asStateFlow()

    fun reset() {
        val value = GameState()
        var tirage : List<Int>
        var targetAndSolution : Pair<Int, List<Step>>?
        do {
            tirage = listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 25, 50, 75, 100).shuffled().subList(0, 6)
            targetAndSolution = findTarget(tirage)
        } while(targetAndSolution == null)

        value.target = targetAndSolution.first
        for (i in 0..4) {
            value.solution[i] = targetAndSolution.second[i]
        }
        for (i in 0..5) {
            value.chiffres[i] = tirage[i]
        }
        for (i in 6..10) {
            value.chiffres[i] = null
        }
        for (i in 0..9) {
            value.selected[i] = null
        }
        for (i in 0..4) {
            value.selectedOps[i] = null
        }

        _state.update { value }
    }

    fun solve() {
        // first reset the game
        _state.update {
            val selected: MutableList<Int?> = MutableList(10) { null }
            val selectedOps: MutableList<String?> = MutableList(5) { null }
            val chiffres = it.chiffres.toMutableList()
            val usedChiffres = MutableList(11) { false }

            for (i in 6..10) {
                chiffres[i] = null
            }

            it.copy(
                selected = selected,
                chiffres = chiffres,
                selectedOps = selectedOps,
                usedChiffres = usedChiffres,
                won = null
            )
        }

        // then, play the solution steps
        for (step in state.value.solution) {
            if (step == null) {
                break
            }
            var found = false
            for (i in 0..10) {
                if (!state.value.usedChiffres[i] && state.value.chiffres[i] == step.a) {
                    chiffreClick(i)
                    found = true
                    break
                }
            }
            if (!found) {
                break
            }
            found = false
            opClick(step.op)
            for (i in 0..10) {
                if (!state.value.usedChiffres[i] && state.value.chiffres[i] == step.b) {
                    chiffreClick(i)
                    found = true
                    break
                }
            }
            if (!found) {
                break
            }
        }
    }

    fun undo() {
        val selPos = with(state.value.selected.indexOf(null)) {
            if (this < 0) state.value.selected.size else this
        }
        if (selPos == 0) {
            return
        }
        val pos = (selPos - 1) / 2
        _state.update {
            val selected = it.selected.toMutableList()
            val selectedOps = it.selectedOps.toMutableList()
            val chiffres = it.chiffres.toMutableList()
            val usedChiffres = it.usedChiffres.toMutableList()

            if (selected[2 * pos + 1] != null) {
                for (i in 10 downTo 0) {
                    if (usedChiffres[i] && chiffres[i] == selected[2 * pos + 1]) {
                        usedChiffres[i] = false
                        break
                    }
                }
                selected[2 * pos + 1] = null
            }
            if (selected[2 * pos] != null) {
                for (i in 10 downTo 0) {
                    if (usedChiffres[i] && chiffres[i] == selected[2 * pos]) {
                        usedChiffres[i] = false
                        break
                    }
                }
                selected[2 * pos] = null
            }
            selectedOps[pos] = null
            chiffres[6 + pos] = null

            it.copy(selected = selected, chiffres = chiffres, selectedOps = selectedOps, usedChiffres = usedChiffres, won = null)
        }
    }

    fun chiffreClick(position : Int) {
        if (state.value.chiffreEnabled.not() || state.value.won != null) {
            return
        }
        if (state.value.usedChiffres[position]) {
            return
        }
        val pos = state.value.selected.indexOf(null)
        if (pos == -1) {
            return
        }
        _state.update {
            val selected = it.selected.toMutableList()
            val chiffres = it.chiffres.toMutableList()
            val usedChiffres = it.usedChiffres.toMutableList()
            var won = it.won
            selected[pos] = chiffres[position]
            usedChiffres[position] = true

            if (pos and 1 == 1) {
                // we selected the second number of an equation --> compute it
                val op = it.selectedOps[pos / 2] ?: return
                val a = selected[pos - 1] ?: return
                val b = selected[pos] ?: return
                chiffres[6 + pos / 2] = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0 && (a % b) == 0) (a / b) else null
                    else -> null
                }
                if (chiffres[6 + pos / 2] == null) {
                    undo()
                } else if (chiffres[6 + pos / 2] == it.target) {
                    won = 6 + pos/2
                }
            }

            it.copy(selected = selected, chiffres = chiffres, usedChiffres = usedChiffres, won = won)
        }
    }

    fun opClick(op: String) {
        if (state.value.opEnabled.not() || state.value.won != null) {
            return
        }
        val pos = state.value.selectedOps.indexOf(null)
        if (pos == -1) {
            return
        }
        // if an op is clicked before selecting a first chiffre, automatically select the last computed chiffre
        if (pos != 0 && state.value.selected.indexOf(null) == 2 * pos) {
            chiffreClick(5 + pos)
        }

        _state.update {
            val selectedOps = it.selectedOps.toMutableList()
            val selected = it.selected.toMutableList()
            selectedOps[pos] = op

            it.copy(selectedOps = selectedOps, selected = selected)
        }
    }

    init {
        reset()
    }

    fun findTarget(chiffres : List<Int>) : Pair<Int, List<Step>>? {
        var nums: MutableList<Int>
        var attempt = 0
        val solution = mutableListOf<Step>()
        do {
            attempt++
            if (attempt > 30) { // if we didn't find a suitable target after 30 attempts, give up!
                return null
            }
            solution.clear()
            nums = chiffres.toMutableList()
            while (nums.size > 1) {
                nums.shuffle()
                val a = nums.removeAt(nums.size - 1)
                val b = nums.removeAt(nums.size - 1)
                val possibleOps = mutableListOf(Pair("+", 3))

                if (a != 1 && b != 1) { // avoid useless multiplications
                    possibleOps.add(Pair("*", 4))
                }

                if (a != b) { // make sure we never get a 0
                    possibleOps.add(Pair("-", 2))
                }

                if (a != 1 && b != 1 && (a % b == 0 || b % a == 0)) { // make sure divisions always return an integer value
                    possibleOps.add(Pair("/", 1))
                }

                val totalWeight = possibleOps.map {
                    it.second
                }.reduce { first, second ->
                    first + second
                }

                var x = Random.nextInt(totalWeight)
                var op: String? = null

                val iterator = possibleOps.iterator()
                while (iterator.hasNext()) {
                    val pair = iterator.next()
                    x -= pair.second
                    if (x < 0) {
                        op = pair.first
                        break
                    }
                }

                val c = when (op) {
                    "-" -> if (a < b) {
                        solution.add(Step(a = b, b = a, op = "-"))
                        b - a
                    } else {
                        solution.add(Step(a = a, b = b, op = "-"))
                        a - b
                    }

                    "/" -> if (a < b) {
                        solution.add(Step(a = b, b = a, op = "/"))
                        b / a
                    } else {
                        solution.add(Step(a = a, b = b, op = "/"))
                        a / b
                    }

                    "*" -> {
                        solution.add(Step(a = a, b = b, op = "*"))
                        a * b
                    }

                    else -> {
                        solution.add(Step(a = a, b = b, op = "+"))
                        a + b
                    }
                }

                nums.add(c)
            }
        } while(!(nums[0] in 101..999))
        return Pair(nums[0], solution)
    }
}