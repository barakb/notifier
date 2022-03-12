package com.totango.notifier.server

class Matcher {
    fun match(pattern: List<String>, notification: List<String>): Boolean {
        if (pattern.size > notification.size + 1){
            return false
        } else
            for ((index, p) in pattern.withIndex()) {
                return if (p == "*") {
                    true
                } else if(notification.size <= index){
                    return false
                } else if (p == "?" || p == notification[index]) {
                    continue
                } else {
                    false
                }
            }
        return pattern.size == notification.size
    }
}