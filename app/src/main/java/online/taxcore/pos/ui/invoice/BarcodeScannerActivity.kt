package online.taxcore.pos.ui.invoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class BarcodeScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null

    companion object {
        fun start(activity: AppCompatActivity, requestCode: Int) {
            val intent = Intent(activity, BarcodeScannerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
        }
    }

    override fun handleResult(result: Result?) {

        finishWithResult(result?.text.orEmpty())
    }

    private fun finishWithResult(barcode: String) {
        val intent = Intent().apply {
            putExtra(InvoiceFragment.BARCODE_EAN_EXTRA, barcode)
        }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
        mScannerView?.setAutoFocus(true)
        //mScannerView?.flash = true
        mScannerView?.setAspectTolerance(0.5f)
    }

    override fun onResume() {
        super.onResume()
        mScannerView?.setResultHandler(this)
        mScannerView?.startCamera()
    }

    override fun onPause() {
        super.onPause()
        mScannerView?.stopCamera()
    }
}
