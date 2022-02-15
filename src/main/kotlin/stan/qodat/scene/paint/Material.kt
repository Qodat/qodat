package stan.qodat.scene.paint

import javafx.scene.paint.Material
import stan.qodat.scene.provider.ViewNodeProvider

interface Material : ViewNodeProvider {

    val fxMaterial : Material

}