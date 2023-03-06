package com.example.shifttestbinlist

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.core.text.underline
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shifttestbinlist.network.BinInfo
import com.example.shifttestbinlist.network.CardCountry
import com.example.shifttestbinlist.ui.main.BinApiStatus
import com.example.shifttestbinlist.ui.main.BinListAdapter
import java.util.*

// sets capitalized text or "?" if it's null
@BindingAdapter("nullableText")
fun bindNullableText(textView: TextView, text: String?) {
    text?.let {
        // capitalize the first letter
        textView.text = text.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.ROOT)
            else it.toString()
        }
        return
    }
    // if text is null, set "?"
    textView.text = "?"
}

// sets "Yes / No" with correct decoration
@BindingAdapter("booleanText")
fun bindBoolean(textView: TextView, booleanText: Boolean?) {
    booleanText?.let {
        textView.text = if (it)
            // "Yes" stays black in "Yes / No"
            SpannableStringBuilder()
                .append(textView.resources.getString(R.string.yes))
                .color(Color.GRAY) {
                    append(" / ").append(textView.resources.getString(R.string.no)) }
        else
            // "No" stays black in "Yes / No"
            SpannableStringBuilder()
                .color(Color.GRAY) {
                    append(textView.resources.getString(R.string.yes)).append(" / ") }
                .append(textView.resources.getString(R.string.no))
        return
    }
    // if text is null, set "Yes / No" in Gray
    textView.text = SpannableStringBuilder().color(Color.GRAY) {
            append(textView.resources.getString(R.string.yes_no)) }
}

// sets "Debit / Credit" with correct decoration
@BindingAdapter("cardTypeText")
fun bindCardTypeText(textView: TextView, text: String?) {
    text?.let {
        textView.text = if (it == "debit")
            // "Debit" stays black in "Debit / Credit"
            SpannableStringBuilder()
                .append(textView.resources.getString(R.string.debit))
                .color(Color.GRAY) {
                    append(" / ").append(textView.resources.getString(R.string.credit)) }
        else
            // "Credit" stays black in "Debit / Credit"
            SpannableStringBuilder()
                .color(Color.GRAY) {
                    append(textView.resources.getString(R.string.debit)).append(" / ") }
                .append(textView.resources.getString(R.string.credit))
        return
    }
    // if text is null, set "Debit / Credit" in Gray
    textView.text = SpannableStringBuilder().color(Color.GRAY) {
        append(textView.resources.getString(R.string.debit_credit)) }
}

// sets country and its emoji or "?" if country name is null
@BindingAdapter("countryText")
fun bindCountryText(textView: TextView, country: CardCountry?) {
    country?.name?.let {
        textView.text = textView.resources.getString(
            R.string.country_text,
            // set emoji or "" if it's null
            country.emoji ?: "",
            // set country name
            it)
        return
    }
    // if country or its name is null, set "?"
    textView.text = "?"
}

// sets latitude and latitude if it's available, if item is null the "?" is set
@BindingAdapter("countryCoordinatesText")
fun bindCountryCoordinatesText(textView: TextView, country: CardCountry?) {
    country?.let {
        // set available coordinates in (latitude: %s, longitude: %s)
        val coordinatesText = textView.resources.getString(
            // coordinates can be either int or float, so the type check is made
            R.string.country_coordinates,
            // set latitude or "?" if it's null
            country.latitude?.let {
                // if float is whole cast it to int else leave as is
                if (it.compareTo(it.toInt()) == 0) it.toInt() else it
            } ?: "?",
            // set longitude or "?" if it's null
            country.latitude?.let {
                // if float is whole cast it to int else leave as is
                if (it.compareTo(it.toInt()) == 0) it.toInt() else it
            } ?: "?")
        /* there is click listener in BinFragment that opens maps with coordinates provided.
        The code below makes text decorated and clickable only if both latitude and longitude
        are available, because there's no point of opening maps if one of them is null */
        textView.text = if (!coordinatesText.contains("?")) {
            // make text decorated and clickable
            textView.isClickable = true
            SpannableStringBuilder().underline {
                color(
                    Color.rgb(1,135,134)
                     /* alternatively can use deprecated method:
                     textView.resources.getColor(R.color.teal_700)
                     not using newer getColor(int, Resources.Theme)
                     as it requires api 23, current min api is 21 */
                ) { append(coordinatesText) } }
        } else {
            // leave text as is and make it not clickable
            textView.isClickable = false
            coordinatesText
        }
        return
    }
    // if country is null, set "?" in (latitude: %s, longitude: %s)
    textView.text = textView.resources.getString(
        R.string.country_coordinates, "?", "?")
    // make text not clickable
    textView.isClickable = false
}

// sets all info about bank or "?" if this info is null
@BindingAdapter("bankInfoText")
fun bindBankInfoText(textView: TextView, bank: Map<String,String>?) {
    bank?.let {
        textView.text = bank.values.joinToString(", ")
        return
    }
    // if bank is null, set "?"
    textView.text = "?"
}

// updates the data shown in the RecyclerView
@BindingAdapter("binListData")
fun bindRecyclerView(recyclerView: RecyclerView,
                     data: List<BinInfo>?) {
    val adapter = recyclerView.adapter as BinListAdapter
    adapter.submitList(data)
}

// displays the BinApiStatus of the network request in the image and text view
@BindingAdapter("binApiStatus", "errorMessage")
fun bindApiStatus(rootLinearLayout: LinearLayout, status: BinApiStatus?, error: String?) {

    val binInfoLayout = rootLinearLayout.findViewById<ConstraintLayout>(R.id.bin_info_layout)
    val statusImage = rootLinearLayout.findViewById<ImageView>(R.id.status_image)
    val errorMessage = rootLinearLayout.findViewById<TextView>(R.id.error_message)

    when (status) {
        // show loading animation
        BinApiStatus.LOADING -> {
            binInfoLayout.visibility = View.GONE
            errorMessage.visibility = View.GONE
            statusImage.visibility = View.VISIBLE
        }
        // show bin information
        BinApiStatus.DONE -> {
            binInfoLayout.visibility = View.VISIBLE
            errorMessage.visibility = View.GONE
            statusImage.visibility = View.GONE
        }
        // show error message
        BinApiStatus.ERROR  -> {
            binInfoLayout.visibility = View.GONE
            errorMessage.visibility = View.VISIBLE
            statusImage.visibility = View.GONE
            errorMessage.text = when (error) {
                "HTTP 400 " -> "Malformed request: BIN must contain only digits."
                "HTTP 404 " -> "No information found for the provided BIN."
                "HTTP 429 " -> "Too many requests, try again later."
                else -> "Connection error."
            }
        }
        // status is null only at start off the app, no changes needed in this case
        // empty bin information layout is shown
        null -> { }
    }
}
