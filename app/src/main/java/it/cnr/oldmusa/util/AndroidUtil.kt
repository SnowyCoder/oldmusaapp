package it.cnr.oldmusa.util

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.charter.CustomLineChart
import it.cnr.oldmusa.charter.CustomViewPortHandler
import java.lang.ref.WeakReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object AndroidUtil {
    val isoSimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val navControllerMBackStack = NavController::class.java.getDeclaredField("mBackStack").apply {
        isAccessible = true
    }
    private val viewToFilterData = WeakHashMap<SwipeRefreshLayout, RefreshFilterData>()


    fun Any.toNullableString(): String? {
        val str = this.toString()
        if (str.isBlank()) return null
        return str
    }

    private fun addLoadingBar(context: Activity, isDimActive: Boolean = false): () -> Unit {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)


        val layout = RelativeLayout(context)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        val params = RelativeLayout.LayoutParams(400, 400)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)

        layout.addView(progressBar, params)

        dialog.setContentView(layout, RelativeLayout.LayoutParams(-1, -1))

        dialog.window!!.attributes = dialog.window!!.attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        if (!isDimActive) {
            dialog.window!!.setDimAmount(0f)
        }
        dialog.show()

        return {
            dialog.dismiss()
        }
    }

    fun AutoCompleteTextView.alwaysComplete() {
        this.threshold = 1
        this.setOnTouchListener { v, event -> if (this.text.isEmpty()) this.showDropDown(); false }
        val textView = this
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.isEmpty() == true) {
                    Handler().postDelayed({
                        //manually show drop down
                        textView.showDropDown()
                    }, 100) // with 100 millis of delay
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    fun View?.onScroll(callback: (x: Int, y: Int) -> Unit) {
        var oldX = 0
        var oldY = 0
        this?.viewTreeObserver?.addOnScrollChangedListener {
            if (oldX != scrollX || oldY != scrollY) {
                callback(scrollX, scrollY)
                oldX = scrollX
                oldY = scrollY
            }
        }
    }

    fun SwipeRefreshLayout.addRefreshFilter(): RefreshFilter {
        var parent = viewToFilterData[this]

        if (parent == null) {
            parent = RefreshFilterData(WeakReference(this))
            viewToFilterData[this] = parent
        }

        return  RefreshFilter(parent)
    }

    fun SwipeRefreshLayout.linkToList(list: AbsListView) {
        val filter = addRefreshFilter()
        list.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                filter.enabled = !if (view == null || view.childCount == 0) {
                    true// There is no view, or else the view is empty, we should allow refreshing
                } else {
                    // We should allow refreshing only if the first element in the adapter is at the
                    // top, note that AbsListView.getChildAt recycles views so you also need to
                    // check if the firsVisibleItem is really the first as the 0th view can be
                    // reused while scrolling.
                    firstVisibleItem == 0 && view.getChildAt(0).top >= 0
                }
            }

            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            }
        })
    }

    fun SwipeRefreshLayout.linkToList(list: RecyclerView) {
        val filter = addRefreshFilter()
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                filter.enabled = !if (view.childCount == 0) {
                    true// There is no view, or else the view is empty, we should allow refreshing
                } else {
                    dy >= 0
                }
            }
        })
    }

    fun SwipeRefreshLayout.linkToChart(graph: CustomLineChart) {
        val filter = addRefreshFilter()
        val handler = graph.viewPortHandler as CustomViewPortHandler
        handler.translationListener = {
            filter.enabled = handler.canScrollUp
        }
    }

    fun parseIsoDate(calendar: Calendar, raw: String): Boolean {
        try {
            calendar.timeInMillis = isoSimpleDateFormat.parse(raw).time
        } catch (e: ParseException) {
            return false
        }

        return true
    }

    fun EditText.dateSelectPopupSetup() {
        this.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) return@setOnFocusChangeListener
            val calendar = Calendar.getInstance()
            
            parseIsoDate(calendar, this.text.toString())
            // Ignore failure, use today as default

            val dialog = DatePickerDialog(context!!, { view, year, monthOfYear, dayOfMonth ->
                calendar.set(year, monthOfYear, dayOfMonth)
                this.setText(isoSimpleDateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            dialog.show()
        }
    }

    /**
     * Displays a loading bar while the query is running.
     * This also disables user interaction
     */
    fun <P : Any> AsyncUtil.RawTask<P>.useLoadingBar(context: Activity, isDimActive: Boolean = false): AsyncUtil.RawTask<P> {
        this.onDone(addLoadingBar(context, isDimActive))
        return this
    }

    fun <P : Any> AsyncUtil.RawTask<P>.useLoadingBar(context: Fragment, isDimActive: Boolean = false): AsyncUtil.RawTask<P> {
        return useLoadingBar(context.activity!!, isDimActive)
    }

    /**
     * Displays a loading bar while the query is running.
     * This also disables user interaction
     */
    fun <P : Any> GraphQlUtil.RawCall<P>.useLoadingBar(context: Activity, isDimActive: Boolean = false): GraphQlUtil.RawCall<P> {
        this.onDone(addLoadingBar(context, isDimActive))
        return this
    }

    fun <P : Any> GraphQlUtil.RawCall<P>.useLoadingBar(context: Fragment, isDimActive: Boolean = false): GraphQlUtil.RawCall<P> {
        return useLoadingBar(context.activity!!, isDimActive)
    }

    fun Fragment.getBackStackEntry(index: Int): NavBackStackEntry? {
        val backStack = navControllerMBackStack.get(findNavController()) as Deque<NavBackStackEntry>
        return backStack.descendingIterator().asSequence().drop(index).firstOrNull()
    }

    class RefreshFilterData(val layout: WeakReference<SwipeRefreshLayout>) {
        var count = 0

        fun increment() {
            if (count++ == 0) layout.get()?.isEnabled = false
        }

        fun decrement() {
            if (--count == 0) layout.get()?.isEnabled = true
        }
    }

    class RefreshFilter(private val parent: RefreshFilterData) {
        private var _enabled = false

        var enabled: Boolean
        get() = _enabled
        set(value) {
            if (value == enabled) return
            if (value) parent.increment()
            else parent.decrement()
            _enabled = value
        }
    }
}