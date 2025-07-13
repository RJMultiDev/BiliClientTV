package com.RobinNotBad.BiliClient.model;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpusParagraph {
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_PIC = 2;
    public static final int TYPE_DIVIDER = 3;
    public static final int TYPE_TEXT_REGULAR = 4;
    public static final int TYPE_LIST = 5;

    public static int FONT_SIZE_BILI_DEFAULT = 17;

    public int align;
    public int type;
    public int fontSize;
    public Object content;

    public OpusParagraph(){}

    public OpusParagraph(JSONObject para) throws JSONException {
        this.type = para.optInt("para_type");
        this.align = para.optInt("align");
        switch (type){
            case TYPE_TEXT:
            case TYPE_TEXT_REGULAR:
                this.content = analyzeText(para.optJSONObject("text"));
                break;
            case TYPE_PIC:
                this.content = analyzePic(para.optJSONObject("pic"));
                break;
            case TYPE_DIVIDER:
                this.content = analyzeDivider(para.optJSONObject("line"));
                break;
                //TODO:列表的解析
                /*
            case TYPE_LIST:

                break;
                 */
            default:
                this.content = "[无法识别段落：" + type + "]";
        }
    }

    public CharSequence analyzeText(JSONObject text) throws JSONException {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if(text == null) return "";
        JSONArray nodes = text.optJSONArray("nodes");
        if(nodes == null) return "";

        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            switch (node.optString("type")) {
                case "TEXT_NODE_TYPE_WORD":
                    JSONObject word = node.getJSONObject("word");

                    int startPosition = stringBuilder.length();
                    stringBuilder.append(word.optString("words",""));
                    int endPosition = stringBuilder.length();

                    //粗体斜体，BOLD/ITALIC/BOLD_ITALIC 正好对应 1/2/3 所以可以这样减少判断量？
                    JSONObject style = word.optJSONObject("style");
                    if(style != null) {
                        boolean bold = style.optBoolean("bold");
                        boolean italic = style.optBoolean("italic");
                        int styleInt = (bold ? Typeface.BOLD : 0) + (italic ? Typeface.ITALIC : 0);
                        if (styleInt != 0)
                            stringBuilder.setSpan(new StyleSpan(styleInt), startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    //字体大小，使用的是相对大小，17号的字体在手表上还是太逆天了
                    int fontSize = word.optInt("font_size",17);
                    if (fontSize != FONT_SIZE_BILI_DEFAULT) {
                        float fontScale = fontSize * 1.0f / FONT_SIZE_BILI_DEFAULT;
                        stringBuilder.setSpan(new RelativeSizeSpan(fontScale), startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    //字体颜色
                    String color = word.optString("color");
                    if(!color.isEmpty() && !color.equals("#18191c"))
                        stringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor(color)), startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    //fontLevel=regular实际上是像这样： |你好世界  因为懒得做，所以直接用白色背景
                    String fontLevel = word.optString("font_level");
                    if(fontLevel.equals("regular"))
                        stringBuilder.setSpan(new BackgroundColorSpan(0x22ffffff), startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    break;

                default:
                    stringBuilder.append("[").append(node.optString("type")).append("]");
            }
        }

        return stringBuilder;
    }

    public String[] analyzePic(JSONObject allJson) throws JSONException {
        if(allJson == null) return new String[0];
        JSONArray picsJson = allJson.optJSONArray("pics");
        if(picsJson == null) return new String[0];
        String[] pics = new String[picsJson.length()];
        for (int i = 0; i < picsJson.length(); i++) {
            pics[i] = picsJson.getJSONObject(i).optString("url");
        }
        return pics;
    }

    public String[] analyzeDivider(JSONObject allJson) throws JSONException {
        if(allJson == null) return new String[0];
        return new String[] {allJson.getJSONObject("pic").optString("url")};
    }
}
