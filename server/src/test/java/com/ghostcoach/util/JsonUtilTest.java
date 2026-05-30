package com.ghostcoach.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilTest {

    private static final String PLAIN_JSON = "{\"key\":\"value\"}";

    @Test
    void stripMarkdownFences_plainJson_returnsUnchanged() {
        assertThat(JsonUtil.stripMarkdownFences(PLAIN_JSON)).isEqualTo(PLAIN_JSON);
    }

    @Test
    void stripMarkdownFences_jsonFence_stripsAndReturnsJson() {
        String fenced = "```json\n" + PLAIN_JSON + "\n```";
        assertThat(JsonUtil.stripMarkdownFences(fenced)).isEqualTo(PLAIN_JSON);
    }

    @Test
    void stripMarkdownFences_genericFence_stripsAndReturnsJson() {
        String fenced = "```\n" + PLAIN_JSON + "\n```";
        assertThat(JsonUtil.stripMarkdownFences(fenced)).isEqualTo(PLAIN_JSON);
    }

    @Test
    void stripMarkdownFences_leadingTrailingWhitespace_trims() {
        assertThat(JsonUtil.stripMarkdownFences("  " + PLAIN_JSON + "  ")).isEqualTo(PLAIN_JSON);
    }

    @Test
    void stripMarkdownFences_null_returnsNull() {
        assertThat(JsonUtil.stripMarkdownFences(null)).isNull();
    }

    @Test
    void stripMarkdownFences_emptyString_returnsEmpty() {
        assertThat(JsonUtil.stripMarkdownFences("")).isEmpty();
    }
}
