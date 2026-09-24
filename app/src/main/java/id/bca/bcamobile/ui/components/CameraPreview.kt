package id.bca.bcamobile.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Menampilkan preview kamera langsung.
 *
 * Sengaja tanpa dekorasi apa pun — bingkai, sudut, dan badge tetap digambar oleh
 * layar yang memanggilnya, jadi tampilannya tidak berubah dari desain.
 */
@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.controller = controller
            }
        },
        // factory hanya jalan sekali; tanpa update, PreviewView menahan controller
        // lama saat instance-nya berganti (mis. setelah izin baru diberikan) dan
        // preview tinggal hitam.
        update = { view -> view.controller = controller },
    )

    // Kamera dilepas begitu composable hilang dari komposisi, termasuk saat
    // layar dipop dari back stack — tanpa ini kamera tetap menyala di latar.
    DisposableEffect(lifecycleOwner, controller) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
}

/**
 * Gerbang izin kamera.
 *
 * Tiga keadaan: belum diminta (langsung minta), ditolak sekali (tampilkan
 * [rationale]), dan ditolak permanen (arahkan ke Pengaturan). Konten kamera
 * baru dirender setelah izin benar-benar ada.
 */
@Composable
fun CameraPermissionGate(
    rationale: @Composable (onRequest: () -> Unit) -> Unit,
    settingsPrompt: @Composable (onOpenSettings: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onGrantedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
        askedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!granted && !askedOnce) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Pemanggil membuat controller kamera hanya setelah izin ada, jadi ia harus
    // tahu begitu status berubah.
    LaunchedEffect(granted) { onGrantedChange(granted) }

    // Izin bisa berubah dari Pengaturan saat aplikasi di latar. Kembali dari sana
    // tidak menyusun ulang komposisi, jadi statusnya dibaca ulang tiap ON_RESUME.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier) {
        when {
            granted -> content()
            askedOnce -> settingsPrompt { context.openAppSettings() }
            else -> rationale { launcher.launch(Manifest.permission.CAMERA) }
        }
    }
}

fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { startActivity(intent) }
}
