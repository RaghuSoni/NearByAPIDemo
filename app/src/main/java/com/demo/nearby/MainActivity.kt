package com.demo.nearby

import android.os.Bundle

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.nearby.databinding.ActivityMainBinding
import com.demo.nearby.login.LoginActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "MainActivity"
    }

    lateinit var binding: ActivityMainBinding

    private var loginTime = System.currentTimeMillis()
    private lateinit var userUUID: String
    private lateinit var messageListener: MessageListener
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var logoutDialog: AlertDialog.Builder
    private lateinit var activeMessage: Message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra(LoginActivity.KEY_USERNAME)
        userUUID = intent.getStringExtra(LoginActivity.KEY_USER_UUID).toString()

        messageListAdapter = MessageListAdapter(this, userUUID)
        messageListAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateEmptyView()
            }

            override fun onChanged() {
                updateEmptyView()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateEmptyView()

               binding.messageListRecycler.post { binding.messageListRecycler.smoothScrollToPosition(messageListAdapter.itemCount - 1) }
            }

            private fun updateEmptyView() {
                val showEmptyView = messageListAdapter.itemCount == 0
                binding.emptyView.visibility = if (showEmptyView) View.VISIBLE else View.GONE
            }
        })

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.messageListRecycler.layoutManager = layoutManager
        binding.messageListRecycler.adapter = messageListAdapter

        messageListener = object : MessageListener() {
            override fun onFound(message: Message) {
                Log.d(TAG, "Found message: ${message.content}")
                val deviceMessage = DeviceMessage.fromNearbyMessage(message)
                if (deviceMessage.creationTime < loginTime) {
                    Log.d(TAG, "Found message was sent before we logged in. Won't add it to chat history.")
                } else {
                    messageListAdapter.add(deviceMessage)
                }
            }

            override fun onLost(message: Message) {
                Log.d(TAG, "Lost sight of message: ${message.content}")
            }
        }

        logoutDialog = AlertDialog.Builder(this)
        logoutDialog
                .setTitle("Are you sure you want to leave?")
                .setMessage("Your chat history will be deleted.")
                .setNegativeButton("No", null)

        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!binding.messageInput.text.toString().trim().isEmpty()) {
                    binding.sendMessageButton.setImageResource(R.drawable.ic_send)
                    binding.sendMessageButton.isEnabled = true
                } else {
                    binding.sendMessageButton.setImageResource(R.drawable.ic_send_disabled)
                    binding.sendMessageButton.isEnabled = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.sendMessageButton.isEnabled = false
        binding.sendMessageButton.setOnClickListener {
            val timestamp = System.currentTimeMillis()

            val deviceMessage = username?.let { it1 ->
                DeviceMessage(userUUID,
                    it1, binding.messageInput.text.toString(), timestamp)
            }

            if (deviceMessage != null) {
                activeMessage = deviceMessage.message
                Log.d(TAG, "Publishing message = ${activeMessage.content}")
                Nearby.getMessagesClient(this).publish(activeMessage)

                messageListAdapter.add(deviceMessage)
            }

            binding.messageInput.setText("")
        }
    }

    public override fun onStart() {
        super.onStart()

        Nearby.getMessagesClient(this).subscribe(messageListener)
    }

    public override fun onStop() {
        if (::activeMessage.isInitialized)
            Nearby.getMessagesClient(this).unpublish(activeMessage)

        Nearby.getMessagesClient(this).unsubscribe(messageListener)

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logoutDialog.setPositiveButton("Yes") { _, _ ->
                    Util.clearSharedPreferences(this@MainActivity)
                    finish()
                }.show()
                true
            }
            R.id.action_clear_chat_history -> {
                messageListAdapter.clear()
                loginTime = System.currentTimeMillis()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        logoutDialog.setPositiveButton("Yes") { _, _ ->
            Util.clearSharedPreferences(this@MainActivity)
            super@MainActivity.onBackPressed()
        }.show()
    }
}
