package com.RobinNotBad.BiliClient.api;

import android.util.Log;

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
        opus.type = Opus.TYPE_DYNAMIC;

        // 这里改成了直接通过接口获取详情
        // Robin之前用的从HTML提取JSON，方向明显错了

        opus.id = id;
        String url = "https://www.bilibili.com/opus/" + id; // 别改成https
        try {
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
            opus.stats = new Stats();
        } catch (IllegalArgumentException e){
            url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail??";
            url += "timezone_offset=-480&platform=web&gaia_source=main_web&id=" + id + "&features=itemOpusStyle,opusBigCover,onlyfansVote,endFooterHidden,decorationCard,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,editable,opusPrivateVisible,avatarAutoTheme&web_location=333.1368&x-bili-device-req-json=%7B%22platform%22:%22web%22,%22device%22:%22pc%22%7D&x-bili-web-req-json=%7B%22spm_id%22:%22333.1368%22%7D";
            Response response = NetWorkUtil.get(ConfInfoApi.signWBI(url));
            ResponseBody responseBody = response.body();
            if(responseBody == null) return opus;

            JSONObject json = new JSONObject(responseBody.string());
            if (json == null) return opus;

            JSONObject item = json.getJSONObject("data").getJSONObject("item");

            JSONObject basic = item.getJSONObject("basic");
            opus.commentId = Long.parseLong(basic.optString("comment_id_str", "0"));
            opus.commentType = basic.optInt("comment_type");

            if(item.isNull("modules")) return opus;    //isNull其实涵盖了!has的情况，之前都是咋想的判断两次，我简直是sb
            JSONObject modules = item.getJSONObject("modules");

            JSONObject module_author = modules.getJSONObject("module_author");
            UserInfo author = new UserInfo();
            author.mid = module_author.getLong("mid");
            author.name = module_author.getString("name");
            author.followed = module_author.optBoolean("following", false);
            author.avatar = module_author.getString("face");
            if (!module_author.isNull("vip"))
                author.vip_nickname_color = module_author.getJSONObject("vip").optString("nickname_color", "");
            opus.pubTime = module_author.getString("pub_time");
            opus.upInfo = author;

            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");

            if (!module_dynamic.isNull("desc")){
                opus.paragraphs = new OpusParagraph[1];
                JSONObject object = new JSONObject();
                object.put("para_type", OpusParagraph.TYPE_OPUS);
                object.put("data", module_dynamic.getJSONObject("desc").getJSONArray("rich_text_nodes"));
                opus.paragraphs[0] = new OpusParagraph(object);
            }
            else if(!module_dynamic.isNull("major")){
                JSONObject major = module_dynamic.getJSONObject("major");

                OpusParagraph[] paragraphs = new OpusParagraph[3];

                // paragraphs的第二位是留给卡片的，这里预先new一个，防止崩溃
                paragraphs[1] = new OpusParagraph();

                if (major.has("opus") && !major.isNull("opus")) {
                    JSONObject dynamic_opus = major.getJSONObject("opus");
                    JSONArray opus_pics = dynamic_opus.getJSONArray("pics");

                    // 为了排版正常，这里必须把列表完整传递给OpusParagraph，让OpusParagraph那边解析
                    // 这么干主要是为了适配这神秘的代码结构，我研究OpusParagraph的使用方法旧研究了半天
                    // by Moye

                    JSONObject object = new JSONObject();
                    object.put("para_type", OpusParagraph.TYPE_OPUS);
                    object.put("data", dynamic_opus.getJSONObject("summary").getJSONArray("rich_text_nodes"));
                    paragraphs[0] = new OpusParagraph(object);

                    object = new JSONObject();
                    object.put("para_type", OpusParagraph.TYPE_PIC);
                    object.put("pic", new JSONObject().put("pics", opus_pics));
                    paragraphs[2] = new OpusParagraph(object);

                    opus.paragraphs = paragraphs;
                }

                if (major.has("archive") && !major.isNull("archive")){
                    // 这里是视频卡片
                }
            }

            JSONObject module_stat = modules.getJSONObject("module_stat");
            Stats stats = new Stats();
            stats.reply = module_stat.getJSONObject("comment").getInt("count");
            stats.like = module_stat.getJSONObject("like").getInt("count");

            opus.stats = stats;
        }
        // B站是会做图文的

        opus.cover = "";
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
