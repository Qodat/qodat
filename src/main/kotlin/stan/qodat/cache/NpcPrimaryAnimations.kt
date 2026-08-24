package stan.qodat.cache

import stan.qodat.cache.impl.oldschool.definition.NpcDefinition

/**
 * Stance / locomotion ids that belong in the first animation list.
 * Skeleton-matched extras from `npc_anims/{id}.json` stay lazy.
 */
object NpcPrimaryAnimations {

    private val EMPTY_INTS = IntArray(0)

    private enum class Family { IDLE, WALK, RUN, CRAWL }

    private enum class Role(val label: String, val family: Family, val head: Boolean) {
        IDLE("Idle", Family.IDLE, true),
        IDLE_ROTATE_LEFT("Idle rotate left", Family.IDLE, false),
        IDLE_ROTATE_RIGHT("Idle rotate right", Family.IDLE, false),
        WALK("Walk", Family.WALK, true),
        ROTATE_180("Rotate 180", Family.WALK, false),
        ROTATE_LEFT("Rotate left", Family.WALK, false),
        ROTATE_RIGHT("Rotate right", Family.WALK, false),
        RUN("Run", Family.RUN, true),
        RUN_ROTATE_180("Run rotate 180", Family.RUN, false),
        RUN_ROTATE_LEFT("Run rotate left", Family.RUN, false),
        RUN_ROTATE_RIGHT("Run rotate right", Family.RUN, false),
        CRAWL("Crawl", Family.CRAWL, true),
        CRAWL_ROTATE_180("Crawl rotate 180", Family.CRAWL, false),
        CRAWL_ROTATE_LEFT("Crawl rotate left", Family.CRAWL, false),
        CRAWL_ROTATE_RIGHT("Crawl rotate right", Family.CRAWL, false),
    }

    /** Decoder order in [stan.qodat.cache.impl.legacy.decoder.LegacyNpcDecoder]. */
    private val legacyRoles = listOf(
        Role.WALK,
        Role.IDLE,
        Role.ROTATE_LEFT,
        Role.ROTATE_RIGHT,
        Role.ROTATE_180,
    )

    fun intIds(npc: NpcDefinition): IntArray {
        val raw = intArrayOf(
            npc.standingAnimation,
            npc.walkingAnimation,
            npc.idleRotateLeftAnimation,
            npc.idleRotateRightAnimation,
            npc.rotateLeftAnimation,
            npc.rotateRightAnimation,
            npc.rotate180Animation,
            npc.runAnimation,
            npc.runRotate180Animation,
            npc.runRotateLeftAnimation,
            npc.runRotateRightAnimation,
            npc.crawlAnimation,
            npc.crawlRotate180Animation,
            npc.crawlRotateLeftAnimation,
            npc.crawlRotateRightAnimation,
        )
        var n = 0
        outer@ for (i in raw.indices) {
            val value = raw[i]
            if (value <= 0) continue
            for (j in 0 until n) {
                if (raw[j] == value) continue@outer
            }
            raw[n++] = value
        }
        return if (n == 0) EMPTY_INTS else if (n == raw.size) raw else raw.copyOf(n)
    }

    fun ids(npc: NpcDefinition): Array<String> = CacheIdStrings.of(intIds(npc))

    fun labels(npc: NpcDefinition): Map<String, String> {
        val rolesById = linkedMapOf<Int, MutableList<Role>>()
        add(rolesById, npc.standingAnimation, Role.IDLE)
        add(rolesById, npc.walkingAnimation, Role.WALK)
        add(rolesById, npc.idleRotateLeftAnimation, Role.IDLE_ROTATE_LEFT)
        add(rolesById, npc.idleRotateRightAnimation, Role.IDLE_ROTATE_RIGHT)
        add(rolesById, npc.rotateLeftAnimation, Role.ROTATE_LEFT)
        add(rolesById, npc.rotateRightAnimation, Role.ROTATE_RIGHT)
        add(rolesById, npc.rotate180Animation, Role.ROTATE_180)
        add(rolesById, npc.runAnimation, Role.RUN)
        add(rolesById, npc.runRotate180Animation, Role.RUN_ROTATE_180)
        add(rolesById, npc.runRotateLeftAnimation, Role.RUN_ROTATE_LEFT)
        add(rolesById, npc.runRotateRightAnimation, Role.RUN_ROTATE_RIGHT)
        add(rolesById, npc.crawlAnimation, Role.CRAWL)
        add(rolesById, npc.crawlRotate180Animation, Role.CRAWL_ROTATE_180)
        add(rolesById, npc.crawlRotateLeftAnimation, Role.CRAWL_ROTATE_LEFT)
        add(rolesById, npc.crawlRotateRightAnimation, Role.CRAWL_ROTATE_RIGHT)
        return compact(rolesById).mapKeys { it.key.toString() }
    }

    /**
     * Labels for 317-style npc defs, whose [animationIds] are
     * walk, idle, rotate left, rotate right, rotate 180.
     */
    fun legacyLabels(animationIds: Array<String>): Map<String, String> {
        val rolesById = linkedMapOf<Int, MutableList<Role>>()
        for ((index, raw) in animationIds.withIndex()) {
            val role = legacyRoles.getOrNull(index) ?: break
            val id = raw.toIntOrNull() ?: continue
            add(rolesById, id, role)
        }
        return compact(rolesById).mapKeys { it.key.toString() }
    }

    private fun add(rolesById: MutableMap<Int, MutableList<Role>>, id: Int, role: Role) {
        if (id <= 0) return
        rolesById.getOrPut(id) { mutableListOf() }.add(role)
    }

    private fun compact(rolesById: Map<Int, List<Role>>): Map<Int, String> =
        rolesById.mapValues { (_, roles) -> compact(roles) }

    private fun compact(roles: List<Role>): String {
        val distinct = roles.distinct()
        if (distinct.size == 1)
            return distinct[0].label
        val byFamily = distinct.groupBy { it.family }
        return Family.entries.mapNotNull { family ->
            val group = byFamily[family] ?: return@mapNotNull null
            group.firstOrNull { it.head }?.label
                ?: group.joinToString(" · ") { it.label }
        }.joinToString(" · ")
    }
}
