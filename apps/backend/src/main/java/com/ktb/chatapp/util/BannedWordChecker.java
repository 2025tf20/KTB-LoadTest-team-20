package com.ktb.chatapp.util;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

public class BannedWordChecker {

    private final Set<String> bannedWords; // optional: keep for debugging/inspection
    private final Node root;

    public BannedWordChecker(Set<String> bannedWords) {
        this.bannedWords = bannedWords == null ? Set.of() :
                bannedWords.stream()
                        .filter(w -> w != null && !w.isBlank())
                        .map(w -> w.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet());

        Assert.notEmpty(this.bannedWords, "Banned words set must not be empty");

        this.root = new Node();
        buildTrie(this.bannedWords);
        buildFailureLinks();
    }

    public boolean containsBannedWord(String message) {
        if (message == null || message.isBlank()) return false;

        String s = message.toLowerCase(Locale.ROOT);

        Node state = root;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);

            while (state != root && !state.next.containsKey(cp)) {
                state = state.fail;
            }

            Node nxt = state.next.get(cp);
            state = (nxt != null) ? nxt : root;

            if (state.output) return true; // 매칭 발견
        }
        return false;
    }

    // ----------------- Aho-Corasick internals -----------------

    private void buildTrie(Set<String> words) {
        for (String w : words) {
            Node cur = root;
            for (int i = 0; i < w.length(); ) {
                int cp = w.codePointAt(i);
                i += Character.charCount(cp);
                cur = cur.next.computeIfAbsent(cp, k -> new Node());
            }
            cur.output = true;
        }
    }

    private void buildFailureLinks() {
        ArrayDeque<Node> q = new ArrayDeque<>();

        root.fail = root;

        // root children
        for (Node child : root.next.values()) {
            child.fail = root;
            child.output |= child.fail.output;
            q.add(child);
        }

        while (!q.isEmpty()) {
            Node cur = q.poll();

            for (Map.Entry<Integer, Node> e : cur.next.entrySet()) {
                int cp = e.getKey();
                Node child = e.getValue();

                Node f = cur.fail;
                while (f != root && !f.next.containsKey(cp)) {
                    f = f.fail;
                }

                Node link = f.next.get(cp);
                child.fail = (link != null) ? link : root;

                // fail 쪽에서 이미 매칭 가능한 패턴이 있으면 전파
                child.output |= child.fail.output;

                q.add(child);
            }
        }
    }

    private static final class Node {
        final Map<Integer, Node> next = new HashMap<>();
        Node fail;
        boolean output;
    }
}
