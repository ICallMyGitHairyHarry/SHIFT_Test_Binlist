package com.example.shifttestbinlist.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shifttestbinlist.R
import com.example.shifttestbinlist.databinding.FragmentBinBinding
import com.example.shifttestbinlist.network.BinApi
import java.util.*


class BinFragment : Fragment() {

    private val viewModel: BinViewModel by viewModels()

    private lateinit var binding: FragmentBinBinding

    private var changedFromItemClick = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // инициализация certRetrofit
        context?.let { BinApi.createCertRetrofit(it) }

        context?.let { viewModel.loadSavedBinList(it.cacheDir) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBinBinding.inflate(inflater)

        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        // Giving the binding access to the BinViewModel
        binding.viewModel = viewModel

        // Set the adapter to RecyclerView and pass BinListener object
        // with a function that will be invoked when the Item is clicked
        binding.binsRecyclerView.adapter = BinListAdapter(
            BinListener({ binItem ->
                // set the item in view model
                viewModel.onBinItemClicked(binItem)
                // set the flag that the text in edit text is changed programmatically
                changedFromItemClick = true
                // show item id in input edit text
                binding.binEditText.setText(binItem.id)
            }) { binItem ->
                // create confirmation window
                /*AlertDialog.Builder(context)
                    .setTitle(getString(R.string.remove_confirm_title, binItem.id))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        // remove item in view model
                        viewModel.onBinItemHold(binItem)
                    }
                    .setNegativeButton(R.string.no, null).show()*/
                Toast.makeText(context,
                    "Something should happen...", Toast.LENGTH_SHORT).show()
                // P.s. explanation on this code is in viewModel.onBinItemHold
                true
            }
        )

        binding.binEditText.setOnKeyListener { view, keyCode, keyEvent ->
            handleKeyEvent(view, keyCode, keyEvent, binding.binEditText.text.toString())
        }

        binding.binEditText.doAfterTextChanged {
            // if text is changed from the code, not by the user
            if (changedFromItemClick) {
                changedFromItemClick = false
            // else text is changed by the user
            // if user entered 4..8 digits
            } else if (checkUserInput(it.toString())) {
                // make request
                viewModel.getBinInfo(it.toString())
            }
        }

        binding.countryCoordinates.setOnClickListener {
            val uri: String = String.format(
                Locale.ENGLISH, "geo:%f,%f",
                viewModel.binData.value?.country?.latitude,
                viewModel.binData.value?.country?.longitude,
            )
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            context?.startActivity(intent)
        }

        return binding.root
    }

    // action when enter button is pressed
    private fun handleKeyEvent(view: View, keyCode: Int,
                               event: KeyEvent, userInput: String): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER
            && event.action == KeyEvent.ACTION_UP) {
            // if user entered 4+ digits
            if (checkUserInput(userInput)) {
                // save next request to history
                viewModel.activateHistoryFlag()
                // make request
                viewModel.getBinInfo(userInput)
                // Hide the keyboard
                val inputMethodManager =
                    context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                return true
            }
        }
        return false
    }

    private fun checkUserInput(input: String): Boolean {
        return if (input.length in 4..8) {
            setErrorTextField(false)
            true
        } else {
            setErrorTextField(true)
            false
        }
    }

    // Sets and resets the text field error status
    private fun setErrorTextField(error: Boolean) {
        if (error) {
            binding.binInputLayout.isErrorEnabled = true
            binding.binInputLayout.error = getString(R.string.insufficient_digits)
        } else {
            binding.binInputLayout.isErrorEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        context?.let { viewModel.saveBinList(it.cacheDir) }
    }

}