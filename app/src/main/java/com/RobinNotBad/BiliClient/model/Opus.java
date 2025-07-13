package com.RobinNotBad.BiliClient.model;

public class Opus {
    public static final int TYPE_DYNAMIC = 1;
    public static final int TYPE_ARTICLE = 2;

    public long id;
    public int type;
    public long commentId;
    public int commentType;
    public String title;
    public String cover;
    public String content;
    public String pubTime;
    public UserInfo upInfo;
    public Stats stats;
    public OpusParagraph[] paragraphs;


    public long parsedId;

    public Opus(int type, long id) {
        this.type = type;
        this.parsedId = id;
    }

    public Opus() {

    }
}
