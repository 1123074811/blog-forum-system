package com.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SensitiveWordFilterService {

    private static final String WORDS_FILE = "sensitive-words.txt";

    private final TrieNode root = new TrieNode();
    private volatile boolean enabled;

    @PostConstruct
    public void init() {
        int loaded = loadWordsFromClasspath();
        enabled = loaded > 0;
        log.info("Sensitive word DFA initialized, loaded {} words", loaded);
    }

    public String filter(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }

        char[] chars = text.toCharArray();
        boolean[] masked = new boolean[chars.length];

        for (int i = 0; i < chars.length; i++) {
            TrieNode node = root;
            int lastMatchIndex = -1;
            for (int j = i; j < chars.length; j++) {
                node = node.children.get(chars[j]);
                if (node == null) {
                    break;
                }
                if (node.end) {
                    lastMatchIndex = j;
                }
            }
            if (lastMatchIndex >= i) {
                for (int k = i; k <= lastMatchIndex; k++) {
                    masked[k] = true;
                }
            }
        }

        StringBuilder result = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            result.append(masked[i] ? '*' : chars[i]);
        }
        return result.toString();
    }

    private int loadWordsFromClasspath() {
        int count = 0;
        ClassPathResource resource = new ClassPathResource(WORDS_FILE);
        if (!resource.exists()) {
            log.warn("Sensitive word file not found: {}", WORDS_FILE);
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (word.isEmpty() || word.startsWith("#")) {
                    continue;
                }
                addWord(word);
                count++;
            }
        } catch (IOException e) {
            log.error("Failed to load sensitive words from {}", WORDS_FILE, e);
        }
        return count;
    }

    private void addWord(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, key -> new TrieNode());
        }
        node.end = true;
    }

    private static class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean end;
    }
}
