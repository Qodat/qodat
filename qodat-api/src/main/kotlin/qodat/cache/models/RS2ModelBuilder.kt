package qodat.cache.models

import qodat.cache.definition.ModelDefinition

class RS2ModelBuilder(vararg modelDefinitions: ModelDefinition) {

    companion object {
        private const val NULL_GLOBAL_PRIORITY = (-1).toByte()
    }

    private var vertexCount = 0
    private var faceCount = 0

    private var faceIdx = 0
    private var vertexIdx = 0

    private var globalPriority = NULL_GLOBAL_PRIORITY

    private var copyFaceTypes = false
    private var copyFacePriorities = false
    private var copyFaceAlphas = false
    private var copyFaceSkins = false
    private var copyFaceTextures = false
    private var copyMayaGroups = false

    private var vertexPositionsX : IntArray
    private var vertexPositionsY : IntArray
    private var vertexPositionsZ : IntArray
    private var vertexSkins : IntArray

    private val faceVertexIndices1: IntArray
    private val faceVertexIndices2: IntArray
    private val faceVertexIndices3: IntArray
    private val faceAlphas: ByteArray?
    private val faceColors: ShortArray
    private val faceTextures: ShortArray?
    private val faceRenderPriorities: ByteArray?
    private val faceRenderTypes: ByteArray?
    private val faceSkins: IntArray?
    private val mayaGroups: Array<IntArray?>?
    private val mayaScales: Array<IntArray?>?

    init {
        for(definition in modelDefinitions){
            vertexCount += definition.getVertexCount()
            faceCount += definition.getFaceCount()
            copyFaceTypes = copyFaceTypes or (definition.getFaceTypes() != null)
            copyFaceAlphas = copyFaceAlphas or (definition.getFaceAlphas() != null)
            if(definition.getFacePriorities() != null || globalPriority != definition.getPriority())
                copyFacePriorities = true
            else if(globalPriority == NULL_GLOBAL_PRIORITY)
                globalPriority = definition.getPriority()
            copyFaceSkins = copyFaceSkins or (definition.getFaceSkins() != null)
            copyFaceTextures = copyFaceTextures or (definition.getFaceTextures() != null)
            if (definition is RS2Model)
                copyMayaGroups = copyMayaGroups or (definition.mayaGroups != null)
        }

        vertexPositionsX = IntArray(vertexCount)
        vertexPositionsY = IntArray(vertexCount)
        vertexPositionsZ = IntArray(vertexCount)
        vertexSkins = IntArray(vertexCount)

        faceVertexIndices1 = IntArray(faceCount)
        faceVertexIndices2 = IntArray(faceCount)
        faceVertexIndices3 = IntArray(faceCount)

        faceRenderPriorities = if(copyFacePriorities) ByteArray(faceCount) else null
        faceRenderTypes = if(copyFaceTypes) ByteArray(faceCount) else null
        faceAlphas = if(copyFaceAlphas) ByteArray(faceCount) else null
        faceColors = ShortArray(faceCount)
        faceSkins = if(copyFaceSkins) IntArray(faceCount) else null
        faceTextures = if(copyFaceTextures) ShortArray(faceCount) {(-1).toShort()} else null
        if(copyMayaGroups) {
            mayaGroups = arrayOfNulls(vertexCount)
            mayaScales = arrayOfNulls(vertexCount)
        } else {
            mayaGroups = null
            mayaScales = null
        }

        for(definition in modelDefinitions){
            for(srcFaceIdx in 0 until definition.getFaceCount()){
                copyFace(definition, srcFaceIdx)
            }
        }
    }

    private fun copyFace(definition: ModelDefinition, srcFaceIdx: Int) {
        faceRenderPriorities?.tryCopy(srcFaceIdx, definition.getFacePriorities())
        faceRenderTypes?.tryCopy(srcFaceIdx, definition.getFaceTypes())
        faceAlphas?.tryCopy(srcFaceIdx, definition.getFaceAlphas())
        faceColors.tryCopy(srcFaceIdx, definition.getFaceColors())
        faceSkins?.tryCopy(srcFaceIdx, definition.getFaceSkins())
        faceTextures?.tryCopy(srcFaceIdx, definition.getFaceTextures())
        faceVertexIndices1[faceIdx] = computeVertexIndex(definition, srcFaceIdx) { it.getFaceVertexIndices1() }
        faceVertexIndices2[faceIdx] = computeVertexIndex(definition, srcFaceIdx) { it.getFaceVertexIndices2() }
        faceVertexIndices3[faceIdx] = computeVertexIndex(definition, srcFaceIdx) { it.getFaceVertexIndices3() }
        faceIdx++
    }

    // TODO(perf): linear scan of merged vertices is O(n^2); hash xyz when combining many models.
    private fun computeVertexIndex(model: ModelDefinition, face: Int, indicesSelector: (ModelDefinition) -> IntArray): Int {

        val localVertexIdx = indicesSelector.invoke(model)[face]
        val x = model.getVertexPositionsX().getOrNull(localVertexIdx)?:return -1
        val y = model.getVertexPositionsY().getOrNull(localVertexIdx)?:return -1
        val z = model.getVertexPositionsZ().getOrNull(localVertexIdx)?:return -1

        for (index in 0 until vertexIdx)
            if (x == vertexPositionsX[index] && y == vertexPositionsY[index] && z == vertexPositionsZ[index])
                return index

        vertexPositionsX[vertexIdx] = x
        vertexPositionsY[vertexIdx] = y
        vertexPositionsZ[vertexIdx] = z
        val skins = model.getVertexSkins()
        if (skins != null)
            vertexSkins[vertexIdx] = skins[localVertexIdx]
        if (model is RS2Model) {
            model.mayaGroups?.get(localVertexIdx)?.let { mayaGroups?.set(vertexIdx, it) }
            model.mayaScales?.get(localVertexIdx)?.let { mayaScales?.set(vertexIdx, it) }
        }
        return vertexIdx++
    }

    fun build(): RS2Model {
        val definition = RS2Model()
        definition.setVertexCount(vertexCount)
        definition.setVertexPositionsX(vertexPositionsX)
        definition.setVertexPositionsY(vertexPositionsY)
        definition.setVertexPositionsZ(vertexPositionsZ)
        definition.setVertexSkins(vertexSkins)
        definition.setFaceCount(faceCount)
        definition.setFaceVertexIndices1(faceVertexIndices1)
        definition.setFaceVertexIndices2(faceVertexIndices2)
        definition.setFaceVertexIndices3(faceVertexIndices3)
        definition.faceRenderPriorities = faceRenderPriorities
        definition.faceRenderTypes = faceRenderTypes
        definition.setFaceAlphas(faceAlphas)
        definition.setFaceColors(faceColors)
        definition.setFaceSkins(faceSkins)
        definition.setFaceTextures(faceTextures)
        definition.setPriority(globalPriority)
        definition.mayaGroups = mayaGroups
        definition.mayaScales = mayaScales
        return definition
    }

    private fun ByteArray.tryCopy(srcIdx: Int, byteArray: ByteArray?) {
        if (byteArray != null)
            this[faceIdx] = byteArray[srcIdx]
    }
    private fun IntArray.tryCopy(srcIdx: Int, intArray: IntArray?) {
        if (intArray != null)
            this[faceIdx] = intArray[srcIdx]
    }
    private fun ShortArray.tryCopy(srcIdx: Int, shortArray: ShortArray?) {
        if (shortArray != null)
            this[faceIdx] = shortArray[srcIdx]
    }
}
