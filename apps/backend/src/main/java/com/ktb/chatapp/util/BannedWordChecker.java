package com.ktb.chatapp.util;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

import java.util.ArrayDeque;

public final class BannedWordChecker {

    // ASCII만 처리(0~127). 메시지/금칙어가 대부분 영문/숫자면 충분히 커버.
    private static final int ALPH = 128;

    private final Set<String> bannedWords; // optional: debugging/inspection

    // Aho-Corasick (array-based)
    // nextFlat[(state * ALPH) + ch] = (nextState + 1), 0 means "no edge"
    private int[] nextFlat;
    private int[] fail;
    private boolean[] out;
    private int size; // number of states (nodes), root = 0

    public BannedWordChecker(Set<String> bannedWords) {
        this.bannedWords = bannedWords == null ? Set.of() :
                bannedWords.stream()
                        .filter(w -> w != null && !w.isBlank())
                        .map(w -> w.toLowerCase(Locale.ROOT)) // build-time only, ok
                        .collect(Collectors.toUnmodifiableSet());

        Assert.notEmpty(this.bannedWords, "Banned words set must not be empty");

        // rough initial capacity: 1 + sum(len)
        int cap = 1;
        for (String w : this.bannedWords) cap += w.length();
        initArrays(Math.max(4, cap));

        buildTrie(this.bannedWords);
        buildFailureLinks();
    }

    public boolean containsBannedWord(String message) {
        if (message == null || message.isBlank()) return false;

        int state = 0; // root
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);

            // ASCII 아니면 그냥 "끊김"으로 처리(원하면 continue 대신 root로 리셋만)
            if (c >= ALPH) {
                state = 0;
                continue;
            }

            int ch = toLowerAscii(c);

            // fail 타면서 전이 가능한 곳 찾기
            int next = nextFlat[state * ALPH + ch];
            while (state != 0 && next == 0) {
                state = fail[state];
                next = nextFlat[state * ALPH + ch];
            }

            state = (next == 0) ? 0 : (next - 1);
            if (out[state]) return true;
        }
        return false;
    }

    // ----------------- internals -----------------

    private void initArrays(int capacityStates) {
        this.size = 1; // root만 존재
        this.nextFlat = new int[capacityStates * ALPH];
        this.fail = new int[capacityStates];
        this.out = new boolean[capacityStates];
        this.fail[0] = 0;
        this.out[0] = false;
    }

    private void ensureCapacity(int neededStates) {
        if (neededStates <= fail.length) return;

        int newCap = Math.max(neededStates, fail.length * 2);
        int[] newNext = new int[newCap * ALPH];
        System.arraycopy(nextFlat, 0, newNext, 0, size * ALPH);
        nextFlat = newNext;

        int[] newFail = new int[newCap];
        System.arraycopy(fail, 0, newFail, 0, size);
        fail = newFail;

        boolean[] newOut = new boolean[newCap];
        System.arraycopy(out, 0, newOut, 0, size);
        out = newOut;
    }

    private void buildTrie(Set<String> words) {
        for (String w : words) {
            int cur = 0;
            for (int i = 0; i < w.length(); i++) {
                char c = w.charAt(i);
                if (c >= ALPH) {
                    // 금칙어에 ASCII 밖 문자가 섞이면 여기서 정책 결정 필요.
                    // 지금은 그냥 무시(끊김) 처리: 루트로 리셋
                    cur = 0;
                    continue;
                }

                int ch = toLowerAscii(c);
                int pos = cur * ALPH + ch;
                int nxt = nextFlat[pos];
                if (nxt == 0) {
                    ensureCapacity(size + 1);
                    nextFlat[pos] = (size + 1); // store index+1
                    nxt = nextFlat[pos];
                    size++;
                }
                cur = nxt - 1;
            }
            out[cur] = true;
        }
    }

    private void buildFailureLinks() {
        // BFS
        ArrayDeque<Integer> q = new ArrayDeque<>();

        // root children: fail = root
        for (int ch = 0; ch < ALPH; ch++) {
            int nxt = nextFlat[ch]; // root(0) * ALPH + ch
            if (nxt != 0) {
                int child = nxt - 1;
                fail[child] = 0;
                // out 전파는 BFS 과정에서 해도 되지만, 여기서 한 번 해도 OK
                out[child] |= out[fail[child]];
                q.add(child);
            }
        }

        while (!q.isEmpty()) {
            int cur = q.poll();

            int curBase = cur * ALPH;
            for (int ch = 0; ch < ALPH; ch++) {
                int nxt = nextFlat[curBase + ch];
                if (nxt == 0) continue;

                int child = nxt - 1;

                int f = fail[cur];
                int link = nextFlat[f * ALPH + ch];
                while (f != 0 && link == 0) {
                    f = fail[f];
                    link = nextFlat[f * ALPH + ch];
                }
                fail[child] = (link == 0) ? 0 : (link - 1);

                // fail 쪽 매칭 전파 (중요)
                out[child] |= out[fail[child]];

                q.add(child);
            }
        }
    }

    private static int toLowerAscii(char c) {
        // 'A'..'Z'만 빠르게 소문자화
        return (c >= 'A' && c <= 'Z') ? (c + 32) : c;
    }
}
