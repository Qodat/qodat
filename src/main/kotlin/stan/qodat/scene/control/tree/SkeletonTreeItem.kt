package stan.qodat.scene.control.tree

import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.animation.AnimationSkeleton
import stan.qodat.scene.runescape.animation.TransformationGroup
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.model.ModelFaceMesh

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   11/02/2021
 */
class SkeletonTreeItem(
    skeleton: AnimationSkeleton,
    private val selectionModel: MultipleSelectionModel<TreeItem<Node>>,
    private val animatedEntity: AnimatedEntity<*>
) : TreeItem<Node>() {

    private val groupModelMap = HashMap<TransformationGroup, HashMap<Int, List<Model>>>()
    private val groupSelectionMeshes = HashMap<Int, Group>()
    private val groupSelectionMeshGroups = HashMap<TransformationGroup, Group>()

    init {
        text("SKELETON", Color.web("#FFC66D"))
        label(skeleton.labelProperty)
        for (group in skeleton.getTransformationGroups()) {
            treeItem {
                text("CONTROL_GROUP", Color.web("#FFC66D"))
                label(group.labelProperty)
                for (index in group.groupIndices){
                    treeItem {
                        hBox {
                            label("group[$index]")
                            comboBox("", TransformationType.values(), group.typeProperty, biDirectional = true)
                        }
                    }
                }
            }
        }
    }

    private fun getSelectionMesh(transformationGroup: TransformationGroup) : Group {
        return groupSelectionMeshGroups.getOrElse(transformationGroup) {
            val map = groupModelMap.getOrPut(transformationGroup) { HashMap() }
            val selectionMeshGroup = Group()
            for (group in transformationGroup.groupIndices){
                selectionMeshGroup.children.add(getSelectionMesh(group, map))
            }
            selectionMeshGroup
        }
    }

    private fun getSelectionMesh(groupIndex: Int, map: HashMap<Int, List<Model>>): Group {

        return groupSelectionMeshes.getOrPut(groupIndex) {

            val group = Group()

            val models = map.getOrPut(groupIndex) {
                animatedEntity.getModels().filter {
                    val faceGroups = it.getFaceGroups()
                    if (groupIndex >= faceGroups.size)
                        return@filter false
                    else
                        return@filter faceGroups[groupIndex].isNotEmpty()
                }
            }
            for ((i, model) in models.withIndex()) {
                for (face in model.getFaceGroup(groupIndex)) {
                    val mesh = ModelFaceMesh(face, if (i > 1)
                        PhongMaterial(Color.ALICEBLUE)
                    else
                        PhongMaterial(Color.RED))
                    val (v1, v2, v3) = model.getVertices(face)
                    val vertexIndex1 = mesh.addVertex(v1, model.getX(v1), model.getY(v1), model.getZ(v1))
                    val vertexIndex2 = mesh.addVertex(v2, model.getX(v2), model.getY(v2), model.getZ(v2))
                    val vertexIndex3 = mesh.addVertex(v3, model.getX(v3), model.getY(v3), model.getZ(v3))
                    val u = floatArrayOf(-1f, -1f, -1f)
                    val v = floatArrayOf(-1f, -1f, -1f)
                    val texIndex1 = mesh.addUV(u[0], v[0])
                    val texIndex2 = mesh.addUV(u[1], v[1])
                    val texIndex3 = mesh.addUV(u[2], v[2])
                    mesh.faces.addAll(
                        vertexIndex1, texIndex1,
                        vertexIndex2, texIndex2,
                        vertexIndex3, texIndex3
                    )
                    mesh.drawModeProperty.set(DrawMode.FILL)
                    mesh.visibleProperty.set(true)
                    group.children.add(mesh.getSceneNode())
                }
            }
            group
        }
    }
}