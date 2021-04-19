package stan.qodat.scene.transform

import javafx.collections.ObservableList

/**
 * Can be played through an [TransformationTimer].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface Transformer<G : TransformationGroup> {

    /**
     * Apply transformation to each [transformable][T] in [transformableList].
     */
    fun update(transformableList: List<Transformable>)

    fun setFrame(frameIndex: Int)

    fun getFrameList() : ObservableList<G>
}