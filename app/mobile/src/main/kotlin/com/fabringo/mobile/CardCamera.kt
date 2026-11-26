package com.fabringo.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun CardCamera(count: Int, onBack: () -> Unit, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun hasPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember { mutableStateOf(hasPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasCamera) {
        if (hasCamera && !granted && !asked) {
            asked = true
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Escanear cartilla", color = Gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
        if (count > 0) Text(addedCardsLabel(count), color = Ice)
        when {
            !hasCamera -> Text("Este dispositivo no tiene una cámara disponible.", color = Ice)
            !granted -> {
                Text("Permite el acceso a la cámara para leer el QR de la cartilla.", color = Ice)
                Button(onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) { Text("Permitir cámara") }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }) { Text("Abrir ajustes") }
            }
            else -> {
                Text("Coloca el QR de la cartilla dentro del recuadro.", color = Ice)
                CameraPreview(onResult, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CameraPreview(onResult: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val publish by rememberUpdatedState(onResult)
    var problem by remember { mutableStateOf(false) }
    val view = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }

    DisposableEffect(view, owner) {
        val main = ContextCompat.getMainExecutor(context)
        val background = Executors.newSingleThreadExecutor()
        val finished = AtomicBoolean(false)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                ).build(),
            )
            .build()
        val decoder = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
        }
        var disposed = false
        var lastFrame = 0L
        analysis.setAnalyzer(background) { image ->
            try {
                val now = SystemClock.elapsedRealtime()
                if (!finished.get() && now - lastFrame >= 120) {
                    lastFrame = now
                    val text = decodeFrame(image, decoder)
                    if (text != null && finished.compareAndSet(false, true)) {
                        main.execute { if (!disposed) publish(text) }
                    }
                }
            } finally {
                image.close()
            }
        }

        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            if (!disposed) {
                try {
                    val ready = future.get()
                    provider = ready
                    val lens = if (ready.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    ready.bindToLifecycle(owner, lens, preview, analysis)
                } catch (_: Exception) {
                    problem = true
                }
            }
        }, main)
        onDispose {
            disposed = true
            finished.set(true)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            background.shutdown()
        }
    }

    val panel = RoundedCornerShape(18.dp)
    Box(modifier.clip(panel).background(Color.Black).border(1.dp, BlueEdge, panel), contentAlignment = Alignment.Center) {
        if (problem) {
            Text("No se pudo abrir la cámara. Vuelve a intentarlo.", color = Ice, modifier = Modifier.padding(18.dp))
        } else {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth(.67f).aspectRatio(1f).border(2.dp, Gold, RoundedCornerShape(14.dp)))
        }
    }
}

private fun decodeFrame(image: ImageProxy, reader: MultiFormatReader): String? {
    val plane = image.planes.firstOrNull() ?: return null
    val width = image.width
    val height = image.height
    val buffer = plane.buffer.duplicate()
    val start = buffer.position()
    if (width < 1 || height < 1 ||
        start + (height - 1) * plane.rowStride + (width - 1) * plane.pixelStride >= buffer.limit()
    ) return null
    val luminance = ByteArray(width * height)
    for (row in 0 until height) {
        val offset = start + row * plane.rowStride
        if (plane.pixelStride == 1) {
            buffer.position(offset)
            buffer.get(luminance, row * width, width)
        } else {
            for (column in 0 until width) {
                luminance[row * width + column] = buffer.get(offset + column * plane.pixelStride)
            }
        }
    }
    val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
    return try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: ReaderException) {
        null
    } finally {
        reader.reset()
    }
}
