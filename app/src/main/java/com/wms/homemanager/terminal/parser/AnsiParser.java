package com.wms.homemanager.terminal.parser;

import com.wms.homemanager.terminal.buffer.TerminalBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * ANSI 转义序列解析器
 * 使用状态机模式解析 ANSI 转义序列
 */
public class AnsiParser {

    /**
     * 解析器状态枚举
     */
    public enum State {
        GROUND,       // 正常字符输出
        ESCAPE,       // 收到 ESC 字符
        CSI_ENTRY,    // CSI 序列入口 (ESC [)
        CSI_PARAM,    // CSI 参数收集
        CSI_INTERMEDIATE, // CSI 中间字符
        OSC_START,    // OSC 序列起始 (ESC ])
        OSC_STRING,   // OSC 字符串收集
        OSC_END,      // OSC 结束
        STRING_START, // 字符串序列开始
        STRING_PARAM  // 字符串参数收集
    }

    // 当前状态
    private State currentState = State.GROUND;

    // 参数缓冲区
    private StringBuilder paramBuffer;
    // 中间字符缓冲区
    private StringBuilder intermediateBuffer;
    // OSC 字符串缓冲区
    private StringBuilder oscBuffer;

    // 参数数组
    private int[] params;
    private int paramIndex;

    // 回调接口
    private AnsiParserCallback callback;

    // 当前文本样式
    private TerminalBuffer.TextStyle currentStyle;

    // UTF-8 解码缓冲区
    private byte[] utf8Buffer = new byte[4];
    private int utf8BufferIndex = 0;
    private int utf8ExpectedBytes = 0;

    /**
     * 解析器回调接口
     */
    public interface AnsiParserCallback {
        /**
         * 输出普通字符
         */
        void onChar(char c);

        /**
         * 输出带样式的字符
         */
        void onChar(char c, TerminalBuffer.TextStyle style);

        /**
         * CSI 序列处理
         * @param command 命令字符
         * @param params 参数数组
         */
        void onCsiSequence(char command, int[] params);

        /**
         * OSC 序列处理
         * @param command OSC 命令
         * @param data OSC 数据
         */
        void onOscSequence(int command, String data);

        /**
         * 控制字符处理
         */
        void onControlChar(char c);

        /**
         * 获取当前文本样式
         */
        TerminalBuffer.TextStyle getCurrentStyle();

        /**
         * 设置当前文本样式
         */
        void setCurrentStyle(TerminalBuffer.TextStyle style);
    }

    public AnsiParser(AnsiParserCallback callback) {
        this.callback = callback;
        this.paramBuffer = new StringBuilder();
        this.intermediateBuffer = new StringBuilder();
        this.oscBuffer = new StringBuilder();
        this.params = new int[16]; // 最多16个参数
        this.currentStyle = new TerminalBuffer.TextStyle();
    }

