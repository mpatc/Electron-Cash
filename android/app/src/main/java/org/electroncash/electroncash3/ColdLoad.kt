package org.electroncash.electroncash3

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.load.*
import kotlinx.android.synthetic.main.main.*

// This provides a dialog to allow users to input a sting, which is then broadcast
// on the bitcoin cash network. Strings are not validated, so invalid user inputs fail
// gracefully and nothing happens. Valid transactoin quickly show up in transactions.

class ColdLoadDialog : AlertDialogFragment() {

    class Model : ViewModel() {}

//    val coldloadDialog by lazy { targetFragment as ColdLoadDialog }
    val model: Model by viewModels()


    init {
        if (daemonModel.wallet!!.callAttr("is_watching_only").toBoolean()) {
            throw ToastException(R.string.this_wallet_is)
        } else if (daemonModel.wallet!!.callAttr("get_receiving_addresses")
                        .asList().isEmpty()) {
            // At least one receiving address is needed to call wallet.dummy_address.
            throw ToastException(
                    R.string.electron_cash_is_generating_your_addresses__please_wait_)
        }
    }

    override fun onBuildDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.load_unbroadcasted)
                .setView(R.layout.load)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.qr_code, null)
                .setPositiveButton(android.R.string.ok, null)
    }
    // This waits for user to click ok, and then broadcasts the user input which
    // hopefully its a hexadecimal representation of a signed tx, in which case it gets
    // broadcast in onOK() todo user validate input
    override fun onShowDialog() {
        super.onShowDialog()
        val ourClipboard = getSystemService(ClipboardManager::class)



        etTransaction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currenttext = etTransaction.text
                //checks if text is blank. further validations can be added here
                 dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = when { currenttext.isNotBlank() -> {true} else -> false }
            }

        })
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onOK() }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { scanQR(this) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        btnPaste.isEnabled = when { !ourClipboard.hasPrimaryClip() -> { false } else -> {true}}
        btnPaste.setOnClickListener {
            val clipdata = ourClipboard.primaryClip
            val cliptext = clipdata!!.getItemAt(0)
            etTransaction.setText(cliptext.text)

        }
    }


    // Receives the result of a QR scan.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            onScan(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onScan(uri: String) {

        etTransaction.setText(uri)
    }

    fun onOK() {
        // try to send user input to network to be broadcast,
        // this should work even if tx is not vaild transaction, but nothing happens
        try {
            val tx = etTransaction.text.toString()
            daemonModel.network.callAttr("broadcast_transaction", tx)
        } catch (e: ToastException) {
            e.show()
        }
        toast(R.string.the_string, Toast.LENGTH_LONG)
        dismiss()
        //send to transactions
        // because if they just broadcasted one, that's probably where they want to go
        (activity as MainActivity).navBottom.selectedItemId = R.id.navTransactions

    }
}