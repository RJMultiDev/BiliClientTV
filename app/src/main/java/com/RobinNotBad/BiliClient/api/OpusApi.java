package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.Opus;
import com.RobinNotBad.BiliClient.model.OpusParagraph;
import com.RobinNotBad.BiliClient.model.Stats;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.JsonUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpusApi {

    public static Opus getOpus(long id) throws IOException, JSONException {
        Opus opus = new Opus();
        opus.id = id;
        String url = "https://www.bilibili.com/opus/" + id;
        Response response = NetWorkUtil.get(url);
        ResponseBody responseBody = response.body();
        if(responseBody == null) return opus;

        String html = responseBody.string();

        JSONObject detail = new JSONObject(JsonUtil.search(html, "detail", ""));  //效率不高 能用就行 死去的jsonUtil居然还能发光发热

        opus.commentId = Integer.parseInt(detail.optString("comment_id_str", "0"));
        opus.commentType = detail.optInt("comment_type");

        if(detail.isNull("modules")) return opus;    //isNull其实涵盖了!has的情况，之前都是咋想的判断两次，我简直是sb
        JSONArray modules = detail.getJSONArray("modules");

        for (int i = 0; i < modules.length(); i++) {
            JSONObject module = modules.getJSONObject(i);
            switch (module.optString("module_type")) {
                case "MODULE_TYPE_TITLE":
                    String title = module.getJSONObject("module_title").getString("text");
                    Logu.d(title);
                    break;
                case "MODULE_TYPE_AUTHOR":
                    JSONObject module_author = module.getJSONObject("module_author");    //我感觉b站也是一个巨大的草台班子，用户信息格式都好几种，头像有avatar有face有head的，他们自己的程序员不累吗……
                    UserInfo author = new UserInfo();
                    author.mid = module_author.getLong("mid");
                    author.name = module_author.getString("name");
                    author.followed = module_author.optBoolean("following", false);
                    author.avatar = module_author.getString("face");
                    if (!module_author.isNull("vip"))
                        author.vip_nickname_color = module_author.getJSONObject("vip").optString("nickname_color", "");

                    opus.pubTime = module_author.getString("pub_time");
                    opus.upInfo = author;
                    break;
                case "MODULE_TYPE_CONTENT":
                    JSONArray paragraphs = module.getJSONObject("module_content").getJSONArray("paragraphs");
                    opus.paragraphs = analyzeParagraphs(paragraphs);
                    break;

            }
        }

        opus.cover = "";
        opus.stats = new Stats();
        return opus;
    }

    public static OpusParagraph[] analyzeParagraphs(JSONArray jsonArray) throws JSONException {
        OpusParagraph[] paragraphs = new OpusParagraph[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject paragraphJson = jsonArray.getJSONObject(i);
            OpusParagraph paragraph = new OpusParagraph(paragraphJson);
            paragraphs[i] = paragraph;
        }
        return paragraphs;
    }
}
