package dev.pschmitt.syncwich.ui.recipes

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

private const val CAMERA_DIRECTORY = "recipe-editor-camera"

/** Camera contract with explicit URI grants for camera apps that do not infer EXTRA_OUTPUT access. */
internal object RecipeEditorTakePictureContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("Recipe photo", input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

/**
 * Creates a private, writable URI for [RecipeEditorTakePictureContract].
 * The editor copies this source through its normal image-cache path after capture, so it remains
 * available to an offline draft even when the eventual Mealie upload cannot run.
 */
internal fun createRecipeEditorCameraUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }
    val output = File(directory, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
}
