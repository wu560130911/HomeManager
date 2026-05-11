package com.wms.homemanager.terminal;

import com.wms.homemanager.terminal.buffer.TerminalBuffer;
import com.wms.homemanager.terminal.parser.AnsiParser;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * AnsiParser 单元测试
 * 测试 ANSI 转义序列解析功能
 * 
 * 注意：AnsiParser 只负责解析序列并调用回调，不负责样式处理
 * 样式处理在 TerminalEmulator 中完成
 */
public class AnsiParserTest {

    private TestAnsiCallback callback;
    private AnsiParser parser;

    @Before
    public void setUp() {
        callback = new TestAnsiCallback();
        parser = new AnsiParser(callback);
    }

    // ========== 控制字符测试 ==========

    @Test
    public void testControlCharacters() {
        parser.parse("\n".getBytes(StandardCharsets.UTF_8), 0, 1);
        assertEquals('\n', callback.getLastControlChar());

        callback.reset();
        parser.parse("\r".getBytes(StandardCharsets.UTF_8), 0, 1);
        assertEquals('\r', callback.getLastControlChar());

        callback.reset();
        parser.parse("\t".getBytes(StandardCharsets.UTF_8), 0, 1);
        assertEquals('\t', callback.getLastControlChar());

        callback.reset();
        parser.parse("\b".getBytes(StandardCharsets.UTF_8), 0, 1);
        assertEquals('\b', callback.getLastControlChar());
    }

    // ========== CSI 光标移动测试 ==========

