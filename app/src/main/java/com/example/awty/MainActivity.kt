package com.example.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    private var isNagging = false
    private val TAG: String = "MainActivity"
    private var broadcastReceiver: BroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userMessage = findViewById<EditText>(R.id.userMessage)
        val userPhoneNum = findViewById<EditText>(R.id.phoneNum)
        val userNagTime = findViewById<EditText>(R.id.notifyDelay)
        val startBtn = findViewById<Button>(R.id.startBtn)

        startBtn.isEnabled = false

        var userMessageNotEmpty = false
        var userPhoneNumNotEmpty = false
        var userNagTimeNotEmpty = false

        userMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val userInput = s.toString()
                Log.i(TAG, "$userInput")
                userMessageNotEmpty = userInput.isNotEmpty()
                startBtn.isEnabled = userMessageNotEmpty && userPhoneNumNotEmpty && userNagTimeNotEmpty
            }
        })

        userPhoneNum.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val userInput = s.toString()
                Log.i(TAG, "$userInput")
                val formatPhone = PhoneNumberUtils.formatNumber(s.toString(), "US")
                if(PhoneNumberUtils.isGlobalPhoneNumber(formatPhone)) {
                    userPhoneNumNotEmpty = userInput.isNotEmpty()
                }
                startBtn.isEnabled = userMessageNotEmpty && userPhoneNumNotEmpty && userNagTimeNotEmpty
            }
        })

        userNagTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val userInput = s.toString()
                Log.i(TAG, "$userInput")
                if(userInput.isNotEmpty()) {
                    userNagTimeNotEmpty = TextUtils.isDigitsOnly(userInput)
                }
                startBtn.isEnabled = userMessageNotEmpty && userPhoneNumNotEmpty && userNagTimeNotEmpty
            }
        })

        startBtn.setOnClickListener {
            val message = userMessage.text.toString()
            var phoneNum = userPhoneNum.text.toString()
            val phonePattern = """^\(?\d{3}\)?[\s-]?\d{3}[\s-]?\d{4}$"""
            if(!Regex(phonePattern).matches(phoneNum)) {
                userPhoneNum.setText("")
                Toast.makeText(this, "Invalid input phone number", Toast.LENGTH_SHORT).show()
                startBtn.isEnabled = false
                return@setOnClickListener
            }
            phoneNum = phoneNum.filter { it.isDigit() }
            val formattedPhone = "(${phoneNum.slice(0..2)}) ${phoneNum.slice(3..5)}-${phoneNum.slice(6..9)}"
            val toastMessage = "$formattedPhone: $message"
            val nagTime = userNagTime.text.toString().toInt()
            if(nagTime == 0) {
                userNagTime.setText("")
                Toast.makeText(this, "Invalid input nag time", Toast.LENGTH_SHORT).show()
                startBtn.isEnabled = false
                return@setOnClickListener
            }
            val currActivity = this
            if(!isNagging) {
                Log.i(TAG, "Start")
                isNagging = !isNagging
                startBtn.text = "Stop"
                if(broadcastReceiver == null) {
                    broadcastReceiver = object: BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            Toast.makeText(currActivity, toastMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                    val intentFilter = IntentFilter("edu.uw.ischool.ryanng20.ALARM")
                    registerReceiver(broadcastReceiver, intentFilter)
                }
                val intent = Intent("edu.uw.ischool.ryanng20.ALARM")
                val pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                sendBroadcast(intent)
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), nagTime * 60000L, pending)
            } else {
                Log.i(TAG, "Stop")
                isNagging = !isNagging
                startBtn.text = "Start"

                broadcastReceiver?.let {
                    unregisterReceiver(it)
                    broadcastReceiver = null
                }
            }

        }

    }


}