package com.example.talklens.activity.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.talklens.util.Language
import com.example.talklens.R
import com.example.talklens.analyzer.TextAnalyzer
import com.example.talklens.databinding.FragmentMainBinding
import com.example.talklens.util.ScopedExecutor
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        // We only need to analyze the part of the image that has text, so we set crop percentages
        // to avoid analyze the entire image from the live camera feed.
        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 74

        // This is an arbitrary number we are using to keep tab of the permission
        // request. Where an app has multiple context for requesting permission,
        // this can help differentiate the different contexts
        private const val REQUEST_CODE_PERMISSIONS = 10

        // This is an array of all the permission specified in the manifest
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val TAG = "MainFragment"
    }

    private var displayId: Int = -1
    private val viewModel: MainViewModel by viewModels()
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var binding: FragmentMainBinding

    /** Blocking camera and inference operations are performed using this executor. */
    private lateinit var cameraExecutor: ExecutorService

    /** UI callbacks are run on this executor. */
    private lateinit var scopedExecutor: ScopedExecutor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    } // OnCreateView

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down the scoped executor. The camera executor will automatically shut down its
        // background threads after 60s of idling.
        scopedExecutor.shutdown()
    } // OnDestroyView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMainBinding.bind(view)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.viewfinder)

        // Initialize our background executor
        cameraExecutor = Executors.newCachedThreadPool()
        scopedExecutor = ScopedExecutor(cameraExecutor)

        viewModel.executor = cameraExecutor

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            viewFinder.post {
                // Keep track of the display in which this view is attached
                displayId = viewFinder.display.displayId

                // Set up the camera and its use cases
                setUpCamera()
            }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Get available language list and set up the target language spinner
        // with default selections.
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )

        binding.targetLangSelector.adapter = adapter
        binding.targetLangSelector.setSelection(adapter.getPosition(Language("en")))
        binding.targetLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.targetLang.value = adapter.getItem(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        viewModel.sourceLang.observe(viewLifecycleOwner, Observer { binding.srcLang.text = it.displayName })
        viewModel.translatedText.observe(viewLifecycleOwner, Observer { resultOrError ->
            resultOrError?.let {
                if (it.error != null) {
                    binding.translatedText.error = resultOrError.error?.localizedMessage
                } else {
                    binding.translatedText.text = resultOrError.result
                }
            }
        })
        viewModel.modelDownloading.observe(viewLifecycleOwner, Observer { isDownloading ->
            binding.progressBar.visibility = if (isDownloading) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
            binding.progressText.visibility = binding.progressBar.visibility
        })

        binding.overlay.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {}

                override fun surfaceCreated(holder: SurfaceHolder) {
                    holder?.let {
                        drawOverlay(
                            it,
                            DESIRED_HEIGHT_CROP_PERCENT,
                            DESIRED_WIDTH_CROP_PERCENT
                        )
                    }
                }
            })
        }
    }

    /**
     * Set up the Camera
     */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                throw IllegalStateException("Camera initialization failed.", e.cause!!)
            }
            // Build and bind the camera use cases
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider){
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // preview use case
        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        // image analyzer use case
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(
                    scopedExecutor,
                    TextAnalyzer(
                        requireContext(),
                        lifecycle,
                        cameraExecutor,
                        viewModel.sourceText,
                        viewModel.imageCropPercentages
                    )
                )
            }

        viewModel.sourceText.observe(viewLifecycleOwner, Observer { binding.srcText.text = it })
        viewModel.imageCropPercentages.observe(viewLifecycleOwner,
            Observer { drawOverlay(binding.overlay.holder, it.first, it.second) })

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: IllegalStateException) {
            Log.e(TAG, "Use case binding failed. This must be running on main thread.", exc)
        }
    }

    /**
     * draw with overlay
     * */
    private fun drawOverlay(
        holder: SurfaceHolder,
        heightCropPercent: Int,
        widthCropPercent: Int
    ) {
        val canvas = holder.lockCanvas()
        val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE
        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = Color.WHITE
        outlinePaint.strokeWidth = 4f
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        // Set rect centered in frame
        val rectTop = surfaceHeight * heightCropPercent / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - heightCropPercent / 2 / 100f)
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, outlinePaint
        )
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 50F

        val overlayText = getString(R.string.overlay_help)
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText(getString(R.string.overlay_help), textX, textY, textPaint)
        holder.unlockCanvasAndPost(canvas)
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by comparing absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE))
            <= abs(previewRatio - ln(RATIO_16_9_VALUE))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { setUpCamera() }
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } // OnRequestPermissionsResult

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    } // AllPermissionsGranted
}