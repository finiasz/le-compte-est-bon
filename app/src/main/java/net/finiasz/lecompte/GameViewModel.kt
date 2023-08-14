package net.finiasz.lecompte

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState(alwaysSolution = true))

    val state : StateFlow<GameState> = _state.asStateFlow()
    val findSolutionJob : MutableState<Job?> = mutableStateOf(null)

    fun reset(alwaysSolution : Boolean) {
        val value = GameState(alwaysSolution = alwaysSolution)
        var tirage : List<Int>
        var targetAndSolution : Pair<Int, List<Step>?>?

        findSolutionJob.value?.cancel()

        do {
            tirage = listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 25, 50, 75, 100).shuffled().subList(0, 6)
            if (alwaysSolution) {
                targetAndSolution = findTarget(tirage)
            } else {
                targetAndSolution = Pair(Random.nextInt(101, 1000), null)
                findSolutionJob.value = viewModelScope.launch {
                    findSolution(tirage, targetAndSolution.first)
                }
            }
        } while(targetAndSolution == null)

        value.target = targetAndSolution.first
        if (alwaysSolution) {
            value.solutionBest = targetAndSolution.first
            for (i in 0..4) {
                value.solution[i] = targetAndSolution.second?.get(i)
            }
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
                won = Won.NOT_WON
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

            it.copy(selected = selected, chiffres = chiffres, selectedOps = selectedOps, usedChiffres = usedChiffres, won = Won.NOT_WON, wonPos = null)
        }
    }

    fun chiffreClick(position : Int) {
        if (state.value.chiffreEnabled.not() || state.value.won != Won.NOT_WON) {
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
            var wonPos = it.wonPos
            selected[pos] = chiffres[position]
            usedChiffres[position] = true

            if (pos and 1 == 1) {
                // we selected the second number of an equation --> compute it
                val op = it.selectedOps[pos / 2] ?: return
                val a = selected[pos - 1] ?: return
                val b = selected[pos] ?: return
                val c = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0 && (a % b) == 0) (a / b) else null
                    else -> null
                }
                chiffres[6 + pos / 2] = c
                if (c == null) {
                    undo()
                } else if (c == it.target) {
                    won = Won.WON_EXACT
                    wonPos = 6 + pos/2
                } else {
                    val best = it.solutionBest
                    if (best != null && (Math.abs(c - it.target) == Math.abs(best - it.target))) {
                        won = Won.WON_BEST
                        wonPos = 6 + pos/2
                    }
                }
            }

            it.copy(selected = selected, chiffres = chiffres, usedChiffres = usedChiffres, wonPos = wonPos, won = won)
        }
    }

    fun opClick(op: String) {
        if (state.value.opEnabled.not() || state.value.won != Won.NOT_WON) {
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

    private suspend fun findSolution(chiffres: List<Int>, target: Int) {
        val best = withContext(Dispatchers.Default) {
            recursiveFindSolution(chiffres, target, listOf())
        }

        if (coroutineContext.isActive) {
            if (best != null) {
                _state.update {
                    val solution = it.solution.toMutableList()
                    for (i in 0..min(best.second.size, 5) - 1) {
                        solution[i] = best.second[i]
                    }

                    // if no exact match, check whether we already have the closest match
                    if (best.first != it.target) {
                        for (i in 6..10) {
                            val c = it.chiffres[i] ?: break

                            if (Math.abs(c - it.target) == Math.abs(best.first - it.target)) {
                                return@update it.copy(solution = solution, solutionBest = best.first, wonPos = i, won = Won.WON_BEST)
                            }
                        }
                    }

                    it.copy(solution = solution, solutionBest = best.first)
                }
            }
        }
    }

    private suspend fun recursiveFindSolution(chiffres: List<Int>, target: Int, steps: List<Step>) : Pair<Int, List<Step>>? {
        if (!coroutineContext.isActive) {
            return null
        }

        var best: Pair<Int, List<Step>>? = null

        for (i in 0 until chiffres.size - 1) {
            for (j in i+1 until chiffres.size) {
                val a = chiffres[i]
                val b = chiffres[j]

                val possibleOps = mutableListOf('+')
                if (a != 1 && b != 1) { // avoid useless multiplications
                    possibleOps.add('*')
                }

                if (a != b) { // make sure we never get a 0
                    possibleOps.add('-')
                }

                if (a != 1 && b != 1 && (a % b == 0 || b % a == 0)) { // make sure divisions always return an integer value
                    possibleOps.add('/')
                }


                var step : Step
                for (op: Char in possibleOps) {
                    val c = when (op) {
                        '-' -> if (a < b) {
                            step = Step(a = b, b = a, op = "-")
                            b - a
                        } else {
                            step = Step(a = a, b = b, op = "-")
                            a - b
                        }

                        '/' -> if (a < b) {
                            step = Step(a = b, b = a, op = "/")
                            b / a
                        } else {
                            step = Step(a = a, b = b, op = "/")
                            a / b
                        }

                        '*' -> {
                            step = Step(a = a, b = b, op = "*")
                            a * b
                        }

                        else -> {
                            step = Step(a = a, b = b, op = "+")
                            a + b
                        }
                    }

                    if (c == target) {
                        return Pair(target, steps.toMutableList().apply { add(step) })
                    }

                    val newBest = if (chiffres.size == 2) {
                        Pair(c, steps.toMutableList().apply { add(step) })
                    } else {
                        recursiveFindSolution(chiffres.filterIndexed { index, _ -> index != i && index != j }.toMutableList().apply { add(c) }, target, steps.toMutableList().apply { add(step) })
                            ?: return null
                    }

                    if (best == null || abs(target - newBest.first) < abs(target - best.first)) {
                        best = newBest
                    }

                    if (best.first == target) {
                        return best
                    }
                }
            }
        }

        return best
    }
}