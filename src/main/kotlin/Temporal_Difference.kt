@file:Suppress("PropertyName", "LocalVariableName", "FunctionName")

import Action.*
import State.*
import kotlin.random.Random


enum class Action {
    RUN,
    PASS,
    SHOOT;

    override fun toString() = name.lowercase().capitalize()
}

data class Trajectory(val s_t: Play, val a_t: Action, val `s_t+1`: State, val r: Int) {

    lateinit var `a_t+1`: Action
    var `Q(s_t+1, a_t+1)`: Double = Double.NaN

    var `V(s_t)` : Double
        get() = s_t.value
        set(value) { s_t.value = value }

    val `V(s_t+1)` : Double
        get() = if (`s_t+1` is Play) `V(s_t)` else 0.0

    var `Q(s, a)` : Double
        get() = s_t[a_t]
        set(value) {
            s_t.set(a_t, value)
        }

    fun selectNextAction(epsilon: Double) {
        val (action, value) = `s_t+1`.epsilonGreedyActionValuePair(epsilon)
        `a_t+1` = action
        `Q(s_t+1, a_t+1)` = value
    }
}

sealed class State {

    private val actionValuesMap = Action.values().associateWith { 0.0 }.toMutableMap()

    operator fun get(action: Action) = actionValuesMap[action]!!

    fun set(action: Action, value: Double) {
        actionValuesMap[action] = value
    }

    fun epsilonGreedyActionValuePair(epsilon: Double) : Pair<Action, Double> {
        return if (Random.nextDouble() < epsilon)
            actionValuesMap.entries.random().toPair()
        else
            maxAction().toPair()
    }

    fun maxAction() = actionValuesMap.maxByOrNull { it.value }!!

    fun maxActionValue() = maxAction().value

    object Play : State() {

        var value = 0.0


        override fun toString() = "Play"
    }
    object Goal : State() {
        override fun toString() = "Goal"
    }
}

fun main() {

    val play_run_play = Trajectory(Play, RUN, Play, r = 2)
    val play_pass_play = Trajectory(Play, PASS, Play, r = 3)
    val play_pass_goal = Trajectory(Play, PASS, Goal, r = 8)
    val play_shoot_play = Trajectory(Play, SHOOT, Play, r = 0)
    val play_shoot_goal = Trajectory(Play, SHOOT, Goal, r = 20)

    val α = 0.5
    var γ = 0.9

//    for (episode in arrayOf(play_run_play, play_shoot_goal)) {
//        episode.apply {
//            println("\\text{Update the value after observation $<$s, $a, ${`s_t+1`}>$}")
//            println("\\begin{align*}")
//            println("V($s) &= V($s) + $α * ($r + $γ * V($s) - V(${`s_t+1`})))\\\\")
//            print("        &= $`V(s_t)` + $α * ($r + $γ * $`V(s_t)` - ${`V(s_t+1)`}))\\\\\n")
//            `V(s_t)` += α * (r + γ * (`V(s_t+1)`) - `V(s_t)`)
//            print("        &= \\mathbf{${`V(s_t)`}}\\\\\n")
//            println("\\end{align*}")
//        }
//    }


//    for (episode in arrayOf(play_run_play, play_pass_play, play_shoot_goal)) {
//        episode.apply {
//            println("\\text{Update the value after observation $<$s, $a, ${`s_t+1`}>$}")
//            println("\\begin{align*}")
//            println("Q($s,$a) &= Q($s, $a) + $α * ($r + $γ * \\underset{a\\in{\\mathcal{A}}}{max}Q(${`s_t+1`}, a) - Q($s, $a)))\\\\")
//            print("        &= $`Q(s, a)` + $α * ($r + $γ * ${`s_t+1`.maxActionValue()} - ${`Q(s, a)`}))\\\\\n")
//            `Q(s, a)` += α * (r + γ * (`s_t+1`.maxActionValue()) - `Q(s, a)`)
//            print("        &= \\mathbf{${`Q(s, a)`}}\n")
//            println("\\end{align*}")
//        }
//    }


//    for (episode in arrayOf(play_run_play, play_pass_play)) {
//        episode.apply {
//            println("\\text{Observe $<$s_t, $a_t, ${`s_t+1`}>$}\$\\longrightarrow\$")
//            selectNextAction(0.5)
//            println("\\text{Select next action}\$\\longrightarrow\$\\text{\$a_{t+1} = $`a_t+1`\$}")
//            println("\\begin{equation*}")
//            println("Q(s_{t+1}, a_{t+1}) = Q(${`s_t+1`}, $`a_t+1`) = $`Q(s_t+1, a_t+1)`")
//            println("\\end{equation*}")
//            println("\\text{Update current state's Q-value}")
//            println("\\begin{align*}")
//            println("Q($s_t,$a_t) &= Q($s_t, $a_t) + $α * ($r + $γ * Q(${`s_t+1`}, ${`a_t+1`}) - Q($s_t, $a_t)))\\\\")
//            print("        &= $`Q(s, a)` + $α * ($r + $γ * ${`s_t+1`.maxActionValue()} - ${`Q(s, a)`}))\\\\\n")
//            `Q(s, a)` += α * (r + γ * (`Q(s_t+1, a_t+1)`) - `Q(s, a)`)
//            print("        &= \\mathbf{${`Q(s, a)`}}\n")
//            println("\\end{align*}")
//        }
//    }

    γ = 1.0
    play_run_play.apply {
        println("\\text{Observe $<$s_t, $a_t, ${`s_t+1`}>$}\$\\longrightarrow\$")
        selectNextAction(0.0)
        println("\\text{Select next action}\$\\longrightarrow\$\\text{\$a_{t+1} = $`a_t+1`\$}")
        println("\\begin{equation*}")
        println("Q(s_{t+1}, a_{t+1}) = Q(${`s_t+1`}, $`a_t+1`) = $`Q(s_t+1, a_t+1)`")
        println("\\end{equation*}")
        println("\\text{Update current state's Q-value}")
        println("\\begin{align*}")
        println("w &= w + α * (r + γ * Q(s_{t+1}, a_{t+1}) - Q(s_t, a_t))) - [f(s_t, a_t), f(s_t, a_t)]\\\\")
        println("  &= [0, 0] + $α * ($r + $γ * Q(${`s_t+1`}, ${`a_t+1`}) - Q($s_t, $a_t))) - [f($s_t, $a_t), f($s_t, $a_t)]\\\\")
        print("        &= $`Q(s, a)` + $α * ($r + $γ * $`Q(s_t+1, a_t+1)` - ${`Q(s, a)`})) - [-1, -1]\\\\\n")
        val w = Array(2) { α * (r + γ * (`Q(s_t+1, a_t+1)`) - `Q(s, a)`) + 1 }
        print("        &= \\mathbf{[${w[0]}, ${w[1]}]}\\\\\n")
        println("w_1 &= ${w[0]}\\\\")
        println("w_2 &= ${w[1]}\\\\")
        println("\\end{align*}")
    }
}