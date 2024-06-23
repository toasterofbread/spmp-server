package dev.toastbits.spms

import kotlinx.cinterop.CValuesRef

expect fun zmqPollerWait(poller: CValuesRef<*>?, event: CValuesRef<libzmq.zmq_poller_event_t>?, timeout: Long): Int
