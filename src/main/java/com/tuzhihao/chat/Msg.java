package com.tuzhihao.chat;

/**
 * Created by tuzhihao on 2018/9/23.
 */
public class Msg {
    /**
     * 登录
     */
    public static final String CMD_LOGIN = "LOGIN";
    /**
     * 查询客户端列表
     */
    public static final String CMD_LIST = "LIST";
    /**
     * P2P聊天
     */
    public static final String CMD_CHAT = "CHAT";
    /**
     * 分隔符
     */
    private final static String SPLITTER = " --- ";
    /**
     * 消息id
     */
    private long id;

    /**
     * 命令
     */
    private String cmd;

    /**
     * 内容
     */
    private String content;

    public static Msg from(String data) {
        if (data == null) {
            return null;
        }
        final String[] split = data.split(SPLITTER);
        if (split.length != 3) {
            return null;
        }
        Msg msg = new Msg();
        msg.setId(Long.valueOf(split[0]));
        msg.setCmd(split[1]);
        msg.setContent(split[2]);
        return msg;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return id + SPLITTER + cmd + SPLITTER + content;
    }
}
