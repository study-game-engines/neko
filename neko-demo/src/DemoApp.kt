package neko.demo

import de.twometer.neko.audio.SoundEngine
import de.twometer.neko.core.AppConfig
import de.twometer.neko.core.NekoApp
import de.twometer.neko.events.KeyPressEvent
import de.twometer.neko.events.RenderForwardEvent
import de.twometer.neko.render.Primitives
import de.twometer.neko.res.*
import de.twometer.neko.scene.AABB
import de.twometer.neko.scene.Color
import de.twometer.neko.scene.component.BoundingBoxProviderComponent
import de.twometer.neko.scene.nodes.*
import de.twometer.neko.util.MathF.toRadians
import imgui.ImGui
import org.greenrobot.eventbus.Subscribe
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*

class DemoApp : NekoApp(AppConfig(windowTitle = "Neko Engine Demo", windowIcon = "icon.png")) {

    private lateinit var girl: ModelNode
    private lateinit var rin: ModelNode
    private lateinit var sky: Sky

    override fun onPreInit() {
        AssetManager.registerPath("./neko-engine/assets")
        AssetManager.registerPath("./neko-demo/assets")
    }

    override fun onPostInit() {
        //Profiler.enabled = true
        //playerController = BenchmarkPlayerController()

        val testFont = FontCache.get("lucida")

        scene.rootNode.attachChild(ModelLoader.load("astronaut.fbx").also {
            it.transform.translation.set(-1.7f, 0.25f, 16f)
            it.transform.rotation.rotateX(toRadians(90f))
            //it.transform.rotation.rotateY(toRadians(-45f))
            it.scanTree(ScanFilters.GEOMETRY) { n ->
                n.attachComponent(BoundingBoxProviderComponent {
                    AABB(Vector3f(), Vector3f(1f, 1f, 1f))
                })
            }

            it.playAnimation(it.animations[1])
        })

        scene.rootNode.attachChild(ModelLoader.load("girl.fbx").also {
            it.animations.add(AnimationCache.get("walk.ani"))
            it.transform.scale.set(0.0001)
            it.transform.translation.set(-1.7f, 0.25f, 16f)
            it.transform.rotation.rotateX(toRadians(90f))
            it.transform.rotation.rotateY(toRadians(-5f))
            it.playAnimation(it.animations[1])
            girl = it
        })

        scene.rootNode.attachChild(ModelLoader.load("test.fbx").also {
            it.transform.scale.set(0.01f)
        })

        scene.rootNode.attachChild(ModelLoader.load("ground.fbx").also {
            it.transform.scale.set(0.0001f)
            it.transform.translation.y = -1f
        })

        val rinPrefab = ModelLoader.load("rin.fbx")

        scene.rootNode.attachChild(rinPrefab.createInstance().also {
            it.transform.translation.set(0.75f, 0f, 0f)
            it.transform.rotation.rotateX(toRadians(90f))
            it.transform.scale.set(0.0001)
            it.playAnimation(it.animations[0])
            it.attachChild(Billboard(testFont, "TheLegend27").also { tag ->
                tag.transform.scale.set(1000f, 1000f, 100f)
                tag.transform.translation.set(0f, 1.7f, 0f)
            })
            rin = it
        })

        scene.rootNode.attachChild(rinPrefab.createInstance().also {
            it.transform.translation.set(-2f, 0f, 0f)
            it.transform.rotation.rotateX(toRadians(90f))
            it.transform.scale.set(0.0001)
            it.playAnimation(it.animations[0])
        })

        scene.rootNode.attachChild(ModelLoader.load("demo-run.fbx").also {
            it.transform.translation.set(5f, 0f, 0f)
            it.transform.scale.set(0.01, 0.01, 0.01)
            it.playAnimation(it.animations[0])
        })

        scene.rootNode.attachChild(PointLight().also { it.color = Color(1f, 1f, 1f, 1f) })
        scene.rootNode.attachChild(PointLight().also {
            it.color = Color(0f, 1f, 1f, 1f)
            it.transform.translation.set(2f, 2f, 0f)
        })

        //scene.rootNode.attachChild(ModelLoader.load("skeld.obj"))

        val lightPositions = RawLoader.loadLines("lights.txt")
        for (pos in lightPositions) {
            val parts = pos.split("|")
            scene.rootNode.attachChild(PointLight().also {
                it.transform.translation.set(Vector3f(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat()))
                if (parts.size > 3)
                    it.color = Color(parts[3].toFloat(), parts[4].toFloat(), parts[5].toFloat(), 1f)
                else
                    it.color = Color(1f, 1f, 1f, 1f)
            })
        }

        scene.rootNode.detachAll {
            it is Geometry && it.material.name.startsWith("Translucent_Glass_Blue")
        }

        sky = Sky(CubemapCache.get("skybox"))
        scene.rootNode.attachChild(sky)

        //SoundEngine.play("Test.ogg")
        SoundEngine.playAt("Test.ogg", Vector3f(-5f, 2f, 10f))
    }

    override fun onTimerTick() {
        //sky.transform.rotation.rotateY(0.0002f)
        rin.transform.translation.z += 0.055f
    }

    var selectedNode: Node? = null

    override fun onRenderFrame() {
        if (ImGui.begin("Scenegraph")) {
            fun drawNode(node: Node) {
                ImGui.pushID(node.id)
                val open = ImGui.treeNode("${node.id} - ${node.javaClass.simpleName} [${node.name}]")
                if (ImGui.isItemClicked()) {
                    selectedNode = node
                }
                if (open) {
                    node.children.forEach(::drawNode)
                    ImGui.treePop()
                }
                ImGui.popID()
            }
            drawNode(scene.rootNode)
        }
        ImGui.end()

        selectedNode?.apply {
            if (ImGui.begin("Node info")) {
                ImGui.text("POS:" + this.transform.translation)
                ImGui.text("ROT:" + this.transform.rotation)
                ImGui.text("SCALE:" + this.transform.scale)

                if (ImGui.button("RESET")) {
                    this.transform.scale.set(1f, 1f, 1f)
                }
            }
            ImGui.end()
        }

        showDebugWindow()


    }

    @Subscribe
    fun onKeyPress(e: KeyPressEvent) {
        if (e.key == GLFW_KEY_ESCAPE) {
            if (guiManager.page == null)
                guiManager.page = DemoPausePage()
            else
                guiManager.page = null
        }
        if (e.key == GLFW_KEY_LEFT_ALT) {
            this.cursorVisible = !this.cursorVisible
        }
        if (e.key == GLFW_KEY_1) {
            girl.playAnimation(girl.animations[0])
        }
        if (e.key == GLFW_KEY_2) {
            girl.playAnimation(girl.animations[1])
        }
    }

    @Subscribe
    fun renderFwd(e: RenderForwardEvent) {
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        val shader = ShaderCache.get("debug.nks")
        shader.bind()

        scene.rootNode.scanTree {
            if (it is Geometry) {
                val aabb = it.aabb!!.transform(it.compositeTransform.matrix)
                shader["modelMatrix"] =
                    Matrix4f().translate(aabb.center).scale(aabb.sizeX * 0.5f, aabb.sizeY * 0.5f, aabb.sizeZ * 0.5f)
                Primitives.unitCube.render()
            }
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
    }
}

fun main() {
    DemoApp().run()
}