    /**
     * 解析数据
     * @param data 数据数组
     * @param offset 起始偏移
     * @param length 数据长度
     */
    public void parse(byte[] data, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            int b = data[i] & 0xFF;
            parseByteWithUtf8(b);
        }
    }

    /**
     * 带 UTF-8 解码的字节解析
     */
    private void parseByteWithUtf8(int b) {
        // 检查是否在 UTF-8 多字节序列中
        if (utf8ExpectedBytes > 0) {
            // 累积 UTF-8 字节
            utf8Buffer[utf8BufferIndex++] = (byte) b;
            utf8ExpectedBytes--;

            if (utf8ExpectedBytes == 0) {
                // 完整的 UTF-8 序列，解码
                String decoded = new String(utf8Buffer, 0, utf8BufferIndex, StandardCharsets.UTF_8);
                for (char c : decoded.toCharArray()) {
                    processDecodedChar(c);
                }
                utf8BufferIndex = 0;
            }
            return;
        }

        // 检查是否是 UTF-8 起始字节
        if ((b & 0x80) != 0) {
            // 多字节 UTF-8 序列
            if ((b & 0xE0) == 0xC0) {
                // 2 字节序列
                utf8ExpectedBytes = 1;
                utf8Buffer[0] = (byte) b;
                utf8BufferIndex = 1;
            } else if ((b & 0xF0) == 0xE0) {
                // 3 字节序列（Box Drawing 字符在这里）
                utf8ExpectedBytes = 2;
                utf8Buffer[0] = (byte) b;
                utf8BufferIndex = 1;
            } else if ((b & 0xF8) == 0xF0) {
                // 4 字节序列
                utf8ExpectedBytes = 3;
                utf8Buffer[0] = (byte) b;
                utf8BufferIndex = 1;
            } else {
                // 无效的 UTF-8 起始字节，当作单字节处理
                processDecodedChar((char) b);
            }
        } else {
            // ASCII 单字节
            processDecodedChar((char) b);
        }
    }

    /**
     * 处理解码后的字符
     */
    private void processDecodedChar(char c) {
        int b = (int) c;
        switch (currentState) {
            case GROUND:
                handleGroundState(c, b);
                break;
            case ESCAPE:
                handleEscapeState(c, b);
                break;
            case CSI_ENTRY:
                handleCsiEntryState(c, b);
                break;
            case CSI_PARAM:
                handleCsiParamState(c, b);
                break;
            case CSI_INTERMEDIATE:
                handleCsiIntermediateState(c, b);
                break;
            case OSC_START:
                handleOscStartState(c, b);
                break;
            case OSC_STRING:
                handleOscStringState(c, b);
                break;
            default:
                currentState = State.GROUND;
                break;
        }
    }

    /**
     * GROUND 状态处理
     */
    private void handleGroundState(char c, int b) {
        if (b == 0x1B) { // ESC
            currentState = State.ESCAPE;
        } else if (b < 0x20) { // 控制字符
            callback.onControlChar(c);
        } else if (b == 0x7F) { // DEL
            callback.onControlChar(c);
        } else {
            // 普通字符
            callback.onChar(c, callback.getCurrentStyle());
        }
    }

    /**
     * ESCAPE 状态处理
     */
    private void handleEscapeState(char c, int b) {
        switch (c) {
            case '[': // CSI
                currentState = State.CSI_ENTRY;
                resetCsiState();
                break;
            case ']': // OSC
                currentState = State.OSC_START;
                resetOscState();
                break;
            case '(': // 字符集选择
            case ')':
            case '*':
            case '+':
                // 简单忽略字符集选择
                currentState = State.GROUND;
                break;
            case 'D': // IND (Index)
                callback.onControlChar((char) 0x1B);
                callback.onChar('D', null);
                currentState = State.GROUND;
                break;
            case 'M': // RI (Reverse Index)
                callback.onControlChar((char) 0x1B);
                callback.onChar('M', null);
                currentState = State.GROUND;
                break;
            case 'E': // NEL (Next Line)
                callback.onControlChar((char) 0x1B);
                callback.onChar('E', null);
                currentState = State.GROUND;
                break;
            case 'c': // RIS (Reset to Initial State)
                callback.onControlChar((char) 0x1B);
                callback.onChar('c', null);
                currentState = State.GROUND;
                break;
            default:
                // 未知的 ESC 序列，回到 GROUND
                currentState = State.GROUND;
                break;
        }
    }

    /**
     * CSI_ENTRY 状态处理
     */
    private void handleCsiEntryState(char c, int b) {
        if (b >= 0x30 && b <= 0x3F) { // 0-9:;<=>?
            paramBuffer.append(c);
            currentState = State.CSI_PARAM;
        } else if (b >= 0x20 && b <= 0x2F) { // 空格到 /
            intermediateBuffer.append(c);
            currentState = State.CSI_INTERMEDIATE;
        } else if (b >= 0x40 && b <= 0x7E) { // @到~
            // 直接执行命令
            dispatchCsiCommand(c);
            currentState = State.GROUND;
        } else {
            // 无效字符，回到 GROUND
            currentState = State.GROUND;
        }
    }

    /**
     * CSI_PARAM 状态处理
     */
    private void handleCsiParamState(char c, int b) {
        if (b >= 0x30 && b <= 0x39) { // 0-9
            paramBuffer.append(c);
        } else if (c == ';') { // 参数分隔符
            addParam();
        } else if (b >= 0x3C && b <= 0x3F) { // <:<=>
            paramBuffer.append(c);
        } else if (b >= 0x20 && b <= 0x2F) { // 空格到 /
            intermediateBuffer.append(c);
            currentState = State.CSI_INTERMEDIATE;
        } else if (b >= 0x40 && b <= 0x7E) { // @到~
            addParam();
            dispatchCsiCommand(c);
            currentState = State.GROUND;
        } else {
            currentState = State.GROUND;
        }
    }

    /**
     * CSI_INTERMEDIATE 状态处理
     */
    private void handleCsiIntermediateState(char c, int b) {
        if (b >= 0x20 && b <= 0x2F) { // 空格到 /
            intermediateBuffer.append(c);
        } else if (b >= 0x30 && b <= 0x3F) { // 0-9:;<=>?
            // 无效，回到 GROUND
            currentState = State.GROUND;
        } else if (b >= 0x40 && b <= 0x7E) { // @到~
            dispatchCsiCommand(c);
            currentState = State.GROUND;
        } else {
            currentState = State.GROUND;
        }
    }

    /**
     * OSC_START 状态处理
     */
    private void handleOscStartState(char c, int b) {
        if (b >= 0x30 && b <= 0x39) { // 0-9
            oscBuffer.append(c);
        } else if (c == ';') {
            oscBuffer.append(c); // 添加分隔符到缓冲区
            currentState = State.OSC_STRING;
        } else {
            currentState = State.GROUND;
        }
    }

    /**
     * OSC_STRING 状态处理
     */
    private void handleOscStringState(char c, int b) {
        if (b == 0x07 || (b == 0x1B)) { // BEL 或 ESC
            dispatchOscCommand();
            if (b == 0x1B) {
                currentState = State.ESCAPE;
            } else {
                currentState = State.GROUND;
            }
        } else if (b == 0x5C && oscBuffer.length() > 0 &&
                   oscBuffer.charAt(oscBuffer.length() - 1) == 0x1B) {
            // ESC \ (String Terminator)
            dispatchOscCommand();
            currentState = State.GROUND;
        } else {
            oscBuffer.append(c);
        }
    }

    /**
     * 重置 CSI 状态
     */
    private void resetCsiState() {
        paramBuffer.setLength(0);
        intermediateBuffer.setLength(0);
        paramIndex = 0;
        for (int i = 0; i < params.length; i++) {
            params[i] = 0;
        }
    }

    /**
     * 重置 OSC 状态
     */
    private void resetOscState() {
        oscBuffer.setLength(0);
    }

    /**
     * 添加参数
     */
    private void addParam() {
        if (paramIndex < params.length) {
            try {
                params[paramIndex] = Integer.parseInt(paramBuffer.toString());
            } catch (NumberFormatException e) {
                params[paramIndex] = 0;
            }
            paramIndex++;
            paramBuffer.setLength(0);
        }
    }

    /**
     * 分发 CSI 命令
     */
    private void dispatchCsiCommand(char command) {
        int[] actualParams = new int[paramIndex];
        System.arraycopy(params, 0, actualParams, 0, paramIndex);
        callback.onCsiSequence(command, actualParams);
    }

    /**
     * 分发 OSC 命令
     */
    private void dispatchOscCommand() {
        String data = oscBuffer.toString();
        int separatorIndex = data.indexOf(';');
        if (separatorIndex > 0) {
            try {
                int command = Integer.parseInt(data.substring(0, separatorIndex));
                String oscData = data.substring(separatorIndex + 1);
                callback.onOscSequence(command, oscData);
            } catch (NumberFormatException e) {
                // 忽略无效的 OSC 命令
            }
        }
    }

    /**
     * 重置解析器状态
     */
    public void reset() {
        currentState = State.GROUND;
        resetCsiState();
        resetOscState();
    }

    /**
     * 获取当前状态
     */
    public State getCurrentState() {
        return currentState;
    }
}
