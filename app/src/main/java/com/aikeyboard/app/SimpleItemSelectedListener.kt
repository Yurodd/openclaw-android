package com.aikeyboard.app

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val onItemSelectedCallback: (Int) -> Unit
) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onItemSelectedCallback(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
}
