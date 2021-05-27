package com.example.arfaceapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {

    private var faceRenderable : ModelRenderable?= null
    private var faceTexture : Texture?= null
    private val faceNodeMap = HashMap<AugmentedFace,AugmentedFaceNode>()
    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadModel()

        arFragment = face_frag as ArFragment
        arFragment.arSceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
        arFragment.arSceneView.scene.addOnUpdateListener {
            if(faceRenderable!=null && faceTexture !=null){
                addTrackedFaces()
                removeUntrackedFaces()
            }
        }
    }

    private fun loadModel(){
        val modelRenderable = ModelRenderable.builder().setSource(this, R.raw.fox_face).build()
        val texture = Texture.builder().setSource(this, R.drawable.clown_face_mesh_texture).build()

        CompletableFuture.allOf(modelRenderable,texture).thenAccept{
            faceRenderable = modelRenderable.get().apply {
                isShadowCaster = false
                isShadowReceiver = false
            }
            faceTexture = texture.get()
        }.exceptionally {
            Toast.makeText(this, "Error loading face model : $it",Toast.LENGTH_SHORT).show()
            null
        }
    }

    private  fun addTrackedFaces(){
        val session = arFragment.arSceneView.session?: return
        val faceList = session.getAllTrackables(AugmentedFace::class.java)
        for(face in faceList){
            if(!faceNodeMap.containsKey(face)){
                AugmentedFaceNode(face).apply {

                    setParent(arFragment.arSceneView.scene)
                    faceRegionsRenderable = faceRenderable
                    faceMeshTexture = faceTexture
                    faceNodeMap[face]= this
                }
            }
        }
    }

    private  fun removeUntrackedFaces(){
        val entries =faceNodeMap.entries
        for(entry in entries){
            val face = entry.key
            if(face.trackingState == TrackingState.STOPPED){
                val  faceNode = entry.value
                faceNode.setParent(null)
                entries.remove(entry)
            }
        }
    }
}