    @Test
    public void testCursorUp() {
        parser.parse("\u001b[5A".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('A', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{5}, callback.getLastCsiParams());
    }

    @Test
    public void testCursorDown() {
        parser.parse("\u001b[3B".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('B', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{3}, callback.getLastCsiParams());
    }

    @Test
    public void testCursorForward() {
        parser.parse("\u001b[10C".getBytes(StandardCharsets.UTF_8), 0, 5);
        assertEquals('C', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{10}, callback.getLastCsiParams());
    }

    @Test
    public void testCursorBackward() {
        parser.parse("\u001b[2D".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('D', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{2}, callback.getLastCsiParams());
    }

    @Test
    public void testCursorPosition() {
        parser.parse("\u001b[10;20H".getBytes(StandardCharsets.UTF_8), 0, 8);
        assertEquals('H', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{10, 20}, callback.getLastCsiParams());
    }

    @Test
    public void testCursorPositionDefault() {
        // ESC[H 无参数时默认为空参数数组
        parser.parse("\u001b[H".getBytes(StandardCharsets.UTF_8), 0, 3);
        assertEquals('H', callback.getLastCsiCommand());
    }

    // ========== CSI 清屏测试 ==========

    @Test
    public void testClearScreen() {
        parser.parse("\u001b[2J".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('J', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{2}, callback.getLastCsiParams());
    }

    @Test
    public void testClearLine() {
        parser.parse("\u001b[K".getBytes(StandardCharsets.UTF_8), 0, 3);
        assertEquals('K', callback.getLastCsiCommand());
    }

    @Test
    public void testClearLineToStart() {
        parser.parse("\u001b[1K".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('K', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{1}, callback.getLastCsiParams());
    }

    @Test
    public void testClearLineToEnd() {
        parser.parse("\u001b[0K".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('K', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{0}, callback.getLastCsiParams());
    }

    // ========== SGR 序列解析测试 ==========
    // 注意：这里只测试解析器正确提取参数，样式应用由 TerminalEmulator 处理

    @Test
    public void testSgrReset() {
        parser.parse("\u001b[0m".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('m', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{0}, callback.getLastCsiParams());
    }

    @Test
    public void testSgrBoldParams() {
        parser.parse("\u001b[1m".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('m', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{1}, callback.getLastCsiParams());
    }

    @Test
    public void testSgrItalicParams() {
        parser.parse("\u001b[3m".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('m', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{3}, callback.getLastCsiParams());
    }

    @Test
    public void testSgrUnderlineParams() {
        parser.parse("\u001b[4m".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals('m', callback.getLastCsiCommand());
        assertArrayEquals(new int[]{4}, callback.getLastCsiParams());
    }

    @Test
    public void testSgrForegroundColors() {
        // 测试标准 16 色前景色参数 (30-37)
        for (int i = 0; i < 8; i++) {
            callback.reset();
            String seq = "\u001b[" + (30 + i) + "m";
            parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
            assertEquals('m', callback.getLastCsiCommand());
            assertEquals(30 + i, callback.getLastCsiParams()[0]);
        }
    }

    @Test
    public void testSgrBackgroundColors() {
        // 测试标准 16 色背景色参数 (40-47)
        for (int i = 0; i < 8; i++) {
            callback.reset();
            String seq = "\u001b[" + (40 + i) + "m";
            parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
            assertEquals('m', callback.getLastCsiCommand());
            assertEquals(40 + i, callback.getLastCsiParams()[0]);
        }
    }

    @Test
    public void testSgr256ColorParams() {
        // ESC[38;5;<n>m - 256 色前景色
        String seq = "\u001b[38;5;196m";
        parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
        assertEquals('m', callback.getLastCsiCommand());
        int[] params = callback.getLastCsiParams();
        assertEquals(38, params[0]);
        assertEquals(5, params[1]);
        assertEquals(196, params[2]);
    }

    @Test
    public void testSgrRgbParams() {
        // ESC[38;2;<r>;<g>;<b>m - RGB 真彩色
        String seq = "\u001b[38;2;255;128;0m";
        parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
        assertEquals('m', callback.getLastCsiCommand());
        int[] params = callback.getLastCsiParams();
        assertEquals(38, params[0]);
        assertEquals(2, params[1]);
        assertEquals(255, params[2]);
        assertEquals(128, params[3]);
        assertEquals(0, params[4]);
    }

    @Test
    public void testSgrMultipleParams() {
        // 组合多个属性: 粗体 + 红色前景 + 蓝色背景
        String seq = "\u001b[1;31;44m";
        parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
        assertEquals('m', callback.getLastCsiCommand());
        int[] params = callback.getLastCsiParams();
        assertEquals(3, params.length);
        assertEquals(1, params[0]);
        assertEquals(31, params[1]);
        assertEquals(44, params[2]);
    }

    // ========== OSC 序列测试 ==========

    @Test
    public void testOscSetTitle() {
        // ESC]0;title BEL
        String seq = "\u001b]0;My Terminal Title\u0007";
        parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
        assertEquals(0, callback.getLastOscCommand());
        assertEquals("My Terminal Title", callback.getLastOscData());
    }

    @Test
    public void testOscSetTitleWithNumber() {
        // ESC]2;title BEL - 设置窗口标题
        String seq = "\u001b]2;Window Title\u0007";
        parser.parse(seq.getBytes(StandardCharsets.UTF_8), 0, seq.length());
        assertEquals(2, callback.getLastOscCommand());
        assertEquals("Window Title", callback.getLastOscData());
    }

    // ========== 普通字符测试 ==========

    @Test
    public void testNormalCharacters() {
        parser.parse("Hello".getBytes(StandardCharsets.UTF_8), 0, 5);
        List<Character> chars = callback.getChars();
        assertEquals(5, chars.size());
        assertEquals('H', (char) chars.get(0));
        assertEquals('e', (char) chars.get(1));
        assertEquals('l', (char) chars.get(2));
        assertEquals('l', (char) chars.get(3));
        assertEquals('o', (char) chars.get(4));
    }

    @Test
    public void testMixedContent() {
        // 混合普通字符和 ANSI 序列
        String input = "Hello \u001b[31mRed\u001b[0m World";
        parser.parse(input.getBytes(StandardCharsets.UTF_8), 0, input.length());
        
        List<Character> chars = callback.getChars();
        assertTrue(chars.size() > 0);
        // 应该收到了两个 CSI 序列
        assertEquals(2, callback.getCsiCount());
    }

    // ========== 未知序列容错测试 ==========

    @Test
    public void testUnknownSequence() {
        // 未知序列不应导致崩溃
        parser.parse("\u001b[?25h".getBytes(StandardCharsets.UTF_8), 0, 6);
        // 解析器应该恢复到 GROUND 状态
        parser.parse("test".getBytes(StandardCharsets.UTF_8), 0, 4);
        assertEquals(4, callback.getChars().size());
    }

    @Test
    public void testMalformedSequence() {
        // 不完整的序列
        parser.parse("\u001b[".getBytes(StandardCharsets.UTF_8), 0, 2);
        // 不应该崩溃，可以继续解析
        parser.parse("A".getBytes(StandardCharsets.UTF_8), 0, 1);
    }

    @Test
    public void testEmptyInput() {
        parser.parse(new byte[0], 0, 0);
        // 不应该崩溃
    }

    @Test
    public void testNullBytes() {
        parser.parse(new byte[]{0, 0, 0}, 0, 3);
        // 不应该崩溃
    }

    // ========== 测试回调类 ==========

    private static class TestAnsiCallback implements AnsiParser.AnsiParserCallback {
        private List<Character> chars = new ArrayList<>();
        private char lastControlChar = 0;
        private char lastCsiCommand = 0;
        private int[] lastCsiParams = null;
        private int lastOscCommand = -1;
        private String lastOscData = null;
        private int csiCount = 0;
        private TerminalBuffer.TextStyle currentStyle = new TerminalBuffer.TextStyle();

        @Override
        public void onChar(char c) {
            chars.add(c);
        }

        @Override
        public void onChar(char c, TerminalBuffer.TextStyle style) {
            chars.add(c);
        }

        @Override
        public void onCsiSequence(char command, int[] params) {
            lastCsiCommand = command;
            lastCsiParams = params;
            csiCount++;
        }

        @Override
        public void onOscSequence(int command, String data) {
            lastOscCommand = command;
            lastOscData = data;
        }

        @Override
        public void onControlChar(char c) {
            lastControlChar = c;
        }

        @Override
        public TerminalBuffer.TextStyle getCurrentStyle() {
            return currentStyle;
        }

        @Override
        public void setCurrentStyle(TerminalBuffer.TextStyle style) {
            if (style != null) {
                this.currentStyle = style;
            }
        }

        public List<Character> getChars() {
            return chars;
        }

        public char getLastControlChar() {
            return lastControlChar;
        }

        public char getLastCsiCommand() {
            return lastCsiCommand;
        }

        public int[] getLastCsiParams() {
            return lastCsiParams;
        }

        public int getLastOscCommand() {
            return lastOscCommand;
        }

        public String getLastOscData() {
            return lastOscData;
        }

        public int getCsiCount() {
            return csiCount;
        }

        public void reset() {
            chars.clear();
            lastControlChar = 0;
            lastCsiCommand = 0;
            lastCsiParams = null;
            lastOscCommand = -1;
            lastOscData = null;
            csiCount = 0;
            currentStyle = new TerminalBuffer.TextStyle();
        }
    }
}
