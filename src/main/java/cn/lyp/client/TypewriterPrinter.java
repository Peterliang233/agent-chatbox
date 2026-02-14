package cn.lyp.client;

import java.io.PrintStream;

public final class TypewriterPrinter {
    private TypewriterPrinter() {
    }

    public static void print(String text, int delayMs) {
        print(System.out, text, delayMs);
    }

    public static void print(PrintStream out, String text, int delayMs) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int delay = Math.max(delayMs, 0);
        for (int i = 0; i < text.length(); i++) {
            out.print(text.charAt(i));
            out.flush();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
