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

// This provides a dialog to allow users to input a string, which is then broadcast
// on the bitcoin cash network. Strings are not validated,
// but broadcast_transaction2 should throw error which is toasted.
// Valid transaction quickly show up in transactions.

class ColdLoadDialog : AlertDialogFragment() {

    class Model : ViewModel() {}

    val model: Model by viewModels()

    override fun onBuildDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.load_transaction)
                .setView(R.layout.load)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.qr_code, null)
                .setPositiveButton(R.string.send, null)
    }

    override fun onShowDialog() {
        super.onShowDialog()
        val ourClipboard = getSystemService(ClipboardManager::class)

        etTransaction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currenttext = etTransaction.text
                //checks if text is blank. further validations can be added here
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = currenttext.isNotBlank()
            }

        })
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onOK() }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { scanQR(this) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
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
            etTransaction.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    fun onOK() {
        // try to send user input to network to be broadcast,
        // this should work even if tx is not vaild transaction, but nothing happens
//        try {
//            val tx = etTransaction.text.toString()
//            daemonModel.network.callAttr("broadcast_transaction", tx)
//        } catch (e: ToastException) {
//            e.show()
//        }

        val tx = etTransaction.text.toString()
        val result = daemonModel.network.callAttr("broadcast_transaction", tx).asList()
        val success = result.get(0).toBoolean()
        if (success) {
            toast(R.string.the_string, Toast.LENGTH_LONG)
            dismiss()
            //send to transactions
            // because if they just broadcasted one, that's probably where they want to go
            (activity as MainActivity).navBottom.selectedItemId = R.id.navTransactions
        } else {
            var message = result.get(1).toString()
            val reError = Regex("^error: (.*)")
            if (message.contains(reError)) {
                message = message.replace(reError, "$1")
            }
            toast(message)
        }
    }
}