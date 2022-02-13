package stan.qodat.scene.control.gizmo;

import com.sun.javafx.scene.CameraHelper;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.fxyz3d.shapes.primitives.CubeMesh;
import us.ihmc.euclid.geometry.Line3D;


/**
 * Used https://github.com/FXyz/FXyzLib/blob/master/src/org/fxyz/tests/Drag3DObject.java
 * to create the test application for the gizmo.
 */
public class GizmoStackoverflow extends Application {

    private final static double AXIS_LENGTH = 80.0;
    private final static double AXIS_WIDTH = 0.5;

    private final static double CONE_RADIUS = 4.0;
    private final static double CONE_HEIGHT = 8.0;

    private final static double TORUS_INNER_RADIUS = 1.0;

    private final Group root = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera();
    private final CameraTransformer cameraTransform = new CameraTransformer();
    private final Gizmo gizmo = new Gizmo();

    CubeMesh cubeMesh = new CubeMesh(200);

    public static void main(String[] args) {
        launch(GizmoStackoverflow.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final Scene scene = new Scene(root, 1024.0, 768.0, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.web("3d3d3d"));

        loadCamera(scene);
        loadControls(scene);

        root.getChildren().addAll(gizmo, cubeMesh);
//        gizmo.controller.getNodeProperty().set(cubeMesh);

        stage.setTitle("Gizmo Example");
        stage.setScene(scene);
        stage.show();
    }

    private void loadCamera(Scene scene){

        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(-3000);
        cameraTransform.ry.setAngle(0.0);
        cameraTransform.rx.setAngle(-45.0);

        scene.setCamera(camera);
    }

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    public static Line3D getPickRay(Camera camera, MouseEvent event)
    {
        us.ihmc.euclid.tuple3D.Point3D point1 = new us.ihmc.euclid.tuple3D.Point3D();
        point1.setX(camera.getLocalToSceneTransform().getTx());
        point1.setY(camera.getLocalToSceneTransform().getTy());
        point1.setZ(camera.getLocalToSceneTransform().getTz());

        us.ihmc.euclid.tuple3D.Point3D point2 = new us.ihmc.euclid.tuple3D.Point3D();
        javafx.geometry.Point3D pointOnProjectionPlane = CameraHelper.pickProjectPlane(camera, event.getSceneX(), event.getSceneY());
        point2.setX(pointOnProjectionPlane.getX());
        point2.setY(pointOnProjectionPlane.getY());
        point2.setZ(pointOnProjectionPlane.getZ());

        return new Line3D(point1, point2);
    }

    private void loadControls(Scene scene) {
        scene.setOnMouseMoved(mouseEvent -> {

        });
        scene.setOnMousePressed(me -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
        });
        scene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            Line3D line3D = getPickRay(camera, me);

//            Transform transform = gizmo.getLocalToSceneTransform();
//            Point3D point3D = gizmo.sceneToLocal(100.0, 1.0, 1.0);

//            System.out.println(point3D);
            gizmo.controller.getPosition().set(
                    1.0, 1.0, 1.0
//                    point3D.getX(),
//                    point3D.getY(),
//                    point3D.getZ()
            );
            gizmo.controller.manipulateRotateGizmo(GizmoUtilKt.toRay(line3D));
        });
        scene.setOnMouseReleased((MouseEvent event)-> {
            gizmo.selectedAxis.set(null);
            gizmo.transformMode = null;
//            gizmo.previousEvent = null;
//            gizmo.axis = null;
        });
    }

    /**
     * @author Stan van der Bend
     */
    public static class Gizmo extends Group {

        private final GizmoController controller;

        public TransformMode transformMode;
        public SimpleObjectProperty<Axis> selectedAxis = new SimpleObjectProperty<>();
        
        public Gizmo() {
            this.controller = new GizmoController(this);
            create();
        }

        public GizmoController getController() {
            return controller;
        }

        public void create() {

            final Box xAxis = createAxisLine(Axis.X, AXIS_LENGTH, AXIS_WIDTH, AXIS_WIDTH);
            final Box yAxis = createAxisLine(Axis.Y, AXIS_WIDTH, AXIS_LENGTH, AXIS_WIDTH);
            final Box zAxis = createAxisLine(Axis.Z, AXIS_WIDTH, AXIS_WIDTH, AXIS_LENGTH);
            final ConeMesh xCone = createTranslateCone(Axis.X);
            final ConeMesh yCone = createTranslateCone(Axis.Y);
            final ConeMesh zCone = createTranslateCone(Axis.Z);

            final DragGroup xDragGroup = new DragGroup(this, Axis.X, xAxis, xCone);
            final DragGroup yDragGroup = new DragGroup(this, Axis.Y, yAxis, yCone);
            final DragGroup zDragGroup = new DragGroup(this, Axis.Z, zAxis, zCone);

            getChildren().addAll(xDragGroup, yDragGroup, zDragGroup);

            final TorusMesh xTorus = createRotationTorus(Axis.X);
            final TorusMesh yTorus = createRotationTorus(Axis.Y);
            final TorusMesh zTorus = createRotationTorus(Axis.Z);
            getChildren().addAll(xTorus, yTorus, zTorus);

            getChildren().add(new AmbientLight(Color.WHITE));
        }

        private Box createAxisLine(Axis axis, double length, double width, double depth) {
            final Box box = new Box(length, width, depth);
            box.setDrawMode(DrawMode.FILL);
            box.setDepthTest(DepthTest.DISABLE);
            box.setCullFace(CullFace.FRONT);
            box.setMaterial(new PhongMaterial(axis.regularColor));
            return box;
        }

        private ConeMesh createTranslateCone(Axis axis) {
            final ConeMesh cone = new ConeMesh(CONE_RADIUS, CONE_HEIGHT);
            cone.setDepthTest(DepthTest.DISABLE);
            cone.setCullFace(CullFace.FRONT);
            cone.setDrawMode(DrawMode.FILL);
            cone.setMaterial(new PhongMaterial(axis.regularColor));
            if(axis == Axis.X){
                cone.setTranslateX(AXIS_LENGTH / 2 + CONE_HEIGHT / 2 + TORUS_INNER_RADIUS);
                cone.setTranslateY(-CONE_RADIUS);
                cone.setRotate(90.0);
            } else if(axis == Axis.Y){
                cone.setTranslateY(AXIS_LENGTH / 2 + TORUS_INNER_RADIUS);
                cone.setRotate(-180.0);
            } else if(axis == Axis.Z){
                cone.setTranslateZ(AXIS_LENGTH / 2 + CONE_HEIGHT / 2 + TORUS_INNER_RADIUS);
                cone.setTranslateY(-CONE_RADIUS);
                cone.setRotationAxis(Rotate.X_AXIS);
                cone.setRotate(-90.0);
            }
            return cone;
        }

        private TorusMesh createRotationTorus(Axis axis) {
            final TorusMesh torus = new TorusMesh(AXIS_LENGTH / 2, TORUS_INNER_RADIUS);
            torus.setDepthTest(DepthTest.DISABLE);
            torus.setCullFace(CullFace.FRONT);
            torus.setDrawMode(DrawMode.FILL);
            torus.setMaterial(new PhongMaterial(axis.regularColor));
            if (axis == Axis.X)
                torus.setRotate(90.0);
            else if (axis == Axis.Y) {
                torus.setRotationAxis(Rotate.Y_AXIS);
                torus.setRotate(-90.0);
            } else if (axis == Axis.Z) {
                torus.setRotationAxis(Rotate.X_AXIS);
                torus.setRotate(-90.0);
            }

            torus.setOnMousePressed(mouseEvent -> {
                transformMode = TransformMode.ROTATE;
                selectedAxis.set(axis);
                torus.setMaterial(new PhongMaterial(axis.regularColor.darker()));
                mouseEvent.consume();
            });
            torus.setOnMouseReleased(mouseEvent -> {
                System.out.println("release "+selectedAxis.get());
                selectedAxis.set(null);
                torus.setMaterial(new PhongMaterial(axis.regularColor));
                mouseEvent.consume();
            });
            torus.setOnMouseEntered(mouseEvent -> {
                if (selectedAxis.get() != axis) {
                    torus.setMaterial(new PhongMaterial(axis.regularColor.brighter()));
                    mouseEvent.consume();
                }
            });
            torus.setOnMouseExited(mouseEvent -> {
                if (selectedAxis.get() != axis) {
                    torus.setMaterial(new PhongMaterial(axis.regularColor));
                    mouseEvent.consume();
                }
            });
            return torus;
        }
    }

    public enum Axis {
        X(Color.DARKRED),
        Y(Color.GREEN),
        Z(Color.BLUE);

        private final Color regularColor;

        Axis(Color regularColor) {
            this.regularColor = regularColor;
        }
    }

    public enum TransformMode {
        TRANSLATE,
        ROTATE
    }

    private static class DragGroup extends Group {

        public DragGroup(Gizmo gizmo, Axis axis, Shape3D... children) {
            super(children);

            setOnMousePressed(mouseEvent -> {
                gizmo.transformMode = TransformMode.TRANSLATE;
                gizmo.selectedAxis.set(axis);
                for (Shape3D node : children)
                    node.setMaterial(new PhongMaterial(axis.regularColor.darker()));
                mouseEvent.consume();
            });
            setOnMouseReleased(mouseEvent -> {
                gizmo.selectedAxis.set(null);
                for (Shape3D node : children)
                    node.setMaterial(new PhongMaterial(axis.regularColor));
                mouseEvent.consume();
            });
            setOnMouseEntered(mouseEvent -> {
                if (gizmo.selectedAxis.get() != axis) {
                    for (Shape3D node : children)
                        node.setMaterial(new PhongMaterial(axis.regularColor.brighter()));
                    mouseEvent.consume();
                }
            });
            setOnMouseExited(mouseEvent -> {
                if (gizmo.selectedAxis.get() != axis) {
                    for (Shape3D node : children)
                        node.setMaterial(new PhongMaterial(axis.regularColor));
                    mouseEvent.consume();
                }
            });
        }
    }

    /**
     * Taken from https://github.com/FXyz/FXyzLib/blob/master/src/org/fxyz/shapes/primitives/TorusMesh.java
     *
     * @author jDub1581
     */
    private static class TorusMesh extends MeshView {

        private static final int DEFAULT_DIVISIONS = 64;
        private static final int DEFAULT_T_DIVISIONS = 64;
        private static final double DEFAULT_RADIUS = 12.5D;
        private static final double DEFAULT_T_RADIUS = 5.0D;
        private static final double DEFAULT_START_ANGLE = 0.0D;
        private static final double DEFAULT_X_OFFSET = 0.0D;
        private static final double DEFAULT_Y_OFFSET = 0.0D;
        private static final double DEFAULT_Z_OFFSET = 1.0D;

        public TorusMesh(double radius, double tRadius) {
            this(DEFAULT_DIVISIONS, DEFAULT_T_DIVISIONS, radius, tRadius);
        }

        public TorusMesh(int rDivs, int tDivs, double radius, double tRadius) {
            setRadiusDivisions(rDivs);
            setTubeDivisions(tDivs);
            setRadius(radius);
            setTubeRadius(tRadius);

            setDepthTest(DepthTest.ENABLE);
            updateMesh();
        }

        private void updateMesh(){
            setMesh(createTorus(
                    getRadiusDivisions(),
                    getTubeDivisions(),
                    (float) getRadius(),
                    (float) getTubeRadius(),
                    (float) getTubeStartAngleOffset(),
                    (float)getxOffset(),
                    (float)getyOffset(),
                    (float)getzOffset()));
        }

        private TriangleMesh createTorus(
                int radiusDivisions,
                int tubeDivisions,
                float radius,
                float tRadius,
                float tubeStartAngle,
                float xOffset,
                float yOffset,
                float zOffset) {

            int numVerts = tubeDivisions * radiusDivisions;
            int faceCount = numVerts * 2;
            float[] points = new float[numVerts * 3],
                    texCoords = new float[numVerts * 2];
            int[] faces = new int[faceCount * 6];

            int pointIndex = 0, texIndex = 0, faceIndex = 0;
            float tubeFraction = 1.0f / tubeDivisions;
            float radiusFraction = 1.0f / radiusDivisions;
            float x, y, z;

            int p0 = 0, p1 = 0, p2 = 0, p3 = 0, t0 = 0, t1 = 0, t2 = 0, t3 = 0;

            // create points
            for (int tubeIndex = 0; tubeIndex < tubeDivisions; tubeIndex++) {

                float radian = tubeStartAngle + tubeFraction * tubeIndex * 2.0f * 3.141592653589793f;

                for (int radiusIndex = 0; radiusIndex < radiusDivisions; radiusIndex++) {

                    float localRadian = radiusFraction * (radiusIndex) * 2.0f * 3.141592653589793f;

                    points[pointIndex + 0] = x = (radius + tRadius * ((float) Math.cos(radian))) * ((float) Math.cos(localRadian) + xOffset);
                    points[pointIndex + 1] = y = (radius + tRadius * ((float) Math.cos(radian))) * ((float) Math.sin(localRadian) + yOffset);
                    points[pointIndex + 2] = z = (tRadius * (float) Math.sin(radian) * zOffset);

                    pointIndex += 3;

                    float r = radiusIndex < tubeDivisions ? tubeFraction * radiusIndex * 2.0F * 3.141592653589793f : 0.0f;
                    texCoords[texIndex] = (0.5F + (float) (Math.sin(r) * 0.5D));
                    texCoords[texIndex + 1] = ((float) (Math.cos(r) * 0.5D) + 0.5F);

                    texIndex += 2;

                }

            }
            //create faces
            for (int point = 0; point < (tubeDivisions); point++) {
                for (int crossSection = 0; crossSection < (radiusDivisions); crossSection++) {
                    p0 = point * radiusDivisions + crossSection;
                    p1 = p0 >= 0 ? p0 + 1 : p0 - (radiusDivisions);
                    p1 = p1 % (radiusDivisions) != 0 ? p0 + 1 : p0 - (radiusDivisions - 1);
                    p2 = (p0 + radiusDivisions) < ((tubeDivisions * radiusDivisions)) ? p0 + radiusDivisions : p0 - (tubeDivisions * radiusDivisions) + radiusDivisions;
                    p3 = p2 < ((tubeDivisions * radiusDivisions) - 1) ? p2 + 1 : p2 - (tubeDivisions * radiusDivisions) + 1;
                    p3 = p3 % (radiusDivisions) != 0 ? p2 + 1 : p2 - (radiusDivisions - 1);

                    t0 = point * (radiusDivisions) + crossSection;
                    t1 = t0 >= 0 ? t0 + 1 : t0 - (radiusDivisions);
                    t1 = t1 % (radiusDivisions) != 0 ? t0 + 1 : t0 - (radiusDivisions - 1);
                    t2 = (t0 + radiusDivisions) < ((tubeDivisions * radiusDivisions)) ? t0 + radiusDivisions : t0 - (tubeDivisions * radiusDivisions) + radiusDivisions;
                    t3 = t2 < ((tubeDivisions * radiusDivisions) - 1) ? t2 + 1 : t2 - (tubeDivisions * radiusDivisions) + 1;
                    t3 = t3 % (radiusDivisions) != 0 ? t2 + 1 : t2 - (radiusDivisions - 1);

                    try {
                        faces[faceIndex] = (p2);
                        faces[faceIndex + 1] = (t3);
                        faces[faceIndex + 2] = (p0);
                        faces[faceIndex + 3] = (t2);
                        faces[faceIndex + 4] = (p1);
                        faces[faceIndex + 5] = (t0);

                        faceIndex += 6;

                        faces[faceIndex] = (p2);
                        faces[faceIndex + 1] = (t3);
                        faces[faceIndex + 2] = (p1);
                        faces[faceIndex + 3] = (t0);
                        faces[faceIndex + 4] = (p3);
                        faces[faceIndex + 5] = (t1);
                        faceIndex += 6;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            TriangleMesh localTriangleMesh = new TriangleMesh();
            localTriangleMesh.getPoints().setAll(points);
            localTriangleMesh.getTexCoords().setAll(texCoords);
            localTriangleMesh.getFaces().setAll(faces);

            return localTriangleMesh;
        }

        private final IntegerProperty radiusDivisions = new SimpleIntegerProperty(DEFAULT_DIVISIONS) {

            @Override
            protected void invalidated() {
                setMesh(createTorus(
                        getRadiusDivisions(),
                        getTubeDivisions(),
                        (float) getRadius(),
                        (float) getTubeRadius(),
                        (float) getTubeStartAngleOffset(),
                        (float)getxOffset(),
                        (float)getyOffset(),
                        (float)getzOffset()));
            }

        };

        public final int getRadiusDivisions() {
            return radiusDivisions.get();
        }

        public final void setRadiusDivisions(int value) {
            radiusDivisions.set(value);
        }

        public IntegerProperty radiusDivisionsProperty() {
            return radiusDivisions;
        }

        private final IntegerProperty tubeDivisions = new SimpleIntegerProperty(DEFAULT_T_DIVISIONS) {

            @Override
            protected void invalidated() {
                updateMesh();
            }
        };
        public final int getTubeDivisions() {
            return tubeDivisions.get();
        }
        public final void setTubeDivisions(int value) {
            tubeDivisions.set(value);
        }
        public IntegerProperty tubeDivisionsProperty() {
            return tubeDivisions;
        }
        private final DoubleProperty radius = new SimpleDoubleProperty(DEFAULT_RADIUS) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getRadius() {
            return radius.get();
        }
        public final void setRadius(double value) {
            radius.set(value);
        }
        public DoubleProperty radiusProperty() {
            return radius;
        }
        private final DoubleProperty tubeRadius = new SimpleDoubleProperty(DEFAULT_T_RADIUS) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getTubeRadius() {
            return tubeRadius.get();
        }
        public final void setTubeRadius(double value) {
            tubeRadius.set(value);
        }
        public DoubleProperty tubeRadiusProperty() {
            return tubeRadius;
        }
        private final DoubleProperty tubeStartAngleOffset = new SimpleDoubleProperty(DEFAULT_START_ANGLE) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getTubeStartAngleOffset() {
            return tubeStartAngleOffset.get();
        }
        public void setTubeStartAngleOffset(double value) {
            tubeStartAngleOffset.set(value);
        }
        public DoubleProperty tubeStartAngleOffsetProperty() {
            return tubeStartAngleOffset;
        }
        private final DoubleProperty xOffset = new SimpleDoubleProperty(DEFAULT_X_OFFSET) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getxOffset() {
            return xOffset.get();
        }
        public void setxOffset(double value) {
            xOffset.set(value);
        }
        public DoubleProperty xOffsetProperty() {
            return xOffset;
        }
        private final DoubleProperty yOffset = new SimpleDoubleProperty(DEFAULT_Y_OFFSET) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getyOffset() {
            return yOffset.get();
        }
        public void setyOffset(double value) {
            yOffset.set(value);
        }
        public DoubleProperty yOffsetProperty() {
            return yOffset;
        }
        private final DoubleProperty zOffset = new SimpleDoubleProperty(DEFAULT_Z_OFFSET) {
            @Override protected void invalidated() {
                updateMesh();
            }
        };
        public final double getzOffset() {
            return zOffset.get();
        }
        public void setzOffset(double value) {
            zOffset.set(value);
        }
        public DoubleProperty zOffsetProperty() {
            return zOffset;
        }
    }

    /**
     * Taken from https://github.com/FXyz/FXyzLib/blob/master/src/org/fxyz/shapes/primitives/ConeMesh.java
     *
     * @author Birdasaur
     * @adapted Dub's CapsuleMesh example
     */
    public static class ConeMesh extends MeshView{
        private static final int DEFAULT_DIVISIONS = 32;
        private static final double DEFAULT_RADIUS = 25.0D;
        private static final double DEFAULT_HEIGHT = 50.0D;
        public ConeMesh() {
            this(DEFAULT_DIVISIONS, DEFAULT_RADIUS, DEFAULT_HEIGHT);
        }
        public ConeMesh(double radius, double height){
            this(DEFAULT_DIVISIONS, radius, height);
        }
        public ConeMesh(int divisions, double radius, double height) {
            setDivisions(divisions);
            setRadius(radius);
            setHeight(height);
            setMesh(createCone(getDivisions(), (float)getRadius(), (float)getHeight()));
        }
        private TriangleMesh createCone(int divisions, float radius, float height) {
            TriangleMesh mesh = new TriangleMesh();
            //Start with the top of the cone, later we will build our faces from these
            mesh.getPoints().addAll(0,0,0); //Point 0: Top of the Cone
            //Generate the segments of the bottom circle (Cone Base)
            double segment_angle = 2.0 * Math.PI / divisions;
            float x, z;
            double angle;
            double halfCount = (Math.PI / 2 - Math.PI / (divisions / 2));
            // Reverse loop for speed!! der
            for(int i=divisions+1;--i >= 0; ) {
                angle = segment_angle * i;
                x = (float)(radius * Math.cos(angle - halfCount));
                z = (float)(radius * Math.sin(angle - halfCount));
                mesh.getPoints().addAll(x,height,z);
            }
            mesh.getPoints().addAll(0,height,0); //Point N: Center of the Cone Base

            //@TODO Birdasaur for now we'll just make an empty texCoordinate group
            //@DUB HELP ME DUBi Wan Kanobi, you are my only hope!
            //I'm not good at determining Texture Coordinates
            mesh.getTexCoords().addAll(0,0);
            //Add the faces "winding" the points generally counter clock wise
            //Must loop through each face, not including first and last points
            for(int i=1;i<=divisions;i++) {
                mesh.getFaces().addAll( //use dummy texCoords, @TODO Upgrade face code to be real
                        0,0,i+1,0,i,0,           // Vertical Faces "wind" counter clockwise
                        divisions+2,0,i,0,i+1,0   // Base Faces "wind" clockwise
                );
            }
            return mesh;
        }
        private final DoubleProperty radius = new SimpleDoubleProperty(){
            @Override
            protected void invalidated() {
                setMesh(createCone(getDivisions(), (float)getRadius(), (float)getHeight()));
            }
        };
        public final double getRadius() {
            return radius.get();
        }
        public final void setRadius(double value) {
            radius.set(value);
        }
        public DoubleProperty radiusProperty() {
            return radius;
        }
        private final DoubleProperty height = new SimpleDoubleProperty(){
            @Override
            protected void invalidated() {
                setMesh(createCone(getDivisions(), (float)getRadius(), (float)getHeight()));
            }
        };
        public final double getHeight() {
            return height.get();
        }
        public final void setHeight(double value) {
            height.set(value);
        }
        public DoubleProperty heightProperty() {
            return height;
        }
        private final IntegerProperty divisions = new SimpleIntegerProperty(){
            @Override
            protected void invalidated() {
                setMesh(createCone(getDivisions(), (float)getRadius(), (float)getHeight()));
            }
        };
        public final int getDivisions() {
            return divisions.get();
        }
        public final void setDivisions(int value) {
            divisions.set(value);
        }
        public IntegerProperty divisionsProperty() {
            return divisions;
        }
    }

    /**
     * Taken from https://github.com/FXyz/FXyzLib/blob/master/src/org/fxyz/cameras/CameraTransformer.java
     */
    public static class CameraTransformer extends Group {

        public enum RotateOrder {
            XYZ, XZY, YXZ, YZX, ZXY, ZYX
        }

        public Translate t  = new Translate();
        public Translate p  = new Translate();
        public Translate ip = new Translate();
        public Rotate rx = new Rotate();{ rx.setAxis(Rotate.X_AXIS); }
        public Rotate ry = new Rotate();{ ry.setAxis(Rotate.Y_AXIS); }
        public Rotate rz = new Rotate();{ rz.setAxis(Rotate.Z_AXIS); }
        public Scale s = new Scale();

        public CameraTransformer() {
            super();
            getTransforms().addAll(t, rz, ry, rx, s);
        }

        public CameraTransformer(CameraTransformer.RotateOrder rotateOrder) {
            super();
            // choose the order of rotations based on the rotateOrder
            switch (rotateOrder) {
                case XYZ: getTransforms().addAll(t, p, rz, ry, rx, s, ip);break;
                case XZY: getTransforms().addAll(t, p, ry, rz, rx, s, ip);break;
                case YXZ: getTransforms().addAll(t, p, rz, rx, ry, s, ip);break;
                case YZX: getTransforms().addAll(t, p, rx, rz, ry, s, ip);  // For Camerabreak;
                case ZXY: getTransforms().addAll(t, p, ry, rx, rz, s, ip);break;
                case ZYX: getTransforms().addAll(t, p, rx, ry, rz, s, ip);break;
            }
        }

        public void setTranslate(double x, double y, double z) {
            t.setX(x);t.setY(y);t.setZ(z);
        }

        public void setTranslate(double x, double y) {
            t.setX(x);t.setY(y);
        }

        public void setTx(double x) { t.setX(x); }
        public void setTy(double y) { t.setY(y); }
        public void setTz(double z) { t.setZ(z); }

        public void setRotate(double x, double y, double z) {
            rx.setAngle(x);ry.setAngle(y);rz.setAngle(z);
        }

        public void setRotateX(double x) { rx.setAngle(x); }
        public void setRotateY(double y) { ry.setAngle(y); }
        public void setRotateZ(double z) { rz.setAngle(z); }
        public void setRx(double x) { rx.setAngle(x); }
        public void setRy(double y) { ry.setAngle(y); }
        public void setRz(double z) { rz.setAngle(z); }

        public void setScale(double scaleFactor) {
            s.setX(scaleFactor);s.setY(scaleFactor);s.setZ(scaleFactor);
        }

        public void setScale(double x, double y, double z) {
            s.setX(x);s.setY(y);s.setZ(z);
        }

        public void setSx(double x) { s.setX(x); }
        public void setSy(double y) { s.setY(y); }
        public void setSz(double z) { s.setZ(z); }

        public void setPivot(double x, double y, double z) {
            p.setX(x);p.setY(y);p.setZ(z);
            ip.setX(-x);ip.setY(-y);ip.setZ(-z);
        }

        public void reset() {
            t.setX(0.0);t.setY(0.0);t.setZ(0.0);
            rx.setAngle(0.0);ry.setAngle(0.0);rz.setAngle(0.0);
            s.setX(1.0);s.setY(1.0);s.setZ(1.0);
            p.setX(0.0);p.setY(0.0);p.setZ(0.0);
            ip.setX(0.0);ip.setY(0.0);ip.setZ(0.0);
        }
    }
}