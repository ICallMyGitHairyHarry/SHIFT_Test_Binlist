package com.example.shifttestbinlist.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shifttestbinlist.network.BinApi
import com.example.shifttestbinlist.network.BinInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.net.ssl.SSLHandshakeException

private const val TAG = "VIEW MODEL"

enum class BinApiStatus { LOADING, ERROR, DONE }

class BinViewModel : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<BinApiStatus>()
    // The external immutable LiveData for the request status
    val status: LiveData<BinApiStatus> = _status

    // single bin request item
    private val _binData = MutableLiveData<BinInfo?>()
    val binData: LiveData<BinInfo?> = _binData

    // list of bin request history used in recyclerView
    private var _binsList = MutableLiveData<MutableList<BinInfo>>(mutableListOf())
    val binsList: LiveData<MutableList<BinInfo>> = _binsList

    // history flag, when true the request is added to history
    private var addToHistoryFlag = false

    // error message from request
    private var _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // создание certRetrofitService
        BinApi.createCertRetrofitService()
    }

    // get bin data from the server
    fun getBinInfo(digits: String) {
        viewModelScope.launch {
            _status.value = BinApiStatus.LOADING
            try {
                // запрос через certRetrofitService
                _binData.value = BinApi.certRetrofitService!!.getBin(digits)
                _status.value = BinApiStatus.DONE
                Log.d(TAG,"Secure connection")
            } catch (e: SSLHandshakeException) {
                // присвоение null, чтобы переменная была доступна для garbage collection
                BinApi.certRetrofitService = null
                // запрос через unsafeRetrofitService
                // unsafeRetrofitService инициализируется при первом обращении
                _binData.value = BinApi.unsafeRetrofitService.getBin(digits)
                _status.value = BinApiStatus.DONE
                Log.d(TAG, "Insecure connection: unsafe okhttp client")
            } catch (e: Exception) {
                _status.value = BinApiStatus.ERROR
                _binData.value = null
                _errorMessage.value = e.message
                Log.d(TAG, "Failure: ${e.message}")
            }
            if (addToHistoryFlag) {
                addBinToHistory(digits)
                addToHistoryFlag = false
            }
        }
    }

    fun activateHistoryFlag () {
        addToHistoryFlag = true
    }

    /*
    Раньше эта функция напрямую вызывалась из фрагмента,
    но таким образом она срабатывала раньше, чем приходил ответ от сервера,
    что приводило к неправильному отображению данных:
    в историю добавлялся предыдущий запрос, что особенно заметно при неуспешном запросе.
    Поэтому теперь функция вызывается через addToHistoryFlag.
    */
    private fun addBinToHistory (id: String) {
        _binData.value?.let {
            it.id = id
            _binsList.value?.add(it)
        }
    }

    // invoked when bin item in recycler view is clicked
    fun onBinItemClicked(binItem: BinInfo) {
        // set the binData object, so its info is displayed on the screen
        _binData.value = binItem
        // set the status to DONE
        _status.value = BinApiStatus.DONE
    }

    // invoked when bin item in recycler view is long clicked
    fun onBinItemHold(binItem: BinInfo) {
         /* При зажатии на элементе recycler view, он должен удаляться из списка.
         Так и происходит, НО:
         1) Если потом добавляешь элементы, то добавляются не новые,
         а те, что раньше были удалены! _binsList.value изменяется корректно,
         возможно проблема какая-то проблема с Recycler view.
         Может notifyItemRemoved или notifyDataSetChanged
         могут решить проблему, но сомневаюсь.
         Upd: в каких-то случаях кидает Invalid view holder adapter positionBinItemViewHolder
         2) Recycler view не обновляется сразу после удаления, а когда происходит
         какое-то изменение экрана, например, открытие клавиатуры.
         Пытался делать extension functions с this.value = this.value, сохранять
         список в другую переменную, удалять элемент из неё и переприсваивать обратно
         - не работает. Однако, когда просто присваиваю mutableListOf() - обновляется сразу.
         Я не смог пофиксить эти баги и найти их в интернете,
         поэтому код сейчас отключен.*/
        _binsList.value?.remove(binItem)
        Log.d(TAG, _binsList.value.toString())
    }

    // save _binsList.value to text file
    fun saveBinList (path: File) {
        /* Знаю, что лучше делать через Room, но не было времени разобраться */
        // create moshi adapter
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val jsonAdapterWrite = moshi.adapter<MutableList<BinInfo>>(
            Types.newParameterizedType(MutableList::class.java, BinInfo::class.java))
        // serialize _binsList.value to JSON string
        val stringToFile = jsonAdapterWrite.toJson(_binsList.value)
        // write to file
        try {
            val writer = FileOutputStream(File(path, "binsList.txt"))
            writer.write(stringToFile.toByteArray())
            writer.close()
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }

    }

    // load _binsList.value from text file
    fun loadSavedBinList (path: File) {
        // read from file
        val stringFromFile: String
        try {
            stringFromFile = File(path, "binsList.txt")
                .bufferedReader().readLine()
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
            return
        }
        // create moshi adapter
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val jsonAdapterWrite = moshi.adapter<MutableList<BinInfo>>(
            Types.newParameterizedType(MutableList::class.java, BinInfo::class.java))
        // deserialize from JSON String to MutableList<BinInfo>
        // and pass it to _binsList.value
        _binsList.value = jsonAdapterWrite.fromJson(stringFromFile)
    }

}