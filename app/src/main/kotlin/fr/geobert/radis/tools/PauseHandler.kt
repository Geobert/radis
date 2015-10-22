package fr.geobert.radis.tools

import android.app.Activity
import android.os.Handler
import android.os.Message
import java.util.*

/**
 * Message Handler class that supports buffering up of messages when the activity is paused i.e. in the background.
 */
public abstract class PauseHandler : Handler() {

    /**
     * Message Queue Buffer
     */
    private val messageQueueBuffer = Collections.synchronizedList(ArrayList<Message>())

    /**
     * Flag indicating the pause state
     */
    private var activity: Activity? = null

    /**
     * Resume the handler.
     */
    @Synchronized public fun resume(activity: Activity) {
        this.activity = activity

        while (messageQueueBuffer.size > 0) {
            val msg = messageQueueBuffer.get(0)
            messageQueueBuffer.removeAt(0)
            sendMessage(msg)
        }
    }

    /**
     * Pause the handler.
     */
    @Synchronized public fun pause() {
        activity = null
    }

    /**
     * Store the message if we have been paused, otherwise handle it now.

     * @param msg Message to handle.
     */
    @Synchronized override fun handleMessage(msg: Message) {
        val act = activity
        if (act == null) {
            val msgCopy = Message()
            msgCopy.copyFrom(msg)
            messageQueueBuffer.add(msgCopy)
        } else {
            processMessage(act, msg)
        }
    }

    /**
     * Notification message to be processed. This will either be directly from
     * handleMessage or played back from a saved message when the activity was
     * paused.

     * @param act     Activity owning this Handler that isn't currently paused.
     * *
     * @param message Message to be handled
     */
    protected abstract fun processMessage(act: Activity, message: Message)

}
