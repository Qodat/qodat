package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.TreeItem
import stan.qodat.javafx.label

class RootSceneTreeItem : TreeItem<Node>() {

    init {
        isExpanded = true
        label("Nodes") {
//            contextMenu = ContextMenu(ExportMenu<AbstractSubScene>().apply {
//                setExportable(SubScene3D)
//                bindAnimation(Properties.selectedAnimation)
//            })
        }
    }


}