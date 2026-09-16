package dev.algoforge.core

object ProjectTemplates {
    fun sourceFor(language: Language): String = when (language) {
        Language.PYTHON -> PYTHON_MAIN
        Language.CPP -> CPP_MAIN
        Language.C -> C_MAIN
        Language.JAVA -> JAVA_MAIN
    }

    private val PYTHON_MAIN = """
        import sys

        def solve() -> None:
            data = sys.stdin.buffer.read().split()
            # TODO: parse input and print the answer.
            _ = data

        if __name__ == "__main__":
            solve()
    """.trimIndent()

    private val CPP_MAIN = """
        #include <bits/stdc++.h>
        using namespace std;

        int main() {
            ios::sync_with_stdio(false);
            cin.tie(nullptr);

            // TODO: parse input and print the answer.
            return 0;
        }
    """.trimIndent()

    private val C_MAIN = """
        #include <stdio.h>

        int main(void) {
            // TODO: parse input and print the answer.
            return 0;
        }
    """.trimIndent()

    private val JAVA_MAIN = """
        import java.io.*;
        import java.util.*;

        public class Main {
            public static void main(String[] args) throws Exception {
                FastScanner in = new FastScanner(System.in);
                StringBuilder out = new StringBuilder();

                // TODO: parse input and print the answer.

                System.out.print(out);
            }

            static final class FastScanner {
                private final InputStream in;
                private final byte[] buffer = new byte[1 << 16];
                private int pointer;
                private int length;

                FastScanner(InputStream in) {
                    this.in = in;
                }

                private int read() throws IOException {
                    if (pointer == length) {
                        length = in.read(buffer);
                        pointer = 0;
                        if (length <= 0) return -1;
                    }
                    return buffer[pointer++];
                }

                String next() throws IOException {
                    int c;
                    do {
                        c = read();
                    } while (c <= ' ' && c != -1);
                    StringBuilder token = new StringBuilder();
                    while (c > ' ') {
                        token.append((char) c);
                        c = read();
                    }
                    return token.toString();
                }

                int nextInt() throws IOException {
                    return Integer.parseInt(next());
                }

                long nextLong() throws IOException {
                    return Long.parseLong(next());
                }
            }
        }
    """.trimIndent()
}
