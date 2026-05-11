package com.wms.homemanager.terminal;

import com.wms.homemanager.terminal.buffer.TerminalBuffer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TerminalBuffer 单元测试
 * 测试终端缓冲区功能
 */
public class TerminalBufferTest {

    private TerminalBuffer buffer;
    private static final int COLUMNS = 80;
    private static final int ROWS = 24;
    private static final int SCROLL_SIZE = 1000;

    @Before
    public void setUp() {
        buffer = new TerminalBuffer(COLUMNS, ROWS, SCROLL_SIZE);
    }

    // ========== 初始化测试 ==========

    @Test
    public void testInitialization() {
        assertEquals(COLUMNS, buffer.getColumns());
        assertEquals(ROWS, buffer.getRows());
        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
        assertTrue(buffer.isCursorVisible());
    }

    @Test
    public void testInitialContentIsEmpty() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                assertEquals(' ', buffer.getChar(row, col));
            }
        }
    }

    // ========== 光标操作测试 ==========

    @Test
    public void testSetCursor() {
        buffer.setCursor(10, 20);
        assertEquals(10, buffer.getCursorRow());
        assertEquals(20, buffer.getCursorCol());
    }

    @Test
    public void testSetCursorClamping() {
        // 测试边界限制
        buffer.setCursor(-5, -5);
        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());

        buffer.setCursor(ROWS + 10, COLUMNS + 10);
        assertEquals(ROWS - 1, buffer.getCursorRow());
        assertEquals(COLUMNS - 1, buffer.getCursorCol());
    }

    @Test
    public void testCursorUp() {
        buffer.setCursor(10, 10);
        buffer.cursorUp(5);
        assertEquals(5, buffer.getCursorRow());
        assertEquals(10, buffer.getCursorCol());
    }

    @Test
    public void testCursorUpBoundary() {
        buffer.setCursor(2, 10);
        buffer.cursorUp(5);
        assertEquals(0, buffer.getCursorRow()); // 不能小于 0
    }

    @Test
    public void testCursorDown() {
        buffer.setCursor(10, 10);
        buffer.cursorDown(5);
        assertEquals(15, buffer.getCursorRow());
        assertEquals(10, buffer.getCursorCol());
    }

    @Test
    public void testCursorDownBoundary() {
        buffer.setCursor(ROWS - 3, 10);
        buffer.cursorDown(5);
        assertEquals(ROWS - 1, buffer.getCursorRow()); // 不能超过最大行
    }

    @Test
    public void testCursorForward() {
        buffer.setCursor(10, 10);
        buffer.cursorForward(20);
        assertEquals(10, buffer.getCursorRow());
        assertEquals(30, buffer.getCursorCol());
    }

    @Test
    public void testCursorBackward() {
        buffer.setCursor(10, 30);
        buffer.cursorBackward(20);
        assertEquals(10, buffer.getCursorRow());
        assertEquals(10, buffer.getCursorCol());
    }

    @Test
    public void testCursorHome() {
        buffer.setCursor(15, 40);
        buffer.cursorHome();
        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    // ========== 字符写入测试 ==========

    @Test
    public void testWriteChar() {
        buffer.writeChar('A');
        assertEquals('A', buffer.getChar(0, 0));
        assertEquals(1, buffer.getCursorCol());
    }

    @Test
    public void testWriteMultipleChars() {
        buffer.writeChar('H');
        buffer.writeChar('e');
        buffer.writeChar('l');
        buffer.writeChar('l');
        buffer.writeChar('o');

        assertEquals('H', buffer.getChar(0, 0));
        assertEquals('e', buffer.getChar(0, 1));
        assertEquals('l', buffer.getChar(0, 2));
        assertEquals('l', buffer.getChar(0, 3));
        assertEquals('o', buffer.getChar(0, 4));
        assertEquals(5, buffer.getCursorCol());
    }

    @Test
    public void testWriteCharWithStyle() {
        TerminalBuffer.TextStyle style = new TerminalBuffer.TextStyle();
        style.foregroundColor = 1; // 红色
        style.bold = true;

        buffer.writeChar('X', style);
        assertEquals('X', buffer.getChar(0, 0));
        assertEquals(1, buffer.getStyle(0, 0).foregroundColor);
        assertTrue(buffer.getStyle(0, 0).bold);
    }

    @Test
    public void testWriteCharAtLineEnd() {
        // 写到行尾应该自动换行
        buffer.setCursor(0, COLUMNS - 1);
        buffer.writeChar('A');
        assertEquals('A', buffer.getChar(0, COLUMNS - 1));
        // 下一个字符应该在下一行
        buffer.writeChar('B');
        assertEquals('B', buffer.getChar(1, 0));
    }

    // ========== 控制字符测试 ==========

    @Test
    public void testCarriageReturn() {
        buffer.setCursor(5, 40);
        buffer.carriageReturn();
        assertEquals(5, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    public void testLineFeed() {
        buffer.setCursor(5, 10);
        buffer.lineFeed();
        assertEquals(6, buffer.getCursorRow());
        assertEquals(10, buffer.getCursorCol());
    }

    @Test
    public void testBackspace() {
        buffer.setCursor(5, 10);
        buffer.backspace();
        assertEquals(5, buffer.getCursorRow());
        assertEquals(9, buffer.getCursorCol());
    }

    @Test
    public void testBackspaceAtLineStart() {
        buffer.setCursor(5, 0);
        buffer.backspace();
        assertEquals(5, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol()); // 不能小于 0
    }

    @Test
    public void testTab() {
        buffer.setCursor(0, 0);
        buffer.tab();
        assertEquals(8, buffer.getCursorCol()); // Tab 到 8

        buffer.setCursor(0, 10);
        buffer.tab();
        assertEquals(16, buffer.getCursorCol()); // Tab 到 16
    }

    // ========== 清除操作测试 ==========

    @Test
    public void testClearLine() {
        // 写入一些字符
        buffer.setCursor(5, 0);
        for (int i = 0; i < 10; i++) {
            buffer.writeChar((char) ('A' + i));
        }

        // 清除该行
        buffer.clearLine(5);

        // 验证行已清空
        for (int i = 0; i < COLUMNS; i++) {
            assertEquals(' ', buffer.getChar(5, i));
        }
    }

    @Test
    public void testClearScreen() {
        // 写入一些字符
        buffer.setCursor(0, 0);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                buffer.writeChar('X');
            }
            buffer.carriageReturn();
            buffer.lineFeed();
        }

        // 清屏
        buffer.clearScreen();

        // 验证屏幕已清空
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                assertEquals(' ', buffer.getChar(row, col));
            }
        }

        // 光标应该在左上角
        assertEquals(0, buffer.getCursorRow());
        assertEquals(0, buffer.getCursorCol());
    }

    // ========== 滚动区域测试 ==========

    @Test
    public void testSetScrollRegion() {
        buffer.setScrollRegion(5, 15);
        // 滚动区域应该在 5-15 行
        // 这是一个内部状态，通过行为测试
    }

    @Test
    public void testResetScrollRegion() {
        buffer.setScrollRegion(5, 15);
        buffer.resetScrollRegion();
        // 滚动区域应该恢复到全屏
    }

    // ========== 滚动缓冲区测试 ==========

    @Test
    public void testScrollOffsetInitiallyZero() {
        assertEquals(0, buffer.getScrollOffset());
    }

    @Test
    public void testCanScrollInitiallyFalse() {
        assertFalse(buffer.canScrollUp());
    }

    // ========== 字符设置测试 ==========

    @Test
    public void testSetChar() {
        TerminalBuffer.TextStyle style = new TerminalBuffer.TextStyle();
        style.foregroundColor = 2;

        buffer.setChar(10, 20, 'Z', style);
        assertEquals('Z', buffer.getChar(10, 20));
        assertEquals(2, buffer.getStyle(10, 20).foregroundColor);
    }

    @Test
    public void testClearChar() {
        buffer.setChar(10, 20, 'X', null);
        buffer.clearChar(10, 20);
        assertEquals(' ', buffer.getChar(10, 20));
    }

    // ========== 行操作测试 ==========

    @Test
    public void testClearToEndOfLine() {
        buffer.setCursor(5, 10);
        // 先写入一些内容
        for (int i = 0; i < 20; i++) {
            buffer.setChar(5, i, 'X', null);
        }
        buffer.setCursor(5, 10);
        buffer.clearToEndOfLine();

        // 前10个字符应该保留
        for (int i = 0; i < 10; i++) {
            assertEquals('X', buffer.getChar(5, i));
        }
        // 后面的应该被清除
        for (int i = 10; i < COLUMNS; i++) {
            assertEquals(' ', buffer.getChar(5, i));
        }
    }

    @Test
    public void testClearFromStartOfLine() {
        buffer.setCursor(5, 10);
        // 先写入一些内容
        for (int i = 0; i < 20; i++) {
            buffer.setChar(5, i, 'X', null);
        }
        buffer.setCursor(5, 10);
        buffer.clearFromStartOfLine();

        // 前11个字符应该被清除 (包括光标位置)
        for (int i = 0; i <= 10; i++) {
            assertEquals(' ', buffer.getChar(5, i));
        }
        // 后面的应该保留
        for (int i = 11; i < 20; i++) {
            assertEquals('X', buffer.getChar(5, i));
        }
    }

    // ========== 光标位置保存/恢复测试 ==========

    @Test
    public void testSaveRestoreCursorPosition() {
        buffer.setCursor(10, 30);
        buffer.saveCursorPosition();

        buffer.setCursor(5, 15);
        assertEquals(5, buffer.getCursorRow());
        assertEquals(15, buffer.getCursorCol());

        buffer.restoreCursorPosition();
        assertEquals(10, buffer.getCursorRow());
        assertEquals(30, buffer.getCursorCol());
    }

    // ========== 字符插入/删除测试 ==========

    @Test
    public void testInsertCharacters() {
        buffer.setCursor(0, 0);
        buffer.writeChar('A');
        buffer.writeChar('B');
        buffer.writeChar('C');

        buffer.setCursor(0, 1);
        buffer.insertCharacters(2);

        // 原来的 'B' 和 'C' 应该向右移动
        assertEquals('A', buffer.getChar(0, 0));
        assertEquals(' ', buffer.getChar(0, 1)); // 插入的空格
        assertEquals(' ', buffer.getChar(0, 2)); // 插入的空格
        assertEquals('B', buffer.getChar(0, 3));
        assertEquals('C', buffer.getChar(0, 4));
    }

    @Test
    public void testDeleteCharacters() {
        buffer.setCursor(0, 0);
        buffer.writeChar('A');
        buffer.writeChar('B');
        buffer.writeChar('C');
        buffer.writeChar('D');
        buffer.writeChar('E');

        buffer.setCursor(0, 1);
        buffer.deleteCharacters(2);

        // 'B' 和 'C' 应该被删除，后面的字符向左移动
        assertEquals('A', buffer.getChar(0, 0));
        assertEquals('D', buffer.getChar(0, 1));
        assertEquals('E', buffer.getChar(0, 2));
    }

    @Test
    public void testEraseCharacters() {
        buffer.setCursor(0, 0);
        buffer.writeChar('A');
        buffer.writeChar('B');
        buffer.writeChar('C');
        buffer.writeChar('D');
        buffer.writeChar('E');

        buffer.setCursor(0, 1);
        buffer.eraseCharacters(2);

        // 'B' 和 'C' 应该被清除，但 'D' 和 'E' 不移动
        assertEquals('A', buffer.getChar(0, 0));
        assertEquals(' ', buffer.getChar(0, 1));
        assertEquals(' ', buffer.getChar(0, 2));
        assertEquals('D', buffer.getChar(0, 3));
        assertEquals('E', buffer.getChar(0, 4));
    }

    // ========== 行插入/删除测试 ==========

    @Test
    public void testInsertLines() {
        // 先写入一些内容
        for (int row = 5; row < 10; row++) {
            buffer.setChar(row, 0, (char) ('0' + row), null);
        }

        buffer.setCursor(6, 0);
        buffer.insertLines(2);

        // 第6、7行应该是空的，原来的内容向下移动
        assertEquals(' ', buffer.getChar(6, 0));
        assertEquals(' ', buffer.getChar(7, 0));
    }

    @Test
    public void testDeleteLines() {
        // 先写入一些内容
        for (int row = 5; row < 10; row++) {
            buffer.setChar(row, 0, (char) ('0' + row), null);
        }

        buffer.setCursor(6, 0);
        buffer.deleteLines(2);

        // 第6、7行的内容应该被删除，下面的内容上移
        assertEquals('8', buffer.getChar(6, 0));
        assertEquals('9', buffer.getChar(7, 0));
    }

    // ========== TextStyle 测试 ==========

    @Test
    public void testTextStyleCopy() {
        TerminalBuffer.TextStyle original = new TerminalBuffer.TextStyle();
        original.foregroundColor = 1;
        original.backgroundColor = 2;
        original.bold = true;
        original.italic = true;

        TerminalBuffer.TextStyle copy = new TerminalBuffer.TextStyle(original);
        assertEquals(original.foregroundColor, copy.foregroundColor);
        assertEquals(original.backgroundColor, copy.backgroundColor);
        assertEquals(original.bold, copy.bold);
        assertEquals(original.italic, copy.italic);
    }

    @Test
    public void testTextStyleReset() {
        TerminalBuffer.TextStyle style = new TerminalBuffer.TextStyle();
        style.foregroundColor = 5;
        style.backgroundColor = 3;
        style.bold = true;
        style.italic = true;
        style.underline = true;
        style.inverse = true;

        style.reset();

        assertEquals(-1, style.foregroundColor);
        assertEquals(-1, style.backgroundColor);
        assertFalse(style.bold);
        assertFalse(style.italic);
        assertFalse(style.underline);
        assertFalse(style.inverse);
    }
}
