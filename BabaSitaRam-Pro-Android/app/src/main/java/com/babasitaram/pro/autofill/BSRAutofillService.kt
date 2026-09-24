package com.babasitaram.pro.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillRequest
import android.service.autofill.SaveRequest

class BSRAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        callback.onSuccess(null)
    }
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}
