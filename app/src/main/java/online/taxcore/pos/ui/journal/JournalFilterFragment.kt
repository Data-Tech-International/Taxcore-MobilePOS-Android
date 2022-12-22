package online.taxcore.pos.ui.journal

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.pawegio.kandroid.longToast
import kotlinx.android.synthetic.main.journal_filters_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.data.local.JournalManager
import online.taxcore.pos.enums.InvoiceType
import online.taxcore.pos.enums.TransactionType
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.utils.hideKeyboard
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ValidFragment")
class JournalFilterFragment : Fragment() {

    private val TEMPLATE_DATE = "MMM dd yyyy HH:mm"
    private var datePickerDialogFrom: DatePickerDialog? = null
    private var datePickerDialogTo: DatePickerDialog? = null
    private var timePickerDialog: TimePickerDialog? = null
    private var timePickerDialogFrom: TimePickerDialog? = null

    private var dateFrom: Date? = null
    private var dateTo: Date? = null
    private var isFromDateSelected = false
    private var isToDateSelected = false

    private var updatedFieldsMap = mutableMapOf<String, Boolean>()

    private var confirmFilterItem: MenuItem? = null

    companion object {
        private var stringDateFrom = ""
        private var stringDateTo = ""
        private var stringTimeFrom = "00:00"
        private var stringTimeTo = "23:59"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.journal_filters_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initField()

        setOnClickListeners()
        setOnChangeListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.filter, menu)

        confirmFilterItem = menu.findItem(R.id.actionConfirmFilter)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.actionConfirmFilter -> {
            takeSearchField()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun initField() {
        if (stringDateFrom.isNotEmpty()) {
            dateFrom = getDateFrom()
            journalFilterFromDateLabel.text = stringDateFrom
            isFromDateSelected = true
        }

        if (stringTimeFrom != "00:00") {
            journalFilterFromTimeLabel.text = stringTimeFrom
        }

        if (stringDateTo.isNotEmpty()) {
            dateTo = getDateTo()
            journalFilterToDateLabel.text = stringDateTo
            isToDateSelected = true
        }

        if (stringTimeTo != "23:59") {
            journalFilterToTimeLabel.text = stringTimeTo
        }

        with(JournalManager) {
            journalFilterBuyerTinEditText.setText(buyerTin)
            journalFilterInvoiceNumberInput.setText(invoice)
            journalFilterInvoiceTypeSpinner.setSelection(invoiceTypePosition)
            journalFilterTransactionTypeSpinner.setSelection(transactionTypePosition)
        }
    }

    private fun setOnClickListeners() {

        journalFilterFromDateLabel.setOnClickListener {
            showFromCalendar()
        }

        journalFilterFromTimeLabel.setOnClickListener {
            if (isFromDateSelected) {
                showFromTime()
            } else {
                longToast(getString(R.string.toast_fill_date_from))
            }
        }

        journalFilterToDateLabel.setOnClickListener {
            showToCalendar()
        }

        journalFilterToTimeLabel.setOnClickListener {
            if (isToDateSelected) {
                showToTime()
            } else {
                longToast(getString(R.string.toast_fill_date_to))
            }
        }

        journalFilterResetButton.setOnClickListener {
            resetAll()
        }
    }

    private fun setOnChangeListeners() {
        journalFilterBuyerTinEditText.onTextChanged {
            updatedFieldsMap["buyerTin"] = it.isNotEmpty()
            validateFilters()
        }

        journalFilterInvoiceNumberInput.onTextChanged {
            updatedFieldsMap["invoiceNo"] = it.isNotEmpty()
            validateFilters()
        }

        journalFilterInvoiceTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    updatedFieldsMap["invoiceType"] = false
                    validateFilters()
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updatedFieldsMap["invoiceType"] = position != 0
                    validateFilters()
                }
            }

        journalFilterTransactionTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    updatedFieldsMap["transactionType"] = false
                    validateFilters()
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updatedFieldsMap["transactionType"] = position != 0
                    validateFilters()
                }
            }
    }

    private fun hasFilterInput() =
        stringDateFrom.isNotEmpty() or stringDateTo.isNotEmpty() or isFormUpdated()

    private fun isFormUpdated(): Boolean = updatedFieldsMap.any { it.value }

    private fun validateFilters() {
        val hasValidInput = hasFilterInput()

        confirmFilterItem?.isEnabled = hasValidInput
    }

    private fun resetAll() {
        dateTo = null
        dateFrom = null
        isToDateSelected = false
        isFromDateSelected = false
        stringDateFrom = ""
        stringDateTo = ""
        stringTimeFrom = "00:00"
        stringTimeTo = "23:59"

        journalFilterBuyerTinEditText.setText("")
        journalFilterInvoiceNumberInput.setText("")

        journalFilterInvoiceTypeSpinner.setSelection(0)
        journalFilterTransactionTypeSpinner.setSelection(0)

        datePickerDialogFrom = null
        datePickerDialogTo = null
        timePickerDialog = null
        timePickerDialogFrom = null

        journalFilterFromDateLabel.text = activity?.resources?.getString(R.string.select_date)
        journalFilterFromTimeLabel.text = activity?.resources?.getString(R.string.select_time)

        journalFilterToDateLabel.text = activity?.resources?.getString(R.string.select_date)
        journalFilterToTimeLabel.text = activity?.resources?.getString(R.string.select_time)

        JournalManager.resetFilter()
    }

    private fun showToTime() {
        val cal = Calendar.getInstance()

        getDateTo()?.let {
            cal.timeInMillis = it.time
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        timePickerDialog = TimePickerDialog(context, timeSetListenerTo, hour, minute, true)
        timePickerDialog?.show()
    }

    private fun showToCalendar() {
        val cal = Calendar.getInstance()

        dateTo?.let {
            cal.timeInMillis = it.time
        }

        datePickerDialogTo = DatePickerDialog(
            context ?: return, datePickerListenerTo,
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            dateFrom?.let {
                datePicker.minDate = it.time
            }
            show()
        }
    }

    private fun showFromTime() {
        val cal = Calendar.getInstance()

        getDateFrom()?.let {
            cal.timeInMillis = it.time
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        timePickerDialogFrom = TimePickerDialog(context, timeSetListenerFrom, hour, minute, true)
        timePickerDialogFrom?.show()
    }

    private fun showFromCalendar() {
        val cal = Calendar.getInstance()

        dateFrom?.let {
            cal.timeInMillis = it.time
        }

        datePickerDialogFrom = DatePickerDialog(
            context ?: return, datePickerListenerFrom,
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            dateTo?.let {
                datePicker.maxDate = it.time
            }
            show()
        }
    }

    private fun takeSearchField() {
        with(JournalManager) {
            buyerTin = journalFilterBuyerTinEditText.text.toString()
            invoice = journalFilterInvoiceNumberInput.text.toString()
            transactionType = getTransactionType()
            invoiceType = getInvoiceType()
            transactionTypePosition = journalFilterTransactionTypeSpinner.selectedItemPosition
            invoiceTypePosition = journalFilterInvoiceTypeSpinner.selectedItemPosition
            dateFrom = getDateFrom()
            dateTo = getDateTo()
        }

        val args = Bundle().apply { putBoolean("isSearch", true) }

        val journalResultsFragment = JournalListFragment()
        journalResultsFragment.arguments = args

        baseActivity()?.hideKeyboard()

        replaceFragment(R.id.baseFragment, journalResultsFragment)
    }

    private fun getDateTo(): Date? {
        return try {
            val date = "$stringDateTo $stringTimeTo"
            SimpleDateFormat(TEMPLATE_DATE, Locale.US).parse(date)
        } catch (e: ParseException) {
            null
        }
    }

    private fun getDateFrom() =
        try {
            val date = "$stringDateFrom $stringTimeFrom"
            SimpleDateFormat(TEMPLATE_DATE, Locale.US).parse(date)
        } catch (e: ParseException) {
            null
        }

    private fun getInvoiceType(): String = when (val selectedInvoiceTypeIndex =
        journalFilterInvoiceTypeSpinner.selectedItemPosition) {
        0 -> ""
        else -> InvoiceType.values()[selectedInvoiceTypeIndex - 1].value
    }

    private fun getTransactionType() = when (val selectedTransactionTypeIndex =
        journalFilterTransactionTypeSpinner.selectedItemPosition) {
        0 -> ""
        else -> TransactionType.values()[selectedTransactionTypeIndex - 1].value
    }


    private val datePickerListenerFrom =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthOfYear)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            dateFrom = cal.time
            isFromDateSelected = true

            val formatDate = formatDate(cal.time)
            journalFilterFromDateLabel?.text = formatDate

            stringDateFrom = formatDate

            validateFilters()
        }

    private val datePickerListenerTo =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthOfYear)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            dateTo = cal.time
            isToDateSelected = true

            val formatDate = formatDate(cal.time)
            journalFilterToDateLabel.text = formatDate

            stringDateTo = formatDate

            validateFilters()
        }

    private val timeSetListenerFrom = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)

        val formatTime = formatTime(cal.time)
        journalFilterFromTimeLabel?.text = formatTime

        stringTimeFrom = formatTime
    }

    private val timeSetListenerTo = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)

        val formatTime = formatTime(cal.time)
        journalFilterToTimeLabel.text = formatTime

        stringTimeTo = formatTime
    }

    private fun formatDate(date: Date) =
        SimpleDateFormat("MMM dd yyyy", Locale.US).format(date)

    private fun formatTime(date: Date) =
        SimpleDateFormat("HH:mm", Locale.US).format(date)
}
