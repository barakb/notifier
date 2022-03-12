package com.totango.notifier.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MatcherTest {

    private val matcher = Matcher()
    /// modify account 35 set attribute 555
    /// subscribe modify account ? set *
    @Test
    fun matchExactlyTheSame() {
        assertThat(matcher.match(listOf("a"), listOf("a"))).isTrue
        assertThat(matcher.match(listOf("a", "b"), listOf("a", "b"))).isTrue
    }
    @Test
    fun dontMatchShortNotification() {
        assertThat(matcher.match(listOf("?"), listOf())).isFalse
        assertThat(matcher.match(listOf("a"), listOf())).isFalse
    }
    @Test
    fun star() {
        assertThat(matcher.match(listOf("*"), listOf())).isTrue
        assertThat(matcher.match(listOf("*"), listOf("a"))).isTrue
        assertThat(matcher.match(listOf("*"), listOf("*"))).isTrue
        assertThat(matcher.match(listOf("*"), listOf("a", "b"))).isTrue
        assertThat(matcher.match(listOf("a", "*"), listOf("a"))).isTrue
    }
    @Test
    fun qm() {
        assertThat(matcher.match(listOf("?"), listOf("a"))).isTrue
        assertThat(matcher.match(listOf("?"), listOf("*"))).isTrue
        assertThat(matcher.match(listOf("?"), listOf("a", "b"))).isFalse
        assertThat(matcher.match(listOf("?", "?"), listOf("a", "b"))).isTrue
    }
    @Test
    fun notMatch() {
        assertThat(matcher.match(listOf("a"), listOf("b"))).isFalse
        assertThat(matcher.match(listOf("a"), listOf("*"))).isFalse
        assertThat(matcher.match(listOf("a"), listOf("a", "b"))).isFalse
        assertThat(matcher.match(listOf("a", "c"), listOf("a", "b"))).isFalse
    }
}