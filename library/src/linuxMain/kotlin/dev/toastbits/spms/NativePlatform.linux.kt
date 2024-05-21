package dev.toastbits.spms

import kotlinx.cinterop.CValuesRef
import libzmq.zmq_poller_wait

actual fun zmqPollerWait(poller: CValuesRef<*>?, event: CValuesRef<libzmq.zmq_poller_event_t>?, timeout: Long): Int =
    zmq_poller_wait(poller, event, timeout)
