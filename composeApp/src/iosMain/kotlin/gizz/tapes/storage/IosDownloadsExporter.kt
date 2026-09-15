package gizz.tapes.storage

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.AppContext
import gizz.tapes.RootViewControllerHolder
import gizz.tapes.api.data.Recording
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.FileSystem
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosDownloadsExporter(
    private val appContext: AppContext,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DownloadsExporter {

    private val logger = Logger.withTag("IosDownloadsExporter")

    // showFolderName is unused here - UIDocumentPickerViewController's export mode can't
    // recursively export a directory (verified: it creates an empty destination folder but
    // drops the contents), so files are exported flat into whatever the user picks.
    override suspend fun export(recording: Recording, showFolderName: String): Result<Unit> = runCatching {
        val urls = recording.files.mapNotNull { file ->
            val path = fileSystem.existingDownloadOrNull(
                appContext.downloadsPath / localPath(recording, file.filename),
                file.filename,
                logger
            ) ?: return@mapNotNull null
            NSURL.fileURLWithPath(path.toString())
        }
        if (urls.isEmpty()) return@runCatching

        val rootViewController = requireNotNull(RootViewControllerHolder.current) {
            "No root view controller to present the export picker from"
        }

        presentExportPicker(urls, rootViewController)
    }

    private suspend fun presentExportPicker(urls: List<NSURL>, rootViewController: UIViewController) =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                // strongly referenced by this closure for as long as the coroutine is suspended -
                // UIDocumentPickerViewController.delegate is a weak reference on its own.
                val delegate = exportPickerDelegate(continuation)
                val picker = UIDocumentPickerViewController(forExportingURLs = urls, asCopy = true)
                picker.delegate = delegate
                rootViewController.presentViewController(picker, animated = true, completion = null)
            }
        }

    private fun exportPickerDelegate(continuation: CancellableContinuation<Unit>) =
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                continuation.resumeWith(Result.success(Unit))
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                // the user declining to export isn't an error - nothing failed.
                continuation.resumeWith(Result.success(Unit))
            }
        }
}
