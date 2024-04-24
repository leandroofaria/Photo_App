package com.example.camera

import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val mainBinding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            // android.Manifest.permission.RECORD_AUDIO
        )
    } else {

        arrayListOf(
            android.Manifest.permission.CAMERA,
            // android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }


    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var capturedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)

        capturedImageView = findViewById(R.id.capturedImageView)

        if (verificarPermissaoMultiplos()) {
            iniciarCamera()
        }

        mainBinding.flipCameraIB.setOnClickListener{
            lensFacing = if(lensFacing == CameraSelector.LENS_FACING_FRONT){
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            vincularCasosUsoCamera()
        }
        mainBinding.captureIB.setOnClickListener{
            capturarFoto()
        }
        mainBinding.flashToggleIB.setOnClickListener{
            definirIconeFlash(camera)
        }
    }

    private fun verificarPermissaoMultiplos(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    iniciarCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        abrirConfiguracaoApp(this)
                    } else {
                        exibirDialogPermissao(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    verificarPermissaoMultiplos()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun iniciarCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            vincularCasosUsoCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs (previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else{
            AspectRatio.RATIO_16_9
        }
    }

    private fun vincularCasosUsoCamera() {
        val screenAspectRatio = aspectRatio(
            mainBinding.previewView.width,
            mainBinding.previewView.height,
        )

        val rotation = mainBinding.previewView.display.rotation

        val resolutionSelector = ResolutionSelector.Builder().setAspectRatioStrategy(
            AspectRatioStrategy(
                screenAspectRatio,
                AspectRatioStrategy.FALLBACK_RULE_AUTO
            )
        )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(mainBinding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector,preview, imageCapture
            )
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun definirIconeFlash(camera: Camera) {
        if(camera.cameraInfo.hasFlashUnit()){
            if(camera.cameraInfo.torchState.value == 0){
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
            }else{
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
            }
        }else{
            Toast.makeText(
                this,
                "Flash não está disponível",
                Toast.LENGTH_LONG
            ).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }

    private fun capturarFoto() {
        val pastaImagem = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Imagens"
        )
        if (!pastaImagem.exists()) {
            pastaImagem.mkdir()
        }

        val nomeArquivo = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }
        val outputOption =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).setMetadata(metadata).build()
            } else {
                val imageFile = File(pastaImagem, nomeArquivo)
                OutputFileOptions.Builder(imageFile)
                    .setMetadata(metadata).build()
            }

        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    savedUri?.let {
                        val previewView = layoutInflater.inflate(R.layout.layout_preview_image, null)
                        val previewImageView = previewView.findViewById<ImageView>(R.id.previewImageView)
                        val closePreviewButton = previewView.findViewById<Button>(R.id.closePreviewButton)

                        previewImageView.setImageURI(savedUri)

                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setView(previewView)
                            .setCancelable(false)
                            .create()

                        closePreviewButton.setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }

                    val message = "Foto capturada com sucesso: $savedUri"
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
    }
}
