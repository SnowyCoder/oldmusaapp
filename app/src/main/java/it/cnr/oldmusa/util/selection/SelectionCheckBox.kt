package it.cnr.oldmusa.util.selection

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import it.cnr.oldmusa.R
import kotlinx.android.parcel.Parcelize


class SelectionCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.checkboxStyle
) : AppCompatCheckBox(context, attrs, defStyleAttr) {

    private var selection: SelectionType = SelectionType.UNSELECTED

    /**
     * Holds a reference to the listener set by a client, if any.
     */
    private var clientListener: OnCheckedChangeListener? = null

    /**
     * This flag is needed to avoid accidentally changing the current [.state] when
     * [.onRestoreInstanceState] calls [.setChecked]
     * evoking our [.privateListener] and therefore changing the real state.
     */
    private var restoring = false

    val selectionState: SelectionType
        get() = selection

    init {
        super.setOnCheckedChangeListener { _, _ -> this.onClick() }
        updateDrawable()
    }


    override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        // we never truly set the listener to the client implementation, instead we only hold
        // a reference to it and evoke it when needed.
        this.clientListener = listener
        super.setOnCheckedChangeListener { _, _ -> this.onClick() }
    }

    private fun onClick() {
        val newState = when(selection) {
            SelectionType.SELECTED, SelectionType.PARTIAL -> SelectionType.UNSELECTED
            SelectionType.UNSELECTED -> SelectionType.SELECTED
        }
        setSelectionState(newState)
    }

    fun setSelectionState(state: SelectionType) {
        if (this.selection != state && !this.restoring) {
            this.selection = state

            this.clientListener?.onCheckedChanged(this, state != SelectionType.UNSELECTED)
            updateDrawable()
        }
    }

    private fun updateDrawable() {
        val drawable = when (selection) {
            SelectionType.SELECTED -> R.drawable.ic_check_box_black_24dp
            SelectionType.UNSELECTED -> R.drawable.ic_check_box_outline_blank_black_24dp
            SelectionType.PARTIAL -> R.drawable.ic_indeterminate_check_box_black_24dp
        }
        setButtonDrawable(drawable)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(selection, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        this.restoring = true
        val saved = state as SavedState

        super.onRestoreInstanceState(saved.superParcel)
        this.selection = saved.selection
        updateDrawable()
        requestLayout()

        this.restoring = false
    }

    @Parcelize
    private data class SavedState(val selection: SelectionType, val superParcel: Parcelable?) : Parcelable

}
