package stan.qodat.scene.runescape.animation

import jagex.MayaAnimation

class AnimationFrameMaya(
    name: String,
    private val duration: Int,
    val index: Int,
    val animation: MayaAnimation,
) : AnimationFrame(name, duration)
{
    override fun getLength() = duration.toLong()
}
