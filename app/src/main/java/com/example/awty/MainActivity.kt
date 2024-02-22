package com.example.awty

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private var isNagging = false
    private val TAG: String = "MainActivity"
    private var broadcastReceiver: BroadcastReceiver? = null
    private var message = ""
    private var phoneNum = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS), 0)
        }

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
            message = userMessage.text.toString()
            phoneNum = userPhoneNum.text.toString()
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
                            // Toast.makeText(currActivity, toastMessage, Toast.LENGTH_SHORT).show()
                            if (context != null) {
                                val smsManager: SmsManager?
                                smsManager = if(Build.VERSION.SDK_INT > 31) {
                                    context.getSystemService(SmsManager::class.java)
                                } else {
                                    SmsManager.getDefault()
                                }
                                try {
                                    smsManager?.sendTextMessage("+1$phoneNum",
                                        null, message, null, null)
                                    Toast.makeText(currActivity, "Sent Message to $phoneNum", Toast.LENGTH_SHORT).show()
                                    Log.i(TAG, "$phoneNum")
                                } catch (e: Exception) {
                                    Toast.makeText(currActivity, "Failed to send Message.", Toast.LENGTH_SHORT).show()
                                    Log.i(TAG, "Failed Text: $e")
                                }
                            }
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